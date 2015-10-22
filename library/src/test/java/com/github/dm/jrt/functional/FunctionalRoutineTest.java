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
import com.github.dm.jrt.core.JRoutine;
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

    private static void internalTestMapConsumer() {

        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .asyncMap(new BiConsumer<String, ResultChannel<String>>() {

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
                           .parallelMap(new BiConsumer<String, ResultChannel<String>>() {

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
                           .syncMap(new BiConsumer<String, ResultChannel<String>>() {

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
                           .asyncMap(new FilterInvocation<String, String>() {

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
                           .parallelMap(new FilterInvocation<String, String>() {

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
                           .syncMap(new FilterInvocation<String, String>() {

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
                           .asyncMap(new Function<String, String>() {

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
                           .parallelMap(new Function<String, String>() {

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
                           .syncMap(new Function<String, String>() {

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
                           .asyncReduce(
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
                           .syncReduce(
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
                           .asyncFilter(notNull())
                           .asyncCall(null, "test")
                           .afterMax(seconds(3))
                           .all()).containsExactly("test");
        assertThat(JRoutine.functional()
                           .buildRoutine()
                           .parallelFilter(notNull())
                           .asyncCall(null, "test")
                           .afterMax(seconds(3))
                           .all()).containsExactly("test");
        assertThat(JRoutine.functional()
                           .buildRoutine()
                           .syncFilter(notNull())
                           .asyncCall(null, "test")
                           .afterMax(seconds(3))
                           .all()).containsExactly("test");
    }

    @Test
    public void testFilterContextError() {

        try {

            JRoutine.functional().buildRoutine().asyncFilter(new Predicate<Object>() {

                public boolean test(final Object o) {

                    return false;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().parallelFilter(new Predicate<Object>() {

                public boolean test(final Object o) {

                    return false;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().syncFilter(new Predicate<Object>() {

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

            JRoutine.functional().buildRoutine().asyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().parallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().syncFilter(null);

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
                    .asyncMap(new BiConsumer<Object, ResultChannel<Object>>() {

                        public void accept(final Object o, final ResultChannel<Object> result) {

                        }
                    });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .parallelMap(new BiConsumer<Object, ResultChannel<Object>>() {

                        public void accept(final Object o, final ResultChannel<Object> result) {

                        }
                    });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .syncMap(new BiConsumer<Object, ResultChannel<Object>>() {

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
                    .asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .parallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

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

            JRoutine.functional().buildRoutine().asyncMap(new FilterInvocation<Object, Object>() {

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
                    .parallelMap(new FilterInvocation<Object, Object>() {

                        public void onInput(final Object input,
                                @NotNull final ResultChannel<Object> result) {

                        }
                    });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().syncMap(new FilterInvocation<Object, Object>() {

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

            JRoutine.functional().buildRoutine().asyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .parallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().asyncMap((FilterInvocation<Object, Object>) null);

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

            JRoutine.functional().buildRoutine().asyncMap(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().parallelMap(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    return null;
                }
            });

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().syncMap(new Function<Object, Object>() {

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

            JRoutine.functional().buildRoutine().asyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().parallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().asyncMap((Function<Object, Object>) null);

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
                           .asyncMap(routine)
                           .asyncCall("test1", "test2")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .parallelMap(routine)
                           .asyncCall("test1", "test2")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutine.functional()
                           .<String>buildRoutine()
                           .invocations()
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .syncMap(routine)
                           .asyncCall("test1", "test2")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {

        try {

            JRoutine.functional().buildRoutine().asyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().parallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional().buildRoutine().asyncMap((Routine<Object, Object>) null);

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
                    .asyncReduce(new BiConsumer<List<?>, ResultChannel<Object>>() {

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
                    .syncReduce(new BiConsumer<List<?>, ResultChannel<Object>>() {

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
                    .asyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.functional()
                    .buildRoutine()
                    .syncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

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
