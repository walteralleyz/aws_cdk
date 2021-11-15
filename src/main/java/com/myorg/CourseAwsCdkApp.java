package com.myorg;

import software.amazon.awscdk.core.App;

public class CourseAwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();
        VpcStack vpcStack = new VpcStack(app, "VPC");
        ClusterStack clusterStack = new ClusterStack(app, "CLUSTER", vpcStack.getVpc());
        SnsStack snsStack = new SnsStack(app, "SNS");
        DdbStack ddbStack = new DdbStack(app, "DDB");

        Service01Stack service01Stack = new Service01Stack(
            app,
            "SERVICE01",
            clusterStack.getCluster(),
            snsStack.getSnsTopic());

        Service02Stack service02Stack = new Service02Stack(
            app,
            "SERVICE02",
            clusterStack.getCluster(),
            snsStack.getSnsTopic(),
            ddbStack.getProductEventsDb()
        );

        RdsStack rdsStack = new RdsStack(app,"RDS", vpcStack.getVpc());

        clusterStack.addDependency(vpcStack);
        rdsStack.addDependency(vpcStack);

        service01Stack.addDependency(clusterStack);
        service01Stack.addDependency(rdsStack);
        service01Stack.addDependency(snsStack);

        service02Stack.addDependency(clusterStack);
        service02Stack.addDependency(snsStack);
        service02Stack.addDependency(ddbStack);
        app.synth();
    }
}
