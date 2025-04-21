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
package org.tomitribe.pixie;

import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.util.Join;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public class SystemSubclassTest extends Assert {

    private static final List<String> intercepted = new ArrayList();
    private static final List<String> events = new ArrayList();

    @Test
    public void test() {


        final System system = new SystemSubclass();

        {
            final Properties props = new Properties();
            props.put("one", "new://" + EventReference.class.getName());
            props.put("red", "new://" + RedObserver.class.getName());
            props.put("green", "new://" + GreenObserver.class.getName());
            props.put("blue", "new://" + BlueObserver.class.getName());
            system.load(props);
        }

        assertEquals("" +
                        "fireEvent:ComponentAdded{type=class org.tomitribe.pixie.SystemSubclassTest$SystemSubclass, component=org.tomitribe.pixie.SystemSubclassTest$SystemSubclass}\n" +
                        "fireEvent:ComponentAdded{type=class org.tomitribe.pixie.SystemSubclassTest$RedObserver, component=org.tomitribe.pixie.SystemSubclassTest$RedObserver}\n" +
                        "fireEvent:ComponentAdded{type=class org.tomitribe.pixie.SystemSubclassTest$GreenObserver, component=org.tomitribe.pixie.SystemSubclassTest$GreenObserver}\n" +
                        "fireEvent:ComponentAdded{type=class org.tomitribe.pixie.SystemSubclassTest$BlueObserver, component=org.tomitribe.pixie.SystemSubclassTest$BlueObserver}\n" +
                        "consumersOf:class org.tomitribe.pixie.SystemSubclassTest$Color\n" +
                        "fireEvent:ComponentAdded{type=class org.tomitribe.pixie.SystemSubclassTest$EventReference, component=org.tomitribe.pixie.SystemSubclassTest$EventReference}",
                Join.join("\n", intercepted));

        events.clear();
        intercepted.clear();

        system.fireEvent(new Color("red"));
        assertEquals("RedObserver{} - Color{name='red'}\n" +
                "GreenObserver{} - Color{name='red'}\n" +
                "BlueObserver{} - Color{name='red'}", Join.join("\n", events));

        assertEquals("fireEvent:Color{name='red'}", Join.join("\n", intercepted));

        events.clear();
        intercepted.clear();

        final EventReference eventReference = system.get(EventReference.class);

        assertNotNull(eventReference);
        assertNotNull(eventReference.getColorConsumer());

        events.clear();
        intercepted.clear();

        eventReference.fire(new Color("blue"));
        assertEquals("RedObserver{} - Color{name='blue'}\n" +
                "GreenObserver{} - Color{name='blue'}\n" +
                "BlueObserver{} - Color{name='blue'}", Join.join("\n", events));

        assertEquals("accept:Color{name='blue'}", Join.join("\n", intercepted));

        events.clear();
        intercepted.clear();

        final Properties properties = new Properties();
        properties.put("purple", "new://" + PurpleObserver.class.getName());
        system.load(properties);

        assertEquals("", Join.join("\n", events));

        assertEquals("" +
                "fireEvent:ComponentAdded{type=class org.tomitribe.pixie.SystemSubclassTest$PurpleObserver, component=org.tomitribe.pixie.SystemSubclassTest$PurpleObserver}", Join.join("\n", intercepted));

        events.clear();
        intercepted.clear();

        eventReference.fire(new Color("green"));

        assertEquals("RedObserver{} - Color{name='green'}\n" +
                "GreenObserver{} - Color{name='green'}\n" +
                "BlueObserver{} - Color{name='green'}\n" +
                "PurpleObserver{} - Color{name='green'}", Join.join("\n", events));

        assertEquals("accept:Color{name='green'}", Join.join("\n", intercepted));

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

    public static class SystemSubclass extends System {
        @Override
        public <E> E fireEvent(final E event) {
            intercepted.add("fireEvent:" + event);
            return super.fireEvent(event);
        }

        @Override
        public <E> Consumer<E> consumersOf(final Class<E> eventClass) {
            intercepted.add("consumersOf:" + eventClass);

            final Consumer<E> consumer = super.consumersOf(eventClass);
            return e -> {
                intercepted.add("accept:" + e);
                consumer.accept(e);
            };
        }

    }
}