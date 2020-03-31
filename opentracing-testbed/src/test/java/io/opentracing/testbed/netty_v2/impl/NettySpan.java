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

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.Tag;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MockSpans are created via MockTracer.buildSpan(...), but they are also returned via calls to
 * MockTracer.finishedSpans(). They provide accessors to all Span state.
 *
 * @see NettyTracer#finishedSpans()
 */
public final class NettySpan implements Span {
    // A simple-as-possible (consecutive for repeatability) id generator.
    private static AtomicLong nextId = new AtomicLong(0);

    private final NettyTracer nettyTracer;
    private NettyContext context;
    private final String parentId; // 0 if there's no parent.
    private final long startMicros;
    private boolean finished;
    private long finishMicros;
    private final Map<String, Object> tags;
    private final List<LogEntry> logEntries = new ArrayList<>();
    private String operationName;
    private final List<Reference> references;

    private final List<RuntimeException> errors = new ArrayList<>();

    public String operationName() {
        return this.operationName;
    }

    @Override
    public NettySpan setOperationName(String operationName) {
        finishedCheck("Setting operationName {%s} on already finished span", operationName);
        this.operationName = operationName;
        return this;
    }

    /**
     * @return the spanId of the Span's first {@value References#CHILD_OF} reference, or the first reference of any type, or 0 if no reference exists.
     * @see NettyContext#spanId()
     * @see NettySpan#references()
     */
    public String parentId() {
        return parentId;
    }

    public long startMicros() {
        return startMicros;
    }

    /**
     * @return the finish time of the Span; only valid after a call to finish().
     */
    public long finishMicros() {
        assert finishMicros > 0 : "must call finish() before finishMicros()";
        return finishMicros;
    }

    /**
     * @return a copy of all tags set on this Span.
     */
    public Map<String, Object> tags() {
        return new HashMap<>(this.tags);
    }

    /**
     * @return a copy of all log entries added to this Span.
     */
    public List<LogEntry> logEntries() {
        return new ArrayList<>(this.logEntries);
    }

    /**
     * @return a copy of exceptions thrown by this class (e.g. adding a tag after span is finished).
     */
    public List<RuntimeException> generatedErrors() {
        return new ArrayList<>(errors);
    }

    public List<Reference> references() {
        return new ArrayList<>(references);
    }

    @Override
    public synchronized NettyContext context() {
        return this.context;
    }

    @Override
    public void finish() {
        this.finish(nowMicros());
    }

    @Override
    public synchronized void finish(long finishMicros) {
        finishedCheck("Finishing already finished span");
        this.finishMicros = finishMicros;
        this.nettyTracer.appendFinishedSpan(this);
        this.finished = true;
    }

    @Override
    public NettySpan setTag(String key, String value) {
        return setObjectTag(key, value);
    }

    @Override
    public NettySpan setTag(String key, boolean value) {
        return setObjectTag(key, value);
    }

    @Override
    public NettySpan setTag(String key, Number value) {
        return setObjectTag(key, value);
    }

    @Override
    public <T> NettySpan setTag(Tag<T> tag, T value) {
        tag.set(this, value);
        return this;
    }

    private synchronized NettySpan setObjectTag(String key, Object value) {
        finishedCheck("Adding tag {%s:%s} to already finished span", key, value);
        tags.put(key, value);
        return this;
    }

    @Override
    public final Span log(Map<String, ?> fields) {
        return log(nowMicros(), fields);
    }

    @Override
    public final synchronized NettySpan log(long timestampMicros, Map<String, ?> fields) {
        finishedCheck("Adding logs %s at %d to already finished span", fields, timestampMicros);
        this.logEntries.add(new LogEntry(timestampMicros, fields));
        return this;
    }

    @Override
    public NettySpan log(String event) {
        return this.log(nowMicros(), event);
    }

    @Override
    public NettySpan log(long timestampMicroseconds, String event) {
        return this.log(timestampMicroseconds, Collections.singletonMap("event", event));
    }

    @Override
    public synchronized Span setBaggageItem(String key, String value) {
        finishedCheck("Adding baggage {%s:%s} to already finished span", key, value);
        this.context = this.context.withBaggageItem(key, value);
        return this;
    }

    @Override
    public synchronized String getBaggageItem(String key) {
        return this.context.getBaggageItem(key);
    }

