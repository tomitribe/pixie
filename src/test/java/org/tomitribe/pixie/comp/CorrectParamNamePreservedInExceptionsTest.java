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
import org.junit.Test;
import org.tomitribe.util.IO;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class CorrectParamNamePreservedInExceptionsTest extends org.junit.Assert {


    private final String config = "" +
            "joe = new://" + DataTypes.class.getName() + "\n" +
            "joe.atIMEUNit = love\n";

    @Test
    public void test() throws IOException {
        final Properties properties = new Properties();
        properties.load(IO.read(config));


        try {
            new System(properties);
            fail();
        } catch (ConstructionFailedException e) {
            assertEquals(e.getCause().getClass(), InvalidParamValueException.class);
            final InvalidParamValueException e1 = (InvalidParamValueException) e.getCause();
            assertEquals(e1.getParamName(), "aTimeUnit");
            assertEquals(e1.getParamValue(), "love");
            assertEquals(e1.getParamType(), TimeUnit.class);
        }
    }


    public static class DataTypes {

        public DataTypes(@Param("aTimeUnit") TimeUnit aTimeUnit) {
        }
    }
}
