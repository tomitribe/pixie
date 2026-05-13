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

import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;
import org.tomitribe.util.IO;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class ParamListTest extends Assert {

    @Test
    public void listOfURI() throws IOException {
        final String config = "" +
                "foo = new://" + UriHolder.class.getName() + "\n" +
                "foo.uris = http://one, http://two/dos\n";

        final Properties properties = new Properties();
        properties.load(IO.read(config));

        final System system = new System(properties);
        final UriHolder actual = system.get(UriHolder.class);

        assertNotNull(actual);
        assertEquals(Arrays.asList(URI.create("http://one"), URI.create("http://two/dos")), actual.getUris());
    }

    @Test
    public void listOfString() throws IOException {
        final String config = "" +
                "foo = new://" + StringListHolder.class.getName() + "\n" +
                "foo.names = alpha, beta, gamma\n";

        final Properties properties = new Properties();
        properties.load(IO.read(config));

        final System system = new System(properties);
        final StringListHolder actual = system.get(StringListHolder.class);

        assertEquals(Arrays.asList("alpha", "beta", "gamma"), actual.getNames());
    }

    @Test
    public void setOfTimeUnit() throws IOException {
        final String config = "" +
                "foo = new://" + TimeUnitSetHolder.class.getName() + "\n" +
                "foo.units = SECONDS, MINUTES, HOURS\n";

        final Properties properties = new Properties();
        properties.load(IO.read(config));

        final System system = new System(properties);
        final TimeUnitSetHolder actual = system.get(TimeUnitSetHolder.class);

        final Set<TimeUnit> expected = new TreeSet<>(Arrays.asList(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS));
        assertEquals(expected, new TreeSet<>(actual.getUnits()));
    }

    @Test
    public void mapOfStringToInteger() {
        final Properties properties = new Properties();
        properties.setProperty("foo", "new://" + MapHolder.class.getName());
        properties.setProperty("foo.counts", "a=1\nb=2\nc=3");

        final System system = new System(properties);
        final MapHolder actual = system.get(MapHolder.class);

        final Map<String, Integer> counts = actual.getCounts();
        assertEquals(Integer.valueOf(1), counts.get("a"));
        assertEquals(Integer.valueOf(2), counts.get("b"));
        assertEquals(Integer.valueOf(3), counts.get("c"));
    }

    public static class UriHolder {
        private final List<URI> uris;

        public UriHolder(@Param("uris") final List<URI> uris) {
            this.uris = uris;
        }

        public List<URI> getUris() {
            return uris;
        }
    }

    public static class StringListHolder {
        private final List<String> names;

        public StringListHolder(@Param("names") final List<String> names) {
            this.names = names;
        }

        public List<String> getNames() {
            return names;
        }
    }

    public static class TimeUnitSetHolder {
        private final Set<TimeUnit> units;

        public TimeUnitSetHolder(@Param("units") final Set<TimeUnit> units) {
            this.units = units;
        }

        public Set<TimeUnit> getUnits() {
            return units;
        }
    }

    public static class MapHolder {
        private final Map<String, Integer> counts;

        public MapHolder(@Param("counts") final Map<String, Integer> counts) {
            this.counts = counts;
        }

        public Map<String, Integer> getCounts() {
            return counts;
        }
    }
}
