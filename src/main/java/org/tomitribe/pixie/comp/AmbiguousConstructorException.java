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

import java.lang.reflect.Constructor;
import java.util.List;

public class AmbiguousConstructorException extends ComponentException {
    private final List<Constructor<?>> found;

    public AmbiguousConstructorException(final Class component, final List<Constructor<?>> found) {
        super(component);
        this.found = found;
    }

    @Override
    public String getMessage() {
        return "Only one eligible constructor allowed: " + component.getName() + "; Found " + found.size();
    }

    public List<Constructor<?>> getFound() {
        return found;
    }
}
