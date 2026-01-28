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

import org.junit.Test;
import org.tomitribe.pixie.Builder;
import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Param;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.tomitribe.pixie.comp.Builders.resolveBuiltType;

public class BuildersTest {

    @Test
    public void happyPathParameterizedBuilderReturnResolvesT() throws Exception {
        final Method builder = Messages.class.getMethod("builder");
        final Method build = ProxyBuilder.class.getMethod("build");

        final Class<?> returnType = resolveBuiltType(builder, ProxyBuilder.class, build);
        assertEquals(Messages.class, returnType);
    }

    @Test
    public void nonGenericBuildReturnsConcreteType() throws Exception {
        final Method builder = MailFactory.class.getMethod("builder");
        final Method build = MailBuilder.class.getMethod("build");

        final Class<?> returnType = resolveBuiltType(builder, MailBuilder.class, build);
        assertEquals(Mail.class, returnType);
    }

    @Test(expected = InvalidBuildMethodException.class)
    public void rawBuilderReturnCannotResolveT() throws Exception {
        final Method builder = RawMessagesFactory.class.getMethod("builder");
        final Method build = ProxyBuilder.class.getMethod("build");

        resolveBuiltType(builder, ProxyBuilder.class, build);
    }

    @Test
    public void concreteSubclassBindsGenericTViaExtends() throws Exception {
        final Method builder = SubclassMessagesFactory.class.getMethod("builder");
        final Method build = ProxyBuilder.class.getMethod("build");

        final Class<?> returnType = resolveBuiltType(builder, ProxyBuilder.class, build);
        assertEquals(Messages.class, returnType);
    }

    @Test
    public void buildDeclaredOnGenericSuperclassResolvesT() throws Exception {
        final Method builder = BaseMessagesFactory.class.getMethod("builder");
        final Method build = BaseBuilder.class.getMethod("build");

        final Class<?> returnType = resolveBuiltType(builder, BaseBuilder.class, build);
        assertEquals(Messages.class, returnType);
    }

    // ----------------------------------------------------------------------
    // Test fixtures
    // ----------------------------------------------------------------------

    public static class ProxyBuilder<T> {
        private final Class<T> type;

        public ProxyBuilder(final Class<T> type) {
            this.type = type;
        }

        private InvocationHandler handler;

        public ProxyBuilder<T> handler(@Param("handler") @Component final InvocationHandler handler) {
            this.handler = handler;
            return this;
        }

        public T build() {
            return (T) Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class[]{type},
                    handler
            );
        }
    }

    public interface Messages {

        void message(final String message);

        @Builder
        static ProxyBuilder<Messages> builder() {
            return new ProxyBuilder<>(Messages.class);
        }
    }

    public static class Mail {
        private final Messages messages;

        public Mail(final Messages messages) {
            this.messages = messages;
        }

        public Messages getMessages() {
            return messages;
        }
    }

    public static class MailBuilder {
        public MailBuilder() {
        }

        public MailBuilder messages(@Param("messages") @Component final Messages messages) {
            return this;
        }

        public Mail build() {
            return new Mail(null);
        }
    }

    public static class MailFactory {
        private MailFactory() {
        }

        @Builder
        public static MailBuilder builder() {
            return new MailBuilder();
        }
    }

    public static class RawMessagesFactory {
        private RawMessagesFactory() {
        }

        @Builder
        public static ProxyBuilder builder() {
            return new ProxyBuilder<>(Messages.class);
        }
    }

    public static class MessagesProxyBuilder extends ProxyBuilder<Messages> {
        public MessagesProxyBuilder() {
            super(Messages.class);
        }
    }

    public static class SubclassMessagesFactory {
        private SubclassMessagesFactory() {
        }

        @Builder
        public static MessagesProxyBuilder builder() {
            return new MessagesProxyBuilder();
        }
    }

    public static class BaseBuilder<T> {
        public BaseBuilder() {
        }

        public T build() {
            return null;
        }
    }

    public static class BaseMessagesFactory {
        private BaseMessagesFactory() {
        }

        @Builder
        public static BaseBuilder<Messages> builder() {
            return new BaseBuilder<>();
        }
    }
}