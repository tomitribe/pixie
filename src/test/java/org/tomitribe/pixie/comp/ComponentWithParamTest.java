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

import org.junit.Test;
import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Default;
import org.tomitribe.pixie.Nullable;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;
import org.tomitribe.util.IO;

import java.io.IOException;
import java.util.Properties;

/**
 * Tests the functionality of removing the name for @Component and having now the annotation together with the @Param annotation
 * in constructor parameters.
 *
 * @Since 3.0 The @Component annotation now requires a @Param annotation to provide a name for the component reference.
 */
public class ComponentWithParamTest extends org.junit.Assert {


    private final String config = "" +
            "bob = new://" + Person.class.getName() + "\n" +
            "bob.name = Bob\n" +
            "bob.origin = @home\n" +
            "home = new://" + Address.class.getName() + "\n" +
            "home.city = Guatemala\n" +
            "home.country = GT\n";


    @Test
    public void testComponentWithParam() throws IOException {
        final Properties properties = new Properties();
        properties.load(IO.read(config));

        try {
            final System system = new System(properties);
            final Person actual = system.get(Person.class);
            assertNotNull(actual);
            assertEquals("Bob", actual.getName());
            assertNotNull(actual.getAddress());
            assertEquals("Guatemala", actual.getAddress().getCity());
            assertEquals("GT", actual.getAddress().getCountry());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }


    }

    @Test
    public void testNullableComponentWithParam() throws IOException {
        final Properties properties = new Properties();
        properties.load(IO.read("person = new://" + PersonWithNullableAddress.class.getName() + "\n" +
                "person.name = Ana\n"));

        final System system = new System(properties);
        final PersonWithNullableAddress person = system.get(PersonWithNullableAddress.class);

        assertNotNull(person);
        assertEquals("Ana", person.getName());
        assertNull(person.getAddress());
    }


    @Test
    public void testMultipleComponentsWithParam() throws IOException {
        final Properties properties = new Properties();
        properties.load(IO.read("person = new://" + PersonWithMultipleAddresses.class.getName() + "\n" +
                "person.name = Alex\n" +
                "person.home = @homeAddress\n" +
                "person.work = @workAddress\n" +
                "homeAddress = new://" + Address.class.getName() + "\n" +
                "homeAddress.city = HomeCity\n" +
                "homeAddress.country = HomeCountry\n" +
                "workAddress = new://" + Address.class.getName() + "\n" +
                "workAddress.city = WorkCity\n" +
                "workAddress.country = WorkCountry\n"));

        final System system = new System(properties);
        final PersonWithMultipleAddresses person = system.get(PersonWithMultipleAddresses.class);

        assertNotNull(person);
        assertNotNull(person.getHomeAddress());
        assertNotNull(person.getWorkAddress());
        assertEquals("HomeCity", person.getHomeAddress().getCity());
        assertEquals("WorkCity", person.getWorkAddress().getCity());
    }

    @Test
    public void testComponentWithParamUsingBuilder() {
        final System system = System.builder()
                .definition(Person.class, "bob")
                .param("name", "Bob")
                .comp("origin", "home")
                .definition(Address.class, "home")
                .param("city", "Guatemala")
                .param("country", "GT")
                .build();

        final Person actual = system.get(Person.class);
        assertNotNull(actual);
        assertEquals("Bob", actual.getName());
        assertNotNull(actual.getAddress());
        assertEquals("Guatemala", actual.getAddress().getCity());
        assertEquals("GT", actual.getAddress().getCountry());
    }


    @Test
    public void testInheritedComponentWithParam() throws IOException {
        final Properties properties = new Properties();
        properties.load(IO.read("child = new://" + ChildPerson.class.getName() + "\n" +
                "child.name = Thomas\n" +
                "child.location = @office\n" +
                "child.age = 30\n" +
                "office = new://" + Address.class.getName() + "\n" +
                "office.city = Seattle\n" +
                "office.country = USA\n"));

        final System system = new System(properties);
        final ChildPerson child = system.get(ChildPerson.class);

        assertNotNull(child);
        assertEquals("Thomas", child.getName());
        assertEquals(30, child.getAge());
        assertNotNull(child.getAddress());
        assertEquals("Seattle", child.getAddress().getCity());
        assertEquals("USA", child.getAddress().getCountry());
    }

    @Test
    public void testComponentWithParamAndDefault() throws IOException {
        final Properties properties = new Properties();
        properties.load(IO.read("person = new://" + PersonWithDefaultAddress.class.getName() + "\n" +
                "person.name = Maria\n" +
                "defaultAddress = new://" + Address.class.getName() + "\n" +
                "defaultAddress.city = DefaultCity\n" +
                "defaultAddress.country = DefaultCountry\n"));

        final System system = new System(properties);
        final PersonWithDefaultAddress person = system.get(PersonWithDefaultAddress.class);

        assertNotNull(person);
        assertNotNull(person.getAddress());
        assertEquals("DefaultCity", person.getAddress().getCity());
    }


    public static class Address {
        private final String city;
        private final String country;

        public Address(@Param("city") String city, @Param("country") String country) {
            this.city = city;
            this.country = country;
        }

        public String getCity() {
            return city;
        }

        public String getCountry() {
            return country;
        }
    }

    public static class Person {
        private final String name;
        private final Address address;

        public Person(@Param("name") String name, @Param("origin") @Component Address address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public Address getAddress() {
            return address;
        }
    }

    public static class PersonWithNullableAddress {
        private final String name;
        private final Address address;

        public PersonWithNullableAddress(@Param("name") String name,
                                         @Param("origin") @Component @Nullable Address address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public Address getAddress() {
            return address;
        }
    }


    public static class PersonWithMultipleAddresses {
        private final String name;
        private final Address homeAddress;
        private final Address workAddress;

        public PersonWithMultipleAddresses(@Param("name") String name,
                                           @Param("home") @Component Address homeAddress,
                                           @Param("work") @Component Address workAddress) {
            this.name = name;
            this.homeAddress = homeAddress;
            this.workAddress = workAddress;
        }

        public String getName() {
            return name;
        }

        public Address getHomeAddress() {
            return homeAddress;
        }

        public Address getWorkAddress() {
            return workAddress;
        }
    }


    public static class BasePersonWithAddress {
        private final String name;
        private final Address address;

        public BasePersonWithAddress(@Param("name") String name,
                                     @Param("location") @Component Address address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public Address getAddress() {
            return address;
        }
    }

    public static class ChildPerson extends BasePersonWithAddress {
        private final int age;

        public ChildPerson(@Param("name") String name,
                           @Param("location") @Component Address address,
                           @Param("age") int age) {
            super(name, address);
            this.age = age;
        }

        public int getAge() {
            return age;
        }
    }


    public static class PersonWithDefaultAddress {
        private final String name;
        private final Address address;

        public PersonWithDefaultAddress(@Param("name") String name,
                                        @Param("origin") @Component @Default("defaultAddress") Address address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public Address getAddress() {
            return address;
        }
    }

}
