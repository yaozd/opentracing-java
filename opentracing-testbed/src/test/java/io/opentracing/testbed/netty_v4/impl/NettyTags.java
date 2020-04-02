package io.opentracing.testbed.netty_v4.impl;

import io.opentracing.tag.StringTag;

/**
 * @Author: yaozh
 * @Description:
 */
public class NettyTags {
    /**
     * HTTP_URL records the url of the incoming request.
     */
    public static final StringTag HTTP_URL = new StringTag("http.url");
}
