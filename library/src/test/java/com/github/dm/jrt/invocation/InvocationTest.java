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

import java.util.List;

import static com.github.dm.jrt.invocation.Invocations.factoryFrom;
import static com.github.dm.jrt.invocation.Invocations.factoryOf;
import static com.github.dm.jrt.invocation.Invocations.filterFrom;
import static com.github.dm.jrt.invocation.Invocations.functionFrom;
import static com.github.dm.jrt.invocation.Invocations.procedureFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocations unit tests.
 * <p/>
 * Created by davide-maestroni on 02/16/2015.
 */
public class InvocationTest {

    private static InvocationFactory<Object, String> createFactory() {

        return factoryFrom(new Supplier<Invocation<Object, String>>() {

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

        return filterFrom(new BiConsumer<Object, ResultChannel<String>>() {

            public void accept(final Object o, final ResultChannel<String> result) {

                result.pass(o.toString());
            }
        });
    }

    private static FilterInvocation<Object, String> createFilter2() {

        return filterFrom(new Function<Object, String>() {

            public String apply(final Object o) {

                return o.toString();
            }
        });
    }

    private static InvocationFactory<?, String> createFunction() {

        return functionFrom(new BiConsumer<List<?>, ResultChannel<String>>() {

            public void accept(final List<?> objects, final ResultChannel<String> result) {

                for (final Object object : objects) {

                    result.pass(object.toString());
                }
            }
        });
    }

    private static InvocationFactory<?, String> createFunction2() {

        return functionFrom(new Function<List<?>, String>() {

            public String apply(final List<?> objects) {

                final StringBuilder builder = new StringBuilder();

                for (final Object object : objects) {

                    builder.append(object.toString());
                }

                return builder.toString();
            }
        });
    }

    private static ProcedureInvocation<String> createProcedure() {

        return procedureFrom(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("test");
            }
        });
    }

    private static ProcedureInvocation<String> createProcedure2() {

        return procedureFrom(new Supplier<String>() {

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

        final Supplier<Invocation<Object, Object>> supplier =
                Functions.constant(PassingInvocation.factoryOf().newInvocation());
        final InvocationFactory<Object, String> factory = createFactory();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isEqualTo(createFactory());
        assertThat(factory).isNotEqualTo(factoryFrom(supplier));
        assertThat(factory).isNotEqualTo(createFilter());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isEqualTo(createFactory().hashCode());
        assertThat(factoryFrom(supplier)).isEqualTo(factoryFrom(supplier));
        assertThat(factoryFrom(supplier).hashCode()).isEqualTo(factoryFrom(supplier).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        try {

            factoryFrom(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(factoryFrom(new Supplier<Invocation<Object, String>>() {

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

        final Functions.Function<Object, ? super Object> identity = Functions.identity();
        final InvocationFactory<Object, String> factory = createFilter2();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isEqualTo(createFilter2());
        assertThat(factory).isNotEqualTo(filterFrom(identity));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isEqualTo(createFilter2().hashCode());
        assertThat(filterFrom(identity)).isEqualTo(filterFrom(identity));
        assertThat(filterFrom(identity).hashCode()).isEqualTo(filterFrom(identity).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilter2Error() {

        try {

            filterFrom((Function<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(filterFrom(new Function<Object, Object>() {

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
        final Functions.BiConsumer<Object, ResultChannel<String>> sink = Functions.biSink();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isEqualTo(createFilter());
        assertThat(factory).isNotEqualTo(filterFrom(sink));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isEqualTo(createFilter().hashCode());
        assertThat(filterFrom(sink)).isEqualTo(filterFrom(sink));
        assertThat(filterFrom(sink).hashCode()).isEqualTo(filterFrom(sink).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilterError() {

        try {

            filterFrom((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(filterFrom(new BiConsumer<Object, ResultChannel<String>>() {

                public void accept(final Object o, final ResultChannel<String> result) {

                    result.pass(o.toString());
                }
            })).buildRoutine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testFunction() {

        final Routine<?, String> routine = JRoutine.on(createFunction()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).eventually().all()).containsOnly("test", "1");
    }

    @Test
    public void testFunction2() {

        final Routine<?, String> routine = JRoutine.on(createFunction2()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).eventually().all()).containsOnly("test1");
    }

    @Test
    public void testFunction2Equals() {

        final InvocationFactory<?, String> factory = createFunction2();
        final Functions.Function<List<?>, ? super List<?>> identity = Functions.identity();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isEqualTo(createFunction2());
        assertThat(factory).isNotEqualTo(functionFrom(identity));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isEqualTo(createFunction2().hashCode());
        assertThat(functionFrom(identity)).isEqualTo(functionFrom(identity));
        assertThat(functionFrom(identity).hashCode()).isEqualTo(functionFrom(identity).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFunction2Error() {

        try {

            functionFrom((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(functionFrom(new Function<List<?>, String>() {

                public String apply(final List<?> objects) {

                    final StringBuilder builder = new StringBuilder();

                    for (final Object object : objects) {

                        builder.append(object.toString());
                    }

                    return builder.toString();
                }
            })).buildRoutine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testFunctionEquals() {

        final InvocationFactory<?, String> factory = createFunction();
        final Functions.BiConsumer<List<?>, ResultChannel<Object>> sink = Functions.biSink();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isEqualTo(createFunction());
        assertThat(factory).isNotEqualTo(functionFrom(sink));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isEqualTo(createFunction().hashCode());
        assertThat(functionFrom(sink)).isEqualTo(functionFrom(sink));
        assertThat(functionFrom(sink).hashCode()).isEqualTo(functionFrom(sink).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFunctionError() {

        try {

            functionFrom((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(functionFrom(new BiConsumer<List<?>, ResultChannel<String>>() {

                public void accept(final List<?> objects, final ResultChannel<String> result) {

                    for (final Object object : objects) {

                        result.pass(object.toString());
                    }
                }
            })).buildRoutine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

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
    public void testNullDelegatedRoutine() {

        try {

            new DelegatingInvocation<Object, Object>(null, DelegationType.ASYNCHRONOUS);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DelegatingInvocation<Object, Object>(JRoutine.on(factoryOf(TestInvocation.class)),
                                                     null);

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
        final Functions.Supplier<String> constant = Functions.constant("test");
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isEqualTo(createProcedure2());
        assertThat(factory).isNotEqualTo(procedureFrom(constant));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isEqualTo(createProcedure2().hashCode());
        assertThat(procedureFrom(constant)).isEqualTo(procedureFrom(constant));
        assertThat(procedureFrom(constant).hashCode()).isEqualTo(
                procedureFrom(constant).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testProcedure2Error() {

        try {

            procedureFrom((Supplier<Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(procedureFrom(new Supplier<Object>() {

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
        final Functions.Consumer<ResultChannel<String>> sink = Functions.sink();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isEqualTo(createProcedure());
        assertThat(factory).isNotEqualTo(procedureFrom(sink));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(factory.hashCode()).isEqualTo(createProcedure().hashCode());
        assertThat(procedureFrom(sink)).isEqualTo(procedureFrom(sink));
        assertThat(procedureFrom(sink).hashCode()).isEqualTo(procedureFrom(sink).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testProcedureError() {

        try {

            procedureFrom((Consumer<ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.on(procedureFrom(new Consumer<ResultChannel<String>>() {

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
