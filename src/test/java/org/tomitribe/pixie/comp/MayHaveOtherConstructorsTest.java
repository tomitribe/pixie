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

public class MayHaveOtherConstructorsTest extends Assert {

    @Test
    public void test() {
        final Properties properties = new Properties();
        properties.put("number", 3);

        final System system = new System(properties);
        final MayHaveOtherConstructors actual = system.get(MayHaveOtherConstructors.class);

        assertEquals(3, actual.getNumber());
    }

    public static class MayHaveOtherConstructors {

        private final int number;

        public MayHaveOtherConstructors(final String something) {
            throw new AssertionError("This constructor should not be called");
        }

        public MayHaveOtherConstructors(final @Option("number") int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }
    }
}