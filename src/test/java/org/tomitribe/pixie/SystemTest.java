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
import org.tomitribe.util.IO;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Properties;
import java.util.function.BinaryOperator;

public class SystemTest extends Assert {

    private final String config = "" +
            "jane=new://org.tomitribe.pixie.SystemTest$Person\n" +
            "jane.age = 37\n" +
            "jane.address=@home\n" +
            "\n" +
            "home=new://org.tomitribe.pixie.SystemTest$Address\n" +
            "home.street=820 Roosevelt Street\n" +
            "home.city=River Falls\n" +
            "home.state=WI\n" +
            "home.zipcode=54022\n";

    private final String configMissingProperties = "" +
            "jane=new://org.tomitribe.pixie.SystemTest$Person\n" +
            "jane.address=@home\n" +
            "\n" +
            "home=new://org.tomitribe.pixie.SystemTest$Address\n" +
            "home.street=820 Roosevelt Street\n" +
            "home.state=WI\n" +
            "home.zipcode=54022\n";

    private final String configAdditionalProperties = "" +
            "jane=new://org.tomitribe.pixie.SystemTest$Person\n" +
            "jane.age = 37\n" +
            "jane.lastname = test\n" +
            "jane.address=@home\n" +
            "\n" +
            "home=new://org.tomitribe.pixie.SystemTest$Address\n" +
            "home.street=820 Roosevelt Street\n" +
            "home.city=River Falls\n" +
            "home.state=WI\n" +
            "home.zipcode=54022\n" +
            "home.planet=Earth\n";

    @Test
    public void test() throws Exception {
        final Properties properties = new Properties();
        properties.load(IO.read(config));

        final System system = new System(properties);

        final Person jane = system.get(Person.class);

        assertNotNull(jane);
        assertEquals("jane", jane.getName());
        assertEquals(37, jane.getAge().intValue());

        final Address address = jane.getAddress();
        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertEquals("River Falls", address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());
    }

    @Test
    public void customObject() throws Exception {
        final Properties properties = new Properties();
        properties.load(IO.read("" +
                "jane=new://org.tomitribe.pixie.SystemTest$Person\n" +
                "jane.age = 37\n" +
                "jane.address=@home\n"));

        final System system = new System();
        system.add("home", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"));
        system.load(properties);

        final Person jane = system.get(Person.class);

        assertNotNull(jane);
        assertEquals("jane", jane.getName());
        assertEquals(37, jane.getAge().intValue());

        final Address address = jane.getAddress();
        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertEquals("River Falls", address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());
    }

    @Test
    public void testAllowMissingProperties() throws Exception {
        final Properties properties = new Properties();
        properties.load(IO.read(configMissingProperties));

        final System system = new System(properties);

        final Person jane = system.get(Person.class);

        assertNotNull(jane);
        assertEquals("jane", jane.getName());
        assertNull(jane.getAge());

        final Address address = jane.getAddress();
        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertNull(address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());
    }

    @Test
    public void testAllowUnusedProperties() throws Exception {
        final Properties properties = new Properties();
        properties.load(IO.read(configAdditionalProperties));

        final System system = new System(properties, true);

        final Person jane = system.get(Person.class);

        assertNotNull(jane);
        assertEquals("jane", jane.getName());
        assertEquals(37, jane.getAge().intValue());

        final Address address = jane.getAddress();
        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertEquals("River Falls", address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());
    }

    @Test
    public void getAnnotated() throws Exception {
        final Properties properties = new Properties();
        properties.load(IO.read("" +
                "red=new://org.tomitribe.pixie.SystemTest$Red\n" +
                "green=new://org.tomitribe.pixie.SystemTest$Green\n" +
                "blue=new://org.tomitribe.pixie.SystemTest$Blue\n" +
                "circle=new://org.tomitribe.pixie.SystemTest$Circle\n" +
                "triangle=new://org.tomitribe.pixie.SystemTest$Triangle\n" +
                "square=new://org.tomitribe.pixie.SystemTest$Square\n"));

        final System system = new System();
        system.load(properties);

        final List<Object> colors = system.getAnnotated(Color.class);
        assertClasses(colors, "org.tomitribe.pixie.SystemTest$Blue\n" +
                "org.tomitribe.pixie.SystemTest$Green\n" +
                "org.tomitribe.pixie.SystemTest$Red");

        final List<Object> shapes = system.getAnnotated(Shape.class);
        assertClasses(shapes, "org.tomitribe.pixie.SystemTest$Circle\n" +
                "org.tomitribe.pixie.SystemTest$Square\n" +
                "org.tomitribe.pixie.SystemTest$Triangle");
    }

    private void assertClasses(final List<Object> colors, final String expected) {
        final String actual = colors.stream()
                .map(Object::getClass)
                .map(Class::getName)
                .sorted()
                .reduce((s, s2) -> s + "\n" + s2)
                .get();

        assertEquals(expected, actual);
    }

    public static class Person {

        private final String name;
        private final Integer age;
        private final Address address;

        public Person(@Name final String name,
                      @Param("age") @Nullable final Integer age,
                      @Param("address")  @Component final Address address) {
            this.name = name;
            this.age = age;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public Address getAddress() {
            return address;
        }

        @Override
        public String toString() {
            return "Person{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", address=" + address +
                    '}';
        }
    }

    @Color
    public static class Red {
    }

    @Color
    public static class Green {
    }

    @Color
    public static class Blue {
    }

    @Shape
    public static class Square {
    }

    @Shape
    public static class Circle {
    }

    @Shape
    public static class Triangle {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Color {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Shape {

    }

    public static class Address {
        private final String street;
        private final String city;
        private final State state;
        private final int zipcode;
        private final String country;

        public Address(@Param("street") @Nullable final String street,
                       @Param("city") @Nullable final String city,
                       @Param("state") @Nullable final State state,
                       @Param("zipcode") @Nullable final int zipcode,
                       @Param("country") @Default("USA") final String country) {
            this.street = street;
            this.city = city;
            this.state = state;
            this.zipcode = zipcode;
            this.country = country;
        }

        public String getStreet() {
            return street;
        }

        public String getCity() {
            return city;
        }

        public State getState() {
            return state;
        }

        public int getZipcode() {
            return zipcode;
        }

        public String getCountry() {
            return country;
        }

        @Override
        public String toString() {
            return "Address{" +
                    "street='" + street + '\'' +
                    ", city='" + city + '\'' +
                    ", state=" + state +
                    ", zipcode=" + zipcode +
                    ", country='" + country + '\'' +
                    '}';
        }
    }

    public static enum State {
        WI, MN, CA;
    }

}