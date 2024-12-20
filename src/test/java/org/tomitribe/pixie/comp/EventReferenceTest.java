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
import org.tomitribe.pixie.observer.Event;
import org.tomitribe.pixie.observer.Observes;
import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.util.Join;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public class EventReferenceTest extends Assert {

    private static final List<String> events = new ArrayList();

    @Test
    public void test() {
        final Properties properties = new Properties();
        properties.put("thing", "new://" + EventReference.class.getName());
        final System system = new System(properties);
        final EventReference eventReference = system.get(EventReference.class);

        assertNotNull(eventReference);
        assertNotNull(eventReference.getColorConsumer());

        events.clear();
        eventReference.fire(new Color("blue"));
        assertEquals("", Join.join("\n", events));

        system.get(ColorObserver.class);

        events.clear();
        eventReference.fire(new Color("green"));
        assertEquals("OrangeObserver{} - Color{name='green'}", Join.join("\n", events));

    }


    public static class EventReference {

        private final Consumer<Color> colorConsumer;

        public EventReference(@Event final Consumer<Color> colorConsumer) {
            this.colorConsumer = colorConsumer;
        }

        public Consumer<Color> getColorConsumer() {
            return colorConsumer;
        }

        public void fire(final Color color) {
            colorConsumer.accept(color);
        }
    }

    public static class ColorObserver {

        public void observes(@Observes Color color) {
            events.add("OrangeObserver{} - " + color);
        }
    }

    public static class Color {
        private final String name;

        public Color(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Color{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}