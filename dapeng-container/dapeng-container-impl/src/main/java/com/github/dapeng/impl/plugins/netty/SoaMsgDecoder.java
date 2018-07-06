package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocolException;
import com.github.dapeng.util.DumpUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import static com.github.dapeng.util.ExceptionUtil.convertToSoaException;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

/**
 * @author ever
 */
@ChannelHandler.Sharable
public class SoaMsgDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaMsgDecoder.class);

    private final Container container;

    SoaMsgDecoder(Container container) {
        this.container = container;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List out) throws Exception {
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(getClass().getSimpleName() + "::decode");
            }

            Object request = parseSoaMsg(msg);

            final TransactionContext transactionContext = TransactionContext.Factory.currentInstance();
            String methodName = transactionContext.getHeader().getMethodName();

            //TODO 将容器线程池信息 transactionContext 进行共享  echo实现方法时可以直接从 transactionContext中拿到 【数据库连接池信息暂时拿不到】
            if (methodName.equalsIgnoreCase("echo")) {
                transactionContext.setAttribute("container-threadPool-info", DumpUtil.dumpThreadPool((ThreadPoolExecutor) container.getDispatcher()));
            }
            /**
             * use AttributeMap to share common data on different  ChannelHandlers
             */
            transactionContext.setAttribute("dapeng_request_timestamp", System.currentTimeMillis());

            out.add(request);
        } catch (Throwable e) {

            SoaException soaException = convertToSoaException(e);
            TransactionContext transactionContext = TransactionContext.Factory.currentInstance();

            SoaHeader soaHeader = transactionContext.getHeader();
            if (soaHeader == null) {
                //todo
            }
            soaHeader.setRespCode(soaException.getCode());
            soaHeader.setRespMessage(soaException.getMessage());

            transactionContext.soaException(soaException);
            SoaResponseWrapper responseWrapper = new SoaResponseWrapper(transactionContext,
                    Optional.ofNullable(null),
                    Optional.ofNullable(null));

            TransactionContext.Factory.removeCurrentInstance();

            ctx.writeAndFlush(responseWrapper).addListener(FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    private <I, REQ, RESP> REQ parseSoaMsg(ByteBuf msg) throws TException {
        TSoaTransport inputSoaTransport = new TSoaTransport(msg);
        SoaMessageProcessor parser = new SoaMessageProcessor(inputSoaTransport);

        final TransactionContext context = TransactionContext.Factory.createNewInstance();

        // parser.service, version, method, header, bodyProtocol
        SoaHeader soaHeader = parser.parseSoaMessage(context);
        ((TransactionContextImpl)context).setHeader(soaHeader);

        updateTransactionCtx((TransactionContextImpl)context, soaHeader);

        MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, context.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));

        Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

        if (application == null) {
            throw new SoaException(SoaCode.NoMatchedService);
        }

        SoaServiceDefinition processor = container.getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

        SoaFunctionDefinition<I, REQ, RESP> soaFunction = (SoaFunctionDefinition<I, REQ, RESP>) processor.functions.get(soaHeader.getMethodName());

        if (soaFunction == null) {
            throw new SoaException(SoaCode.NoMatchedMethod);
        }

        TProtocol contentProtocol = parser.getContentProtocol();
        REQ args;
        try {
            args = soaFunction.reqSerializer.read(contentProtocol);
        } catch (SoaException e) {
            if (e.getCode().equals(SoaCode.StructFieldNull.getCode())) {
                e.setCode(SoaCode.ServerReqFieldNull.getCode());
                e.setMsg(SoaCode.ServerReqFieldNull.getMsg());
            }
            //反序列化出错
            LOGGER.error(DumpUtil.dumpToStr(msg));
            throw e;
        } catch (TException | OutOfMemoryError e) {
            //反序列化出错
            LOGGER.error(DumpUtil.dumpToStr(msg));
            throw e;
        }
        contentProtocol.readMessageEnd();

        if (LOGGER.isDebugEnabled()) {
            String debugLog = "request[seqId:" + context.seqId() + "]:"
                    + "service[" + soaHeader.getServiceName()
                    + "]:version[" + soaHeader.getVersionName()
                    + "]:method[" + soaHeader.getMethodName() + "]"
                    + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "") + " "
                    + (soaHeader.getUserId().isPresent() ? " userId:" + soaHeader.getUserId().get() : "") + " "
                    + (soaHeader.getUserIp().isPresent() ? " userIp:" + soaHeader.getUserIp().get() : "");
            LOGGER.debug(getClass().getSimpleName() + "::decode " + debugLog + ", payload:\n" + args);
        }
        return args;
    }

    private void updateTransactionCtx(TransactionContextImpl ctx, SoaHeader soaHeader) {
        if (soaHeader.getCallerMid().isPresent()) {
            ctx.callerMid(soaHeader.getCallerMid().get());
        }
        ctx.callerIp(soaHeader.getCallerIp().orElse(null));
        if (soaHeader.getUserId().isPresent()) {
            ctx.userId(soaHeader.getUserId().get());
        }
        if (soaHeader.getCallerPort().isPresent()) {
            ctx.callerPort(soaHeader.getCallerPort().get());
        }
        if (soaHeader.getOperatorId().isPresent()) {
            ctx.operatorId(soaHeader.getOperatorId().get());
        }
        if (soaHeader.getCallerTid().isPresent()) {
            ctx.callerTid(soaHeader.getCallerTid().get());
        }
        if (soaHeader.getTimeout().isPresent()) {
            ctx.timeout(soaHeader.getTimeout().get());
        }

        ctx.calleeTid(DapengUtil.generateTid());
        ctx.sessionTid(soaHeader.getSessionTid().orElse(ctx.calleeTid()));
    }
}
