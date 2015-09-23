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
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocations unit tests.
 * <p/>
 * Created by davide-maestroni on 02/16/2015.
 */
public class InvocationTest {

    private static final Supplier<Invocation<Object, Object>> sSupplier =
            new Supplier<Invocation<Object, Object>>() {

                public Invocation<Object, Object> get() {

                    return PassingInvocation.factoryOf().newInvocation();
                }
            };

    private static InvocationFactory<Object, String> createFactory() {

        return Invocations.factory(new Supplier<Invocation<Object, String>>() {

            public Invocation<Object, String> get() {

                return new FilterInvocation<Object, String>() {

                    public void onInput(final Object input,
                            @NotNull final ResultChannel<String> result) {

                        result.pass(input.toString());
                    }
                };
            }
        });
    }

    private static FilterInvocation<Object, String> createFilter() {

        return Invocations.filter(new BiConsumer<Object, ResultChannel<String>>() {

            public void accept(final Object o, final ResultChannel<String> result) {

                result.pass(o.toString());
            }
        });
    }

    private static FilterInvocation<Object, String> createFilter2() {

        return Invocations.filter(new Function<Object, String>() {

            public String apply(final Object o) {

                return o.toString();
            }
        });
    }

    private static ProcedureInvocation<String> createProcedure() {

        return Invocations.procedure(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("test");
            }
        });
    }

    private static ProcedureInvocation<String> createProcedure2() {

        return Invocations.procedure(new Supplier<String>() {

            public String get() {

                return "test";
            }
        });
    }

    @Test
    public void testFactory() {

        final Routine<Object, String> routine = JRoutine.on(createFactory()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).eventually().all()).containsOnly("test", "1");
    }

    @Test
    public void testFactoryEquals() {

        final InvocationFactory<Object, String> factory = createFactory();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo(createFilter());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isNotEqualTo(createFactory().hashCode());
        final Supplier<Invocation<Object, Object>> supplier = sSupplier;
        assertThat(Invocations.factory(supplier)).isEqualTo(Invocations.factory(supplier));
        assertThat(Invocations.factory(supplier).hashCode()).isEqualTo(
                Invocations.factory(supplier).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        try {

            Invocations.factory(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(Invocations.factory(new Supplier<Invocation<Object, String>>() {

                public Invocation<Object, String> get() {

                    return new FilterInvocation<Object, String>() {

                        public void onInput(final Object input,
                                @NotNull final ResultChannel<String> result) {

                            result.pass(input.toString());
                        }
                    };
                }
            })).buildRoutine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testFilter() {

        final Routine<Object, String> routine = JRoutine.on(createFilter()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).eventually().all()).containsOnly("test", "1");
    }

    @Test
    public void testFilter2() {

        final Routine<Object, String> routine = JRoutine.on(createFilter2()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).eventually().all()).containsOnly("test", "1");
    }

    @Test
    public void testFilter2Equals() {

        final InvocationFactory<Object, String> factory = createFilter2();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createFilter2());
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isNotEqualTo(createFilter2().hashCode());
        final Functions.Function<Object, ? super Object> identity = Functions.identity();
        assertThat(Invocations.filter(identity)).isEqualTo(Invocations.filter(identity));
        assertThat(Invocations.filter(identity).hashCode()).isEqualTo(
                Invocations.filter(identity).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilter2Error() {

        try {

            Invocations.filter((Function<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(Invocations.filter(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    return o.toString();
                }
            })).buildRoutine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testFilterEquals() {

        final InvocationFactory<Object, String> factory = createFilter();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createFilter());
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isNotEqualTo(createFilter().hashCode());
        final Functions.BiConsumer<Object, ResultChannel<String>> sink = Functions.biSink();
        assertThat(Invocations.filter(sink)).isEqualTo(Invocations.filter(sink));
        assertThat(Invocations.filter(sink).hashCode()).isEqualTo(
                Invocations.filter(sink).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilterError() {

        try {

            Invocations.filter((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(Invocations.filter(new BiConsumer<Object, ResultChannel<String>>() {

                public void accept(final Object o, final ResultChannel<String> result) {

                    result.pass(o.toString());
                }
            })).buildRoutine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

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

            @NotNull
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

    @Test
    public void testProcedure() {

        final Routine<Void, String> routine = JRoutine.on(createProcedure()).buildRoutine();
        assertThat(routine.asyncCall().eventually().all()).containsOnly("test");
    }

    @Test
    public void testProcedure2() {

        final Routine<Void, String> routine = JRoutine.on(createProcedure2()).buildRoutine();
        assertThat(routine.asyncCall().eventually().all()).containsOnly("test");
    }

    @Test
    public void testProcedure2Equals() {

        final InvocationFactory<Void, String> factory = createProcedure2();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createProcedure2());
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isNotEqualTo(createProcedure2().hashCode());
        final Functions.Supplier<String> constant = Functions.constant("test");
        assertThat(Invocations.procedure(constant)).isEqualTo(Invocations.procedure(constant));
        assertThat(Invocations.procedure(constant).hashCode()).isEqualTo(
                Invocations.procedure(constant).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testProcedure2Error() {

        try {

            Invocations.procedure((Supplier<Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(Invocations.procedure(new Supplier<Object>() {

                public Object get() {

                    return "test";
                }
            })).buildRoutine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testProcedureEquals() {

        final InvocationFactory<Void, String> factory = createProcedure();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createProcedure());
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isNotEqualTo(createProcedure().hashCode());
        final Functions.Consumer<ResultChannel<String>> sink = Functions.sink();
        assertThat(Invocations.procedure(sink)).isEqualTo(Invocations.procedure(sink));
        assertThat(Invocations.procedure(sink).hashCode()).isEqualTo(
                Invocations.procedure(sink).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testProcedureError() {

        try {

            Invocations.procedure((Consumer<ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(Invocations.procedure(new Consumer<ResultChannel<String>>() {

                public void accept(final ResultChannel<String> result) {

                    result.pass("test");
                }
            })).buildRoutine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    private static class TestInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @NotNull final ResultChannel<Object> result) {

        }
    }
}
