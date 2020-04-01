package io.opentracing.testbed.netty_v3.impl;

import io.opentracing.propagation.TextMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NettyExtractAdapter implements TextMap {

    private Set<Map.Entry<String, String>> headers;

    public NettyExtractAdapter() {
        Map<String, String> map = new HashMap<>();
        map.put("traceid", "traceid-e0ca2246-3541");
        map.put("spanid", "spanid-4452-8e8d-b993f5c060dc");
        this.headers = map.entrySet();
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return this.headers.iterator();
    }

    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("This class should be used only with Tracer.inject()!");
    }
}
