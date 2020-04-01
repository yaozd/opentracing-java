package io.opentracing.testbed.netty_v3;

import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.testbed.netty_v3.impl.*;
import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.opentracing.testbed.TestUtils.finishedSpansSize;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;

public class _MainTest {
    private final NettyTracer tracer = new NettyTracer(new ThreadLocalScopeManager(),
            NettyTracer.Propagator.TEXT_MAP);

    @Test
    public void test() {
        NettySpan parent = tracer.buildSpan("parent").start();
        NettySpan child = tracer.buildSpan("child").asChildOf(parent).start();
        child.finish();
        parent.finish();
        List<NettySpan> spans = tracer.finishedSpans();
        assertEquals(2, spans.size());
        for (NettySpan span : spans) {
            System.out.println(span.toString());
        }
    }

    @Test
    public void extractAndInjectTest() {
        //extract
        SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS, new NettyExtractAdapter());
        NettySpan parent = tracer.buildSpan("parent").asChildOf(extractedContext).start();
        //inject
        tracer.inject(parent.context(), Format.Builtin.HTTP_HEADERS, new NettyInjectAdapter());
        //
        NettySpan child = tracer.buildSpan("child").asChildOf(parent).start();
        child.setTag(NettyTags.HTTP_URL.getKey(), "www.google.com");
        child.finish();
        parent.finish();
        List<NettySpan> spans = tracer.finishedSpans();
        assertEquals(2, spans.size());
        for (NettySpan span : spans) {
            System.out.println(span.toString());
        }
    }

    @Test
    public void nettyClientTest() {
        NettyTracerContext nettyTracerContext = new NettyTracerContext();
        NettyClient client = new NettyClient(nettyTracerContext);
        client.send("task1", 300);
        //client.send("task2", 200);
        //client.send("task3", 1000);
        nettyTracerContext.getGatewaySpan().finish();
        await().atMost(5, TimeUnit.SECONDS).until(finishedSpansSize(nettyTracerContext.getTracer()), equalTo(2));
        List<NettySpan> spans = nettyTracerContext.getTracer().finishedSpans();
        assertEquals(2, spans.size());
        for (NettySpan span : spans) {
            System.out.println(span.toString());
        }
    }
}
