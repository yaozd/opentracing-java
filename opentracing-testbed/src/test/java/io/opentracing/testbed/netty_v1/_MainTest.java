package io.opentracing.testbed.netty_v1;

import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.opentracing.testbed.TestUtils.finishedSpansSize;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;

public class _MainTest {
    @Test
    public void test() throws Exception {
        NettyChannel nettyChannel=new NettyChannel();
        NettyClient client = new NettyClient(nettyChannel);
        Span span = nettyChannel.getGatewaySpan();
        //try (Scope scope = tracer.activateSpan(span)) {
        client.send(span,"task1", 300);
        client.send(span,"task2", 200);
        //client.send(span,"task3", 100);
        //}
        span.finish();
        await().atMost(5, TimeUnit.SECONDS).until(finishedSpansSize(nettyChannel.getTracer()), equalTo(2));

        List<MockSpan> spans = nettyChannel.getTracer().finishedSpans();
        assertEquals(2, spans.size());
        for (MockSpan mockSpan : spans) {
            System.out.println(mockSpan.toString());
        }
//        assertEquals("parent", spans.get(3).operationName());
//
//        MockSpan parentSpan = spans.get(3);
//        for (int i = 0; i < 3; i++) {
//            assertEquals(true, parentSpan.finishMicros() >= spans.get(i).finishMicros());
//            assertEquals(parentSpan.context().traceId(), spans.get(i).context().traceId());
//            assertEquals(parentSpan.context().spanId(), spans.get(i).parentId());
//        }
//
//        assertNull(tracer.scopeManager().activeSpan());
    }
}
