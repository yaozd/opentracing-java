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
package io.opentracing.testbed.netty_v2.impl;

import io.opentracing.*;
import io.opentracing.propagation.*;
import io.opentracing.tag.Tag;
import io.opentracing.util.ThreadLocalScopeManager;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * NettyTracer makes it easy to test the semantics of OpenTracing instrumentation.
 * <p>
 * By using a NettyTracer as an io.opentracing.Tracer implementation for unittests, a developer can assert that Span
 * properties and relationships with other Spans are defined as expected by instrumentation code.
 * <p>
 * The NettyTracerTest has simple usage examples.
 */
public class NettyTracer implements Tracer {
    private final List<NettySpan> finishedSpans = new ArrayList<>();
    private final Propagator propagator;
    private final ScopeManager scopeManager;
    private boolean isClosed;

    public NettyTracer() {
        this(new ThreadLocalScopeManager(), Propagator.TEXT_MAP);
    }

    public NettyTracer(ScopeManager scopeManager) {
        this(scopeManager, Propagator.TEXT_MAP);
    }

    public NettyTracer(ScopeManager scopeManager, Propagator propagator) {
        this.scopeManager = scopeManager;
        this.propagator = propagator;
    }

    /**
     * Create a new NettyTracer that passes through any calls to inject() and/or extract().
     */
    public NettyTracer(Propagator propagator) {
        this(new ThreadLocalScopeManager(), propagator);
    }

    /**
     * Clear the finishedSpans() queue.
     * <p>
     * Note that this does *not* have any effect on Spans created by NettyTracer that have not finish()ed yet; those
     * will still be enqueued in finishedSpans() when they finish().
     */
    public synchronized void reset() {
        this.finishedSpans.clear();
    }

    /**
     * @return a copy of all finish()ed NettySpans started by this NettyTracer (since construction or the last call to
     * NettyTracer.reset()).
     * @see NettyTracer#reset()
     */
    public synchronized List<NettySpan> finishedSpans() {
        return new ArrayList<>(this.finishedSpans);
    }

    /**
     * @return all finish()ed Traces(Spans) started by this NettyTracer grouped by traceId and spanId in HashMap format.
     */
    public synchronized Map<String, Map<String, NettySpan>> finishedTraces() {
        Map<String, Map<String, NettySpan>> result = new LinkedHashMap<>();

        for (NettySpan span : this.finishedSpans) {
            String traceId = span.context().toTraceId();

            Map<String, NettySpan> spanId2Span = result.get(traceId);
            if (null == spanId2Span) {
                spanId2Span = new LinkedHashMap<>();
                result.put(traceId, spanId2Span);
            }

            String spanId = span.context().toSpanId();
            spanId2Span.put(spanId, span);
        }

        return result;
    }

    /**
     * Noop method called on {@link Span#finish()}.
     */
    protected void onSpanFinished(NettySpan nettySpan) {
    }

    /**
     * Propagator allows the developer to intercept and verify any calls to inject() and/or extract().
     * <p>
     * By default, NettyTracer uses Propagator.PRINTER which simply logs such calls to System.out.
     *
     * @see NettyTracer#(Propagator)
     */
    public interface Propagator {
        <C> void inject(NettySpan.NettyContext ctx, Format<C> format, C carrier);

        <C> NettySpan.NettyContext extract(Format<C> format, C carrier);

        Propagator PRINTER = new Propagator() {
            @Override
            public <C> void inject(NettySpan.NettyContext ctx, Format<C> format, C carrier) {
                System.out.println("inject(" + ctx + ", " + format + ", " + carrier + ")");
            }

            @Override
            public <C> NettySpan.NettyContext extract(Format<C> format, C carrier) {
                System.out.println("extract(" + format + ", " + carrier + ")");
                return null;
            }
        };

