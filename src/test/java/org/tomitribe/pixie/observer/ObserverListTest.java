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
import org.tomitribe.pixie.event.ObserverAdded;
import org.tomitribe.pixie.event.ObserverRemoved;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ObserverListTest {

    @Test
    public void noDuplicates() {
        final ObserverManager observers = new ObserverManager();
        final Object observer = new Object() {
            public void observe(@Observes final Object o) {
            }
        };

        Assert.assertTrue(observers.addObserver(observer));
        Assert.assertFalse(observers.addObserver(observer));
        Assert.assertFalse(observers.addObserver(observer));
        Assert.assertFalse(observers.addObserver(observer));

        Assert.assertTrue(observers.removeObserver(observer));
        Assert.assertFalse(observers.removeObserver(observer));
        Assert.assertFalse(observers.removeObserver(observer));
        Assert.assertFalse(observers.removeObserver(observer));
    }

    @Test
    public void observerAddAndRemoveEvents() {

        final Object observer = new Object() {
            public void observe(@Observes final Object o) {
            }
        };

        final List<String> invoked = new ArrayList<String>();
        final ObserverManager observers = new ObserverManager();
        observers.addObserver(new Object() {
            public void observe(@Observes final ObserverAdded o) {
                invoked.add(o.getClass().getSimpleName());
                Assert.assertSame(observer, o.getObserver());
            }

            public void observe(@Observes final ObserverRemoved o) {
                invoked.add(o.getClass().getSimpleName());
                Assert.assertSame(observer, o.getObserver());
            }
        });

        invoked.clear();

        observers.addObserver(observer);
        observers.removeObserver(observer);
        observers.addObserver(observer);
        observers.removeObserver(observer);

        Util.assertEvent(invoked,
                "ObserverAdded",
                "ObserverRemoved",
                "ObserverAdded",
                "ObserverRemoved"
        );
    }

}
