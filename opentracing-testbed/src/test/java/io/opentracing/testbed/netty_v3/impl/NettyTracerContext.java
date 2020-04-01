package io.opentracing.testbed.netty_v3.impl;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;


/**
 * @Author: yaozh
 * @Description:
 */
public class NettyTracerContext {
    private final NettyTracer tracer;
    private Span gatewaySpan;
    private Span targetSpan;

    public NettyTracerContext() {
        this.tracer = new NettyTracer(new NettyScopeManager(this), NettyTracer.Propagator.TEXT_MAP);
        SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS, new NettyExtractAdapter());
        this.gatewaySpan = this.tracer.buildSpan("parent").asChildOf(extractedContext).start();
    }

    public NettyTracer getTracer() {
        return tracer;
    }
    public Span getGatewaySpan() {
        return gatewaySpan;
    }

    public synchronized Span getTargetSpan() {
        if (targetSpan == null) {
            this.targetSpan = this.tracer.buildSpan("target").asChildOf(this.gatewaySpan).start();
        }
        return targetSpan;
    }
}
