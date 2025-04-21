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
import org.tomitribe.pixie.Event;
import org.tomitribe.pixie.Observes;
import org.tomitribe.pixie.System;
import org.tomitribe.util.Join;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public class MultipleConsumersTest extends Assert {

    private static final List<String> events = new ArrayList();

    @Test
    public void test() {
        final System system = System.builder()
                .definition(EventReference.class, "one")
                .definition(RedObserver.class, "red")
                .definition(GreenObserver.class, "green")
                .definition(BlueObserver.class, "blue")
                .build();

        final EventReference eventReference = system.get(EventReference.class);

        assertNotNull(eventReference);
        assertNotNull(eventReference.getColorConsumer());

        events.clear();
        eventReference.fire(new Color("blue"));
        assertEquals("RedObserver{} - Color{name='blue'}\n" +
                "GreenObserver{} - Color{name='blue'}\n" +
                "BlueObserver{} - Color{name='blue'}", Join.join("\n", events));

        system.get(GreenObserver.class);

        events.clear();

        final Properties properties = new Properties();
        properties.put("purple", "new://" + PurpleObserver.class.getName());
        system.load(properties);

        eventReference.fire(new Color("green"));

        assertEquals("RedObserver{} - Color{name='green'}\n" +
                "GreenObserver{} - Color{name='green'}\n" +
                "BlueObserver{} - Color{name='green'}\n" +
                "PurpleObserver{} - Color{name='green'}", Join.join("\n", events));

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

    public static class RedObserver {

        public void observes(@Observes Color color) {
            events.add(this.getClass().getSimpleName() + "{} - " + color);
        }
    }

    public static class GreenObserver {

        public void observes(@Observes Color color) {
            events.add(this.getClass().getSimpleName() + "{} - " + color);
        }
    }

    public static class BlueObserver {

        public void observes(@Observes Color color) {
            events.add(this.getClass().getSimpleName() + "{} - " + color);
        }
    }

    public static class PurpleObserver {

        public void observes(@Observes Color color) {
            events.add(this.getClass().getSimpleName() + "{} - " + color);
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