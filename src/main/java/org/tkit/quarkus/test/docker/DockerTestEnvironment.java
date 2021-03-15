/*
 * Copyright 2020 tkit.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tkit.quarkus.test.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The docker test environment.
 */
public class DockerTestEnvironment {

    public static final String SYS_PROP_TEST_INTEGRATION = "test.integration";

    private static final Logger log = LoggerFactory.getLogger(DockerTestEnvironment.class);

    private Map<String, DockerComposeService> containers = new HashMap<>();

    private Map<Integer, List<DockerComposeService>> containerProperties = new HashMap<>();

    private Network network;

    public DockerTestEnvironment() {
        String dockerComposeFilePath = System.getProperty("test.docker.compose.file", "./src/test/resources/docker-compose.yml");
        File dockerComposeFile = new File(dockerComposeFilePath);

        if (!dockerComposeFile.exists()) {
            dockerComposeFile = new File("./src/test/resources/docker-compose.yaml");
        }

        load(dockerComposeFile);
    }

    public DockerTestEnvironment(String dockerComposeFile) {
        load(new File(dockerComposeFile));
    }

    public DockerComposeService getService(String name) {
        return containers.get(name);
    }

    public Network getNetwork() {
        return network;
    }

    public void load(File dockerComposeFile) {
        network = Network.newNetwork();

        boolean integrationTest = Boolean.getBoolean(SYS_PROP_TEST_INTEGRATION);

        Yaml yaml = new Yaml();

        Path dir = dockerComposeFile.toPath().getParent();
        try (InputStream fileInputStream = Files.newInputStream(dockerComposeFile.toPath())) {
            Map<String, Object> map = yaml.load(fileInputStream);
            Object services = map.get("services");
            if (services instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) services;
                data.forEach((k, v) -> {
                    ContainerConfig config = ContainerConfig.createContainerProperties(k, (Map<String, Object>) v);
                    if ((integrationTest && config.integrationTest) || (!integrationTest && config.unitTest)) {
                        DockerComposeService service = DockerComposeService.createDockerComposeService(network, config, dir);
                        containerProperties.computeIfAbsent(service.getConfig().priority, x -> new ArrayList<>()).add(service);
                        containers.put(k, service);
                    }
                });
            }
        } catch (IOException e) {
            log.warn("Failed to read YAML from {}", dockerComposeFile.getAbsolutePath(), e);
        }
    }

    public void start() {
        System.out.println("Docker client ping ...");
        DockerClientFactory.instance().client().pingCmd().exec();

        List<Integer> priorities = new ArrayList<>(containerProperties.keySet());
        Collections.sort(priorities);

        // integration tests
        boolean integrationTest = Boolean.getBoolean(SYS_PROP_TEST_INTEGRATION);

        priorities.forEach(p -> {
            List<DockerComposeService> services = containerProperties.get(p);
            List<String> names = services.stream().map(DockerComposeService::getName).collect(Collectors.toList());
            System.out.println(String.format("------------------------------\nStart test containers\npriority: %s\nServices: %s\nintegration test: %s\n------------------------------", p, names, integrationTest));
            services.parallelStream().forEach(s -> s.start(this, integrationTest));
        });
    }

    public void stop() {
        // integration tests
        boolean integrationTest = Boolean.getBoolean(SYS_PROP_TEST_INTEGRATION);

        containers.values().parallelStream().forEach(p -> p.stop(integrationTest));
    }

}
