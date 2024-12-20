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

public class InvalidOptionValueException extends ComponentException {

    private final String optionName;
    private final String optionValue;
    private final Class optionType;


    public InvalidOptionValueException(final Class component, final Throwable cause, final String optionName, final String optionValue, final Class optionType) {
        super(component, cause);
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.optionType = optionType;
    }

    @Override
    public String getMessage() {
        return String.format("Cannot convert value of option '%s' to type %s: %s", optionName, optionType.getName(), optionValue);
    }

    public String getOptionName() {
        return optionName;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public Class getOptionType() {
        return optionType;
    }
}
