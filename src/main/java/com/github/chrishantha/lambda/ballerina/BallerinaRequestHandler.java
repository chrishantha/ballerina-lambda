package com.github.chrishantha.lambda.ballerina;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.connector.api.ConnectorUtils;
import org.ballerinalang.connector.api.Executor;
import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BServiceUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.util.StringUtils;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.net.http.BallerinaHttpServerConnector;
import org.ballerinalang.net.http.Constants;
import org.ballerinalang.net.http.HttpDispatcher;
import org.ballerinalang.net.http.HttpResource;
import org.wso2.transport.http.netty.message.HTTPCarbonMessage;
import org.wso2.transport.http.netty.message.HttpMessageDataStreamer;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BallerinaRequestHandler implements RequestHandler<Request, Response> {

    private static final String BAL_PACKAGE_ENV_VAR = "BAL_PACKAGE";
    private static final String BAL_SERVICE_PATH_ENV_VAR = "BAL_SERVICE_PATH";
    private static final String BAL_SERVICE_METHOD_ENV_VAR = "BAL_SERVICE_METHOD";
    private static final String HEADER_FUNCTION_NAME = "x-function-name";
    private static final String HEADER_FUNCTION_VERSION = "x-function-version";
    private static final String HEADER_REMAINING_TIME_IN_MILLIS = "x-remaining-time-in-millis";
    private static final String HEADER_UP_TIME = "x-up-time-in-millis";
    private static final String HEADER_VM_STARTUP_TIME = "x-vm-startup-time-in-millis";
    private static final String HEADER_HANDLER_INIT_TIME = "x-handler-init-time-in-millis";
    private static final String HEADER_PROCESSING_TIME = "x-processing-time-in-millis";
    private static final String HEADER_OS_NAME = "x-os-name";
    private static final String HEADER_OS_ARCH = "x-os-arch";
    private static final String HEADER_OS_VERSION = "x-os-version";
    private static final String HEADER_AVAILABLE_PROCESSORS = "x-available-processors";
    private static final String HEADER_FREE_MEMORY = "x-free-memory-in-bytes";
    private static final String HEADER_MAX_MEMORY = "x-max-memory-in-bytes";
    private static final String HEADER_TOTAL_MEMORY = "x-total-memory-in-bytes";

    private final CompileResult compileResult;
    private final String BAL_PACKAGE;
    private final String BAL_SERVICE_PATH;
    private final String BAL_SERVICE_METHOD;

    private final long vmStartupTime;
    private final long handlerInitTime;

    public BallerinaRequestHandler() {
        long startNanoTime = System.nanoTime();
        long startTime = System.currentTimeMillis();
        long vmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        vmStartupTime = startTime - vmStartTime;
        System.out.println(String.format("VM Startup Time: %d ms", vmStartupTime));
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
        handlerInitTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
        System.out.println(String.format("Request Handler Initialized. Initialization Time %d ms",
                handlerInitTime));
    }

    private String getEnvVar(String name, String def) {
        String value = System.getenv(name);
        return value != null ? value : def;
    }

    public Response handleRequest(Request request, Context context) {
        long startNanoTime = System.nanoTime();
        LambdaLogger logger = context.getLogger();
        logger.log(String.format("Received : %s", request.getBody()));
        String responseBody = invokeBallerinaService(request.getBody());
        Map<String, String> headers = new HashMap<>();
        Response response = new Response();
        response.setHeaders(headers);
        response.setBody(responseBody);
        // AWS Request ID is already available as a header. See: x-amzn-RequestId
        headers.put(HEADER_FUNCTION_NAME, context.getFunctionName());
        headers.put(HEADER_FUNCTION_VERSION, context.getFunctionVersion());
        headers.put(HEADER_REMAINING_TIME_IN_MILLIS, String.valueOf(context.getRemainingTimeInMillis()));
        headers.put(HEADER_UP_TIME, String.valueOf(ManagementFactory.getRuntimeMXBean().getUptime()));
        headers.put(HEADER_VM_STARTUP_TIME, String.valueOf(vmStartupTime));
        headers.put(HEADER_HANDLER_INIT_TIME, String.valueOf(handlerInitTime));
        headers.put(HEADER_OS_NAME, System.getProperty("os.name"));
        headers.put(HEADER_OS_ARCH, System.getProperty("os.arch"));
        headers.put(HEADER_OS_VERSION, System.getProperty("os.version"));
        headers.put(HEADER_AVAILABLE_PROCESSORS, String.valueOf(Runtime.getRuntime().availableProcessors()));
        headers.put(HEADER_FREE_MEMORY, String.valueOf(Runtime.getRuntime().freeMemory()));
        headers.put(HEADER_MAX_MEMORY, String.valueOf(Runtime.getRuntime().maxMemory()));
        headers.put(HEADER_TOTAL_MEMORY, String.valueOf(Runtime.getRuntime().totalMemory()));
        long requestProcessingTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
        headers.put(HEADER_PROCESSING_TIME, String.valueOf(requestProcessingTime));
        return response;
    }

    public String invokeBallerinaService(String payload) {
        HTTPTestRequest request = MessageUtils.generateHTTPMessage(BAL_SERVICE_PATH, BAL_SERVICE_METHOD,
                payload);
        BallerinaHttpServerConnector httpServerConnector = (BallerinaHttpServerConnector) ConnectorUtils.
                getBallerinaServerConnector(compileResult.getProgFile(), Constants.HTTP_PACKAGE_PATH);
        TestHttpFutureListener futureListener = new TestHttpFutureListener(request);
        request.setFutureListener(futureListener);
        HttpResource resource = HttpDispatcher.findResource(httpServerConnector.getHttpServicesRegistry(), request);
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
        return StringUtils.getStringFromInputStream(new HttpMessageDataStreamer(responseMsg)
                .getInputStream());
    }
}