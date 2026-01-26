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
import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Default;
import org.tomitribe.pixie.Event;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Nullable;
import org.tomitribe.pixie.Observes;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Asserts that the builder and factory can be in a different
 * class than the class built.
 */
public class BuilderClassTest extends Assert {


    @Test
    public void test() throws Exception {
        final System system = System.builder()

                .definition(LinkObserver.class, "links")

                .definition(PersonBuilder.class, "jane")
                .param("age", 37)
                .comp("address", "home")

                .definition(AddressBuilder.class, "home")
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

        final LinkObserver linkObserver = system.get(LinkObserver.class);
        assertNotNull(linkObserver);
        assertEquals(0, linkObserver.getUris().size());

        jane.getLinks().accept(URI.create("foo://bar?one=uno"));

        assertEquals(1, linkObserver.getUris().size());
        assertEquals(URI.create("foo://bar?one=uno"), linkObserver.getUris().get(0));

    }

    public static class LinkObserver {
        final List<URI> uris = new ArrayList<>();

        public void observe(@Observes URI uri) {
            this.uris.add(uri);
        }

        public List<URI> getUris() {
            return uris;
        }
    }

    public static class Person {

        private final String name;
        private final Integer age;
        private final Address address;
        private final Consumer<URI> links;

        private Person(final String name,
                       final Integer age,
                       final Address address,
                       final Consumer<URI> links) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.links = links;
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

        public Consumer<URI> getLinks() {
            return links;
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

        private Address(final String street,
                        final String city,
                        final State state,
                        final int zipcode,
                        final String country) {
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

public static class PersonBuilder {

    private String name;
    private Integer age;
    private Address address;
    private Consumer<URI> links;

    public PersonBuilder name(@Name final String name) {
        this.name = name;
        return this;
    }

    public PersonBuilder age(@Param("age") @Nullable final Integer age) {
        this.age = age;
        return this;
    }

    public PersonBuilder address(@Param("address") @Component final Address address) {
        this.address = address;
        return this;
    }

    public PersonBuilder link(@Event final Consumer<URI> links) {
        this.links = links;
        return this;
    }

    public Person build() {
        return new Person(name, age, address, links);
    }

    @org.tomitribe.pixie.Builder
    public static PersonBuilder builder() {
        return new PersonBuilder();
    }
}

    public static class AddressBuilder {

        private String street;
        private String city;
        private State state;
        private int zipcode;
        private String country;

        public AddressBuilder street(@Param("street") final String street) {
            this.street = street;
            return this;
        }

        public AddressBuilder city(@Param("city") final String city) {
            this.city = city;
            return this;
        }

        public AddressBuilder state(@Param("state") final State state) {
            this.state = state;
            return this;
        }

        public AddressBuilder zipcode(@Param("zipcode") final int zipcode) {
            this.zipcode = zipcode;
            return this;
        }

        public AddressBuilder country(@Param("country") @Default("USA") final String country) {
            this.country = country;
            return this;
        }

        public Address build() {
            return new Address(street, city, state, zipcode, country);
        }

        @org.tomitribe.pixie.Builder
        public static AddressBuilder builder() {
            return new AddressBuilder();
        }

    }
}
