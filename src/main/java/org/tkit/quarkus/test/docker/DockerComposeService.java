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

import org.tkit.quarkus.test.docker.properties.TestProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DockerComposeService {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeService.class);

    private TestGenericContainer container;

    private ContainerConfig config;

    protected DockerComposeService(Network network, ContainerConfig config, Path dir) {
        this.config = config;
        this.container = createContainer(network, config, dir);
    }

    public static DockerComposeService createDockerComposeService(Network network, ContainerConfig config, Path dir) {
        return new DockerComposeService(network, config, dir);
    }

    public String getName() {
        return config.name;
    }

    public ContainerConfig getConfig() {
        return config;
    }

    public GenericContainer<?> getContainer() {
        return container;
    }

    public void start(DockerTestEnvironment environment, boolean integrationTest) {
        if (container == null) {
            return;
        }
        // update environment variables
        List<TestProperty> te = new ArrayList<>(config.commonVariables.environments);
        if (integrationTest) {
            te.addAll(config.integrationVariables.environments);
        } else {
            te.addAll(config.unitVariables.environments);
        }
        Map<String, String> env = createValues(environment, te);
        System.out.println(String.format("[tkit-quarkus-test] Service: '%s' add test environment variables: %s", config.name, env));
        container.withEnv(env);

        // start container
        container.start();

        // update properties
        List<TestProperty> tp = new ArrayList<>(config.commonVariables.properties);
        if (integrationTest) {
            tp.addAll(config.integrationVariables.properties);
        } else {
            tp.addAll(config.unitVariables.properties);
        }
        Map<String, String> prop = createValues(environment, tp);
        System.out.println(String.format("[tkit-quarkus-test] Service: '%s' update test properties: %s", config.name, prop));
        prop.forEach(System::setProperty);
    }

    private static Map<String, String> createValues(DockerTestEnvironment environment, List<TestProperty> properties) {
        return properties.stream().collect(Collectors.toMap(p -> p.name, p -> p.getValue(environment)));
    }

    public void stop(boolean integrationTest) {
        // clear system properties
        List<TestProperty> tp = new ArrayList<>(config.commonVariables.properties);
        if (integrationTest) {
            tp.addAll(config.integrationVariables.properties);
        } else {
            tp.addAll(config.unitVariables.properties);
        }
        tp.forEach(p -> System.clearProperty(p.name));

        // stop container
        container.stop();
    }

    public Integer getPort(int port) {
        return getPort(container, port);
    }

    public static Integer getPort(GenericContainer<?> container, int port) {
        try {
            return container.getMappedPort(port);
        } catch (IllegalArgumentException ex) {
            log.warn("Using fixed port for the container {} and port {}", container.getContainerId(), port);
            return port;
        }
    }

    public String getHost() {
        return getHost(container);
    }

    public static String getHost(GenericContainer<?> container) {
        return container.getContainerIpAddress();
    }

    public String getUrl(int port) {
        return getUrl(container, port);
    }

    public static String getUrl(GenericContainer<?> container, int port) {
        return "http://" + getHost(container) + ":" + getPort(container, port);
    }

    protected TestGenericContainer createContainer(Network network, ContainerConfig config, Path dir) {

        try (TestGenericContainer result = new TestGenericContainer(config.image)) {
            result.withNetwork(network).withNetworkAliases(config.name);
            // docker command
            if (config.command != null && !config.command.isEmpty()) {
                String[] cmd = config.command.toArray(new String[0]);
                result.withCommand(cmd);
            }
            // image pull policy
            switch ( config.imagePull) {
                case ALWAYS:
                    result.withImagePullPolicy(PullPolicy.alwaysPull());
                    break;
                case MAX_AGE:
                    result.withImagePullPolicy(PullPolicy.ageBased(config.imagePullDuration));
                    break;
                case DEFAULT:
                    result.withImagePullPolicy(PullPolicy.defaultPolicy());
            }

            // wait log rule
            if (config.waitLogRegex != null) {
                result.waitingFor(Wait.forLogMessage(config.waitLogRegex, config.waitLogTimes));
            }

            // update log flag
            if (config.log) {
                result.withLogConsumer(ContainerLogger.create(config.name));
            }

            // environments
            config.environments.forEach(result::withEnv);

            // volumes
            config.volumes.forEach((k, v) -> {
                String key = k;
                if (key.startsWith("./")) {
                    key = key.substring(1);
                }
                MountableFile mf;
                try {
                    mf = MountableFile.forClasspathResource(key);
                    System.out.printf("[tkit-quarkus-test] Service: '%s' find volume path in classpath: %s%n", config.name, key);
                } catch (Exception ex) {
                    System.err.printf("[tkit-quarkus-test] Service: '%s' could not find volume path in classpath: %s%n", config.name, key);

                    Path path = Paths.get(k);
                    if (!Files.exists(path)) {
                        // check path relative to the compose file
                        Path p1 = dir.resolve(k);
                        if (Files.exists(p1)) {
                            path = p1;
                        }
                    }
                    if (!Files.exists(path)) {
                        System.err.printf("[tkit-quarkus-test] Service: '%s' could not find volume path in system: %s%n", config.name, k);
                        throw new RuntimeException("Missing volume resources " + v);
                    }
                    mf = MountableFile.forHostPath(path);
                    System.err.printf("[tkit-quarkus-test] Service: '%s' find volume path `%s` in system `%s`%n", config.name, k,path);
                }

                result.withCopyFileToContainer(mf, v);
            });

            // ports
            config.ports.values().stream().map(Integer::parseInt).forEach(result::addExposedPort);
            if (config.fixedPorts) {
                config.ports.forEach((key, value) -> result.withFixedExposedPort(Integer.parseInt(key), Integer.parseInt(value)));
            }

            return result;
        }
    }

}
