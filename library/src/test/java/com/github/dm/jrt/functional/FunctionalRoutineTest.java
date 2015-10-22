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
package com.github.dm.jrt.functional;

import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static com.github.dm.jrt.functional.Functions.notNull;
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

        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .thenAsyncAccumulate(new BiFunction<String, String, String>() {

                               public String apply(final String s, final String s2) {

                                   return s + s2;
                               }
                           })
                           .asyncCall("test1", "test2", "test3")
                           .afterMax(seconds(3))
                           .all()).containsExactly("test1test2test3");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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

        assertThat(JRoutine.functional()
                           .buildRoutine()
                           .asyncCall("test")
                           .afterMax(seconds(3))
                           .all()).containsExactly("test");
        assertThat(JRoutine.functional().buildFrom(new Supplier<String>() {

            public String get() {

                return "test";
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsExactly("test");
        assertThat(JRoutine.functional()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .buildFrom(new CommandInvocation<String>() {

                               public void onResult(@NotNull final ResultChannel<String> result) {

                                   result.pass("test1", "test2", "test3");
                               }
                           })
                           .asyncCall()
                           .afterMax(seconds(3))
                           .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutine.functional()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .buildFrom(new Consumer<ResultChannel<String>>() {

                               public void accept(final ResultChannel<String> result) {

                                   result.pass("test1", "test2", "test3");
                               }
                           })
                           .asyncCall()
                           .afterMax(seconds(3))
                           .all()).containsExactly("test1", "test2", "test3");
    }

    private static void internalTestLift() {

        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .thenLift(
                                   new Function<FunctionalRoutine<String, String>,
                                           FunctionalRoutine<String, String>>() {

                                       public FunctionalRoutine<String, String> apply(
                                               final FunctionalRoutine<String, String> routine) {

                                           return JRoutine.functional()
                                                          .<String>buildRoutine()
                                                          .thenSyncFilter(notNull())
                                                          .thenAsyncMap(routine);
                                       }
                                   })
                           .asyncCall("test1", null, "test2", null)
                           .afterMax(seconds(3))
                           .all()).containsExactly("test1", "test2");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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

                                           return JRoutine.functional()
                                                          .<String>buildRoutine()
                                                          .thenSyncFilter(notNull())
                                                          .thenAsyncMap(routine);
                                       }
                                   })
                           .asyncCall("test1", null, "test2", null)
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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

                                           return JRoutine.functional()
                                                          .<String>buildRoutine()
                                                          .thenSyncFilter(notNull())
                                                          .thenAsyncMap(routine);
                                       }
                                   })
                           .asyncCall("test1", null, "test2", null)
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
    }

    private static void internalTestMapConsumer() {

        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .thenParallelMap(new BiConsumer<String, ResultChannel<String>>() {

                               public void accept(final String s,
                                       final ResultChannel<String> result) {

                                   result.pass(s.toUpperCase());
                               }
                           })
                           .asyncCall("test1", "test2")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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

        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .thenParallelMap(new FilterInvocation<String, String>() {

                               public void onInput(final String input,
                                       @NotNull final ResultChannel<String> result) {

                                   result.pass(input.toUpperCase());
                               }
                           })
                           .asyncCall("test1", "test2")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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

        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .thenParallelMap(new Function<String, String>() {

                               public String apply(final String s) {

                                   return s.toUpperCase();
                               }
                           })
                           .asyncCall("test1", "test2")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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

        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .thenAsyncReduce(
                                   new BiConsumer<List<? extends String>, ResultChannel<String>>() {

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
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .thenSyncReduce(
                                   new BiConsumer<List<? extends String>, ResultChannel<String>>() {

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

        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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

            JRoutine.functional()
                    .buildRoutine()
                    .thenAsyncAccumulate(new BiFunction<Object, Object, Object>() {

                        public Object apply(final Object o, final Object o2) {

                            return null;
                        }
                    });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
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

            JRoutine.functional().buildRoutine().thenAsyncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenSyncAccumulate(null);

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

            JRoutine.functional().buildFrom(new Consumer<ResultChannel<String>>() {

                public void accept(final ResultChannel<String> stringResultChannel) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildFrom(new CommandInvocation<String>() {

                public void onResult(@NotNull final ResultChannel<String> result) {

                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildFrom(new Supplier<String>() {

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

            JRoutine.functional().buildFrom((Consumer<ResultChannel<String>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildFrom((CommandInvocation<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildFrom((Supplier<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFilter() {

        assertThat(JRoutine.functional()
                           .buildRoutine()
                           .thenAsyncFilter(notNull())
                           .asyncCall(null, "test")
                           .afterMax(seconds(3))
                           .all()).containsExactly("test");
        assertThat(JRoutine.functional()
                           .buildRoutine()
                           .thenParallelFilter(notNull())
                           .asyncCall(null, "test")
                           .afterMax(seconds(3))
                           .all()).containsExactly("test");
        assertThat(JRoutine.functional()
                           .buildRoutine()
                           .thenSyncFilter(notNull())
                           .asyncCall(null, "test")
                           .afterMax(seconds(3))
                           .all()).containsExactly("test");
    }

    @Test
    public void testFilterContextError() {

        try {

            JRoutine.functional().buildRoutine().thenAsyncFilter(new Predicate<Object>() {

                public boolean test(final Object o) {

                    return false;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenParallelFilter(new Predicate<Object>() {

                public boolean test(final Object o) {

                    return false;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenSyncFilter(new Predicate<Object>() {

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

            JRoutine.functional().buildRoutine().thenAsyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenParallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenSyncFilter(null);

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

            JRoutine.functional().buildRoutine().thenFlatLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenLift(null);

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

            JRoutine.functional()
                    .buildRoutine()
                    .thenAsyncMap(new BiConsumer<Object, ResultChannel<Object>>() {

                        public void accept(final Object o, final ResultChannel<Object> result) {

                        }
                    });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .thenParallelMap(new BiConsumer<Object, ResultChannel<Object>>() {

                        public void accept(final Object o, final ResultChannel<Object> result) {

                        }
                    });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
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

            JRoutine.functional()
                    .buildRoutine()
                    .thenAsyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .thenParallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .thenAsyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

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

            JRoutine.functional()
                    .buildRoutine()
                    .thenAsyncMap(new FilterInvocation<Object, Object>() {

                        public void onInput(final Object input,
                                @NotNull final ResultChannel<Object> result) {

                        }
                    });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .thenParallelMap(new FilterInvocation<Object, Object>() {

                        public void onInput(final Object input,
                                @NotNull final ResultChannel<Object> result) {

                        }
                    });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
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

            JRoutine.functional()
                    .buildRoutine()
                    .thenAsyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .thenParallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .thenAsyncMap((FilterInvocation<Object, Object>) null);

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

            JRoutine.functional().buildRoutine().thenAsyncMap(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenParallelMap(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenSyncMap(new Function<Object, Object>() {

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

            JRoutine.functional().buildRoutine().thenAsyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenParallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenAsyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapRoutine() {

        final Routine<String, String> routine = JRoutine.on(new UpperCase()).buildRoutine();
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .thenAsyncMap(routine)
                           .asyncCall("test1", "test2")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .thenParallelMap(routine)
                           .asyncCall("test1", "test2")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
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

            JRoutine.functional().buildRoutine().thenAsyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenParallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenAsyncMap((Routine<Object, Object>) null);

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

            JRoutine.functional()
                    .buildRoutine()
                    .thenAsyncReduce(new BiConsumer<List<?>, ResultChannel<Object>>() {

                        public void accept(final List<?> objects,
                                final ResultChannel<Object> result) {

                        }
                    });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
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

            JRoutine.functional()
                    .buildRoutine()
                    .thenAsyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
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

            JRoutine.functional().buildRoutine().thenAsyncReduce(new Function<List<?>, Object>() {

                public Object apply(final List<?> objects) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenSyncReduce(new Function<List<?>, Object>() {

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

            JRoutine.functional().buildRoutine().thenAsyncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().thenSyncReduce((Function<List<?>, Object>) null);

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
