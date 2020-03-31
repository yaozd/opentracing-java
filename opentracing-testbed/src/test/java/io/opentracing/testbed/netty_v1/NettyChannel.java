package io.opentracing.testbed.netty_v1;

import io.opentracing.Span;
import io.opentracing.mock.MockTracer;


public class NettyChannel {
    private final MockTracer tracer;
    private Span gatewaySpan;
    private Span targetSpan;
    private SpanType spanType;

    public NettyChannel() {
        this.spanType = SpanType.GATEWAY;
        this.tracer = new MockTracer(new NettyScopeManager(this), MockTracer.Propagator.TEXT_MAP);
        this.gatewaySpan = getTracer().buildSpan("parent").start();
        //this.setTargetSpan(tracer.buildSpan("target").asChildOf(getGatewaySpan()).start());
    }


    public Span getGatewaySpan() {
        return gatewaySpan;
    }

    public void switchSpanType(SpanType spanType) {
        this.spanType = spanType;
    }

    public synchronized Span getTargetSpan() {
        if (targetSpan == null) {
            this.targetSpan = tracer.buildSpan("target").asChildOf(getGatewaySpan()).start();
        }
        return targetSpan;
    }

    public void setTargetSpan(Span targetSpan) {
        this.targetSpan = targetSpan;
    }

    public Span getSpan() {
        if (SpanType.TARGET.equals(spanType)) {
            return targetSpan;
        }
        return gatewaySpan;
    }

    public MockTracer getTracer() {
        return tracer;
    }

    public enum SpanType {
        GATEWAY, TARGET;
    }
}
