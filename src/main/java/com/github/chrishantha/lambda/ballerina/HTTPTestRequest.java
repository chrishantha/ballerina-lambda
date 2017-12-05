package com.github.chrishantha.lambda.ballerina;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.wso2.transport.http.netty.contract.ServerConnectorException;
import org.wso2.transport.http.netty.message.HTTPCarbonMessage;

/**
 * Extended HTTPCarbonMessage to handle error path.
 */
public class HTTPTestRequest extends HTTPCarbonMessage {

    private TestHttpFutureListener futureListener;
    private HTTPCarbonMessage httpCarbonMessage;

    public HTTPTestRequest() {
        super(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, ""));
    }

    @Override
    public TestHttpResponseStatusFuture respond(HTTPCarbonMessage httpCarbonMessage) throws ServerConnectorException {
        this.httpCarbonMessage = httpCarbonMessage;
        if (this.getFutureListener() != null) {
            getFutureListener().setResponseMsg(httpCarbonMessage);
            this.httpCarbonMessage = null;
        }
        return new TestHttpResponseStatusFuture();
    }

    public TestHttpFutureListener getFutureListener() {
        return futureListener;
    }

    public void setFutureListener(TestHttpFutureListener futureListener) {
        this.futureListener = futureListener;
        if (httpCarbonMessage != null) {
            this.futureListener.setResponseMsg(httpCarbonMessage);
        }
    }
}
