package com.github.chrishantha.lambda.ballerina;


import io.netty.handler.codec.http.HttpHeaders;
import org.ballerinalang.net.http.Constants;
import org.ballerinalang.runtime.message.BallerinaMessageDataSource;
import org.ballerinalang.runtime.message.StringDataSource;
import org.wso2.carbon.messaging.Header;
import org.wso2.transport.http.netty.message.HttpMessageDataStreamer;

import java.util.List;
import java.util.Locale;

/**
 * Utility methods for generating a message.
 */
public class MessageUtils {

    public static HTTPTestRequest generateHTTPMessage(String path, String method) {
        return generateHTTPMessage(path, method, null, null);
    }

    public static HTTPTestRequest generateHTTPMessage(String path, String method, BallerinaMessageDataSource payload) {
        return generateHTTPMessage(path, method, null, payload);
    }

    public static HTTPTestRequest generateHTTPMessage(String path, String method, String payload) {
        return generateHTTPMessage(path, method, null, new StringDataSource(payload));
    }

    public static HTTPTestRequest generateHTTPMessage(String path, String method, List<Header> headers,
                                                      BallerinaMessageDataSource payload) {

        HTTPTestRequest carbonMessage = new HTTPTestRequest();

        // Set meta data
        carbonMessage.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL,
                Constants.PROTOCOL_HTTP);
        carbonMessage.setProperty(org.wso2.carbon.messaging.Constants.LISTENER_INTERFACE_ID,
                Constants.DEFAULT_INTERFACE);
        // Set url
        carbonMessage.setProperty(org.wso2.carbon.messaging.Constants.TO, path);

        // Set method
        carbonMessage.setProperty(Constants.HTTP_METHOD, method.trim().toUpperCase(Locale.getDefault()));

        // Set Headers
        HttpHeaders httpHeaders = carbonMessage.getHeaders();
        if (headers != null) {
            for (Header header : headers) {
                httpHeaders.set(header.getName(), header.getValue());
            }
        }

        // Set message body
        if (payload != null) {
            payload.setOutputStream(new HttpMessageDataStreamer(carbonMessage).getOutputStream());
            carbonMessage.setMessageDataSource(payload);
            carbonMessage.setAlreadyRead(true);
        } else {
            carbonMessage.setEndOfMsgAdded(true);
        }
        return carbonMessage;
    }

}