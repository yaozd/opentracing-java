package io.opentracing.testbed.netty_v2;

import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.testbed.netty_v2.impl.NettyExtractAdapter;
import io.opentracing.testbed.netty_v2.impl.NettySpan;
import io.opentracing.testbed.netty_v2.impl.NettyTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.Test;

import java.util.List;

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
    public void extractTest() {
        SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS, new NettyExtractAdapter());
        NettySpan parent = tracer.buildSpan("parent").asChildOf(extractedContext).start();
        NettySpan child = tracer.buildSpan("child").asChildOf(parent).start();
        child.finish();
        parent.finish();
        List<NettySpan> spans = tracer.finishedSpans();
        assertEquals(2, spans.size());
        for (NettySpan span : spans) {
            System.out.println(span.toString());
        }
    }
}
