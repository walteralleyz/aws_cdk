package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;

public class VpcStack extends Stack {
    private Vpc vpc;

    public VpcStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public VpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        // example resource
        // final Queue queue = Queue.Builder.create(this, "CourseAwsCdkQueue")
        //         .visibilityTimeout(Duration.seconds(300))
        //         .build();

        vpc = Vpc.Builder.create(this, "VPC01")
            .maxAzs(3)
            .build();
    }

    public Vpc getVpc() { return vpc; }
}
