package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Service01Stack extends Stack {
    public Service01Stack(final Construct scope, final String id, Cluster cluster, SnsTopic topic) {
        this(scope, id, null, cluster, topic);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster, SnsTopic topic) {
        super(scope, id, props);

        Map<String, String> envVariables = new HashMap<String, String>() {{
            put("SPRING_DATASOURCE_URL", "jdbc:mariadb://" + Fn.importValue("rds-address")
                + ":3306/aws_course?createDatabaseIfNotExist=true");
            put("SPRING_DATASOURCE_USERNAME", "admin");
            put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("rds-password"));
            put("AWS_REGION", "sa-east-1");
            put("AWS_SNS_TOPIC_PRODUCT_EVENTS_ARN", topic.getTopic().getTopicArn());
        }};

        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder
            .create(this, "ALB01")
            .serviceName("SERVICE01")
            .cluster(cluster)
            .cpu(512)
            .memoryLimitMiB(1024)
            .desiredCount(2)
            .listenerPort(8080)
            .taskImageOptions(
                ApplicationLoadBalancedTaskImageOptions.builder()
                    .containerName("aws_course")
                    .image(ContainerImage.fromRegistry("walteralleyz/aws_course:1.3.0"))
                    .containerPort(8080)
                    .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "SERVICE01LOGGROUP")
                                .logGroupName("SERVICE01")
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .build())
                            .streamPrefix("SERVICE01")
                        .build()))
                    .environment(envVariables)
                    .build()
            )
            .publicLoadBalancer(true)
            .build();

        service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
            .path("/actuator/health")
            .port("8080")
            .healthyHttpCodes("200")
            .build());

        ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
            .build());

        scalableTaskCount.scaleOnCpuUtilization("SERVICE01AUTOSCALING", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
            .build());

        topic.getTopic().grantPublish(
            service01.getService().getTaskDefinition().getTaskRole());
    }
}
