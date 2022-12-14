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

import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.junit.NativeImageTest;
import io.quarkus.test.junit.NativeTestExtension;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.tkit.quarkus.test.docker.DockerTestEnvironment.SYS_PROP_TEST_INTEGRATION;

public class StartTestcontainersCondition implements ExecutionCondition {

    private static final Logger log = LoggerFactory.getLogger(StartTestcontainersCondition.class);

    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult
            .enabled("@NativeImageTest disable native image start");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<NativeImageTest> disabled = findAnnotation(element, NativeImageTest.class);
        if (disabled.isPresent()) {
            ExtensionContext root = context.getRoot();
            ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
            NativeTestExtension.ExtensionState state = store.get(NativeTestExtension.ExtensionState.class.getName(), NativeTestExtension.ExtensionState.class);
            if (state == null) {
                log.info("Tkit Testcontainers for @NativeImageTest starting ...");

                Optional<QuarkusTestcontainers> anno = findAnnotation(element, QuarkusTestcontainers.class);
                if (anno.isPresent()) {
                    QuarkusTestcontainers qtc = anno.get();
                    log.info("Tkit Testcontainers integration test: {}", qtc.integrationTest());
                    System.setProperty(SYS_PROP_TEST_INTEGRATION, "" + qtc.integrationTest());
                }

                TestResourceManager testResourceManager = new TestResourceManager(context.getRequiredTestClass());
                Map<String, String> systemProps = testResourceManager.start();

                state = createState(testResourceManager, systemProps);
                store.put(NativeTestExtension.ExtensionState.class.getName(), state);
                
                if (systemProps != null) {
                    systemProps.forEach(System::setProperty);
                }
                log.info("Tkit Testcontainers for @NativeImageTest started! System properties: {}", systemProps);
            }
        }
        return ENABLED;
    }

    public static NativeTestExtension.ExtensionState createState(TestResourceManager testResourceManager, Map<String, String> systemProps) {
        try {
            Closeable closable = () -> {
            };
            Constructor<?> constructor = NativeTestExtension.ExtensionState.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            return (NativeTestExtension.ExtensionState) constructor.newInstance(new NativeTestExtension(), testResourceManager, closable, systemProps);
        } catch (Exception ex) {
            throw new IllegalStateException("Error create state", ex);
        }
    }

}