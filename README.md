# Running a Ballerina Service as a Serverless function in AWS Lambda

This project implements an AWS Request Handler, which is triggered by AWS Lambda when there is an event.
The request handler invokes the Ballerina Service resource and returns the response.

The Ballerina service is compiled using an unofficial API in the constructor. 
The handler method locates the service resource in the compiled Ballerina service and executes it programmatically.

For more information, see [Ballerina Services in Serverless World](https://medium.com/@chrishantha/ballerina-services-in-serverless-world-b54c5e7382a0)

## How to run your own Ballerina service in AWS Lambda

Clone the ballerina-lambda repository.

    git clone https://github.com/chrishantha/ballerina-lambda --depth=1
    cd ballerina-lambda/

Save ballerina service in ballerina-services directory. Currently, the ballerina-lambda expects a ballerina
package name, which includes the Ballerina service. Therefore, you need to make sure that the ballerina
service is in a package. There is already a sample `helloWorldService` inside the ballerina-services directory.

Now build the maven project.

    mvn clean package

There should be a zip file inside the `target` directory.

Use the zip file as the package for AWS Lambda function.

Following must be specified as the handler method.

    com.github.chrishantha.lambda.ballerina.BallerinaRequestHandler::handleRequest

You need to configure following environment variables.

 Environment Variable | Description | Default
------------ | ------------- | -------------
BAL_PACKAGE | The name of the Ballerina package, which includes the Ballerina Service. | echoService
BAL_SERVICE_PATH | The full path for the Ballerina service resource. | /echo/
BAL_SERVICE_METHOD | The HTTP Method used by the Ballerina service resource | POST


## License

Licensed under the Apache License, Version 2.0
