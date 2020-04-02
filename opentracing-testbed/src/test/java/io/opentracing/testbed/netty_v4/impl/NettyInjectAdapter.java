package io.opentracing.testbed.netty_v4.impl;

import io.opentracing.propagation.TextMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @Author: yaozh
 * @Description:
 */
public class NettyInjectAdapter implements TextMap {

    private Map<String, String> headers = new HashMap<>();

    public NettyInjectAdapter() {
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("This class should be used only with Tracer.Iterator()!");
    }

    @Override
    public void put(String key, String value) {
        headers.put(key, value);
        System.out.println(key+":"+value);
    }
}
