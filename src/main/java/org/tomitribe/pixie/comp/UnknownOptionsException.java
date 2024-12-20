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

import org.tomitribe.util.Join;

import java.util.List;

public class UnknownOptionsException extends ComponentException {

    private List<String> options;

    public UnknownOptionsException(final Class component, final List<String> options) {
        super(component);
        this.options = options;
    }

    @Override
    public String getMessage() {
        return String.format("Unknown options: %s%n  Component: %s", Join.join(", ", options), component.getName());
    }

    public List<String> getOptions() {
        return options;
    }
}
