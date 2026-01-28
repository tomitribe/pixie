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

public class BuildersWildcardsTest {

    @Test
    public void extendsWildcardResolvesToUpperBound() throws Exception {
        final Method builder = ExtendsMessages.class.getMethod("builder");
        final Method build = BuildersTest.ProxyBuilder.class.getMethod("build");

        final Class<?> returnType = resolveBuiltType(builder, BuildersTest.ProxyBuilder.class, build);
        assertEquals(BuildersTest.Messages.class, returnType);
    }

    @Test
    public void superWildcardResolvesToLowerBound() throws Exception {
        final Method builder = SuperMessages.class.getMethod("builder");
        final Method build = BuildersTest.ProxyBuilder.class.getMethod("build");

        final Class<?> returnType = resolveBuiltType(builder, BuildersTest.ProxyBuilder.class, build);
        assertEquals(BuildersTest.Messages.class, returnType);
    }

    @Test
    public void unboundedWildcardResolvesToObject() throws Exception {
        final Method builder = Unbounded.class.getMethod("builder");
        final Method build = BuildersTest.ProxyBuilder.class.getMethod("build");

        final Class<?> returnType = resolveBuiltType(builder, BuildersTest.ProxyBuilder.class, build);
        assertEquals(Object.class, returnType);
    }

    public interface ExtendsMessages extends BuildersTest.Messages {

        @Builder
        static BuildersTest.ProxyBuilder<? extends BuildersTest.Messages> builder() {
            return new BuildersTest.ProxyBuilder<>(BuildersTest.Messages.class);
        }
    }

    public interface SuperMessages extends BuildersTest.Messages {

        @Builder
        static BuildersTest.ProxyBuilder<? super BuildersTest.Messages> builder() {
            return new BuildersTest.ProxyBuilder<>(BuildersTest.Messages.class);
        }
    }

    public interface Unbounded {

        @Builder
        static BuildersTest.ProxyBuilder<?> builder() {
            return new BuildersTest.ProxyBuilder<>(Object.class);
        }
    }
}