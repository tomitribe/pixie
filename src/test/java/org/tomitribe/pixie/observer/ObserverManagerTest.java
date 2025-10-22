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

import org.junit.Test;
import org.tomitribe.pixie.Observes;
import org.tomitribe.pixie.event.ObserverFailed;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ObserverManagerTest {
    @Test
    public void destroy() {
        final ObserverManager mgr = new ObserverManager();
        final Obs observer = new Obs();
        observer.last = null;
        mgr.addObserver(observer);
        mgr.fireEvent("hello");
        assertEquals("hello", observer.last);
        observer.last = null;
        mgr.destroy();
        mgr.fireEvent("I miss you");
        assertNull(observer.last);
    }

    public static class Obs {
        public volatile String last;

        public void listen(@Observes final String event) {
            last = event;
        }
    }

    @Test
    public void exceptionWhenFireEvent() {
        final ObserverManager mgr = new ObserverManager();
        final CountDownLatch latch = new CountDownLatch(3);
        final FailingObserver failingObserver = new FailingObserver(latch);
        mgr.addObserver(failingObserver);
        mgr.fireEvent("event1");
        mgr.fireEvent("event2");
        mgr.fireEvent("event3");
        assertEquals(0, latch.getCount());
    }

    @Test
    public void exceptionWhenConsumer() {
        final ObserverManager mgr = new ObserverManager();
        final CountDownLatch latch = new CountDownLatch(3);
        final FailingObserver failingObserver = new FailingObserver(latch);
        mgr.addObserver(failingObserver);
        final Consumer<String> consumer = mgr.consumersOf(String.class);
        consumer.accept("event1");
        consumer.accept("event2");
        consumer.accept("event3");
        assertEquals(0, latch.getCount());
    }

    @Test
    public void consumerOfNoObserver() {
        final ObserverManager mgr = new ObserverManager();
        final CountDownLatch latch = new CountDownLatch(3);
        final FailingObserver failingObserver = new FailingObserver(latch);
        mgr.addObserver(failingObserver);
        final Consumer<NoObserverEvent> consumer = mgr.consumersOf(NoObserverEvent.class);
        consumer.accept(new NoObserverEvent("hello"));
    }

    @Test
    public void fireEventNoObserver() {
        final ObserverManager mgr = new ObserverManager();
        final CountDownLatch latch = new CountDownLatch(3);
        final FailingObserver failingObserver = new FailingObserver(latch);
        mgr.addObserver(failingObserver);
        mgr.fireEvent(new NoObserverEvent("hello"));
    }

    public static class NoObserverEvent {
        private final String value;
        public NoObserverEvent(final String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    public static class FailingObserver {
        private final CountDownLatch latch;

        public FailingObserver(final CountDownLatch latch) {
            this.latch = latch;
        }

        public void fail(@Observes final String event) {
            throw new IllegalStateException("I always fail");
        }

        public void failed(@Observes final ObserverFailed event) {
            latch.countDown();
        }
    }


}
