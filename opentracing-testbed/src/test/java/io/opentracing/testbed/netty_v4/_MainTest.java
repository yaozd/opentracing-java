package io.opentracing.testbed.netty_v4;

import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.testbed.netty_v4.impl.*;
import io.opentracing.util.ThreadLocalScopeManager;
import org.apache.lucene.util.RamUsageEstimator;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
        child.setBaggageItem("sample", "1");
        child.setBaggageItem("sample", "2");
        child.finish();
        parent.finish();
        List<NettySpan> spans = tracer.finishedSpans();
        assertEquals(2, spans.size());
        for (NettySpan span : spans) {
            System.out.println(span.toString());
        }
        System.err.println("tracer humanSizeOf:" + RamUsageEstimator.sizeOf(tracer));
        System.err.println("tracer humanSizeOf:" + RamUsageEstimator.humanReadableUnits(RamUsageEstimator.sizeOf(tracer)));
        System.err.println("tracer humanSizeOf:" + RamUsageEstimator.humanReadableUnits(RamUsageEstimator.sizeOf(tracer) * 500000));
        //
        System.err.println("tracer humanSizeOf:" + RamUsageEstimator.humanSizeOf(tracer));
        System.err.println("span humanSizeOf:" + RamUsageEstimator.humanSizeOf(parent));
        System.err.println("span.context humanSizeOf:" + RamUsageEstimator.humanSizeOf(parent.context()));
        System.err.println("span.context.baggageItems humanSizeOf:" + RamUsageEstimator.humanSizeOf(parent.context().baggageItems()));
        System.err.println("span.context.traceId humanSizeOf:" + RamUsageEstimator.humanSizeOf(parent.context().traceId()));
        System.err.println("span.context.spanId humanSizeOf:" + RamUsageEstimator.humanSizeOf(parent.context().spanId()));
        System.err.println("span.tags humanSizeOf:" + RamUsageEstimator.humanSizeOf(parent.tags()));
        System.err.println("span.logEntries humanSizeOf:" + RamUsageEstimator.humanSizeOf(parent.logEntries()));
        System.err.println("span.references humanSizeOf:" + RamUsageEstimator.humanSizeOf(parent.references()));
        //
        System.err.println("span shallowSizeOf:" + RamUsageEstimator.shallowSizeOf(parent));
        //
        System.err.println("==========================NettyTracer Size======================================");
        System.err.println(FieldSizeUtil.getString(tracer, NettyTracer.class));
        //
        System.err.println("==========================NettySpan Size======================================");
        System.err.println(FieldSizeUtil.getString(parent, NettySpan.class));
        //
        System.err.println("==========================NettySpan context Size======================================");
        System.err.println(FieldSizeUtil.getString(parent.context(), NettySpan.NettyContext.class));
    }

    @Test
    public void extractAndInjectAndBaggageTest() {
        //extract
        SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS, new NettyExtractAdapter());
        NettySpan parent = tracer.buildSpan("parent").asChildOf(extractedContext).start();
        parent.setBaggageItem("baggage", "xxxxxxxxx");
        parent.setBaggageItem("sampler", "sampler-value");
        parent.setBaggageItem("percentage", "percentage-value");
        //inject
        tracer.inject(parent.context(), Format.Builtin.HTTP_HEADERS, new NettyInjectAdapter());
        //
        System.out.println("===================child span===================");
        NettySpan child = tracer.buildSpan("child").asChildOf(parent).start();
        tracer.inject(child.context(), Format.Builtin.HTTP_HEADERS, new NettyInjectAdapter());
        //
        child.setTag(NettyTags.HTTP_URL.getKey(), "www.google.com");
        child.finish();
        parent.finish();
        List<NettySpan> spans = tracer.finishedSpans();
        assertEquals(2, spans.size());
        for (NettySpan span : spans) {
            System.out.println(span.toString());
        }
        System.err.println("shallowSizeOf:" + RamUsageEstimator.shallowSizeOf(tracer));
        System.err.println("span humanSizeOf:" + RamUsageEstimator.humanSizeOf(parent));
        System.err.println("tracer humanSizeOf:" + RamUsageEstimator.humanSizeOf(tracer));
    }

    @Test
    public void nettyClientTest() {
        NettyTracerContext nettyTracerContext = new NettyTracerContext();
        NettyClient client = new NettyClient(nettyTracerContext);
        client.send("task1", 300);
        //client.send("task2", 200);
        //client.send("task3", 1000);
        nettyTracerContext.getGatewaySpan().finish();
        await().atMost(5, TimeUnit.SECONDS).until(() -> nettyTracerContext.getTracer().finishedSpans().size(), equalTo(2));
        List<NettySpan> spans = nettyTracerContext.getTracer().finishedSpans();
        assertEquals(2, spans.size());
        for (NettySpan span : spans) {
            System.out.println(span.toString());
        }
        System.err.println("nettyTracerContext humanSizeOf:" + RamUsageEstimator.humanSizeOf(nettyTracerContext));
    }
}
