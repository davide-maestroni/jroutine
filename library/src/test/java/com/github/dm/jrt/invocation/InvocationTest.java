/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.invocation;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static com.github.dm.jrt.invocation.Invocations.factoryOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocations unit tests.
 * <p/>
 * Created by davide-maestroni on 02/16/2015.
 */
public class InvocationTest {

    @Test
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testInvocationFactory() {

        assertThat(factoryOf(TestInvocation.class).newInvocation()).isExactlyInstanceOf(
                TestInvocation.class);
        assertThat(factoryOf(
                ClassToken.tokenOf(TestInvocation.class)).newInvocation()).isExactlyInstanceOf(
                TestInvocation.class);
        assertThat(factoryOf(new TestInvocation()).newInvocation()).isExactlyInstanceOf(
                TestInvocation.class);
    }

    @Test
    public void testInvocationFactoryEquals() {

        final InvocationFactory<Object, Object> factory = factoryOf(TestInvocation.class);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(new InvocationFactory<Object, Object>() {

            @NotNull
            @Override
            public Invocation<Object, Object> newInvocation() {

                return new TemplateInvocation<Object, Object>() {};
            }
        });
        assertThat(factoryOf(TestInvocation.class).hashCode()).isEqualTo(
                factoryOf(TestInvocation.class).hashCode());
        assertThat(factoryOf(TestInvocation.class)).isEqualTo(factoryOf(TestInvocation.class));
        assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class)).hashCode()).isEqualTo(
                factoryOf(TestInvocation.class).hashCode());
        assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class))).isEqualTo(
                factoryOf(TestInvocation.class));
        assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class)).hashCode()).isEqualTo(
                factoryOf(ClassToken.tokenOf(TestInvocation.class)).hashCode());
        assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class))).isEqualTo(
                factoryOf(ClassToken.tokenOf(TestInvocation.class)));
        assertThat(factoryOf(TestInvocation.class).hashCode()).isNotEqualTo(
                factoryOf(new TemplateInvocation<Object, Object>() {}, this).hashCode());
        assertThat(factoryOf(TestInvocation.class)).isNotEqualTo(
                factoryOf(new TemplateInvocation<Object, Object>() {}, this));
        assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class)).hashCode()).isNotEqualTo(
                factoryOf(new TemplateInvocation<Object, Object>() {}, this).hashCode());
        assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class))).isNotEqualTo(
                factoryOf(new TemplateInvocation<Object, Object>() {}, this));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullClassError() {

        try {

            factoryOf((Class<TestInvocation>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            factoryOf((Class<TestInvocation>) null, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullInvocationError() {

        try {

            factoryOf((TestInvocation) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            factoryOf((TestInvocation) null, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullTokenError() {

        try {

            factoryOf((ClassToken<TestInvocation>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            factoryOf((ClassToken<TestInvocation>) null, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class TestInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @NotNull final ResultChannel<Object> result) {

        }
    }
}
