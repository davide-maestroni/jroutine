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
package com.github.dm.jrt.function;

import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Functional routine unit tests.
 * <p/>
 * Created by davide-maestroni on 10/22/2015.
 */
public class FunctionalRoutineTest {

    public static void internalTestAccumulate() {

        assertThat(Functions.functional()
                            .thenAsyncAccumulate(new BiFunction<String, String, String>() {

                                public String apply(final String s, final String s2) {

                                    return s + s2;
                                }
                            })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
        assertThat(
                Functions.functional().thenSyncAccumulate(new BiFunction<String, String, String>() {

                    public String apply(final String s, final String s2) {

                        return s + s2;
                    }
                }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenAsyncAccumulate(new BiFunction<String, String, String>() {

                                public String apply(final String s, final String s2) {

                                    return s + s2;
                                }
                            })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenSyncAccumulate(new BiFunction<String, String, String>() {

                                public String apply(final String s, final String s2) {

                                    return s + s2;
                                }
                            })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
    }

    private static void internalTestBuilder() {

        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.factoryOf())
                            .asyncCall("test")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test");
        assertThat(Functions.functional().routineFrom(new Supplier<String>() {

            public String get() {

                return "test";
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsExactly("test");
        assertThat(Functions.functional().routineFrom(new CommandInvocation<String>() {

            public void onResult(@NotNull final ResultChannel<String> result) {

                result.pass("test1", "test2", "test3");
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsOnly("test1", "test2", "test3");
        assertThat(Functions.functional().routineFrom(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("test1", "test2", "test3");
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsOnly("test1", "test2", "test3");
        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .routineFrom(new CommandInvocation<String>() {

                                public void onResult(@NotNull final ResultChannel<String> result) {

                                    result.pass("test1", "test2", "test3");
                                }
                            })
                            .asyncCall()
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1", "test2", "test3");
        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .routineFrom(new Consumer<ResultChannel<String>>() {

                                public void accept(final ResultChannel<String> result) {

                                    result.pass("test1", "test2", "test3");
                                }
                            })
                            .asyncCall()
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1", "test2", "test3");
    }

    private static void internalTestLift() {

        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenLift(
                                    new Function<FunctionalRoutine<String, String>,
                                            FunctionalRoutine<String, String>>() {

                                        public FunctionalRoutine<String, String> apply(
                                                final FunctionalRoutine<String, String> routine) {

                                            return Functions.functional()
                                                            .thenSyncFilter(
                                                                    Functions.<String>notNull())
                                                            .thenAsyncMap(routine);
                                        }
                                    })
                            .asyncCall("test1", null, "test2", null)
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1", "test2");
        assertThat(Functions.functional()
                            .thenAsyncMap(new FilterInvocation<String, String>() {

                                public void onInput(final String input,
                                        @NotNull final ResultChannel<String> result) {

                                    result.pass(input.toUpperCase());
                                }
                            })
                            .thenFlatLift(
                                    new Function<FunctionalRoutine<String, String>,
                                            FunctionalRoutine<String, String>>() {

                                        public FunctionalRoutine<String, String> apply(
                                                final FunctionalRoutine<String, String> routine) {

                                            return Functions.functional()
                                                            .thenSyncFilter(
                                                                    Functions.<String>notNull())
                                                            .thenAsyncMap(routine);
                                        }
                                    })
                            .asyncCall("test1", null, "test2", null)
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenAsyncMap(new FilterInvocation<String, String>() {

                                public void onInput(final String input,
                                        @NotNull final ResultChannel<String> result) {

                                    result.pass(input.toUpperCase());
                                }
                            })
                            .thenLift(
                                    new Function<FunctionalRoutine<String, String>,
                                            Routine<String, String>>() {

                                        public FunctionalRoutine<String, String> apply(
                                                final FunctionalRoutine<String, String> routine) {

                                            return Functions.functional()
                                                            .thenSyncFilter(
                                                                    Functions.<String>notNull())
                                                            .thenAsyncMap(routine);
                                        }
                                    })
                            .asyncCall("test1", null, "test2", null)
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
    }

    private static void internalTestMapConsumer() {

        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(new BiConsumer<String, ResultChannel<String>>() {

                                public void accept(final String s,
                                        final ResultChannel<String> result) {

                                    result.pass(s.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenParallelMap(new BiConsumer<String, ResultChannel<String>>() {

                                public void accept(final String s,
                                        final ResultChannel<String> result) {

                                    result.pass(s.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(new BiConsumer<String, ResultChannel<String>>() {

                                public void accept(final String s,
                                        final ResultChannel<String> result) {

                                    result.pass(s.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(new BiConsumer<String, ResultChannel<String>>() {

                                public void accept(final String s,
                                        final ResultChannel<String> result) {

                                    result.pass(s.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenParallelMap(new BiConsumer<String, ResultChannel<String>>() {

                                public void accept(final String s,
                                        final ResultChannel<String> result) {

                                    result.pass(s.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(new BiConsumer<String, ResultChannel<String>>() {

                                public void accept(final String s,
                                        final ResultChannel<String> result) {

                                    result.pass(s.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
    }

    private static void internalTestMapFilter() {

        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(new FilterInvocation<String, String>() {

                                public void onInput(final String input,
                                        @NotNull final ResultChannel<String> result) {

                                    result.pass(input.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional().thenParallelMap(new FilterInvocation<String, String>() {

            public void onInput(final String input, @NotNull final ResultChannel<String> result) {

                result.pass(input.toUpperCase());
            }
        }).asyncCall("test1", "test2").afterMax(seconds(3)).all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(new FilterInvocation<String, String>() {

                                public void onInput(final String input,
                                        @NotNull final ResultChannel<String> result) {

                                    result.pass(input.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(new FilterInvocation<String, String>() {

                                public void onInput(final String input,
                                        @NotNull final ResultChannel<String> result) {

                                    result.pass(input.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenParallelMap(new FilterInvocation<String, String>() {

                                public void onInput(final String input,
                                        @NotNull final ResultChannel<String> result) {

                                    result.pass(input.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(new FilterInvocation<String, String>() {

                                public void onInput(final String input,
                                        @NotNull final ResultChannel<String> result) {

                                    result.pass(input.toUpperCase());
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
    }

    private static void internalTestMapFunction() {

        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(new Function<String, String>() {

                                public String apply(final String s) {

                                    return s.toUpperCase();
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional().thenParallelMap(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).asyncCall("test1", "test2").afterMax(seconds(3)).all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(new Function<String, String>() {

                                public String apply(final String s) {

                                    return s.toUpperCase();
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(new Function<String, String>() {

                                public String apply(final String s) {

                                    return s.toUpperCase();
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenParallelMap(new Function<String, String>() {

                                public String apply(final String s) {

                                    return s.toUpperCase();
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(new Function<String, String>() {

                                public String apply(final String s) {

                                    return s.toUpperCase();
                                }
                            })
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
    }

    private static void internalTestReduceConsumer() {

        assertThat(Functions.functional()
                            .thenAsyncReduce(
                                    new BiConsumer<List<? extends String>, ResultChannel<String>>
                                            () {

                                        public void accept(final List<? extends String> strings,
                                                final ResultChannel<String> result) {

                                            final StringBuilder builder = new StringBuilder();

                                            for (final String string : strings) {

                                                builder.append(string);
                                            }

                                            result.pass(builder.toString());
                                        }
                                    })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
        assertThat(Functions.functional()
                            .thenSyncReduce(
                                    new BiConsumer<List<? extends String>, ResultChannel<String>>
                                            () {

                                        public void accept(final List<? extends String> strings,
                                                final ResultChannel<String> result) {

                                            final StringBuilder builder = new StringBuilder();

                                            for (final String string : strings) {

                                                builder.append(string);
                                            }

                                            result.pass(builder.toString());
                                        }
                                    })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenAsyncReduce(
                                    new BiConsumer<List<? extends String>, ResultChannel<String>>
                                            () {

                                        public void accept(final List<? extends String> strings,
                                                final ResultChannel<String> result) {

                                            final StringBuilder builder = new StringBuilder();

                                            for (final String string : strings) {

                                                builder.append(string);
                                            }

                                            result.pass(builder.toString());
                                        }
                                    })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenSyncReduce(
                                    new BiConsumer<List<? extends String>, ResultChannel<String>>
                                            () {

                                        public void accept(final List<? extends String> strings,
                                                final ResultChannel<String> result) {

                                            final StringBuilder builder = new StringBuilder();

                                            for (final String string : strings) {

                                                builder.append(string);
                                            }

                                            result.pass(builder.toString());
                                        }
                                    })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
    }

    private static void internalTestReduceFunction() {

        assertThat(Functions.functional()
                            .thenAsyncReduce(new Function<List<? extends String>, String>() {

                                public String apply(final List<? extends String> strings) {

                                    final StringBuilder builder = new StringBuilder();

                                    for (final String string : strings) {

                                        builder.append(string);
                                    }

                                    return builder.toString();
                                }
                            })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
        assertThat(Functions.functional()
                            .thenSyncReduce(new Function<List<? extends String>, String>() {

                                public String apply(final List<? extends String> strings) {

                                    final StringBuilder builder = new StringBuilder();

                                    for (final String string : strings) {

                                        builder.append(string);
                                    }

                                    return builder.toString();
                                }
                            })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenAsyncReduce(new Function<List<? extends String>, String>() {

                                public String apply(final List<? extends String> strings) {

                                    final StringBuilder builder = new StringBuilder();

                                    for (final String string : strings) {

                                        builder.append(string);
                                    }

                                    return builder.toString();
                                }
                            })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenSyncReduce(new Function<List<? extends String>, String>() {

                                public String apply(final List<? extends String> strings) {

                                    final StringBuilder builder = new StringBuilder();

                                    for (final String string : strings) {

                                        builder.append(string);
                                    }

                                    return builder.toString();
                                }
                            })
                            .asyncCall("test1", "test2", "test3")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test1test2test3");
    }

    @Test
    public void testAccumulate() {

        internalTestAccumulate();
    }

    @Test
    public void testAccumulateContextError() {

        try {

            Functions.functional().thenAsyncAccumulate(new BiFunction<Object, Object, Object>() {

                public Object apply(final Object o, final Object o2) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenSyncAccumulate(new BiFunction<Object, Object, Object>() {

                public Object apply(final Object o, final Object o2) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncAccumulate(new BiFunction<Object, Object, Object>() {

                         public Object apply(final Object o, final Object o2) {

                             return null;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncAccumulate(new BiFunction<Object, Object, Object>() {

                         public Object apply(final Object o, final Object o2) {

                             return null;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testAccumulateNullPointerError() {

        try {

            Functions.functional().thenAsyncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilder() {

        internalTestBuilder();
    }

    @Test
    public void testBuilderContextError() {

        try {

            Functions.functional().routineFrom(new Consumer<ResultChannel<String>>() {

                public void accept(final ResultChannel<String> stringResultChannel) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().routineFrom(new CommandInvocation<String>() {

                public void onResult(@NotNull final ResultChannel<String> result) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().routineFrom(new Supplier<String>() {

                public String get() {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBuilderNullPointerError() {

        try {

            Functions.functional().routineFrom((Consumer<ResultChannel<String>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().routineFrom((CommandInvocation<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().routineFrom((Supplier<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFilter() {

        assertThat(Functions.functional()
                            .thenAsyncFilter(Functions.notNull())
                            .asyncCall(null, "test")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test");
        assertThat(Functions.functional()
                            .thenParallelFilter(Functions.notNull())
                            .asyncCall(null, "test")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test");
        assertThat(Functions.functional()
                            .thenSyncFilter(Functions.notNull())
                            .asyncCall(null, "test")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.factoryOf())
                            .thenAsyncFilter(Functions.notNull())
                            .asyncCall(null, "test")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.factoryOf())
                            .thenParallelFilter(Functions.notNull())
                            .asyncCall(null, "test")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.factoryOf())
                            .thenSyncFilter(Functions.notNull())
                            .asyncCall(null, "test")
                            .afterMax(seconds(3))
                            .all()).containsExactly("test");
    }

    @Test
    public void testFilterContextError() {

        try {

            Functions.functional().thenAsyncFilter(new Predicate<Object>() {

                public boolean test(final Object o) {

                    return false;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenParallelFilter(new Predicate<Object>() {

                public boolean test(final Object o) {

                    return false;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenSyncFilter(new Predicate<Object>() {

                public boolean test(final Object o) {

                    return false;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncFilter(new Predicate<Object>() {

                         public boolean test(final Object o) {

                             return false;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelFilter(new Predicate<Object>() {

                         public boolean test(final Object o) {

                             return false;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncFilter(new Predicate<Object>() {

                         public boolean test(final Object o) {

                             return false;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {

        try {

            Functions.functional().thenAsyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenParallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncMap(PassingInvocation.factoryOf()).thenAsyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncMap(PassingInvocation.factoryOf()).thenSyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testLift() {

        internalTestLift();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testLiftNullPointerError() {

        try {

            Functions.functional().thenSyncMap(PassingInvocation.factoryOf()).thenFlatLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncMap(PassingInvocation.factoryOf()).thenLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapConsumer() {

        internalTestMapConsumer();
    }

    @Test
    public void testMapConsumerContextError() {

        try {

            Functions.functional().thenAsyncMap(new BiConsumer<Object, ResultChannel<Object>>() {

                public void accept(final Object o, final ResultChannel<Object> result) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenParallelMap(new BiConsumer<Object, ResultChannel<Object>>() {

                public void accept(final Object o, final ResultChannel<Object> result) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenSyncMap(new BiConsumer<Object, ResultChannel<Object>>() {

                public void accept(final Object o, final ResultChannel<Object> result) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncMap(new BiConsumer<Object, ResultChannel<Object>>() {

                         public void accept(final Object o, final ResultChannel<Object> result) {

                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelMap(new BiConsumer<Object, ResultChannel<Object>>() {

                         public void accept(final Object o, final ResultChannel<Object> result) {

                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncMap(new BiConsumer<Object, ResultChannel<Object>>() {

                         public void accept(final Object o, final ResultChannel<Object> result) {

                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapConsumerNullPointerError() {

        try {

            Functions.functional().thenAsyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenParallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFactory() {

        final UpperCase factory = new UpperCase();
        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(factory)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenParallelMap(factory)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(factory)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(factory)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenParallelMap(factory)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(factory)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFactoryNullPointerError() {

        try {

            Functions.functional().thenAsyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenParallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFilter() {

        internalTestMapFilter();
    }

    @Test
    public void testMapFilterContextError() {

        try {

            Functions.functional().thenAsyncMap(new FilterInvocation<Object, Object>() {

                public void onInput(final Object input,
                        @NotNull final ResultChannel<Object> result) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenParallelMap(new FilterInvocation<Object, Object>() {

                public void onInput(final Object input,
                        @NotNull final ResultChannel<Object> result) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenSyncMap(new FilterInvocation<Object, Object>() {

                public void onInput(final Object input,
                        @NotNull final ResultChannel<Object> result) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncMap(new FilterInvocation<Object, Object>() {

                         public void onInput(final Object input,
                                 @NotNull final ResultChannel<Object> result) {

                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelMap(new FilterInvocation<Object, Object>() {

                         public void onInput(final Object input,
                                 @NotNull final ResultChannel<Object> result) {

                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncMap(new FilterInvocation<Object, Object>() {

                         public void onInput(final Object input,
                                 @NotNull final ResultChannel<Object> result) {

                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {

        try {

            Functions.functional().thenAsyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenParallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFunction() {

        internalTestMapFunction();
    }

    @Test
    public void testMapFunctionContextError() {

        try {

            Functions.functional().thenAsyncMap(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenParallelMap(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenSyncMap(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncMap(new Function<Object, Object>() {

                         public Object apply(final Object o) {

                             return null;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelMap(new Function<Object, Object>() {

                         public Object apply(final Object o) {

                             return null;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncMap(new Function<Object, Object>() {

                         public Object apply(final Object o) {

                             return null;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFunctionNullPointerError() {

        try {

            Functions.functional().thenAsyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenParallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapRoutine() {

        final Routine<String, String> routine = JRoutine.on(new UpperCase()).buildRoutine();
        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(routine)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenParallelMap(routine)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(routine)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenAsyncMap(routine)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .thenParallelMap(routine)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsOnly("TEST1", "TEST2");
        assertThat(Functions.functional()
                            .thenSyncMap(PassingInvocation.<String>factoryOf())
                            .invocations()
                            .withOutputOrder(OrderType.BY_CALL)
                            .set()
                            .thenSyncMap(routine)
                            .asyncCall("test1", "test2")
                            .afterMax(seconds(3))
                            .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {

        try {

            Functions.functional().thenAsyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenParallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenParallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullPointer() {

        try {

            new DefaultFunctionalRoutine<Object, Object>(null, DelegationType.ASYNC);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultFunctionalRoutine<Object, Object>(JRoutine.on(PassingInvocation.factoryOf()),
                                                         null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReduceConsumer() {

        internalTestReduceConsumer();
    }

    @Test
    public void testReduceConsumerContextError() {

        try {

            Functions.functional()
                     .thenAsyncReduce(new BiConsumer<List<?>, ResultChannel<Object>>() {

                         public void accept(final List<?> objects,
                                 final ResultChannel<Object> result) {

                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenSyncReduce(new BiConsumer<List<?>, ResultChannel<Object>>() {

                public void accept(final List<?> objects, final ResultChannel<Object> result) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncReduce(new BiConsumer<List<?>, ResultChannel<Object>>() {

                         public void accept(final List<?> objects,
                                 final ResultChannel<Object> result) {

                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncReduce(new BiConsumer<List<?>, ResultChannel<Object>>() {

                         public void accept(final List<?> objects,
                                 final ResultChannel<Object> result) {

                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testReduceConsumerNullPointerError() {

        try {

            Functions.functional()
                     .thenAsyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReduceFunction() {

        internalTestReduceFunction();
    }

    @Test
    public void testReduceFunctionContextError() {

        try {

            Functions.functional().thenAsyncReduce(new Function<List<?>, Object>() {

                public Object apply(final List<?> objects) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional().thenSyncReduce(new Function<List<?>, Object>() {

                public Object apply(final List<?> objects) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncReduce(new Function<List<?>, Object>() {

                         public Object apply(final List<?> objects) {

                             return null;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncReduce(new Function<List<?>, Object>() {

                         public Object apply(final List<?> objects) {

                             return null;
                         }
                     });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testReduceFunctionNullPointerError() {

        try {

            Functions.functional().thenAsyncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional().thenSyncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenAsyncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Functions.functional()
                     .thenSyncMap(PassingInvocation.factoryOf())
                     .thenSyncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class UpperCase extends FilterInvocation<String, String> {

        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

            result.pass(input.toUpperCase());
        }
    }
}
