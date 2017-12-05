package com.github.chrishantha.lambda.ballerina;

import org.wso2.transport.http.netty.contractimpl.HttpResponseStatus;
import org.wso2.transport.http.netty.contractimpl.HttpResponseStatusFuture;

/**
 * Test HttpResponseStatusFuture implementation for service tests
 */
public class TestHttpResponseStatusFuture extends HttpResponseStatusFuture {

    public TestHttpResponseStatusFuture sync() throws InterruptedException {
        return this;
    }

    public HttpResponseStatus getStatus() {
        return new HttpResponseStatus(null);
    }
}
