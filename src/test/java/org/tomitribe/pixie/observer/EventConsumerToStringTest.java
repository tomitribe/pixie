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
package org.tomitribe.pixie.observer;

import org.tomitribe.pixie.Observes;
import org.tomitribe.pixie.event.AfterEvent;
import org.tomitribe.pixie.event.BeforeEvent;
import org.junit.*;

import java.util.Date;
import java.util.function.Consumer;

public class EventConsumerToStringTest extends org.junit.Assert {

    @Test
    public void ignored() throws Exception {
        final ObserverManager observerManager = new ObserverManager();

        final Consumer<Date> dateConsumer = observerManager.consumersOf(Date.class);

        assertEquals("ConsumerReference{type=java.util.Date} IGNORED", dateConsumer.toString());
    }

    @Test
    public void single() throws Exception {
        final ObserverManager observerManager = new ObserverManager();

        final Consumer<Date> dateConsumer = observerManager.consumersOf(Date.class);
        assertEquals("ConsumerReference{type=java.util.Date} IGNORED", dateConsumer.toString());

        observerManager.addObserver(new Red());

        final String testClass = this.getClass().getName();
        assertEquals("ConsumerReference{type=java.util.Date} " +
                        "public void " + testClass + "$Red.observe(java.util.Date)",
                dateConsumer.toString());

    }

    @Test
    public void multiple() throws Exception {
        final ObserverManager observerManager = new ObserverManager();

        final Consumer<Date> dateConsumer = observerManager.consumersOf(Date.class);
        assertEquals("ConsumerReference{type=java.util.Date} IGNORED", dateConsumer.toString());

        observerManager.addObserver(new Red());
        observerManager.addObserver(new Green());
        observerManager.addObserver(new Blue());
        observerManager.addObserver(new Yellow());

        final String testClass = this.getClass().getName();
        assertEquals("ConsumerReference{type=java.util.Date} InvocationList{invocations=4}\n" +
                        "public void " + testClass + "$Yellow.observe(org.tomitribe.pixie.event.BeforeEvent)\n" +
                        "public void " + testClass + "$Red.observe(java.util.Date)\n" +
                        "public void " + testClass + "$Green.observe(java.util.Date)\n" +
                        "public void " + testClass + "$Blue.observe(org.tomitribe.pixie.event.AfterEvent)\n",
                dateConsumer.toString());

    }


    private static class Red {
        public void observe(@Observes Date date){}
    }

    private static class Green {
        public void observe(@Observes Date date){}
    }

    private static class Blue {
        public void observe(@Observes AfterEvent<Date> date){}
    }

    private static class Yellow {
        public void observe(@Observes BeforeEvent<Date> date){}
    }
}
