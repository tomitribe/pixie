/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tomitribe.pixie.comp;

import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.System;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class SystemReferenceTest extends Assert {

    @Test
    public void test() {
        final Properties properties = new Properties();
        properties.put("thing", "new://" + SystemReference.class.getName());
        final System system = new System(properties);
        final SystemReference systemReference = system.get(SystemReference.class);

        assertNotNull(systemReference);
        assertSame(system, systemReference.getSystem());
    }

    public static class SystemReference {

        private final System system;

        public SystemReference(final @Component("system") System system) {
            this.system = system;
        }

        public System getSystem() {
            return system;
        }
    }
}