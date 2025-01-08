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

import org.tomitribe.pixie.Default;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Nullable;
import org.tomitribe.pixie.Option;
import org.tomitribe.pixie.System;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class NullableOptionTest extends Assert {

    @Test
    public void nullableOption() {
        final System system = new System(new Properties());
        final NullableOpt nullableOpt = system.get(NullableOpt.class);

        assertNotNull(nullableOpt);
        assertNull(nullableOpt.getName()); // name not set
        assertNull(nullableOpt.getPhone());
    }

    @Test
    public void setNullableOption() {
        final String expectedPhone = "001122334455";
        final System system = new System(new Properties() {{
            setProperty("foo", "new://" + NullableOpt.class.getName());
            setProperty("foo.phone", expectedPhone);
        }});
        final NullableOpt nullableOpt = system.get(NullableOpt.class);

        assertNotNull(nullableOpt);
        assertEquals("foo", nullableOpt.getName());
        assertEquals(expectedPhone, nullableOpt.getPhone());
    }

    @Test
    public void invalidNullableWithDefault () {
        final System system = new System(new Properties());

        try {
            system.get(InvalidNullableOpt.class);
            fail(InvalidNullableWithDefaultException.class.getSimpleName() + " should have been thrown");

        } catch (final ConstructionFailedException e) {
            assertTrue(InvalidNullableWithDefaultException.class.isInstance(e.getCause()));
            assertEquals("Invalid usage of @Nullable with @Default for option 'phone' with value: 0011223344",
                    e.getCause().getMessage());
        }
    }

    public static class NullableOpt {
        private final String name;
        private final String phone;

        public NullableOpt(@Name final String name,
                           @Option("phone") @Nullable final String phone) {

            this.name = name;
            this.phone = phone;
        }

        public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }
    }

    public static class InvalidNullableOpt {

        public InvalidNullableOpt(@Name final String name,
                                  @Option("phone") @Default("0011223344") @Nullable final String phone) {

        }
    }

}