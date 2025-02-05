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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class EventConsumerAndObserverTest extends Assert {

    @Test
    public void test() throws Exception {
        final System system = System.builder()
                .definition(ShoppingCart.class)
                .definition(EmailReceipt.class)
                .definition(LogOrder.class)
                .build();

        final LogOrder logOrder = system.get(LogOrder.class);
        final EmailReceipt emailReceipt = system.get(EmailReceipt.class);
        final ShoppingCart shoppingCart = system.get(ShoppingCart.class);

        assertEquals(0, logOrder.getOrdersProcessed().size());
        assertEquals(0, emailReceipt.getOrdersProcessed().size());

        shoppingCart.order();

        assertEquals(1, logOrder.getOrdersProcessed().size());
        assertEquals("order101", logOrder.getOrdersProcessed().get(0).getId());

        assertEquals(1, emailReceipt.getOrdersProcessed().size());
        assertEquals("order101", emailReceipt.getOrdersProcessed().get(0).getId());
    }

    @Test
    public void observeEverything() throws Exception {
        final System system = System.builder()
                .definition(LogOrder.class)
                .definition(LogEverything.class)
                .build();

        final LogOrder logOrder = system.get(LogOrder.class);
        final LogEverything logEverything = system.get(LogEverything.class);

        assertEquals(0, logOrder.getOrdersProcessed().size());

        system.fireEvent(new OrderProcessed("order111"));
        system.fireEvent(new StripeOrderProcessed("order222"));
        system.fireEvent(new Update("New version available"));

        assertEquals(2, logOrder.getOrdersProcessed().size());
        assertEquals("order111", logOrder.getOrdersProcessed().get(0).getId());
        assertEquals("order222", logOrder.getOrdersProcessed().get(1).getId());

        assertTrue(logEverything.getEvents().size() >= 3);
        assertTrue(logEverything.getEvents().stream().anyMatch(o -> o.getClass().equals(OrderProcessed.class)));
        assertTrue(logEverything.getEvents().stream().anyMatch(o -> o.getClass().equals(StripeOrderProcessed.class)));
        assertTrue(logEverything.getEvents().stream().anyMatch(o -> o.getClass().equals(Update.class)));
    }

    public static class ShoppingCart {

        private final AtomicInteger number = new AtomicInteger(100);
        private final Consumer<OrderProcessed> orderProcessedConsumer;

        public ShoppingCart(@Event final Consumer<OrderProcessed> orderProcessedConsumer) {
            this.orderProcessedConsumer = orderProcessedConsumer;
        }

        public void order() {
            orderProcessedConsumer.accept(new OrderProcessed("order" + number.incrementAndGet()));
        }
    }

    public static class EmailReceipt {
        private final List<OrderProcessed> ordersProcessed = new ArrayList<>();

        public void email(@Observes OrderProcessed orderProcessed) {
            this.ordersProcessed.add(orderProcessed);
        }

        public List<OrderProcessed> getOrdersProcessed() {
            return ordersProcessed;
        }
    }

    public static class LogOrder {
        private final List<OrderProcessed> ordersProcessed = new ArrayList<>();

        public void email(@Observes OrderProcessed orderProcessed) {
            this.ordersProcessed.add(orderProcessed);
        }

        public List<OrderProcessed> getOrdersProcessed() {
            return ordersProcessed;
        }
    }

    public static class LogEverything {
        private final List<Object> events = new ArrayList<>();

        public void email(@Observes Object event) {
            this.events.add(event);
        }

        public List<Object> getEvents() {
            return events;
        }
    }

    public static class OrderProcessed {
        private final String id;

        public OrderProcessed(final String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "OrderProcessed{" +
                    "id='" + id + '\'' +
                    '}';
        }
    }

    public static class StripeOrderProcessed extends OrderProcessed {

        public StripeOrderProcessed(final String id) {
            super(id);
        }


        @Override
        public String toString() {
            return "StripeOrderProcessed{" +
                    "id='" + getId() + '\'' +
                    '}';
        }
    }

    public static class Update {
        private final String id;

        public Update(final String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "Update{" +
                    "id='" + id + '\'' +
                    '}';
        }
    }


}