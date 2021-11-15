package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;

import java.util.Map;

public class Service02Stack extends Stack {
    public Service02Stack(
        final Construct scope,
        final String id,
        Cluster cluster,
        SnsTopic topic,
        Table productEventsDdb
    ) {
        this(scope, id, null, cluster, topic, productEventsDdb);
    }

    public Service02Stack(
        final Construct scope,
        final String id,
        final StackProps props,
        Cluster cluster,
        SnsTopic topic,
        Table productEventsDdb
    ) {
        super(scope, id, props);

        Queue productEventsDLQ = Queue.Builder.create(this, "PRODUCTEVENTSDLQ")
            .queueName("product-events-dlq")
            .build();

        DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
            .queue(productEventsDLQ)
            .maxReceiveCount(3)
            .build();

        Queue productEventQueue = Queue.Builder.create(this, "PRODUCTEVENTS")
            .queueName("product-events")
            .deadLetterQueue(deadLetterQueue)
            .build();

        SqsSubscription sqsSubscription = SqsSubscription.Builder.create(productEventQueue).build();
        topic.getTopic().addSubscription(sqsSubscription);

        Map<String, String> envVariables = Map.of(
            "AWS_REGION", "sa-east-1",
            "AWS_SQS_QUEUE_PRODUCT_EVENTS_NAME", productEventQueue.getQueueName()
        );

        ApplicationLoadBalancedFargateService service02 = ApplicationLoadBalancedFargateService.Builder
            .create(this, "ALB02")
            .serviceName("SERVICE02")
            .cluster(cluster)
            .cpu(512)
            .memoryLimitMiB(1024)
            .desiredCount(2)
            .listenerPort(9090)
            .taskImageOptions(
                ApplicationLoadBalancedTaskImageOptions.builder()
                    .containerName("aws_course_p2")
                    .image(ContainerImage.fromRegistry("walteralleyz/aws_course_p2:1.2.0"))
                    .containerPort(9090)
                    .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "SERVICE02LOGGROUP")
                            .logGroupName("SERVICE02")
                            .removalPolicy(RemovalPolicy.DESTROY)
                            .build())
                        .streamPrefix("SERVICE02")
                        .build()))
                    .environment(envVariables)
                    .build()
            )
            .publicLoadBalancer(true)
            .build();

        service02.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
            .path("/actuator/health")
            .port("9090")
            .healthyHttpCodes("200")
            .build());

        productEventQueue.grantConsumeMessages(service02.getTaskDefinition().getTaskRole());
        productEventsDdb.grantReadWriteData(service02.getTaskDefinition().getTaskRole());
    }
}
