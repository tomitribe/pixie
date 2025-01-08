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
import org.tomitribe.pixie.Default;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Option;
import org.tomitribe.pixie.System;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

public class MultipleComponentIssuesExceptionTest extends Assert {

    private MultipleComponentIssuesException e;
    // UnknownPropertyException
    // ComponentReferenceSyntaxException
    // MissingRequiredOptionException

    @Before
    public void setup() {
        try {
            final Properties properties = new Properties();
            properties.put("joe", "new://" + Person.class.getName());
            properties.put("thing", "new://" + MultipleComponentIssues.class.getName());
            properties.put("thing.color", "orange"); // UnknownPropertyException
            properties.put("thing.shape", "@red");  // UnknownPropertyException
            properties.put("thing.person", "joe");  // ComponentReferenceSyntaxException

            new System(properties);
            throw new AssertionError("ConstructionFailedException should have been thrown");
        } catch (ConstructionFailedException e) {
            assertNotNull(e.getCause());
            assertEquals(e.getComponent(), MultipleComponentIssues.class);

            assertTrue(e.getCause() instanceof MultipleComponentIssuesException);
            this.e = (MultipleComponentIssuesException) e.getCause();
        }
    }

    @Test
    public void testGetMessage() throws Exception {
        final String component = MultipleComponentIssues.class.getName();

        assertEquals("5 issues found\n" +
                " - UnknownPropertyException          : Unknown property `color = orange`\n" +
                " - UnknownPropertyException          : Unknown property `shape = @red`\n" +
                " - ComponentReferenceSyntaxException : Invalid property `person = joe` - Component references must start with @\n" +
                " - MissingRequiredOptionException    : Missing required option 'red' for component " + component + "\n" +
                " - MissingRequiredOptionException    : Missing required option 'crimson' for component " + component + "\n",
                e.getMessage());
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals(MultipleComponentIssues.class, e.getComponent());
    }

    @Test
    public void getIssues() throws Exception {
        final List<? extends Throwable> issues = e.getIssues();

        assertTrue(issues.get(0) instanceof UnknownPropertyException);
        assertEquals("color", ((UnknownPropertyException) issues.get(0)).getKey());

        assertTrue(issues.get(1) instanceof UnknownPropertyException);
        assertEquals("shape", ((UnknownPropertyException) issues.get(1)).getKey());

        assertTrue(issues.get(2) instanceof ComponentReferenceSyntaxException);
        assertEquals("person", ((ComponentReferenceSyntaxException) issues.get(2)).getKey());

        assertTrue(issues.get(3) instanceof MissingRequiredOptionException);
        assertEquals("red", ((MissingRequiredOptionException) issues.get(3)).getOptionName());

        assertTrue(issues.get(4) instanceof MissingRequiredOptionException);
        assertEquals("crimson", ((MissingRequiredOptionException) issues.get(4)).getOptionName());
    }

    public static class MultipleComponentIssues {

        public MultipleComponentIssues(
                final @Name String name,
                final @Component("person") Person badSyntax,
                final @Option("red") String missingOption1,    // MissingRequiredOptionException
                final @Option("crimson") String missingOption2, // MissingRequiredOptionException
                final @Option("green") @Default("thing") String green
        ) {
        }
    }

    public static class Person {

    }


}