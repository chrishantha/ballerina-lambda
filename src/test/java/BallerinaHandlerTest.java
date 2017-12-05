package com.github.chrishantha.lambda.ballerina;

import org.testng.Assert;
import org.testng.annotations.Test;

public class BallerinaHandlerTest {

    @Test
    public void echoTest() {
        String payload = "Hello, World!";
        Assert.assertEquals(new BallerinaRequestHandler().invokeBallerinaService(payload), payload);
    }

}
