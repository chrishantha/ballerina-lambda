package com.github.chrishantha.lambda.ballerina;


import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import org.ballerinalang.net.http.Constants;
import org.wso2.carbon.messaging.Header;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Utility methods for generating a message.
 */
public class MessageUtils {

    public static HTTPTestRequest generateHTTPMessage(String path, String method, String payload) {
        return generateHTTPMessage(path, method, null, payload);
    }

    public static HTTPTestRequest generateHTTPMessage(String path, String method, List<Header> headers,
                                                      String payload) {
        HTTPTestRequest carbonMessage = new HTTPTestRequest();
        carbonMessage.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL,
                Constants.PROTOCOL_HTTP);
        carbonMessage.setProperty(org.wso2.carbon.messaging.Constants.LISTENER_INTERFACE_ID,
                Constants.DEFAULT_INTERFACE);
        // Set url
        carbonMessage.setProperty(org.wso2.carbon.messaging.Constants.TO, path);
        carbonMessage.setProperty(Constants.HTTP_METHOD, method.trim().toUpperCase(Locale.getDefault()));
        carbonMessage.setProperty(Constants.LOCAL_ADDRESS,
                new InetSocketAddress(Constants.HTTP_DEFAULT_HOST, 9090));
        carbonMessage.setProperty(Constants.LISTENER_PORT, 9090);
        carbonMessage.setProperty(Constants.RESOURCE_ARGS, new HashMap<String, String>());
        HttpHeaders httpHeaders = carbonMessage.getHeaders();
        if (headers != null) {
            for (Header header : headers) {
                httpHeaders.set(header.getName(), header.getValue());
            }
        }
        if (payload != null) {
            carbonMessage.addHttpContent(new DefaultLastHttpContent(Unpooled.wrappedBuffer(payload.getBytes())));
        } else {
            carbonMessage.setEndOfMsgAdded(true);
        }
        return carbonMessage;
    }

}