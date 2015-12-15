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
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.Invocations;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream routine unit tests.
 * <p/>
 * Created by davide-maestroni on 10/22/2015.
 */
public class StreamRoutineTest {

    @Test
    public void testAccumulate() {

        assertThat(
                Streams.streamRoutine().asyncAccumulate(new BiFunction<String, String, String>() {

                    public String apply(final String s, final String s2) {

                        return s + s2;
                    }
                }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(Streams.streamRoutine().syncAccumulate(new BiFunction<String, String, String>() {

            public String apply(final String s, final String s2) {

                return s + s2;
            }
        }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncAccumulate(new BiFunction<String, String, String>() {

                              public String apply(final String s, final String s2) {

                                  return s + s2;
                              }
                          })
                          .asyncCall("test1", "test2", "test3")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncAccumulate(new BiFunction<String, String, String>() {

                              public String apply(final String s, final String s2) {

                                  return s + s2;
                              }
                          })
                          .asyncCall("test1", "test2", "test3")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testAccumulateNullPointerError() {

        try {

            Streams.streamRoutine().asyncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).asyncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).syncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilder() {

        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.factoryOf())
                          .asyncCall("test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(
                Streams.streamRoutine().syncOf().asyncCall().afterMax(seconds(3)).all()).isEmpty();
        assertThat(Streams.streamRoutine()
                          .syncOf((Object[]) null)
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamRoutine()
                          .syncOf("test")
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamRoutine()
                          .syncOf("test1", "test2", "test3")
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine()
                          .syncOf(Arrays.asList("test1", "test2", "test3"))
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine().syncFrom(new Supplier<String>() {

            public String get() {

                return "test";
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsExactly("test");
        assertThat(Streams.streamRoutine()
                          .syncFrom(new Strings("test1", "test2", "test3"))
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine().syncFrom(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("test1", "test2", "test3");
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsOnly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncFrom(new Strings("test1", "test2", "test3"))
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncFrom(new Consumer<ResultChannel<String>>() {

                              public void accept(final ResultChannel<String> result) {

                                  result.pass("test1", "test2", "test3");
                              }
                          })
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(
                Streams.streamRoutine().asyncOf().asyncCall().afterMax(seconds(3)).all()).isEmpty();
        assertThat(Streams.streamRoutine()
                          .asyncOf((Object[]) null)
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamRoutine()
                          .asyncOf("test")
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamRoutine()
                          .asyncOf("test1", "test2", "test3")
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine()
                          .asyncOf(Arrays.asList("test1", "test2", "test3"))
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine().asyncFrom(new Supplier<String>() {

            public String get() {

                return "test";
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsExactly("test");
        assertThat(Streams.streamRoutine()
                          .asyncFrom(new Strings("test1", "test2", "test3"))
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine().asyncFrom(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("test1", "test2", "test3");
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsOnly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncFrom(new Strings("test1", "test2", "test3"))
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncFrom(new Consumer<ResultChannel<String>>() {

                              public void accept(final ResultChannel<String> result) {

                                  result.pass("test1", "test2", "test3");
                              }
                          })
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBuilderNullPointerError() {

        try {

            Streams.streamRoutine().syncFrom((Consumer<ResultChannel<String>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncFrom((CommandInvocation<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncFrom((Supplier<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testError() {

        final AtomicReference<RoutineException> ex = new AtomicReference<RoutineException>();
        OutputChannel<Object> channel =
                Streams.streamRoutine().syncError(new Consumer<RoutineException>() {

                    public void accept(final RoutineException e) {

                        ex.set(e);
                    }
                }).asyncInvoke().after(seconds(3)).result();
        channel.abort(new NumberFormatException());
        channel.afterMax(seconds(3)).checkComplete();
        assertThat(ex.get().getCause()).isExactlyInstanceOf(NumberFormatException.class);
        channel = Streams.streamRoutine().asyncError(new Consumer<RoutineException>() {

            public void accept(final RoutineException e) {

                ex.set(e);
            }
        }).asyncInvoke().after(seconds(3)).result();
        channel.abort(new NumberFormatException());
        channel.afterMax(seconds(3)).checkComplete();
        assertThat(ex.get().getCause()).isExactlyInstanceOf(NumberFormatException.class);
        channel = Streams.streamRoutine()
                         .syncMap(PassingInvocation.factoryOf())
                         .syncError(new Consumer<RoutineException>() {

                             public void accept(final RoutineException e) {

                                 ex.set(e);
                             }
                         })
                         .asyncInvoke()
                         .after(seconds(3))
                         .result();
        channel.abort(new NumberFormatException());
        channel.afterMax(seconds(3)).checkComplete();
        assertThat(ex.get().getCause()).isExactlyInstanceOf(NumberFormatException.class);
        channel = Streams.streamRoutine()
                         .syncMap(PassingInvocation.factoryOf())
                         .asyncError(new Consumer<RoutineException>() {

                             public void accept(final RoutineException e) {

                                 ex.set(e);
                             }
                         })
                         .asyncInvoke()
                         .after(seconds(3))
                         .result();
        channel.abort(new NumberFormatException());
        channel.afterMax(seconds(3)).checkComplete();
        assertThat(ex.get().getCause()).isExactlyInstanceOf(NumberFormatException.class);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testErrorNullPointerError() {

        try {

            Streams.streamRoutine().syncError(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().asyncError(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).syncError(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).asyncError(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFilter() {

        assertThat(Streams.streamRoutine()
                          .asyncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamRoutine()
                          .parallelFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamRoutine()
                          .syncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.factoryOf())
                          .asyncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.factoryOf())
                          .parallelFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.factoryOf())
                          .syncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {

        try {

            Streams.streamRoutine().asyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().parallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).asyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).parallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).syncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testForEach() {

        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(Streams.streamRoutine().syncForEach(new Consumer<String>() {

            public void accept(final String s) {

                list.add(s);
            }
        }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(Streams.streamRoutine().asyncForEach(new Consumer<String>() {

            public void accept(final String s) {

                list.add(s);
            }
        }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncForEach(new Consumer<String>() {

                              public void accept(final String s) {

                                  list.add(s);
                              }
                          })
                          .asyncCall("test1", "test2", "test3")
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncForEach(new Consumer<String>() {

                              public void accept(final String s) {

                                  list.add(s);
                              }
                          })
                          .asyncCall("test1", "test2", "test3")
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testForEachNullPointerError() {

        try {

            Streams.streamRoutine().syncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().asyncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).syncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).asyncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testLift() {

        assertThat(Streams.streamRoutine()
                          .asyncMap(new UpperCase())
                          .flatLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.streamRoutine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.streamRoutine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamRoutine()
                          .asyncMap(new UpperCase())
                          .syncLift(
                                  new Function<StreamRoutine<String, String>, Routine<String,
                                          String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.streamRoutine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.streamRoutine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.streamRoutine()
                          .asyncMap(new UpperCase())
                          .asyncLift(
                                  new Function<StreamRoutine<String, String>, Routine<String,
                                          String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.streamRoutine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.streamRoutine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2");
        assertThat(Streams.streamRoutine()
                          .asyncMap(new UpperCase())
                          .parallelLift(
                                  new Function<StreamRoutine<String, String>, Routine<String,
                                          String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.streamRoutine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testLiftNullPointerError() {

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).flatLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).syncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).asyncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap(PassingInvocation.factoryOf()).parallelLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapConsumer() {

        assertThat(Streams.streamRoutine()
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
        assertThat(Streams.streamRoutine()
                          .parallelMap(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
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
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
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
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelMap(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
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

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapConsumerNullPointerError() {

        try {

            Streams.streamRoutine().asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().parallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFactory() {

        final InvocationFactory<String, String> factory = Invocations.factoryOf(UpperCase.class);
        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .parallelMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFactoryNullPointerError() {

        try {

            Streams.streamRoutine().asyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().parallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFilter() {

        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .parallelMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {

        try {

            Streams.streamRoutine().asyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().parallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFunction() {

        assertThat(Streams.streamRoutine()
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
        assertThat(Streams.streamRoutine().parallelMap(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).asyncCall("test1", "test2").afterMax(seconds(3)).all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
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
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
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
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelMap(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
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

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFunctionNullPointerError() {

        try {

            Streams.streamRoutine().asyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().parallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapRoutine() {

        final Routine<String, String> routine = JRoutine.on(new UpperCase()).buildRoutine();
        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .parallelMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
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

            Streams.streamRoutine().asyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().parallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReduceConsumer() {

        assertThat(Streams.streamRoutine()
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
        assertThat(Streams.streamRoutine()
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
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
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
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
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
    @SuppressWarnings("ConstantConditions")
    public void testReduceConsumerNullPointerError() {

        try {

            Streams.streamRoutine().asyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReduceFunction() {

        assertThat(
                Streams.streamRoutine().asyncReduce(new Function<List<? extends String>, String>() {

                    public String apply(final List<? extends String> strings) {

                        final StringBuilder builder = new StringBuilder();

                        for (final String string : strings) {

                            builder.append(string);
                        }

                        return builder.toString();
                    }
                }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(
                Streams.streamRoutine().syncReduce(new Function<List<? extends String>, String>() {

                    public String apply(final List<? extends String> strings) {

                        final StringBuilder builder = new StringBuilder();

                        for (final String string : strings) {

                            builder.append(string);
                        }

                        return builder.toString();
                    }
                }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncReduce(new Function<List<? extends String>, String>() {

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
        assertThat(Streams.streamRoutine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncReduce(new Function<List<? extends String>, String>() {

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
    @SuppressWarnings("ConstantConditions")
    public void testReduceFunctionNullPointerError() {

        try {

            Streams.streamRoutine().asyncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine().syncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.streamRoutine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class Strings extends CommandInvocation<String> {

        private final String[] mStrings;

        private Strings(final String... strings) {

            mStrings = (strings != null) ? strings.clone() : null;
        }

        public void onResult(@NotNull final ResultChannel<String> result) {

            result.pass(mStrings);
        }
    }

    private static class UpperCase extends FilterInvocation<String, String> {

        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

            result.pass(input.toUpperCase());
        }
    }
}
