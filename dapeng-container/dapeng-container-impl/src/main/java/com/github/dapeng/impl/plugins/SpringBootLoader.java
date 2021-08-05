package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.lifecycle.LifecycleProcessorFactory;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.lifecycle.LifeCycleAware;
import com.github.dapeng.core.lifecycle.LifeCycleEvent;
import com.github.dapeng.impl.container.DapengApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SpringBoot应用启动，单实例应用，不支持多实例
 * @author zhongwm
 */
public class SpringBootLoader implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAppLoader.class);
    private final Container container;
    private final ApplicationContext applicationContext;

    public SpringBootLoader(Container container, ApplicationContext applicationContext) {
        this.container = container;
        this.applicationContext = applicationContext;
    }

    @Override
    public void start() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        lauch(applicationContext, classLoader);
        LOGGER.info("application started");
    }

    private void lauch(ApplicationContext appContext, ClassLoader appClassLoader) {
        appContext.onStart(new LifeCycleEvent(LifeCycleEvent.LifeCycleEventEnum.START));

        Map<String, SoaServiceDefinition> definitions = appContext.getServiceDefinitions();

        Map<String, ServiceInfo> serviceInfoMap = toServiceInfos(definitions);

        // LifeCycle Support, only service support Lifecyle now.
        List<LifeCycleAware> collect = definitions.values().stream()
                .filter(definition -> LifeCycleAware.class.isInstance(definition.iface))
                .map(definition -> (LifeCycleAware) (definition.iface))
                .collect(Collectors.toList());

        LifecycleProcessorFactory.getLifecycleProcessor().addLifecycles(collect);

        // Build an Application
        Application application = new DapengApplication(new ArrayList<>(serviceInfoMap.values()), appClassLoader);

        LOGGER.info("start to boot app");

        if (!application.getServiceInfos().isEmpty()) {
            // fixme only registerApplication
            Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap = toSoaServiceDefinitionMap(serviceInfoMap, definitions);
            container.registerAppProcessors(serviceDefinitionMap);

            container.registerAppMap(toApplicationMap(serviceDefinitionMap, application));
            //fire a zk event
            container.registerApplication(application);
        }
    }

    private Map<ProcessorKey, Application> toApplicationMap(Map<ProcessorKey,
            SoaServiceDefinition<?>> serviceDefinitionMap, Application application) {

        Map<ProcessorKey, Application> map = serviceDefinitionMap.keySet().stream().
                collect(Collectors.toMap(Function.identity(), processorKey -> application));
        return map;
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::stop");
        applicationContext.onStop(new LifeCycleEvent(LifeCycleEvent.LifeCycleEventEnum.STOP));
        LOGGER.warn("Plugin:SpringBootLoader stoped..");
    }

    private Map<String, ServiceInfo> toServiceInfos(Map<String, SoaServiceDefinition> processorMap) {

        try {
            Map<String, ServiceInfo> serviceInfoMap = new HashMap<>(processorMap.size());
            for (Map.Entry<String, SoaServiceDefinition> processorEntry : processorMap.entrySet()) {
                String processorKey = processorEntry.getKey();
                SoaServiceDefinition<?> processor = processorEntry.getValue();

                long count = new ArrayList<>(Arrays.asList(processor.iface.getClass().getInterfaces()))
                        .stream()
                        .filter(m -> "org.springframework.aop.framework.Advised".equals(m.getName()))
                        .count();

                Class<?> ifaceClass = (Class) (count > 0 ?
                        processor.iface.getClass().getMethod("getTargetClass").invoke(processor.iface) :
                        processor.iface.getClass());

                Service service = processor.ifaceClass.getAnnotation(Service.class);
                //
                assert (service != null);

                /**
                 * customConfig 封装到 ServiceInfo 中
                 */
                Map<String, Optional<CustomConfigInfo>> methodsConfigMap = new HashMap<>();

                processor.functions.forEach((key, function) -> {
                    methodsConfigMap.put(key, function.getCustomConfigInfo());
                });

                //判断有没有 接口实现的版本号   默认为IDL定义的版本号
                ServiceVersion serviceVersionAnnotation = ifaceClass.isAnnotationPresent(ServiceVersion.class) ? ifaceClass.getAnnotationsByType(ServiceVersion.class)[0] : null;
                String version = serviceVersionAnnotation != null ? serviceVersionAnnotation.version() : service.version();

                //封装方法的慢服务时间
                HashMap<String, Long> methodsMaxProcessTimeMap = new HashMap<>(16);
                Arrays.asList(ifaceClass.getMethods()).forEach(item -> {
                    if (processor.functions.keySet().contains(item.getName())) {
                        long maxProcessTime = SoaSystemEnvProperties.SOA_MAX_PROCESS_TIME;
                        maxProcessTime = item.isAnnotationPresent(MaxProcessTime.class) ? item.getAnnotation(MaxProcessTime.class).maxTime() : maxProcessTime;
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("{}:{}:{} ; maxProcessTime:{} ", service.name(), version, item.getName(), maxProcessTime);
                        }
                        methodsMaxProcessTimeMap.put(item.getName(), maxProcessTime);
                    }
                });

                if (serviceVersionAnnotation == null || serviceVersionAnnotation.isRegister()) {
                    ServiceInfo serviceInfo = new ServiceInfo(service.name(), version, "service", ifaceClass, processor.getConfigInfo(), methodsConfigMap, methodsMaxProcessTimeMap);
                    serviceInfoMap.put(processorKey, serviceInfo);
                }

            }
            return serviceInfoMap;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Reflect service failed", ex);
        }
    }


    private Map<ProcessorKey, SoaServiceDefinition<?>> toSoaServiceDefinitionMap(
            Map<String, ServiceInfo> serviceInfoMap,
            Map<String, SoaServiceDefinition> processorMap) {

        Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitions = serviceInfoMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> new ProcessorKey(entry.getValue().serviceName, entry.getValue().version),
                        entry -> processorMap.get(entry.getKey())));
        return serviceDefinitions;
    }

}
