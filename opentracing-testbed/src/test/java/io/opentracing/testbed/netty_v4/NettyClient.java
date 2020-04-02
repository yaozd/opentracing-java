package io.opentracing.testbed.netty_v4;

import io.opentracing.Span;
import io.opentracing.testbed.netty_v4.impl.NettyTracerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.opentracing.testbed.TestUtils.sleep;

/**
 * @Author: yaozh
 * @Description:
 */
public class NettyClient {
    private static final Logger logger = LoggerFactory.getLogger(io.opentracing.testbed.netty_v1.NettyClient.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final NettyTracerContext nettyTracerContext;

    public NettyClient(NettyTracerContext nettyChannel) {
        this.nettyTracerContext = nettyChannel;
    }

    public Future<Object> send(final Object message, final long milliseconds) {

        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                logger.info("Child thread with message '{}' started", message);
                for (int i = 0; i < 100; i++) {
                    Span span = nettyTracerContext.getTargetSpan();
                    System.out.println("span id:" + span.context().toSpanId());
                    // Simulate work.
                    sleep(milliseconds);
                    span.finish();
                }
                logger.info("Child thread with message '{}' finished", message);
                return message + "::response";
            }
        });
    }
}
