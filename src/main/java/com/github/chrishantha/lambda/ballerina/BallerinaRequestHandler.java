package com.github.chrishantha.lambda.ballerina;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.connector.api.Executor;
import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BServiceUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.net.http.Constants;
import org.ballerinalang.net.http.HttpDispatcher;
import org.ballerinalang.net.http.HttpResource;
import org.wso2.transport.http.netty.message.HTTPCarbonMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BallerinaRequestHandler implements RequestHandler<Request, Response> {

    private static final String BAL_PACKAGE_ENV_VAR = "BAL_PACKAGE";
    private static final String BAL_SERVICE_PATH_ENV_VAR = "BAL_SERVICE_PATH";
    private static final String BAL_SERVICE_METHOD_ENV_VAR = "BAL_SERVICE_METHOD";
    private static final String HEADER_FUNCTION_NAME = "x-bal-lambda-function-name";
    private static final String HEADER_FUNCTION_VERSION = "x-bal-lambda-function-version";
    private static final String HEADER_REMAINING_TIME_IN_MILLIS = "x-bal-lambda-remaining-time-in-millis";

    private final CompileResult compileResult;
    private final String BAL_PACKAGE;
    private final String BAL_SERVICE_PATH;
    private final String BAL_SERVICE_METHOD;

    public BallerinaRequestHandler() {
        System.setProperty("java.util.logging.manager", "org.ballerinalang.logging.BLogManager");
        // Check for BAL_FILE_NAME environment variable
        BAL_PACKAGE = getEnvVar(BAL_PACKAGE_ENV_VAR, "echoService");
        BAL_SERVICE_PATH = getEnvVar(BAL_SERVICE_PATH_ENV_VAR, "/echo/");
        BAL_SERVICE_METHOD = getEnvVar(BAL_SERVICE_METHOD_ENV_VAR, "POST");
        compileResult = BCompileUtil.compile(this, "./", BAL_PACKAGE);
        if (compileResult.getErrorCount() > 0) {
            throw new RuntimeException("There are errors in the Ballerina Service");
        }
        BServiceUtil.runService(compileResult);
        System.out.println("Request Handler Initialized.");
    }

    private String getEnvVar(String name, String def) {
        String value = System.getenv(name);
        return value != null ? value : def;
    }

    public Response handleRequest(Request request, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(String.format("Received : %s", request.getBody()));
        String responseBody = invokeBallerinaService(request.getBody());
        Map<String, String> headers = new HashMap<>();
        // AWS Request ID is already available as a header. See: x-amzn-RequestId
        headers.put(HEADER_FUNCTION_NAME, context.getFunctionName());
        headers.put(HEADER_FUNCTION_VERSION, context.getFunctionVersion());
        headers.put(HEADER_REMAINING_TIME_IN_MILLIS, String.valueOf(context.getRemainingTimeInMillis()));
        Response response = new Response();
        response.setHeaders(headers);
        response.setBody(responseBody);
        return response;
    }

    public String invokeBallerinaService(String payload) {
        HTTPTestRequest request = MessageUtils.generateHTTPMessage(BAL_SERVICE_PATH, BAL_SERVICE_METHOD,
                payload);
        TestHttpFutureListener futureListener = new TestHttpFutureListener(request);
        request.setFutureListener(futureListener);
        HttpResource resource = HttpDispatcher.findResource(request);
        if (resource == null) {
            return null;
        }
        //TODO below should be fixed properly
        //basically need to find a way to pass information from server connector side to client connector side
        Map<String, Object> properties = null;
        if (request.getProperty(Constants.SRC_HANDLER) != null) {
            Object srcHandler = request.getProperty(Constants.SRC_HANDLER);
            properties = Collections.singletonMap(Constants.SRC_HANDLER, srcHandler);
        }
        BValue[] signatureParams = HttpDispatcher.getSignatureParameters(resource, request);
        ConnectorFuture future = Executor.submit(resource.getBalResource(), properties, signatureParams);
        futureListener.setRequestStruct(signatureParams[0]);
        future.setConnectorFutureListener(futureListener);
        futureListener.sync();
        HTTPCarbonMessage responseMsg = futureListener.getResponseMsg();
        return responseMsg.getMessageDataSource().getMessageAsString();
    }
}