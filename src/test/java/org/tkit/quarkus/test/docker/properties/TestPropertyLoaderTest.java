package org.tkit.quarkus.test.docker.properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.tkit.quarkus.test.docker.ContainerConfig;
import org.tkit.quarkus.test.docker.DockerComposeService;
import org.tkit.quarkus.test.docker.DockerTestEnvironment;
import org.tkit.quarkus.test.docker.TestGenericContainer;

import java.io.File;
import java.nio.file.Path;

public class TestPropertyLoaderTest {

    @Test
    public void createTestPropertyTest() {
        DockerTestEnvironment de = new DockerTestEnvironment() {
            @Override
            public DockerComposeService getService(String name) {
                return new DockerComposeService(null, null, null) {
                    @Override
                    public String getHost() {
                        return "DUMMY";
                    }
                    @Override
                    protected TestGenericContainer createContainer(Network network, ContainerConfig config, Path dir) {
                        return null;
                    }
                };
            }
        };

        TestProperty prop1 = TestPropertyLoader.createTestProperty("KAFKA_CFG_ADVERTISED_LISTENERS", "PLAINTEXT://tkit-events-import-kafka:9092,PLAINTEXT_HOST://$${host:tkit-events-import-zookeeper}:9093");
        TestProperty prop2 = TestPropertyLoader.createTestProperty("KAFKA_CFG_ADVERTISED_LISTENERS", "$${host:tkit-events-import-zookeeper}");


        Assertions.assertEquals("PLAINTEXT://tkit-events-import-kafka:9092,PLAINTEXT_HOST://DUMMY:9093", prop1.getValue(de));
        Assertions.assertEquals("DUMMY", prop2.getValue(de));
    }
}
