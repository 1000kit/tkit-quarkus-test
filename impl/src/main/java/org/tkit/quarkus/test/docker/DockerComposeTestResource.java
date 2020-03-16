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

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The docker compose test resource.
 */
public class DockerComposeTestResource implements QuarkusTestResourceLifecycleManager{

    /**
     * The docker test environment.
     */
    protected DockerTestEnvironment environment;

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> start() {
        environment = new DockerTestEnvironment();
        environment.start();
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        environment.stop();
    }

    /**
     * Inject all {@link DockerComposeService} in the test class.
     * @param testInstance the test instance
     */
    @Override
    public void inject(Object testInstance) {
        List<Field> fields = getDockerComposeServiceFields(testInstance.getClass());
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                DockerComposeService s = environment.getService(f.getAnnotation(DockerService.class).value());
                f.set(testInstance, s);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Gets all docker compose fields for the class.
     *
     * @param clazz the test class.
     * @return the corresponding list of fields.
     */
    private static List<Field> getDockerComposeServiceFields(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        List<Field> result = new ArrayList<>(getDockerComposeServiceFields(clazz.getSuperclass()));
        result.addAll(
                Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> DockerComposeService.class.isAssignableFrom(f.getType()))
                        .filter(f -> f.getAnnotation(DockerService.class) != null)
                        .filter(f -> !f.getAnnotation(DockerService.class).value().isEmpty())
                        .collect(Collectors.toList())
        );
        return result;
    }

}
