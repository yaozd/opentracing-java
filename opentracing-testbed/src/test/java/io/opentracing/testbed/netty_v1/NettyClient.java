package io.opentracing.testbed.netty_v1;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.opentracing.testbed.TestUtils.sleep;

public class NettyClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final NettyChannel nettyChannel;

    public NettyClient(NettyChannel nettyChannel) {
        this.nettyChannel = nettyChannel;
    }

    public Future<Object> send(final Span parentSpan,final Object message, final long milliseconds) {

        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                logger.info("Child thread with message '{}' started", message);

                //try (Scope parentScope = cont.activate()) {
                for (int i = 0; i < 100; i++) {


                    Span span = nettyChannel.getTargetSpan();
                    System.out.println("span id:"+span.context().toSpanId());
                    //try (Scope subtaskScope = tracer.activateSpan(span)) {
                        // Simulate work.
                        sleep(milliseconds);
                    //}
                    span.finish();
            }
                //}
                logger.info("Child thread with message '{}' finished", message);
                return message + "::response";
            }
        });
    }
}
