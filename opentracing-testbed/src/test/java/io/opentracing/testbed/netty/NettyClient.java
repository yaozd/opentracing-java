package io.opentracing.testbed.netty;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.testbed.AutoFinishScope;
import io.opentracing.testbed.AutoFinishScopeManager;
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
    private final Tracer tracer;

    public NettyClient(Tracer tracer) {
        this.tracer = tracer;
    }

    public Future<Object> send(final Object message, final long milliseconds) {
        final NettyScope.Continuation cont = ((NettyScopeManager)tracer.scopeManager()).captureScope();

        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                logger.info("Child thread with message '{}' started", message);

                try (Scope parentScope = cont.activate()) {

                    Span span = tracer.buildSpan("subtask").start();
                    try (Scope subtaskScope = tracer.activateSpan(span)) {
                        // Simulate work.
                        sleep(milliseconds);
                    }
                }

                logger.info("Child thread with message '{}' finished", message);
                return message + "::response";
            }
        });
    }
}
