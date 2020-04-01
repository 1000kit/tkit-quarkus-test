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

package org.tkit.quarkus.test.docker.properties;

import org.tkit.quarkus.test.docker.DockerTestEnvironment;

public class TestValueProperty extends TestProperty {

    String value;

    @Override
    public String getValue(DockerTestEnvironment environment) {
      return value;
    }

    public static TestValueProperty createTestProperty(String name, String data) {
        TestValueProperty r = new TestValueProperty();
        r.name = name;
        r.value = data;
        return r;
    }
}
