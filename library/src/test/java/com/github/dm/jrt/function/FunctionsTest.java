/*
 * Copyright 2016 Davide Maestroni
 *
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

package com.github.dm.jrt.function;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.dm.jrt.function.Functions.biSink;
import static com.github.dm.jrt.function.Functions.castTo;
import static com.github.dm.jrt.function.Functions.constant;
import static com.github.dm.jrt.function.Functions.consumerCommand;
import static com.github.dm.jrt.function.Functions.consumerFactory;
import static com.github.dm.jrt.function.Functions.consumerFilter;
import static com.github.dm.jrt.function.Functions.functionFactory;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.function.Functions.identity;
import static com.github.dm.jrt.function.Functions.isEqual;
import static com.github.dm.jrt.function.Functions.isInstanceOf;
import static com.github.dm.jrt.function.Functions.isNull;
import static com.github.dm.jrt.function.Functions.isSame;
import static com.github.dm.jrt.function.Functions.max;
import static com.github.dm.jrt.function.Functions.maxBy;
import static com.github.dm.jrt.function.Functions.min;
import static com.github.dm.jrt.function.Functions.minBy;
import static com.github.dm.jrt.function.Functions.negative;
import static com.github.dm.jrt.function.Functions.notNull;
import static com.github.dm.jrt.function.Functions.positive;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.sink;
import static com.github.dm.jrt.function.Functions.supplierCommand;
import static com.github.dm.jrt.function.Functions.supplierFactory;
import static com.github.dm.jrt.function.Functions.wrap;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Functions unit tests.
 * <p/>
 * Created by davide-maestroni on 09/24/2015.
 */
public class FunctionsTest {

