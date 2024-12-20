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
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

public class MissingComponentClassExceptionTest extends Assert {

    private MissingComponentClassException e;

    @Before
    public void setup() {
        try {
            final Properties properties = new Properties();
            properties.put("color", "new://io.superbiz.Red");
            new System(properties);
            throw new AssertionError("MissingComponentClassException should have been thrown");
        } catch (MissingComponentClassException e) {
            this.e = e;
        }
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals("Cannot load component class: io.superbiz.Red", e.getMessage());
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals("io.superbiz.Red", e.getComponent());
    }

}