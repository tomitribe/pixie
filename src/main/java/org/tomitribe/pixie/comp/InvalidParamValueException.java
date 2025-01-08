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

public class InvalidParamValueException extends ComponentException {

    private final String paramName;
    private final String paramValue;
    private final Class paramType;


    public InvalidParamValueException(final Class component, final Throwable cause, final String paramName, final String paramValue, final Class paramType) {
        super(component, cause);
        this.paramName = paramName;
        this.paramValue = paramValue;
        this.paramType = paramType;
    }

    @Override
    public String getMessage() {
        return String.format("Cannot convert value of param '%s' to type %s: %s", paramName, paramType.getName(), paramValue);
    }

    public String getParamName() {
        return paramName;
    }

    public String getParamValue() {
        return paramValue;
    }

    public Class getParamType() {
        return paramType;
    }
}
