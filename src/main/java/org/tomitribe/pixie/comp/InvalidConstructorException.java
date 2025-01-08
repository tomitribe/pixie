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

import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Default;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Param;
import org.tomitribe.util.PrintString;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Iterator;

public class InvalidConstructorException extends ComponentException {

    private final Constructor constructor;

    public InvalidConstructorException(final Class component, final Constructor constructor) {
        super(component, buildMessage(constructor));
        this.constructor = constructor;
    }

    private static String buildMessage(final Constructor constructor){

        int missing = 0;
        final PrintString c = new PrintString();
        c.printf("%s(%n", constructor.getDeclaringClass().getSimpleName());

        final Iterator<Parameter> parameters = Arrays.asList(constructor.getParameters()).iterator();
        while (parameters.hasNext()) {
            final Parameter parameter = parameters.next();
            c.print("    ");
            if (Constructors.isFullyAnnotated(parameter)) {
                if (parameter.isAnnotationPresent(Component.class)) {
                    c.printf("@Component(\"%s\") ", parameter.getAnnotation(Component.class).value());
                }
                if (parameter.isAnnotationPresent(Param.class)) {
                    c.printf("@Param(\"%s\") ", parameter.getAnnotation(Param.class).value());
                }
                if (parameter.isAnnotationPresent(Name.class)) {
                    c.print("@Name ");
                }
                if (parameter.isAnnotationPresent(Default.class)) {
                    c.printf("@Default(\"%s\") ", parameter.getAnnotation(Default.class).value());
                }
            } else {
                c.printf("/* missing */ ");
                missing++;
            }

            c.printf("%s%n", parameter.getType().getSimpleName());


        }
        c.printf("  )");

        return String.format("Constructor is missing %s parameter annotations%n  %s%n  %s%n  Add @Component, @Param or @Name",
                missing,
                constructor.getDeclaringClass().getName(),
                c.toString()
                );
    }

    public Constructor getConstructor() {
        return constructor;
    }
}
