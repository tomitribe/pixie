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

import java.util.Properties;

public class SystemBuilderTest extends Assert {

    @Test
    public void test() throws Exception {
        final System system = System.builder()

                .definition(Person.class, "jane")
                .param("age", 37)
                .comp("address", "home")

                .definition(Address.class, "home")
                .param("street", "820 Roosevelt Street")
                .param("city", "River Falls")
                .param("state", "WI")
                .param("zipcode", "54022")

                .build();

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

    /**
     * The name for the 'address' reference in Person is not specified
     * @throws Exception
     */
    @Test
    public void typeRef() throws Exception {
        final System system = System.builder()

                .definition(Person.class, "jane")
                .param("age", 37)

                .definition(Address.class, "home")
                .param("street", "820 Roosevelt Street")
                .param("city", "River Falls")
                .param("state", "WI")
                .param("zipcode", "54022")

                .build();

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

    /**
     * The name for the 'address' reference in Person is not specified AND
     * The name for the Address definition is not specified
     * @throws Exception
     */
    @Test
    public void typeRef2() throws Exception {
        final System system = System.builder()

                .definition(Person.class, "jane")
                .param("age", 37)

                .definition(Address.class)
                .param("street", "820 Roosevelt Street")
                .param("city", "River Falls")
                .param("state", "WI")
                .param("zipcode", "54022")

                .build();

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

    /**
     * The name for the 'address' reference in Person is not specified AND
     * The Address instance is added and not built by Pixie
     */
    @Test
    public void add() throws Exception {
        final System system = System.builder()
                .add(new Address("820 Roosevelt Street","River Falls", State.WI,54022, "USA"))

                .definition(Person.class, "jane")
                .param("age", 37)
                .build();

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

    /**
     * Here we set a component value using an actual instance of the thing we need
     *
     * Internally, Pixie can only support references to things by name or type so
     * we
     * @throws Exception
     */
    @Test
    public void component() throws Exception {

        final System system = System.builder()
                .definition(Person.class)
                .param("age", "37")
                .comp("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

        final Person jane = system.get(Person.class);

        assertNotNull(jane);
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
     * Here we set a component value using an actual instance of the thing we need
     *
     * Internally, Pixie can only support references to things by name or type so
     * we
     * @throws Exception
     */
    @Test
    public void optional() throws Exception {

        final System system = System.builder()
                .definition(Person.class)
                .optional("age", "37")
                .optional("address", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .optional("direction", new Address("820 Roosevelt Street", "River Falls", State.WI, 54022, "USA"))
                .build();

        final Person jane = system.get(Person.class);

        assertNotNull(jane);
        assertEquals(37, jane.getAge().intValue());

        final Address address = jane.getAddress();
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