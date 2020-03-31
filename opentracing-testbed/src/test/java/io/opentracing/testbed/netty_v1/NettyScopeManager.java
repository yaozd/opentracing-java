/*
 * Copyright 2016-2020 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.testbed.netty_v1;

import io.opentracing.ScopeManager;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The operation mode of this class contrasts with the 0.32
 * deprecation of auto finishing {@link Span}s upon {@link Scope#close()}.
 * See https://github.com/opentracing/opentracing-java/issues/291
 * <p>
 * A {@link ScopeManager} implementation that uses ref-counting to automatically finish {@link Span}s.
 *
 */
public class NettyScopeManager implements ScopeManager {
    private NettyChannel nettyChannel;
    public NettyScopeManager(NettyChannel nettyChannel) {
        this.nettyChannel=nettyChannel;
    }

    @Override
    public NettyScope activate(Span span) {
        System.out.println("activate(Span span)");
        return new NettyScope(this, new AtomicInteger(1), span);
    }

    @Override
    public Span activeSpan() {
        //读取parent span 的信息
        //return nettyChannel.getSpan();
        return nettyChannel.getGatewaySpan();

    }
}
