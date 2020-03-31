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

import io.opentracing.Scope;
import io.opentracing.Span;


import java.util.concurrent.atomic.AtomicInteger;

/**
 * The operation mode of this class contrasts with the 0.32
 * deprecation of auto finishing {@link Span}s upon {@link Scope#close()}.

 * {@link NettyScope} is a {@link Scope} implementation that uses ref-counting
 * to automatically finish the wrapped {@link Span}.
 *
 */
public class NettyScope implements Scope {
    final NettyScopeManager manager;
    final AtomicInteger refCount;
    private final Span wrapped;
    //private final NettyScope toRestore;

    NettyScope(NettyScopeManager manager, AtomicInteger refCount, Span wrapped) {
        System.out.println("create scope");
        this.manager = manager;
        this.refCount = refCount;
        this.wrapped = wrapped;
        //this.toRestore = manager.tlsScope.get();
    }

    public class Continuation {
        public Continuation() {
            refCount.incrementAndGet();
        }

        public NettyScope activate() {
            return new NettyScope(manager, refCount, wrapped);
        }
    }

    public Continuation capture() {
        return new Continuation();
    }

    @Override
    public void close() {
//        if (manager.tlsScope.get() != this) {
//            return;
//        }

        if (refCount.decrementAndGet() == 0) {
            wrapped.finish();
        }

//        manager.tlsScope.set(toRestore);
    }

    public Span span() {
        return wrapped;
    }
}