        Propagator BINARY = new Propagator() {
            static final int BUFFER_SIZE = 128;

            @Override
            public <C> void inject(NettySpan.NettyContext ctx, Format<C> format, C carrier) {
                if (!(carrier instanceof BinaryInject)) {
                    throw new IllegalArgumentException("Expected BinaryInject, received " + carrier.getClass());
                }

                BinaryInject binary = (BinaryInject) carrier;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ObjectOutputStream objStream = null;
                try {
                    objStream = new ObjectOutputStream(stream);
                    objStream.writeUTF(ctx.spanId());
                    objStream.writeUTF(ctx.traceId());

                    for (Map.Entry<String, String> entry : ctx.baggageItems()) {
                        objStream.writeUTF(entry.getKey());
                        objStream.writeUTF(entry.getValue());
                    }
                    objStream.flush(); // *need* to flush ObjectOutputStream.

                    byte[] buff = stream.toByteArray();
                    binary.injectionBuffer(buff.length).put(buff);

                } catch (IOException e) {
                    throw new RuntimeException("Corrupted state", e);
                } finally {
                    if (objStream != null) {
                        try {
                            objStream.close();
                        } catch (Exception e2) {
                        }
                    }
                }
            }

            @Override
            public <C> NettySpan.NettyContext extract(Format<C> format, C carrier) {
                if (!(carrier instanceof BinaryExtract)) {
                    throw new IllegalArgumentException("Expected BinaryExtract, received " + carrier.getClass());
                }

                String traceId = null;
                String spanId = null;
                Map<String, String> baggage = new HashMap<>();

                BinaryExtract binary = (BinaryExtract) carrier;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ObjectInputStream objStream = null;
                try {
                    ByteBuffer extractBuff = binary.extractionBuffer();
                    byte[] buff = new byte[extractBuff.remaining()];
                    extractBuff.get(buff);

                    objStream = new ObjectInputStream(new ByteArrayInputStream(buff));
                    spanId = objStream.readUTF();
                    traceId = objStream.readUTF();

                    while (objStream.available() > 0) {
                        baggage.put(objStream.readUTF(), objStream.readUTF());
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Corrupted state", e);
                } finally {
                    if (objStream != null) {
                        try {
                            objStream.close();
                        } catch (Exception e2) {
                        }
                    }
                }

                if (traceId != null && spanId != null) {
                    return new NettySpan.NettyContext(traceId, spanId, baggage);
                }

                return null;
            }
        };

        Propagator TEXT_MAP = new Propagator() {
            public static final String SPAN_ID_KEY = "spanid";
            public static final String TRACE_ID_KEY = "traceid";
            public static final String BAGGAGE_KEY_PREFIX = "baggage-";

            @Override
            public <C> void inject(NettySpan.NettyContext ctx, Format<C> format, C carrier) {
                if (carrier instanceof TextMapInject) {
                    TextMapInject textMap = (TextMapInject) carrier;
                    for (Map.Entry<String, String> entry : ctx.baggageItems()) {
                        textMap.put(BAGGAGE_KEY_PREFIX + entry.getKey(), entry.getValue());
                    }
                    textMap.put(SPAN_ID_KEY, String.valueOf(ctx.spanId()));
                    textMap.put(TRACE_ID_KEY, String.valueOf(ctx.traceId()));
                } else {
                    throw new IllegalArgumentException("Unknown carrier");
                }
            }

            @Override
            public <C> NettySpan.NettyContext extract(Format<C> format, C carrier) {
                String traceId = null;
                String spanId = null;
                Map<String, String> baggage = new HashMap<>();

                if (carrier instanceof TextMapExtract) {
                    TextMapExtract textMap = (TextMapExtract) carrier;
                    for (Map.Entry<String, String> entry : textMap) {
                        if (TRACE_ID_KEY.equals(entry.getKey())) {
                            traceId = entry.getValue();
                        } else if (SPAN_ID_KEY.equals(entry.getKey())) {
                            spanId = entry.getValue();
                        } else if (entry.getKey().startsWith(BAGGAGE_KEY_PREFIX)) {
                            String key = entry.getKey().substring((BAGGAGE_KEY_PREFIX.length()));
                            baggage.put(key, entry.getValue());
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Unknown carrier");
                }

                if (traceId != null && spanId != null) {
                    return new NettySpan.NettyContext(traceId, spanId, baggage);
                }

                return null;
            }
        };
    }

    @Override
    public ScopeManager scopeManager() {
        return this.scopeManager;
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return new SpanBuilder(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        this.propagator.inject((NettySpan.NettyContext) spanContext, format, carrier);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        return this.propagator.extract(format, carrier);
    }

    @Override
    public Span activeSpan() {
        return this.scopeManager.activeSpan();
    }

    @Override
    public Scope activateSpan(Span span) {
        return this.scopeManager.activate(span);
    }

    @Override
    public synchronized void close() {
        this.isClosed = true;
        this.finishedSpans.clear();
    }

    synchronized void appendFinishedSpan(NettySpan nettySpan) {
        if (isClosed)
            return;

        this.finishedSpans.add(nettySpan);
        this.onSpanFinished(nettySpan);
    }

    private SpanContext activeSpanContext() {
        Span span = activeSpan();
        if (span == null) {
            return null;
        }

        return span.context();
    }

    public final class SpanBuilder implements Tracer.SpanBuilder {
        private final String operationName;
        private long startMicros;
        private List<NettySpan.Reference> references = new ArrayList<>();
        private boolean ignoringActiveSpan;
        private Map<String, Object> initialTags = new HashMap<>();

        SpanBuilder(String operationName) {
            this.operationName = operationName;
        }

        @Override
        public SpanBuilder asChildOf(SpanContext parent) {
            return addReference(References.CHILD_OF, parent);
        }

        @Override
        public SpanBuilder asChildOf(Span parent) {
            if (parent == null) {
                return this;
            }
            return addReference(References.CHILD_OF, parent.context());
        }

        @Override
        public SpanBuilder ignoreActiveSpan() {
            ignoringActiveSpan = true;
            return this;
        }

        @Override
        public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
            if (referencedContext != null) {
                this.references.add(new NettySpan.Reference((NettySpan.NettyContext) referencedContext, referenceType));
            }
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, String value) {
            this.initialTags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, boolean value) {
            this.initialTags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, Number value) {
            this.initialTags.put(key, value);
            return this;
        }

        @Override
        public <T> Tracer.SpanBuilder withTag(Tag<T> tag, T value) {
            this.initialTags.put(tag.getKey(), value);
            return this;
        }

        @Override
        public SpanBuilder withStartTimestamp(long microseconds) {
            this.startMicros = microseconds;
            return this;
        }

        @Override
        public NettySpan start() {
            if (this.startMicros == 0) {
                this.startMicros = NettySpan.nowMicros();
            }
            SpanContext activeSpanContext = activeSpanContext();
            if (references.isEmpty() && !ignoringActiveSpan && activeSpanContext != null) {
                references.add(new NettySpan.Reference((NettySpan.NettyContext) activeSpanContext, References.CHILD_OF));
            }
            return new NettySpan(NettyTracer.this, operationName, startMicros, initialTags, references);
        }
    }
}
