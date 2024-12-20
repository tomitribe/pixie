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

import org.tomitribe.pixie.System;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class NullableComponentTest extends Assert {

    @Test
    public void nullableOption() {
        final System system = new System(new Properties());
        final NullableOpt nullableOpt = system.get(NullableOpt.class);

        assertNotNull(nullableOpt);
        assertNull(nullableOpt.getName()); // name not set
        assertNull(nullableOpt.getCreds());
    }

    @Test
    public void setNullableOption() {
        final System system = new System(new Properties() {{
            setProperty("foo", "new://" + NullableOpt.class.getName());
            setProperty("foo.credentials", "@creds");

            setProperty("creds", "new://" + BasicAuth.class.getName());
            setProperty("creds.username", "bla");
            setProperty("creds.password", "secret");
        }});
        final NullableOpt nullableOpt = system.get(NullableOpt.class);

        assertNotNull(nullableOpt);
        assertEquals("foo", nullableOpt.getName());
    }

    public static class NullableOpt {
        private final String name;
        private final BasicAuth creds;

        public NullableOpt(@Name final String name,
                           @Component("credentials") @Nullable final BasicAuth creds) {

            this.name = name;
            this.creds = creds;
        }

        public String getName() {
            return name;
        }

        public BasicAuth getCreds() {
            return creds;
        }
    }

    public static class BasicAuth {

        public BasicAuth (@Option("username") @Nullable final String username,
                          @Option("password") @Nullable final String password) {

        }

    }
}