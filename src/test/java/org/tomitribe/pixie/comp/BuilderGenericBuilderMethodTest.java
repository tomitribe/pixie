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

import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.pixie.Builder;
import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Asserts that the builder and factory can be in a different
 * class than the class built.
 */
public class BuilderGenericBuilderMethodTest extends Assert {

    @Test
    public void test() throws Exception {
        final System system = System.builder()
                .definition(Mail.class, "mail")
                .definition(Handler.class, "handler")
                .definition(Messages.class)
                .build();

        final Handler handler = system.get(Handler.class);

        final Mail mail = system.get(Mail.class);
        mail.getMessages().message("First call");
        mail.getMessages().message("Second call");
        mail.getMessages().message("Last call");

        final List<Invocation> invocations = handler.getInvocations();

        assertEquals("" +
                        "Invocation{method=public abstract void org.tomitribe.pixie.comp.BuilderGenericBuilderMethodTest$Messages.message(java.lang.String), args=[First call]}\n" +
                        "Invocation{method=public abstract void org.tomitribe.pixie.comp.BuilderGenericBuilderMethodTest$Messages.message(java.lang.String), args=[Second call]}\n" +
                        "Invocation{method=public abstract void org.tomitribe.pixie.comp.BuilderGenericBuilderMethodTest$Messages.message(java.lang.String), args=[Last call]}",
                invocations.stream()
                        .map(Invocation::toString)
                        .reduce((s, s2) -> s + "\n" + s2)
                        .orElse(""));
    }

    public static class Mail {
        private final Messages messages;

        public Mail(@Param("messages") @Component final Messages messages) {
            this.messages = messages;
        }

        public Messages getMessages() {
            return messages;
        }
    }


    public static class ProxyBuilder<T> {
        private final Class<T> type;

        public ProxyBuilder(final Class<T> type) {
            this.type = type;
        }

        private InvocationHandler handler;

        public ProxyBuilder<T> handler(final @Param("handler") @Component InvocationHandler handler) {
            this.handler = handler;
            return this;
        }

        public T build() {
            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{type}, handler);
        }
    }


    public interface Messages {

        void message(final String message);

        @Builder
        static ProxyBuilder<Messages> builder() {
            return new ProxyBuilder<>(Messages.class);
        }
    }

    public static class Handler implements InvocationHandler {

        private final List<Invocation> invocations = new ArrayList<>();

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            invocations.add(new Invocation(method, args));
            return null;
        }

        public List<Invocation> getInvocations() {
            return invocations;
        }
    }

    public static class Invocation {
        private final Method method;
        private final Object[] args;

        public Invocation(final Method method, final Object[] args) {
            this.method = method;
            this.args = args;
        }

        public Method getMethod() {
            return method;
        }

        public Object[] getArgs() {
            return args;
        }

        @Override
        public String toString() {
            return "Invocation{" +
                    "method=" + method +
                    ", args=" + Arrays.toString(args) +
                    '}';
        }
    }

}
