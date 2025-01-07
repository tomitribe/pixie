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

import org.junit.Test;
import org.tomitribe.pixie.comp.Component;
import org.tomitribe.pixie.comp.ConstructionFailedException;
import org.tomitribe.pixie.comp.Default;
import org.tomitribe.pixie.comp.Name;
import org.tomitribe.pixie.comp.Nullable;
import org.tomitribe.pixie.comp.Option;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InstanceBuilderTest {

    @Test
    public void options() throws Exception {

        final Address address = Instance.builder(Address.class)
                .option("street", "820 Roosevelt Street")
                .option("city", "River Falls")
                .option("state", "WI")
                .option("zipcode", "54022")
                .build();

        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertEquals("River Falls", address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());
    }

    @Test
    public void optionStronglyTyped() throws Exception {

        final Address address = Instance.builder(Address.class)
                .option("street", "820 Roosevelt Street")
                .option("city", "River Falls")
                .option("state", State.WI)
                .option("zipcode", 54022)
                .build();

        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertEquals("River Falls", address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());
    }

    /**
     * Here we set a component value using an actual instance of the thing we need
     *
     * Internally, Pixie can only support references to things by name or type so
     * we
     * @throws Exception
     */
    @Test
    public void component() throws Exception {

        final Person jane = Instance.builder(Person.class)
                .option("age", "37")
                .comp("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

        assertNotNull(jane);
        assertEquals("instance", jane.getName());
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
    public void componentRefByType() throws Exception {

        final Person person = Instance.builder(Person.class)
                .option("age", "37")
                .add("anything", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

        assertNotNull(person);
        assertEquals("instance", person.getName());
        assertEquals(37, person.getAge().intValue());

        final Address address = person.getAddress();
        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertEquals("River Falls", address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());

    }

    /**
     * Specify the value of the objects @Name parameter via passing
     * it in via the builder(class, string) method
     */
    @Test
    public void name() throws Exception {

        final Person jane = Instance.builder(Person.class, "jane")
                .option("age", "37")
                .comp("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

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

    /**
     * Do not specify the component name for our reference to Address.
     * Let the system resolve a valid value by type alone
     */
    @Test
    public void typeRef() throws Exception {

        final Person person = Instance.builder(Person.class)
                .option("age", "37")
                .add("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

        assertNotNull(person);
        assertEquals("instance", person.getName());
        assertEquals(37, person.getAge().intValue());

        final Address address = person.getAddress();
        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertEquals("River Falls", address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());
    }

    /**
     * Do not specify a name for the Address instance we add AND
     * Do not specify the component name for our reference to Address.
     */
    @Test
    public void unamedTypeRef() throws Exception {

        final Person person = Instance.builder(Person.class)
                .option("age", "37")
                .add(new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

        assertNotNull(person);
        assertEquals("instance", person.getName());
        assertEquals(37, person.getAge().intValue());

        final Address address = person.getAddress();
        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertEquals("River Falls", address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());
    }

    @Test(expected = ConstructionFailedException.class)
    public void failOnUnusedProperties() throws Exception {

        Instance.builder(Person.class)
                .option("age", "37")
                .option("height", "70")
                .add("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

    }

    @Test
    public void warnOnUnusedProperties() throws Exception {

        final Person person = Instance.builder(Person.class)
                .warnOnUnusedProperties()
                .option("age", "37")
                .option("height", "70")
                .add("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

        assertNotNull(person);
        assertEquals("instance", person.getName());
        assertEquals(37, person.getAge().intValue());

        final Address address = person.getAddress();
        assertNotNull(address);
        assertEquals("820 Roosevelt Street", address.getStreet());
        assertEquals("River Falls", address.getCity());
        assertEquals(State.WI, address.getState());
        assertEquals(54022, address.getZipcode());
        assertEquals("USA", address.getCountry());
    }

    public static class Person {

        private final String name;
        private final Integer age;
        private final Address address;

        public Person(@Name final String name,
                      @Option("age") @Nullable final Integer age,
                      @Component("address") final Address address) {
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

    public static class Address {
        private final String street;
        private final String city;
        private final State state;
        private final int zipcode;
        private final String country;

        public Address(@Option("street") @Nullable final String street,
                       @Option("city") @Nullable final String city,
                       @Option("state") @Nullable final State state,
                       @Option("zipcode") @Nullable final int zipcode,
                       @Option("country") @Default("USA") final String country) {
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
