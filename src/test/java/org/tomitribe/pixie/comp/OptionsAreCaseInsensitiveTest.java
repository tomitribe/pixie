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

import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class OptionsAreCaseInsensitiveTest extends Assert {

    @Test
    public void lowercase() {
        final System system = new System(new Properties(){{
            put("myclass", "new://" + MyClass.class.getName());
            put("myclass.lowercase", "lowercase");
            put("myclass.uppercase", "uppercase");
            put("myclass.camelcase", "camelcase");
            put("myclass.randomcase", "randomcase");
        }});
        assertNotNull(system.get(MyClass.class));
    }

    @Test
    public void upercase() {
        final System system = new System(new Properties(){{
            put("myclass", "new://" + MyClass.class.getName());
            put("myclass.LOWERCASE", "lowercase");
            put("myclass.UPPERCASE", "uppercase");
            put("myclass.CAMELCASE", "camelcase");
            put("myclass.RANDOMCASE", "randomcase");
        }});
        assertNotNull(system.get(MyClass.class));
    }

    @Test
    public void camercase() {
        final System system = new System(new Properties(){{
            put("myclass", "new://" + MyClass.class.getName());
            put("myclass.lowerCase", "lowercase");
            put("myclass.upperCase", "uppercase");
            put("myclass.camelCase", "camelcase");
            put("myclass.randomCase", "randomcase");
        }});
        assertNotNull(system.get(MyClass.class));
    }

    @Test
    public void randomcase() {
        final System system = new System(new Properties(){{
            put("myclass", "new://" + MyClass.class.getName());
            put("myclass.loWercaSe", "lowercase");
            put("myclass.upperCASE", "uppercase");
            put("myclass.camELCase", "camelcase");
            put("myclass.RaNdomCase", "randomcase");
        }});
        assertNotNull(system.get(MyClass.class));
    }

    public static class MyClass {

        public MyClass(
                @Param("lowercase") final String lowercase,
                @Param("UPPERCASE") final String uppercase,
                @Param("camelCase") final String camelcase,
                @Param("rAndOmCasE") final String randomcase
        ) {
        }
    }

}
