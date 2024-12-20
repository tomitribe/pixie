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

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Trixie {

    private static final Logger LOGGER = Logger.getLogger(Trixie.class.getSimpleName());

    private static final System SYSTEM = new System();

    private static final AtomicBoolean loaded = new AtomicBoolean();
    
    private Trixie() {
        // no-op
        // Trixie can't be instantiated.
    }

    public static boolean isLoaded() {
        return loaded.get();
    }
    
    /**
     * It is ok to call this method from tests, but do not call it from production code.
     * <p>
     * Yes, it will work.  Yes, it is neat.  No, we don't want things created the user
     * cannot see.  Current efforts are to put all components in a single trixie.properties
     * file the user can update.
     */
    public static void load(final Properties properties) {
        try {
            if (!loaded.compareAndSet(false, true)) {
                LOGGER.info(String.format("Trixie already initialized for classloader %s - Loaded %s properties",
                                          Thread.currentThread().getContextClassLoader(), properties.size()));
                return;
            }
            SYSTEM.load(properties);
            LOGGER.info(String.format("TRIXIE SUCCESS - Loaded %s properties", properties.size()));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, String.format("TRIXIE FAILED - %s", e.getMessage()), e);
        }
    }

    public static void close() {
        try {
            SYSTEM.close();
            LOGGER.info("TRIXIE SUCCESS - Closed");

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, String.format("TRIXIE FAILED - %s", e.getMessage()), e);
        }
    }

    public static <E> E fireEvent(final E event) {
        return SYSTEM.fireEvent(event);
    }

    public static boolean addObserver(final Object observer) {
        return SYSTEM.addObserver(observer);
    }

    public static boolean removeObserver(final Object observer) {
        return SYSTEM.removeObserver(observer);
    }

    /**
     * @param type Class
     * @return the object associated with the class type or null
     * @throws IllegalStateException of the component isn't found
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(final Class<T> type) {
        return SYSTEM.get(type);
    }

    /**
     * @param type   Class
     * @param create Should the instance be lazily created?
     * @return the object associated with the class type or null
     * @throws IllegalStateException of the component isn't found
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(final Class<T> type, final boolean create) {
        return SYSTEM.get(type, create);
    }

}
