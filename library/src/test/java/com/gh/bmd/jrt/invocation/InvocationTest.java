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
package com.gh.bmd.jrt.invocation;

import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.core.JRoutine;
import com.gh.bmd.jrt.invocation.DelegatingInvocation.DelegationType;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.Reflection;

import org.junit.Test;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocations unit tests.
 * <p/>
 * Created by davide-maestroni on 2/16/15.
 */
public class InvocationTest {

    @Test
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testInvocationFactory() {

        assertThat(Invocations.factoryOf(TestInvocation.class).newInvocation()).isExactlyInstanceOf(
                TestInvocation.class);
        assertThat(Invocations.factoryOf(ClassToken.tokenOf(TestInvocation.class))
                              .newInvocation()).isExactlyInstanceOf(TestInvocation.class);
        assertThat(Invocations.factoryOf(new TestInvocation()).newInvocation()).isExactlyInstanceOf(
                TestInvocation.class);
    }

    @Test
    public void testInvocationFactoryEquals() {

        final InvocationFactory<Object, Object> factory =
                Invocations.factoryOf(TestInvocation.class);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(new InvocationFactory<Object, Object>() {

            @Nonnull
            @Override
            public Invocation<Object, Object> newInvocation() {

                return new TemplateInvocation<Object, Object>() {};
            }
        });
        assertThat(Invocations.factoryOf(TestInvocation.class).hashCode()).isEqualTo(
                Invocations.factoryOf(TestInvocation.class).hashCode());
        assertThat(Invocations.factoryOf(TestInvocation.class)).isEqualTo(
                Invocations.factoryOf(TestInvocation.class));
        assertThat(Invocations.factoryOf(ClassToken.tokenOf(TestInvocation.class))
                              .hashCode()).isEqualTo(
                Invocations.factoryOf(TestInvocation.class).hashCode());
        assertThat(Invocations.factoryOf(ClassToken.tokenOf(TestInvocation.class))).isEqualTo(
                Invocations.factoryOf(TestInvocation.class));
        assertThat(Invocations.factoryOf(ClassToken.tokenOf(TestInvocation.class))
                              .hashCode()).isEqualTo(
                Invocations.factoryOf(ClassToken.tokenOf(TestInvocation.class)).hashCode());
        assertThat(Invocations.factoryOf(ClassToken.tokenOf(TestInvocation.class))).isEqualTo(
                Invocations.factoryOf(ClassToken.tokenOf(TestInvocation.class)));
        assertThat(Invocations.factoryOf(TestInvocation.class).hashCode()).isNotEqualTo(
                Invocations.factoryOf(new TemplateInvocation<Object, Object>() {}, this)
                           .hashCode());
        assertThat(Invocations.factoryOf(TestInvocation.class)).isNotEqualTo(
                Invocations.factoryOf(new TemplateInvocation<Object, Object>() {}, this));
        assertThat(Invocations.factoryOf(ClassToken.tokenOf(TestInvocation.class))
                              .hashCode()).isNotEqualTo(
                Invocations.factoryOf(new TemplateInvocation<Object, Object>() {}, this)
                           .hashCode());
        assertThat(Invocations.factoryOf(ClassToken.tokenOf(TestInvocation.class))).isNotEqualTo(
                Invocations.factoryOf(new TemplateInvocation<Object, Object>() {}, this));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullClassError() {

        try {

            Invocations.factoryOf((Class<TestInvocation>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Invocations.factoryOf((Class<TestInvocation>) null, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullDelegatedRoutine() {

        try {

            new DelegatingInvocation<Object, Object>(null, DelegationType.ASYNCHRONOUS);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DelegatingInvocation<Object, Object>(
                    JRoutine.on(Invocations.factoryOf(TestInvocation.class)), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullInvocationError() {

        try {

            Invocations.factoryOf((TestInvocation) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Invocations.factoryOf((TestInvocation) null, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullTokenError() {

        try {

            Invocations.factoryOf((ClassToken<TestInvocation>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Invocations.factoryOf((ClassToken<TestInvocation>) null, Reflection.NO_ARGS);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class TestInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

        }
    }
}
