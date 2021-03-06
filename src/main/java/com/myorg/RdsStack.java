package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;

import java.util.Collections;

public class RdsStack extends Stack {
    public RdsStack(final Construct scope, final String id, Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
        super(scope, id, props);

        CfnParameter databasePassword = CfnParameter.Builder.create(this, "dbPasswd")
            .type("String")
            .description("The RDS instance password")
            .build();

        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));

        DatabaseInstance databaseInstance = DatabaseInstance.Builder.create(this, "RDS01")
            .instanceIdentifier("aws-course-db")
            .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                .version(MysqlEngineVersion.VER_8_0)
                .build()))
            .vpc(vpc)
            .credentials(Credentials.fromUsername("admin", CredentialsFromUsernameOptions.builder()
                .password(SecretValue.plainText(databasePassword.getValueAsString()))
                .build()))
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
            .multiAz(false)
            .allocatedStorage(10)
            .securityGroups(Collections.singletonList(iSecurityGroup))
            .vpcSubnets(SubnetSelection.builder()
                .subnets(vpc.getPrivateSubnets())
                .build())
            .build();

        CfnOutput.Builder.create(this, "rds_endpoint_address")
            .exportName("rds-address")
            .value(databaseInstance.getDbInstanceEndpointAddress())
            .build();

        CfnOutput.Builder.create(this, "rds_password")
            .exportName("rds-password")
            .value(databasePassword.getValueAsString())
            .build();
    }
}
