package io.opentracing.testbed.netty_v4.impl;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;

/**
 * @Author: yaozh
 * @Description:
 */
public class NettyScopeManager implements ScopeManager {
    private final NettyTracerContext nettyTracerContext;

    public NettyScopeManager(NettyTracerContext nettyTracerContext) {
        this.nettyTracerContext=nettyTracerContext;
    }

    @Override
    public Scope activate(Span span) {
        throw new UnsupportedOperationException("This class should be used only with NettyScopeManager.activate(Span span)!");
    }

    @Override
    public Span activeSpan() {
        return nettyTracerContext.getGatewaySpan();
    }
}
