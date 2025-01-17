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
}
