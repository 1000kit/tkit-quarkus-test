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
import org.tkit.quarkus.test.docker.properties.TestPropertyLoader;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerConfig {

    public static final Integer DEFAULT_PRIORITY = 100;

    public String name;

    public List<String> command = new ArrayList<>();

    public boolean integrationTest = true;

    public boolean unitTest = true;

    public PullPolicy imagePull = PullPolicy.DEFAULT;

    public Duration imagePullDuration;

    public Integer priority = DEFAULT_PRIORITY;

    public String image;

    public String waitLogRegex;

    public int waitLogTimes = 1;

    public boolean log = true;

    public Map<String, String> environments;

    public Map<String, String> volumes;

    public Map<String, String> ports;

    public Variables commonVariables = new Variables("test.property.", "test.env.");

    public Variables unitVariables = new Variables("test.unit.property.", "test.unit.env.");

    public Variables integrationVariables = new Variables("test.integration.property.", "test.integration.env.");

    public boolean fixedPorts = false;

    ContainerConfig(String name, Map<String, Object> data) {
        this.name = name;
        load(data);
    }

    public static ContainerConfig createContainerProperties(String name, Map<String, Object> data) {
        return new ContainerConfig(name, data);
    }

    private void load(Map<String, Object> data) {

        // docker image
        image = (String) data.get("image");
        // docker compose environments
        environments = getMap(data, "environment");
        // docker compose volumes
        volumes = getMapFromList(data, "volumes", ":");
        // docker compose ports
        ports = getMapFromList(data, "ports", ":");
        // command
        Object cmd = data.get("command");
        if (cmd instanceof String) {
            command = Collections.singletonList((String) cmd);
        } else {
            command = getList(data, "command");
        }


        // labels
        Map<String, String> labels = getMapFromList(data, "labels", "=");
        if (!labels.isEmpty()) {
            // check if the service is only for the integration test
            integrationTest = getLabelBoolean(labels, "test.integration", true);
            unitTest = getLabelBoolean(labels, "test.unit", true);

            // image pull policy
            imagePull = PullPolicy.valueOf(labels.getOrDefault("test.image.pull", PullPolicy.DEFAULT.name()));
            if (imagePull == PullPolicy.MAX_AGE) {
                imagePullDuration = Duration.parse(labels.getOrDefault("test.image.pull.max_age", "PT10"));
            }

            // wait log rule
            waitLogRegex = labels.getOrDefault("test.Wait.forLogMessage.regex", null);
            waitLogTimes = getLabelInteger(labels, "test.Wait.forLogMessage.times", 1);

            // update log flag
            log = getLabelBoolean(labels, "test.Log", true);

            // update priority
            priority = getLabelInteger(labels, "test.priority", DEFAULT_PRIORITY);

            // fixed ports
            fixedPorts = getLabelBoolean(labels, "test.ports.fixed", false);
        }

        // test properties store in the labels
        labels.forEach((k, v) -> {
            if (!commonVariables.readVariable(k,v)) {
                if (!unitVariables.readVariable(k,v)) {
                    integrationVariables.readVariable(k,v);
                }
            }
        });

    }



    private static Map<String, String> getMap(Map<String, Object> properties, String key) {
        Object map = properties.get(key);
        if (map instanceof Map) {
            return (Map<String, String>) map;
        }
        return Collections.emptyMap();
    }

    private static List<String> getList(Map<String, Object> properties, String key) {
        Object list = properties.get(key);
        if (list instanceof List) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    private static Map<String, String> getMapFromList(Map<String, Object> properties, String key, String regex) {
        List<String> list = getList(properties, key);
        if (!list.isEmpty()) {
            return list.stream().map(s -> s.split(regex, 2))
                    .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
        }
        return Collections.emptyMap();
    }

    private static boolean getLabelBoolean(Map<String, String> labels, String name, boolean defaultValue) {
        return Boolean.parseBoolean(
                labels.getOrDefault(name, Boolean.toString(defaultValue))
        );
    }

    private static int getLabelInteger(Map<String, String> labels, String name, int defaultValue) {
        return Integer.parseInt(
                labels.getOrDefault(name, Integer.toString(defaultValue))
        );
    }


    public enum PullPolicy {

        DEFAULT,

        ALWAYS,

        MAX_AGE
    }

    public static class Variables {

        public String propertyPrefix;

        public List<TestProperty> properties = new ArrayList<>();

        public String envPrefix;

        public List<TestProperty> environments = new ArrayList<>();

        public Variables(String pf, String ep) {
            propertyPrefix = pf;
            envPrefix = ep;
        }

        public boolean readVariable(String key, String value) {
            if (key.startsWith(propertyPrefix)) {
                String k = key.substring(propertyPrefix.length());
                properties.add(TestPropertyLoader.createTestProperty(k, value));
                return true;
            }
            if (key.startsWith(envPrefix)) {
                String k = key.substring(envPrefix.length());
                environments.add(TestPropertyLoader.createTestProperty(k, value));
                return true;
            }
            return false;
        }
    }
}