    @NotNull
    private static CommandInvocation<String> createCommand() {

        return consumerCommand(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("test");
            }
        });
    }

    @NotNull
    private static CommandInvocation<String> createCommand2() {

        return supplierCommand(new Supplier<String>() {

            public String get() {

                return "test";
            }
        });
    }

    @NotNull
    private static InvocationFactory<Object, String> createFactory() {

        return supplierFactory(new Supplier<Invocation<Object, String>>() {

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

    @NotNull
    private static FilterInvocation<Object, String> createFilter() {

        return consumerFilter(new BiConsumer<Object, ResultChannel<String>>() {

            public void accept(final Object o, final ResultChannel<String> result) {

                result.pass(o.toString());
            }
        });
    }

    @NotNull
    private static FilterInvocation<Object, String> createFilter2() {

        return functionFilter(new com.github.dm.jrt.function.Function<Object, String>() {

            public String apply(final Object o) {

                return o.toString();
            }
        });
    }

    @NotNull
    private static FilterInvocation<String, String> createFilter3() {

        return predicateFilter(new Predicate<String>() {

            public boolean test(final String s) {

                return s.length() > 0;
            }
        });
    }

    @NotNull
    private static InvocationFactory<Object, String> createFunction() {

        return consumerFactory(new BiConsumer<List<?>, ResultChannel<String>>() {

            public void accept(final List<?> objects, final ResultChannel<String> result) {

                for (final Object object : objects) {

                    result.pass(object.toString());
                }
            }
        });
    }

    @NotNull
    private static InvocationFactory<Object, String> createFunction2() {

        return functionFactory(new com.github.dm.jrt.function.Function<List<?>, String>() {

            public String apply(final List<?> objects) {

                final StringBuilder builder = new StringBuilder();

                for (final Object object : objects) {

                    builder.append(object.toString());
                }

                return builder.toString();
            }
        });
    }

    @Test
    public void testBiConsumer() {

        final TestBiConsumer consumer1 = new TestBiConsumer();
        final BiConsumerWrapper<Object, Object> consumer2 = wrap(consumer1);
        assertThat(wrap(consumer2)).isSameAs(consumer2);
        consumer2.accept("test", "test");
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        final TestBiConsumer consumer3 = new TestBiConsumer();
        final BiConsumerWrapper<Object, Object> consumer4 = consumer2.andThen(consumer3);
        consumer4.accept("test", "test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
    }

    @Test
    public void testBiConsumerContext() {

        assertThat(wrap(new TestBiConsumer()).hasStaticScope()).isTrue();
        assertThat(wrap(new BiConsumer<Object, Object>() {

            public void accept(final Object o, final Object o2) {

            }
        }).hasStaticScope()).isFalse();
    }

    @Test
    public void testBiConsumerEquals() {

        final TestBiConsumer consumer1 = new TestBiConsumer();
        assertThat(wrap(consumer1)).isEqualTo(wrap(consumer1));
        final BiConsumerWrapper<Object, Object> consumer2 = wrap(consumer1);
        assertThat(consumer2).isEqualTo(consumer2);
        assertThat(consumer2).isNotEqualTo(null);
        assertThat(consumer2).isNotEqualTo("test");
        assertThat(wrap(consumer1).andThen(consumer2).hashCode()).isEqualTo(
                consumer2.andThen(consumer2).hashCode());
        assertThat(wrap(consumer1).andThen(consumer2)).isEqualTo(consumer2.andThen(consumer2));
        assertThat(consumer2.andThen(consumer2)).isEqualTo(wrap(consumer1).andThen(consumer2));
        assertThat(wrap(consumer1).andThen(consumer2).hashCode()).isEqualTo(
                consumer2.andThen(consumer1).hashCode());
        assertThat(wrap(consumer1).andThen(consumer2)).isEqualTo(consumer2.andThen(consumer1));
        assertThat(consumer2.andThen(consumer1)).isEqualTo(wrap(consumer1).andThen(consumer2));
        assertThat(wrap(consumer1).andThen(consumer2).hashCode()).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
        assertThat(wrap(consumer1).andThen(consumer2)).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)));
        assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
                wrap(consumer1).andThen(consumer2));
        assertThat(wrap(consumer1).andThen(consumer1).hashCode()).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
        assertThat(wrap(consumer1).andThen(consumer1)).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)));
        assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
                wrap(consumer1).andThen(consumer1));
        assertThat(consumer2.andThen(consumer1).hashCode()).isNotEqualTo(
                consumer2.andThen(biSink()).hashCode());
        assertThat(consumer2.andThen(consumer1)).isNotEqualTo(consumer2.andThen(biSink()));
        assertThat(consumer2.andThen(biSink())).isNotEqualTo(consumer2.andThen(consumer1));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBiConsumerError() {

        try {

            new BiConsumerWrapper<Object, Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap((BiConsumer<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap(new TestBiConsumer()).andThen(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBiFunction() {

        final TestBiFunction function1 = new TestBiFunction();
        final BiFunctionWrapper<Object, Object, Object> function2 = wrap(function1);
        assertThat(wrap(function2)).isSameAs(function2);
        assertThat(function2.apply("test", function1)).isSameAs(function1);
        assertThat(function1.isCalled()).isTrue();
        function1.reset();
        final TestFunction function = new TestFunction();
        final BiFunctionWrapper<Object, Object, Object> function3 = function2.andThen(function);
        assertThat(function3.apply("test", function1)).isSameAs(function1);
        assertThat(function1.isCalled()).isTrue();
        assertThat(function.isCalled()).isTrue();
        assertThat(Functions.<String, String>first().andThen(new Function<String, Integer>() {

            public Integer apply(final String s) {

                return s.length();
            }
        }).andThen(new Function<Integer, Integer>() {

            public Integer apply(final Integer integer) {

                return integer * 3;
            }
        }).apply("test", "long test")).isEqualTo(12);
        assertThat(Functions.<String, Integer>second().andThen(new Function<Integer, Integer>() {

            public Integer apply(final Integer integer) {

                return integer + 2;
            }
        }).apply("test", 3)).isEqualTo(5);
    }

    @Test
    public void testBiFunctionContext() {

        assertThat(wrap(new TestBiFunction()).hasStaticScope()).isTrue();
        assertThat(wrap(new BiFunction<Object, Object, Object>() {

            public Object apply(final Object o, final Object o2) {

                return null;
            }
        }).hasStaticScope()).isFalse();
    }

    @Test
    public void testBiFunctionEquals() {

        final TestBiFunction function1 = new TestBiFunction();
        assertThat(wrap(function1)).isEqualTo(wrap(function1));
        final BiFunctionWrapper<Object, Object, Object> function2 = wrap(function1);
        assertThat(function2).isEqualTo(function2);
        assertThat(function2).isNotEqualTo(null);
        assertThat(function2).isNotEqualTo("test");
        final TestFunction function = new TestFunction();
        assertThat(wrap(function1).andThen(function).hashCode()).isEqualTo(
                function2.andThen(function).hashCode());
        assertThat(wrap(function1).andThen(function)).isEqualTo(function2.andThen(function));
        assertThat(function2.andThen(function)).isEqualTo(wrap(function1).andThen(function));
        assertThat(wrap(function1).andThen(wrap(function)).hashCode()).isEqualTo(
                function2.andThen(function).hashCode());
        assertThat(wrap(function1).andThen(wrap(function))).isEqualTo(function2.andThen(function));
        assertThat(function2.andThen(function)).isEqualTo(wrap(function1).andThen(wrap(function)));
        assertThat(wrap(function1).andThen(wrap(function)).hashCode()).isNotEqualTo(
                function2.andThen(wrap(function).andThen(function)).hashCode());
        assertThat(wrap(function1).andThen(wrap(function))).isNotEqualTo(
                function2.andThen(wrap(function).andThen(function)));
        assertThat(function2.andThen(wrap(function).andThen(function))).isNotEqualTo(
                wrap(function1).andThen(wrap(function)));
        assertThat(wrap(function1).andThen(function).hashCode()).isNotEqualTo(
                function2.andThen(wrap(function).andThen(function)).hashCode());
        assertThat(wrap(function1).andThen(function)).isNotEqualTo(
                function2.andThen(wrap(function).andThen(function)));
        assertThat(function2.andThen(wrap(function).andThen(function))).isNotEqualTo(
                wrap(function1).andThen(function));
        assertThat(function2.andThen(function).hashCode()).isNotEqualTo(
                function2.andThen(identity()).hashCode());
        assertThat(function2.andThen(function)).isNotEqualTo(function2.andThen(identity()));
        assertThat(function2.andThen(identity())).isNotEqualTo(function2.andThen(function));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBiFunctionError() {

        try {

            new BiFunctionWrapper<Object, Object, Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap((BiFunction<?, ?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap(new TestBiFunction()).andThen(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBiSink() {

        final TestBiConsumer consumer1 = new TestBiConsumer();
        final BiConsumerWrapper<Object, Object> consumer2 = biSink().andThen(consumer1);
        consumer2.accept("test", "test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(biSink()).isSameAs(biSink());
    }

    @Test
    public void testCastPredicate() {

        final FunctionWrapper<Object, Number> function = castTo(Number.class);
        function.apply(1);
        function.apply(3.5);

        try {

            function.apply("test");

            fail();

        } catch (final ClassCastException ignored) {

        }

        final FunctionWrapper<Object, List<String>> function1 =
                castTo(new ClassToken<List<String>>() {});
        function1.apply(new ArrayList<String>());
        function1.apply(new CopyOnWriteArrayList<String>());

        try {

            fail(function1.apply(Arrays.asList(1, 2)).get(0));

        } catch (final ClassCastException ignored) {

        }

        try {

            function1.apply("test");

            fail();

        } catch (final ClassCastException ignored) {

        }
    }

    @Test
    public void testCastPredicateEquals() {

        final FunctionWrapper<Object, Number> function = castTo(Number.class);
        assertThat(function).isEqualTo(function);
        assertThat(function).isEqualTo(castTo(Number.class));
        assertThat(function).isNotEqualTo(castTo(String.class));
        assertThat(function).isNotEqualTo("");
        assertThat(function.hashCode()).isEqualTo(castTo(Number.class).hashCode());
        final FunctionWrapper<Object, List<String>> function1 =
                castTo(new ClassToken<List<String>>() {});
        assertThat(function1).isEqualTo(function1);
        assertThat(function1).isEqualTo(castTo(new ClassToken<List<String>>() {}));
        assertThat(function1).isNotEqualTo(castTo(new ClassToken<String>() {}));
        assertThat(function1).isNotEqualTo("");
        assertThat(function1.hashCode()).isEqualTo(
                castTo(new ClassToken<List<String>>() {}).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCastPredicateError() {

        try {

            castTo((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            castTo((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testCommand() {

        final Routine<Void, String> routine = JRoutine.on(createCommand()).buildRoutine();
        assertThat(routine.asyncCall().afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testCommand2() {

        final Routine<Void, String> routine = JRoutine.on(createCommand2()).buildRoutine();
        assertThat(routine.asyncCall().afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testCommand2Equals() {

        final InvocationFactory<Void, String> factory = createCommand2();
        final SupplierWrapper<String> constant = constant("test");
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createCommand2());
        assertThat(factory).isNotEqualTo(supplierCommand(constant));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(supplierCommand(constant)).isEqualTo(supplierCommand(constant));
        assertThat(supplierCommand(constant).hashCode()).isEqualTo(
                supplierCommand(constant).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCommand2Error() {

        try {

            supplierCommand(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testCommandEquals() {

        final InvocationFactory<Void, String> factory = createCommand();
        final ConsumerWrapper<ResultChannel<String>> sink = sink();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createCommand());
        assertThat(factory).isNotEqualTo(consumerCommand(sink));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(consumerCommand(sink)).isEqualTo(consumerCommand(sink));
        assertThat(consumerCommand(sink).hashCode()).isEqualTo(consumerCommand(sink).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCommandError() {

        try {

            consumerCommand(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testConstantSupplier() {

        final TestFunction function = new TestFunction();
        final SupplierWrapper<Object> supplier = constant("test").andThen(function);
        assertThat(supplier.get()).isEqualTo("test");
        assertThat(function.isCalled()).isTrue();
    }

    @Test
    public void testConstantSupplierEquals() {

        final SupplierWrapper<String> supplier = constant("test");
        assertThat(supplier).isEqualTo(supplier);
        assertThat(supplier).isEqualTo(constant("test"));
        assertThat(supplier).isNotEqualTo(constant(1));
        assertThat(supplier).isNotEqualTo("");
        assertThat(supplier.hashCode()).isEqualTo(constant("test").hashCode());
    }

    @Test
    public void testConsumer() {

        final TestConsumer consumer1 = new TestConsumer();
        final ConsumerWrapper<Object> consumer2 = wrap(consumer1);
        assertThat(wrap(consumer2)).isSameAs(consumer2);
        consumer2.accept("test");
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        final TestConsumer consumer3 = new TestConsumer();
        final ConsumerWrapper<Object> consumer4 = consumer2.andThen(consumer3);
        consumer4.accept("test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
    }

    @Test
    public void testConsumerContext() {

        assertThat(wrap(new TestConsumer()).hasStaticScope()).isTrue();
        assertThat(wrap(new Consumer<Object>() {

            public void accept(final Object o) {

            }
        }).hasStaticScope()).isFalse();
    }

    @Test
    public void testConsumerEquals() {

        final TestConsumer consumer1 = new TestConsumer();
        assertThat(wrap(consumer1)).isEqualTo(wrap(consumer1));
        final ConsumerWrapper<Object> consumer2 = wrap(consumer1);
        assertThat(consumer2).isEqualTo(consumer2);
        assertThat(consumer2).isNotEqualTo(null);
        assertThat(consumer2).isNotEqualTo("test");
        assertThat(wrap(consumer1).andThen(consumer2).hashCode()).isEqualTo(
                consumer2.andThen(consumer2).hashCode());
        assertThat(wrap(consumer1).andThen(consumer2)).isEqualTo(consumer2.andThen(consumer2));
        assertThat(consumer2.andThen(consumer2)).isEqualTo(wrap(consumer1).andThen(consumer2));
        assertThat(wrap(consumer1).andThen(consumer2).hashCode()).isEqualTo(
                consumer2.andThen(consumer1).hashCode());
        assertThat(wrap(consumer1).andThen(consumer2)).isEqualTo(consumer2.andThen(consumer1));
        assertThat(consumer2.andThen(consumer1)).isEqualTo(wrap(consumer1).andThen(consumer2));
        assertThat(wrap(consumer1).andThen(consumer2).hashCode()).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
        assertThat(wrap(consumer1).andThen(consumer2)).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)));
        assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
                wrap(consumer1).andThen(consumer2));
        assertThat(wrap(consumer1).andThen(consumer1).hashCode()).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
        assertThat(wrap(consumer1).andThen(consumer1)).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)));
        assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
                wrap(consumer1).andThen(consumer1));
        assertThat(consumer2.andThen(consumer1).hashCode()).isNotEqualTo(
                consumer2.andThen(sink()).hashCode());
        assertThat(consumer2.andThen(consumer1)).isNotEqualTo(consumer2.andThen(sink()));
        assertThat(consumer2.andThen(sink())).isNotEqualTo(consumer2.andThen(consumer1));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConsumerError() {

        try {

            new ConsumerWrapper<Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap((Consumer<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap(new TestConsumer()).andThen(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testEqualToPredicate() {

        final PredicateWrapper<Object> predicate = isEqual("test");
        assertThat(predicate.test("test")).isTrue();
        assertThat(predicate.test(1)).isFalse();
        assertThat(predicate.test(null)).isFalse();
        final PredicateWrapper<Object> predicate1 = isEqual(null);
        assertThat(predicate1.test("test")).isFalse();
        assertThat(predicate1.test(1)).isFalse();
        assertThat(predicate1.test(null)).isTrue();
    }

    @Test
    public void testEqualToPredicateEquals() {

        final PredicateWrapper<Object> predicate = isEqual("test");
        assertThat(predicate).isEqualTo(predicate);
        assertThat(predicate).isEqualTo(isEqual("test"));
        assertThat(predicate).isNotEqualTo(isEqual(1.1));
        assertThat(predicate).isNotEqualTo("");
        assertThat(predicate.hashCode()).isEqualTo(isEqual("test").hashCode());
        assertThat(isEqual(null)).isEqualTo(isNull());
    }

    @Test
    public void testFactory() {

        final Routine<Object, String> routine = JRoutine.on(createFactory()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).afterMax(seconds(1)).all()).containsOnly("test",
                                                                                         "1");
    }

    @Test
    public void testFactoryEquals() {

        final Supplier<Invocation<Object, Object>> supplier =
                constant(PassingInvocation.factoryOf().newInvocation());
        final InvocationFactory<Object, String> factory = createFactory();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo(supplierFactory(supplier));
        assertThat(factory).isNotEqualTo(createFilter());
        assertThat(factory).isNotEqualTo("");
        assertThat(supplierFactory(supplier)).isEqualTo(supplierFactory(supplier));
        assertThat(supplierFactory(supplier).hashCode()).isEqualTo(
                supplierFactory(supplier).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        try {

            supplierFactory(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFilter() {

        final Routine<Object, String> routine = JRoutine.on(createFilter()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).afterMax(seconds(1)).all()).containsOnly("test",
                                                                                         "1");
    }

    @Test
    public void testFilter2() {

        final Routine<Object, String> routine = JRoutine.on(createFilter2()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).afterMax(seconds(1)).all()).containsOnly("test",
                                                                                         "1");
    }

    @Test
    public void testFilter2Equals() {

        final FunctionWrapper<Object, ? super Object> identity = identity();
        final InvocationFactory<Object, String> factory = createFilter2();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createFilter2());
        assertThat(factory).isNotEqualTo(functionFilter(identity));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(functionFilter(identity)).isEqualTo(functionFilter(identity));
        assertThat(functionFilter(identity).hashCode()).isEqualTo(
                functionFilter(identity).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilter2Error() {

        try {

            functionFilter((Function<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFilter3() {

        final Routine<String, String> routine = JRoutine.on(createFilter3()).buildRoutine();
        assertThat(routine.asyncCall("test", "").afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testFilter3Equals() {

        final PredicateWrapper<Object> negative = negative();
        final InvocationFactory<String, String> factory = createFilter3();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createFilter3());
        assertThat(factory).isNotEqualTo(predicateFilter(negative));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(predicateFilter(negative)).isEqualTo(predicateFilter(negative));
        assertThat(predicateFilter(negative).hashCode()).isEqualTo(
                predicateFilter(negative).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilter3Error() {

        try {

            predicateFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFilterEquals() {

        final InvocationFactory<Object, String> factory = createFilter();
        final BiConsumerWrapper<Object, ResultChannel<String>> sink = biSink();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createFilter());
        assertThat(factory).isNotEqualTo(consumerFilter(sink));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(consumerFilter(sink)).isEqualTo(consumerFilter(sink));
        assertThat(consumerFilter(sink).hashCode()).isEqualTo(consumerFilter(sink).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilterError() {

        try {

            consumerFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFunction() {

        final TestFunction function1 = new TestFunction();
        final FunctionWrapper<Object, Object> function2 = wrap(function1);
        assertThat(wrap(function2)).isSameAs(function2);
        assertThat(function2.apply("test")).isEqualTo("test");
        assertThat(function1.isCalled()).isTrue();
        function1.reset();
        final TestFunction function3 = new TestFunction();
        final FunctionWrapper<Object, Object> function4 = function2.andThen(function3);
        assertThat(function4.apply("test")).isEqualTo("test");
        assertThat(function1.isCalled()).isTrue();
        assertThat(function3.isCalled()).isTrue();
        final FunctionWrapper<String, Integer> function5 = wrap(new Function<String, Integer>() {

            public Integer apply(final String s) {

                return s.length();
            }
        }).andThen(new Function<Integer, Integer>() {

            public Integer apply(final Integer integer) {

                return integer * 3;
            }
        });
        assertThat(function5.apply("test")).isEqualTo(12);
        assertThat(function5.compose(new Function<String, String>() {

            public String apply(final String s) {

                return s + s;
            }
        }).apply("test")).isEqualTo(24);
    }

    @Test
    public void testFunctionContext() {

        assertThat(wrap(new TestFunction()).hasStaticScope()).isTrue();
        assertThat(wrap(new Function<Object, Object>() {

            public Object apply(final Object o) {

                return null;
            }
        }).hasStaticScope()).isFalse();
    }

    @Test
    public void testFunctionEquals() {

        final TestFunction function1 = new TestFunction();
        assertThat(wrap(function1)).isEqualTo(wrap(function1));
        final FunctionWrapper<Object, Object> function2 = wrap(function1);
        assertThat(function2).isEqualTo(function2);
        assertThat(function2).isNotEqualTo(null);
        assertThat(function2).isNotEqualTo("test");
        assertThat(wrap(function1).andThen(function2).hashCode()).isEqualTo(
                function2.andThen(function2).hashCode());
        assertThat(wrap(function1).andThen(function2)).isEqualTo(function2.andThen(function2));
        assertThat(function2.andThen(function2)).isEqualTo(wrap(function1).andThen(function2));
        assertThat(wrap(function1).andThen(function2).hashCode()).isEqualTo(
                function2.andThen(function1).hashCode());
        assertThat(wrap(function1).andThen(function2)).isEqualTo(function2.andThen(function1));
        assertThat(function2.andThen(function1)).isEqualTo(wrap(function1).andThen(function2));
        assertThat(wrap(function1).andThen(function2).hashCode()).isNotEqualTo(
                function2.andThen(function2.andThen(function1)).hashCode());
        assertThat(wrap(function1).andThen(function2)).isNotEqualTo(
                function2.andThen(function2.andThen(function1)));
        assertThat(function2.andThen(function2.andThen(function1))).isNotEqualTo(
                wrap(function1).andThen(function2));
        assertThat(wrap(function1).andThen(function1).hashCode()).isNotEqualTo(
                function2.andThen(function2.andThen(function1)).hashCode());
        assertThat(wrap(function1).andThen(function1)).isNotEqualTo(
                function2.andThen(function2.andThen(function1)));
        assertThat(function2.andThen(function2.andThen(function1))).isNotEqualTo(
                wrap(function1).andThen(function1));
        assertThat(function2.andThen(function1).hashCode()).isNotEqualTo(
                function2.andThen(identity()).hashCode());
        assertThat(function2.andThen(function1)).isNotEqualTo(function2.andThen(identity()));
        assertThat(function2.andThen(identity())).isNotEqualTo(function2.andThen(function1));
        assertThat(wrap(function1).compose(function2).hashCode()).isEqualTo(
                function2.compose(function2).hashCode());
        assertThat(wrap(function1).compose(function2)).isEqualTo(function2.compose(function2));
        assertThat(function2.compose(function2)).isEqualTo(wrap(function1).compose(function2));
        assertThat(wrap(function1).compose(function2).hashCode()).isEqualTo(
                function2.compose(function1).hashCode());
        assertThat(wrap(function1).compose(function2)).isEqualTo(function2.compose(function1));
        assertThat(function2.compose(function1)).isEqualTo(wrap(function1).compose(function2));
        assertThat(wrap(function1).compose(function2).hashCode()).isNotEqualTo(
                function2.compose(function2.compose(function1)).hashCode());
        assertThat(wrap(function1).compose(function2)).isNotEqualTo(
                function2.compose(function2.compose(function1)));
        assertThat(function2.compose(function2.compose(function1))).isNotEqualTo(
                wrap(function1).compose(function2));
        assertThat(wrap(function1).compose(function1).hashCode()).isNotEqualTo(
                function2.compose(function2.compose(function1)).hashCode());
        assertThat(wrap(function1).compose(function1)).isNotEqualTo(
                function2.compose(function2.compose(function1)));
        assertThat(function2.compose(function2.compose(function1))).isNotEqualTo(
                wrap(function1).compose(function1));
        assertThat(function2.compose(function1).hashCode()).isNotEqualTo(
                function2.compose(identity()).hashCode());
        assertThat(function2.compose(function1)).isNotEqualTo(function2.compose(identity()));
        assertThat(function2.compose(identity())).isNotEqualTo(function2.compose(function1));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFunctionError() {

        try {

            new FunctionWrapper<Object, Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap((Function<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap(new TestFunction()).andThen(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            identity().compose(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFunctionFactory() {

        final Routine<Object, String> routine = JRoutine.on(createFunction()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).afterMax(seconds(1)).all()).containsOnly("test",
                                                                                         "1");
    }

    @Test
    public void testFunctionFactory2() {

        final Routine<Object, String> routine = JRoutine.on(createFunction2()).buildRoutine();
        assertThat(routine.asyncCall("test", 1).afterMax(seconds(1)).all()).containsOnly("test1");
    }

    @Test
    public void testFunctionFactory2Equals() {

        final InvocationFactory<?, String> factory = createFunction2();
        final FunctionWrapper<List<?>, ? super List<?>> identity = identity();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createFunction2());
        assertThat(factory).isNotEqualTo(functionFactory(identity));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(functionFactory(identity)).isEqualTo(functionFactory(identity));
        assertThat(functionFactory(identity).hashCode()).isEqualTo(
                functionFactory(identity).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFunctionFactory2Error() {

        try {

            functionFactory(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFunctionFactoryEquals() {

        final InvocationFactory<?, String> factory = createFunction();
        final BiConsumerWrapper<List<?>, ResultChannel<Object>> sink = biSink();
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(createFunction());
        assertThat(factory).isNotEqualTo(consumerFactory(sink));
        assertThat(factory).isNotEqualTo(createFactory());
        assertThat(factory).isNotEqualTo("");
        assertThat(consumerFactory(sink)).isEqualTo(consumerFactory(sink));
        assertThat(consumerFactory(sink).hashCode()).isEqualTo(consumerFactory(sink).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFunctionFactoryError() {

        try {

            consumerFactory(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testIdentity() {

        final TestFunction function1 = new TestFunction();
        final FunctionWrapper<Object, Object> function2 = identity().andThen(function1);
        assertThat(function2.apply("test")).isEqualTo("test");
        assertThat(function1.isCalled()).isTrue();
        assertThat(identity()).isSameAs(identity());
    }

    @Test
    public void testInstanceOfPredicate() {

        final PredicateWrapper<Object> predicate = isInstanceOf(String.class);
        assertThat(predicate.test("test")).isTrue();
        assertThat(predicate.test(1)).isFalse();
        assertThat(predicate.test(null)).isFalse();
    }

    @Test
    public void testInstanceOfPredicateEquals() {

        final PredicateWrapper<Object> predicate = isInstanceOf(String.class);
        assertThat(predicate).isEqualTo(predicate);
        assertThat(predicate).isEqualTo(isInstanceOf(String.class));
        assertThat(predicate).isNotEqualTo(isInstanceOf(Integer.class));
        assertThat(predicate).isNotEqualTo("");
        assertThat(predicate.hashCode()).isEqualTo(isInstanceOf(String.class).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInstanceOfPredicateError() {

        try {

            isInstanceOf(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMaxBiFunction() {

        final BiFunctionWrapper<String, String, String> function = max();
        assertThat(function.apply("A TEST", "test")).isEqualTo("test");
        assertThat(function.andThen(new Function<String, String>() {

            public String apply(final String s) {

                return s.toLowerCase();
            }
        }).apply("A TEST", "test")).isEqualTo("test");
        assertThat(function.apply("2", "1")).isEqualTo("2");
    }

    @Test
    public void testMaxBiFunctionEquals() {

        final BiFunctionWrapper<String, String, String> function = max();
        assertThat(function).isEqualTo(function);
        assertThat(function).isEqualTo(max());
        assertThat(function).isNotEqualTo(maxBy(new Comparator<String>() {

            public int compare(final String o1, final String o2) {

                return o2.compareTo(o1);
            }
        }));
        assertThat(function).isNotEqualTo(null);
        assertThat(function).isNotEqualTo("");
        assertThat(function.hashCode()).isEqualTo(max().hashCode());
    }

    @Test
    public void testMaxByBiFunction() {

        final BiFunctionWrapper<String, String, String> function =
                maxBy(String.CASE_INSENSITIVE_ORDER);
        assertThat(function.apply("TEST", "a test")).isEqualTo("TEST");
        assertThat(function.andThen(new Function<String, String>() {

            public String apply(final String s) {

                return s.toLowerCase();
            }
        }).apply("TEST", "a test")).isEqualTo("test");
        assertThat(function.apply("2", "1")).isEqualTo("2");
    }

    @Test
    public void testMaxByBiFunctionEquals() {

        final BiFunctionWrapper<String, String, String> function =
                maxBy(String.CASE_INSENSITIVE_ORDER);
        assertThat(function).isEqualTo(function);
        assertThat(function).isEqualTo(maxBy(String.CASE_INSENSITIVE_ORDER));
        assertThat(function).isNotEqualTo(maxBy(new Comparator<String>() {

            public int compare(final String o1, final String o2) {

                return o2.compareTo(o1);
            }
        }));
        assertThat(function).isNotEqualTo(null);
        assertThat(function).isNotEqualTo("");
        assertThat(function.hashCode()).isEqualTo(maxBy(String.CASE_INSENSITIVE_ORDER).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMaxByBiFunctionError() {

        try {

            maxBy(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMinBiFunction() {

        final BiFunctionWrapper<String, String, String> function = min();
        assertThat(function.apply("A TEST", "test")).isEqualTo("A TEST");
        assertThat(function.andThen(new Function<String, String>() {

            public String apply(final String s) {

                return s.toLowerCase();
            }
        }).apply("A TEST", "test")).isEqualTo("a test");
        assertThat(function.apply("2", "1")).isEqualTo("1");
    }

    @Test
    public void testMinBiFunctionEquals() {

        final BiFunctionWrapper<String, String, String> function = min();
        assertThat(function).isEqualTo(function);
        assertThat(function).isEqualTo(min());
        assertThat(function).isNotEqualTo(minBy(new Comparator<String>() {

            public int compare(final String o1, final String o2) {

                return o2.compareTo(o1);
            }
        }));
        assertThat(function).isNotEqualTo(null);
        assertThat(function).isNotEqualTo("");
        assertThat(function.hashCode()).isEqualTo(min().hashCode());
    }

    @Test
    public void testMinByBiFunction() {

        final BiFunctionWrapper<String, String, String> function =
                minBy(String.CASE_INSENSITIVE_ORDER);
        assertThat(function.apply("TEST", "a test")).isEqualTo("a test");
        assertThat(function.andThen(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).apply("TEST", "a test")).isEqualTo("A TEST");
        assertThat(function.apply("2", "1")).isEqualTo("1");
    }

    @Test
    public void testMinByBiFunctionEquals() {

        final BiFunctionWrapper<String, String, String> function =
                minBy(String.CASE_INSENSITIVE_ORDER);
        assertThat(function).isEqualTo(function);
        assertThat(function).isEqualTo(minBy(String.CASE_INSENSITIVE_ORDER));
        assertThat(function).isNotEqualTo(minBy(new Comparator<String>() {

            public int compare(final String o1, final String o2) {

                return o1.compareTo(o2);
            }
        }));
        assertThat(function).isNotEqualTo(null);
        assertThat(function).isNotEqualTo("");
        assertThat(function.hashCode()).isEqualTo(minBy(String.CASE_INSENSITIVE_ORDER).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMinByBiFunctionError() {

        try {

            minBy(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testPredicate() {

        final TestPredicate predicate1 = new TestPredicate();
        final PredicateWrapper<Object> predicate2 = wrap(predicate1);
        assertThat(wrap(predicate2)).isSameAs(predicate2);
        assertThat(predicate2.test(this)).isTrue();
        assertThat(predicate1.isCalled()).isTrue();
        predicate1.reset();
        final TestPredicate predicate3 = new TestPredicate();
        final PredicateWrapper<Object> predicate4 = predicate2.and(predicate3);
        assertThat(predicate4.test(this)).isTrue();
        assertThat(predicate1.isCalled()).isTrue();
        assertThat(predicate3.isCalled()).isTrue();
        predicate1.reset();
        predicate3.reset();
        assertThat(predicate4.test(null)).isFalse();
        assertThat(predicate1.isCalled()).isTrue();
        assertThat(predicate3.isCalled()).isFalse();
        predicate1.reset();
        predicate3.reset();
        final PredicateWrapper<Object> predicate5 = predicate2.or(predicate3);
        assertThat(predicate5.test(this)).isTrue();
        assertThat(predicate1.isCalled()).isTrue();
        assertThat(predicate3.isCalled()).isFalse();
        predicate1.reset();
        predicate3.reset();
        assertThat(predicate5.test(null)).isFalse();
        assertThat(predicate1.isCalled()).isTrue();
        assertThat(predicate3.isCalled()).isTrue();
        predicate1.reset();
        predicate3.reset();
        final PredicateWrapper<Object> predicate6 = predicate4.negate();
        assertThat(predicate6.test(this)).isFalse();
        assertThat(predicate1.isCalled()).isTrue();
        assertThat(predicate3.isCalled()).isTrue();
        predicate1.reset();
        predicate3.reset();
        assertThat(predicate6.test(null)).isTrue();
        assertThat(predicate1.isCalled()).isTrue();
        assertThat(predicate3.isCalled()).isFalse();
        predicate1.reset();
        predicate3.reset();
        final PredicateWrapper<Object> predicate7 = predicate5.negate();
        assertThat(predicate7.test(this)).isFalse();
        assertThat(predicate1.isCalled()).isTrue();
        assertThat(predicate3.isCalled()).isFalse();
        predicate1.reset();
        predicate3.reset();
        assertThat(predicate7.test(null)).isTrue();
        assertThat(predicate1.isCalled()).isTrue();
        assertThat(predicate3.isCalled()).isTrue();
        predicate1.reset();
        predicate3.reset();
        assertThat(negative().or(positive()).test(null)).isTrue();
        assertThat(negative().and(positive()).test("test")).isFalse();
        assertThat(notNull().or(isNull()).test(null)).isTrue();
        assertThat(notNull().and(isNull()).test("test")).isFalse();
    }

    @Test
    public void testPredicateContext() {

        assertThat(wrap(new TestPredicate()).hasStaticScope()).isTrue();
        assertThat(wrap(new Predicate<Object>() {

            public boolean test(final Object o) {

                return false;
            }
        }).hasStaticScope()).isFalse();
    }

    @Test
    public void testPredicateEquals() {

        final TestPredicate predicate1 = new TestPredicate();
        assertThat(wrap(predicate1)).isEqualTo(wrap(predicate1));
        final PredicateWrapper<Object> predicate2 = wrap(predicate1);
        assertThat(predicate2).isEqualTo(predicate2);
        assertThat(predicate2).isNotEqualTo(null);
        assertThat(predicate2).isNotEqualTo("test");
        assertThat(wrap(predicate1).and(predicate2).hashCode()).isEqualTo(
                predicate2.and(predicate2).hashCode());
        assertThat(wrap(predicate1).and(predicate2)).isEqualTo(predicate2.and(predicate2));
        assertThat(predicate2.and(predicate2)).isEqualTo(wrap(predicate1).and(predicate2));
        assertThat(wrap(predicate1).and(predicate2).hashCode()).isEqualTo(
                predicate2.and(predicate1).hashCode());
        assertThat(wrap(predicate1).and(predicate2)).isEqualTo(predicate2.and(predicate1));
        assertThat(predicate2.and(predicate1)).isEqualTo(wrap(predicate1).and(predicate2));
        assertThat(wrap(predicate1).and(predicate2).hashCode()).isNotEqualTo(
                predicate2.and(predicate2.and(predicate1)).hashCode());
        assertThat(wrap(predicate1).and(predicate2)).isNotEqualTo(
                predicate2.and(predicate2.and(predicate1)));
        assertThat(predicate2.and(predicate2.and(predicate1))).isNotEqualTo(
                wrap(predicate1).and(predicate2));
        assertThat(wrap(predicate1).and(predicate1).hashCode()).isNotEqualTo(
                predicate2.and(predicate2.and(predicate1)).hashCode());
        assertThat(wrap(predicate1).and(predicate1)).isNotEqualTo(
                predicate2.and(predicate2.and(predicate1)));
        assertThat(predicate2.and(predicate2.and(predicate1))).isNotEqualTo(
                wrap(predicate1).and(predicate1));
        assertThat(predicate2.and(predicate1).hashCode()).isNotEqualTo(
                predicate2.and(positive()).hashCode());
        assertThat(predicate2.and(predicate1)).isNotEqualTo(predicate2.and(positive()));
        assertThat(predicate2.and(positive())).isNotEqualTo(predicate2.and(predicate1));
        assertThat(wrap(predicate1).or(predicate2).hashCode()).isEqualTo(
                predicate2.or(predicate2).hashCode());
        assertThat(wrap(predicate1).or(predicate2)).isEqualTo(predicate2.or(predicate2));
        assertThat(predicate2.or(predicate2)).isEqualTo(wrap(predicate1).or(predicate2));
        assertThat(wrap(predicate1).or(predicate2).hashCode()).isEqualTo(
                predicate2.or(predicate1).hashCode());
        assertThat(wrap(predicate1).or(predicate2)).isEqualTo(predicate2.or(predicate1));
        assertThat(predicate2.or(predicate1)).isEqualTo(wrap(predicate1).or(predicate2));
        assertThat(wrap(predicate1).or(predicate2).hashCode()).isNotEqualTo(
                predicate2.or(predicate2.or(predicate1)).hashCode());
        assertThat(wrap(predicate1).or(predicate2)).isNotEqualTo(
                predicate2.or(predicate2.or(predicate1)));
        assertThat(predicate2.or(predicate2.or(predicate1))).isNotEqualTo(
                wrap(predicate1).or(predicate2));
        assertThat(wrap(predicate1).or(predicate1).hashCode()).isNotEqualTo(
                predicate2.or(predicate2.or(predicate1)).hashCode());
        assertThat(wrap(predicate1).or(predicate1)).isNotEqualTo(
                predicate2.or(predicate2.or(predicate1)));
        assertThat(predicate2.or(predicate2.or(predicate1))).isNotEqualTo(
                wrap(predicate1).or(predicate1));
        assertThat(predicate2.or(predicate1).hashCode()).isNotEqualTo(
                predicate2.or(positive()).hashCode());
        assertThat(predicate2.or(predicate1)).isNotEqualTo(predicate2.or(positive()));
        assertThat(predicate2.or(positive())).isNotEqualTo(predicate2.or(predicate1));
        assertThat(predicate2.negate().negate()).isEqualTo(wrap(predicate1));
        assertThat(predicate2.and(predicate1).negate()).isEqualTo(
                predicate2.negate().or(wrap(predicate1).negate()));
        assertThat(predicate2.and(predicate1).negate().hashCode()).isEqualTo(
                predicate2.negate().or(wrap(predicate1).negate()).hashCode());
        final PredicateWrapper<Object> chain =
                predicate2.negate().or(predicate2.negate().and(predicate2.negate()));
        assertThat(predicate2.and(predicate2.or(predicate1)).negate()).isEqualTo(chain);
        assertThat(negative().negate()).isEqualTo(positive());
        assertThat(positive().negate()).isEqualTo(negative());
        assertThat(notNull().negate()).isEqualTo(isNull());
        assertThat(isNull().negate()).isEqualTo(notNull());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testPredicateError() {

        try {

            new PredicateWrapper<Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap((Predicate<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap(new TestPredicate()).and(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap(new TestPredicate()).or(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            negative().and(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            positive().or(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testSameAsPredicate() {

        final Identity instance = new Identity();
        final PredicateWrapper<Object> predicate = isSame(instance);
        assertThat(predicate.test(instance)).isTrue();
        assertThat(predicate.test(new Identity())).isFalse();
        assertThat(predicate.test(1)).isFalse();
        assertThat(predicate.test(null)).isFalse();
        final PredicateWrapper<Object> predicate1 = isSame(null);
        assertThat(predicate1.test(instance)).isFalse();
        assertThat(predicate1.test(1)).isFalse();
        assertThat(predicate1.test(null)).isTrue();
    }

    @Test
    public void testSameAsPredicateEquals() {

        final Identity instance = new Identity();
        final PredicateWrapper<Object> predicate = isSame(instance);
        assertThat(predicate).isEqualTo(predicate);
        assertThat(predicate).isEqualTo(isSame(instance));
        assertThat(predicate).isNotEqualTo(isSame(new Identity()));
        assertThat(predicate).isNotEqualTo(isSame(1.1));
        assertThat(predicate).isNotEqualTo("");
        assertThat(predicate.hashCode()).isEqualTo(isSame(instance).hashCode());
        assertThat(isSame(null)).isEqualTo(isNull());
    }

    @Test
    public void testSink() {

        final TestConsumer consumer1 = new TestConsumer();
        final ConsumerWrapper<Object> consumer2 = sink().andThen(consumer1);
        consumer2.accept("test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(sink()).isSameAs(sink());
    }

    @Test
    public void testSupplier() {

        final TestSupplier supplier1 = new TestSupplier();
        final SupplierWrapper<Object> supplier2 = wrap(supplier1);
        assertThat(wrap(supplier2)).isSameAs(supplier2);
        assertThat(supplier2.get()).isSameAs(supplier1);
        assertThat(supplier1.isCalled()).isTrue();
        supplier1.reset();
        final TestFunction function = new TestFunction();
        final SupplierWrapper<Object> supplier3 = supplier2.andThen(function);
        assertThat(supplier3.get()).isSameAs(supplier1);
        assertThat(supplier1.isCalled()).isTrue();
        assertThat(function.isCalled()).isTrue();
        assertThat(constant("test").andThen(new Function<String, Integer>() {

            public Integer apply(final String s) {

                return s.length();
            }
        }).andThen(new Function<Integer, Integer>() {

            public Integer apply(final Integer integer) {

                return integer * 3;
            }
        }).get()).isEqualTo(12);
    }

    @Test
    public void testSupplierContext() {

        assertThat(wrap(new TestSupplier()).hasStaticScope()).isTrue();
        assertThat(wrap(new Supplier<Object>() {

            public Object get() {

                return null;
            }
        }).hasStaticScope()).isFalse();
    }

    @Test
    public void testSupplierEquals() {

        final TestSupplier supplier1 = new TestSupplier();
        assertThat(wrap(supplier1)).isEqualTo(wrap(supplier1));
        final SupplierWrapper<Object> supplier2 = wrap(supplier1);
        assertThat(supplier2).isEqualTo(supplier2);
        assertThat(supplier2).isNotEqualTo(null);
        assertThat(supplier2).isNotEqualTo("test");
        final TestFunction function = new TestFunction();
        assertThat(wrap(supplier1).andThen(function).hashCode()).isEqualTo(
                supplier2.andThen(function).hashCode());
        assertThat(wrap(supplier1).andThen(function)).isEqualTo(supplier2.andThen(function));
        assertThat(supplier2.andThen(function)).isEqualTo(wrap(supplier1).andThen(function));
        assertThat(wrap(supplier1).andThen(wrap(function)).hashCode()).isEqualTo(
                supplier2.andThen(function).hashCode());
        assertThat(wrap(supplier1).andThen(wrap(function))).isEqualTo(supplier2.andThen(function));
        assertThat(supplier2.andThen(function)).isEqualTo(wrap(supplier1).andThen(wrap(function)));
        assertThat(wrap(supplier1).andThen(wrap(function)).hashCode()).isNotEqualTo(
                supplier2.andThen(wrap(function).andThen(function)).hashCode());
        assertThat(wrap(supplier1).andThen(wrap(function))).isNotEqualTo(
                supplier2.andThen(wrap(function).andThen(function)));
        assertThat(supplier2.andThen(wrap(function).andThen(function))).isNotEqualTo(
                wrap(supplier1).andThen(wrap(function)));
        assertThat(wrap(supplier1).andThen(function).hashCode()).isNotEqualTo(
                supplier2.andThen(wrap(function).andThen(function)).hashCode());
        assertThat(wrap(supplier1).andThen(function)).isNotEqualTo(
                supplier2.andThen(wrap(function).andThen(function)));
        assertThat(supplier2.andThen(wrap(function).andThen(function))).isNotEqualTo(
                wrap(supplier1).andThen(function));
        assertThat(supplier2.andThen(function).hashCode()).isNotEqualTo(
                supplier2.andThen(identity()).hashCode());
        assertThat(supplier2.andThen(function)).isNotEqualTo(supplier2.andThen(identity()));
        assertThat(supplier2.andThen(identity())).isNotEqualTo(supplier2.andThen(function));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSupplierError() {

        try {

            new SupplierWrapper<Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            wrap(new TestSupplier()).andThen(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class Identity {

        @Override
        public boolean equals(final Object obj) {

            return (obj != null) && (obj.getClass() == Identity.class);
        }
    }

    private static class TestBiConsumer implements BiConsumer<Object, Object> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public void accept(final Object in1, final Object in2) {

            mIsCalled = true;
        }
    }

    private static class TestBiFunction implements BiFunction<Object, Object, Object> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public Object apply(final Object in1, final Object in2) {

            mIsCalled = true;
            return in2;
        }


    }

    private static class TestConsumer implements Consumer<Object> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public void accept(final Object out) {

            mIsCalled = true;
        }
    }

    private static class TestFunction implements Function<Object, Object> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public Object apply(final Object in) {

            mIsCalled = true;
            return in;
        }
    }

    private static class TestPredicate implements Predicate<Object> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public boolean test(final Object in) {

            mIsCalled = true;
            return in != null;
        }
    }

    private static class TestSupplier implements Supplier<Object> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public Object get() {

            mIsCalled = true;
            return this;
        }
    }
}
