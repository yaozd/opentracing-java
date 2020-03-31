package io.opentracing.testbed.netty;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockTracer.Propagator;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.opentracing.testbed.TestUtils.finishedSpansSize;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.core.IsEqual.equalTo;

public class _MainTest {
    private static final Logger logger = LoggerFactory.getLogger(_MainTest.class);
    private final MockTracer tracer = new MockTracer(new NettyScopeManager(),
            Propagator.TEXT_MAP);
    @Test
    public void test() throws Exception {
        NettyClient client = new NettyClient(tracer);
        Span span = tracer.buildSpan("parent").start();
        try (Scope scope = tracer.activateSpan(span)) {
            client.send("task1", 300);
            client.send("task2", 200);
            client.send("task3", 100);
        }

        await().atMost(15, TimeUnit.SECONDS).until(finishedSpansSize(tracer), equalTo(4));

        List<MockSpan> spans = tracer.finishedSpans();
        assertEquals(4, spans.size());
        assertEquals("parent", spans.get(3).operationName());

        MockSpan parentSpan = spans.get(3);
        for (int i = 0; i < 3; i++) {
            assertEquals(true, parentSpan.finishMicros() >= spans.get(i).finishMicros());
            assertEquals(parentSpan.context().traceId(), spans.get(i).context().traceId());
            assertEquals(parentSpan.context().spanId(), spans.get(i).parentId());
        }

        assertNull(tracer.scopeManager().activeSpan());
    }
}