    /**
     * MockContext implements a Dapper-like opentracing.SpanContext with a trace- and span-id.
     * <p>
     * Note that parent ids are part of the MockSpan, not the MockContext (since they do not need to propagate
     * between processes).
     */
    public static final class NettyContext implements SpanContext {
        private final String traceId;
        private final Map<String, String> baggage;
        private final String spanId;

        /**
         * A package-protected constructor to create a new NettyContext. This should only be called by NettySpan and/or
         * NettyTracer.
         *
         * @param baggage the NettyContext takes ownership of the baggage parameter
         * @see NettyContext#withBaggageItem(String, String)
         */
        public NettyContext(String traceId, String spanId, Map<String, String> baggage) {
            this.baggage = baggage;
            this.traceId = traceId;
            this.spanId = spanId;
        }

        public String getBaggageItem(String key) {
            return this.baggage.get(key);
        }

        public String toTraceId() {
            return String.valueOf(traceId);
        }

        public String toSpanId() {
            return String.valueOf(spanId);
        }

        public String traceId() {
            return traceId;
        }

        public String spanId() {
            return spanId;
        }

        /**
         * Create and return a new (immutable) NettyContext with the added baggage item.
         */
        public NettyContext withBaggageItem(String key, String val) {
            Map<String, String> newBaggage = new HashMap<>(this.baggage);
            newBaggage.put(key, val);
            return new NettyContext(this.traceId, this.spanId, newBaggage);
        }

        @Override
        public Iterable<Map.Entry<String, String>> baggageItems() {
            return baggage.entrySet();
        }
    }

    public static final class LogEntry {
        private final long timestampMicros;
        private final Map<String, ?> fields;

        public LogEntry(long timestampMicros, Map<String, ?> fields) {
            this.timestampMicros = timestampMicros;
            this.fields = fields;
        }

        public long timestampMicros() {
            return timestampMicros;
        }

        public Map<String, ?> fields() {
            return fields;
        }
    }

    public static final class Reference {
        private final NettyContext context;
        private final String referenceType;

        public Reference(NettyContext context, String referenceType) {
            this.context = context;
            this.referenceType = referenceType;
        }

        public NettyContext getContext() {
            return context;
        }

        public String getReferenceType() {
            return referenceType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Reference reference = (Reference) o;
            return Objects.equals(context, reference.context) &&
                    Objects.equals(referenceType, reference.referenceType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(context, referenceType);
        }
    }

    NettySpan(NettyTracer tracer, String operationName, long startMicros, Map<String, Object> initialTags, List<Reference> refs) {
        this.nettyTracer = tracer;
        this.operationName = operationName;
        this.startMicros = startMicros;
        if (initialTags == null) {
            this.tags = new HashMap<>();
        } else {
            this.tags = new HashMap<>(initialTags);
        }
        if (refs == null) {
            this.references = Collections.emptyList();
        } else {
            this.references = new ArrayList<>(refs);
        }
        NettyContext parent = findPreferredParentRef(this.references);
        if (parent == null) {
            // We're a root Span.
            this.context = new NettyContext(String.valueOf(nextId()), String.valueOf(nextId()), new HashMap<String, String>());
            this.parentId = "-1";
        } else {
            // We're a child Span.
            this.context = new NettyContext(parent.traceId, String.valueOf(nextId()), mergeBaggages(this.references));
            this.parentId = parent.spanId;
        }
    }

    private static NettyContext findPreferredParentRef(List<Reference> references) {
        if (references.isEmpty()) {
            return null;
        }
        for (Reference reference : references) {
            if (References.CHILD_OF.equals(reference.getReferenceType())) {
                return reference.getContext();
            }
        }
        return references.get(0).getContext();
    }

    private static Map<String, String> mergeBaggages(List<Reference> references) {
        Map<String, String> baggage = new HashMap<>();
        for (Reference ref : references) {
            if (ref.getContext().baggage != null) {
                baggage.putAll(ref.getContext().baggage);
            }
        }
        return baggage;
    }

    static String nextId() {
        //return nextId.addAndGet(1);
        return UUID.randomUUID().toString();
    }

    static long nowMicros() {
        return System.currentTimeMillis() * 1000;
    }

    private synchronized void finishedCheck(String format, Object... args) {
        if (finished) {
            RuntimeException ex = new IllegalStateException(String.format(format, args));
            errors.add(ex);
            throw ex;
        }
    }

    @Override
    public String toString() {
        return "{" +
                "traceId:" + context.traceId() +
                ", spanId:" + context.spanId() +
                ", parentId:" + parentId +
                ", operationName:\"" + operationName + "\"}";
    }
}
