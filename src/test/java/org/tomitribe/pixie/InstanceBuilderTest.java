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
import org.tomitribe.pixie.comp.ConstructionFailedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InstanceBuilderTest {

    @Test
    public void options() throws Exception {

        final Address address = Instance.builder(Address.class)
                .param("street", "820 Roosevelt Street")
                .param("city", "River Falls")
                .param("state", "WI")
                .param("zipcode", "54022")
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
                .param("street", "820 Roosevelt Street")
                .param("city", "River Falls")
                .param("state", State.WI)
                .param("zipcode", 54022)
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
                .param("age", "37")
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
                .param("age", "37")
                .optional(new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
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

    @Test
    public void addOptionAndComponent() throws Exception {

        final Person person = Instance.builder(Person.class)
                .optional("age", "37")
                .optional("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .optional("direction", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
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
                .param("age", "37")
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
                .param("age", "37")
                .optional("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
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
                .param("age", "37")
                .optional(new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
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
                .param("age", "37")
                .param("height", "70")
                .optional("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

    }

    @Test
    public void warnOnUnusedProperties() throws Exception {

        final Person person = Instance.builder(Person.class)
                .warnOnUnusedProperties()
                .param("age", "37")
                .param("height", "70")
                .optional("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
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
                      @Param("age") @Nullable final Integer age,
                      @Param("address") @Component final Address address) {
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
