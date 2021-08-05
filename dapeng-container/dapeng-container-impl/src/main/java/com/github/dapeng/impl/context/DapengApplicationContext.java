package com.github.dapeng.impl.context;

import com.github.dapeng.core.ApplicationContext;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 从Spring ApplicationContext 获取
 * ApplicationContext 从外部传进来
 * @author zhongwm
 */
public class DapengApplicationContext implements ApplicationContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(DapengApplicationContext.class);

    private org.springframework.context.ApplicationContext applicationContext;

    public DapengApplicationContext(org.springframework.context.ApplicationContext applicationContext) {
        LOGGER.info("Init DapengApplicationContext...");
        this.applicationContext = applicationContext;
    }

    @Override
    public Map<String, SoaServiceDefinition> getServiceDefinitions() {
        return applicationContext.getBeansOfType(SoaServiceDefinition.class);
    }

}
