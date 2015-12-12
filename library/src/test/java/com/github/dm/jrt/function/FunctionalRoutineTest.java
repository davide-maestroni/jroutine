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
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.Invocations;
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

    @Test
    public void testAccumulate() {

        assertThat(JFunctional.routine()
                              .asyncAccumulate(new BiFunction<String, String, String>() {

                                  public String apply(final String s, final String s2) {

                                      return s + s2;
                                  }
                              })
                              .asyncCall("test1", "test2", "test3")
                              .afterMax(seconds(3))
                              .all()).containsExactly("test1test2test3");
        assertThat(JFunctional.routine()
                              .syncAccumulate(new BiFunction<String, String, String>() {

                                  public String apply(final String s, final String s2) {

                                      return s + s2;
                                  }
                              })
                              .asyncCall("test1", "test2", "test3")
                              .afterMax(seconds(3))
                              .all()).containsExactly("test1test2test3");
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .asyncAccumulate(new BiFunction<String, String, String>() {

                                  public String apply(final String s, final String s2) {

                                      return s + s2;
                                  }
                              })
                              .asyncCall("test1", "test2", "test3")
                              .afterMax(seconds(3))
                              .all()).containsExactly("test1test2test3");
        assertThat(JFunctional.routine()
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

            JFunctional.routine().asyncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .asyncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .syncAccumulate(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilder() {

        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.factoryOf())
                              .asyncCall("test")
                              .afterMax(seconds(3))
                              .all()).containsExactly("test");
        assertThat(JFunctional.routine().syncFrom(new Supplier<String>() {

            public String get() {

                return "test";
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsExactly("test");
        assertThat(JFunctional.routine()
                              .syncFrom(new Strings("test1", "test2", "test3"))
                              .asyncCall()
                              .afterMax(seconds(3))
                              .all()).containsOnly("test1", "test2", "test3");
        assertThat(JFunctional.routine().syncFrom(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> result) {

                result.pass("test1", "test2", "test3");
            }
        }).asyncCall().afterMax(seconds(3)).all()).containsOnly("test1", "test2", "test3");
        assertThat(JFunctional.routine()
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .syncFrom(new Strings("test1", "test2", "test3"))
                              .asyncCall()
                              .afterMax(seconds(3))
                              .all()).containsExactly("test1", "test2", "test3");
        assertThat(JFunctional.routine()
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
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBuilderNullPointerError() {

        try {

            JFunctional.routine().syncFrom((Consumer<ResultChannel<String>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncFrom((CommandInvocation<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncFrom((Supplier<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFilter() {

        assertThat(JFunctional.routine()
                              .asyncFilter(Functions.notNull())
                              .asyncCall(null, "test")
                              .afterMax(seconds(3))
                              .all()).containsExactly("test");
        assertThat(JFunctional.routine()
                              .parallelFilter(Functions.notNull())
                              .asyncCall(null, "test")
                              .afterMax(seconds(3))
                              .all()).containsExactly("test");
        assertThat(JFunctional.routine()
                              .syncFilter(Functions.notNull())
                              .asyncCall(null, "test")
                              .afterMax(seconds(3))
                              .all()).containsExactly("test");
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.factoryOf())
                              .asyncFilter(Functions.notNull())
                              .asyncCall(null, "test")
                              .afterMax(seconds(3))
                              .all()).containsExactly("test");
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.factoryOf())
                              .parallelFilter(Functions.notNull())
                              .asyncCall(null, "test")
                              .afterMax(seconds(3))
                              .all()).containsExactly("test");
        assertThat(JFunctional.routine()
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

            JFunctional.routine().asyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().parallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncMap(PassingInvocation.factoryOf()).asyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .parallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncMap(PassingInvocation.factoryOf()).syncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testLift() {

        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .lift(new Function<FunctionalRoutine<String, String>,
                                      FunctionalRoutine<String, String>>() {

                                  public FunctionalRoutine<String, String> apply(
                                          final FunctionalRoutine<String, String> routine) {

                                      return JFunctional.routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                  }
                              })
                              .asyncCall("test1", null, "test2", null)
                              .afterMax(seconds(3))
                              .all()).containsExactly("test1", "test2");
        assertThat(JFunctional.routine()
                              .asyncMap(new UpperCase())
                              .flatLift(
                                      new Function<FunctionalRoutine<String, String>,
                                              FunctionalRoutine<String, String>>() {

                                          public FunctionalRoutine<String, String> apply(
                                                  final FunctionalRoutine<String, String> routine) {

                                              return JFunctional.routine()
                                                                .syncFilter(
                                                                        Functions.<String>notNull())
                                                                .asyncMap(routine);
                                          }
                                      })
                              .asyncCall("test1", null, "test2", null)
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .asyncMap(new UpperCase())
                              .lift(new Function<FunctionalRoutine<String, String>,
                                      Routine<String, String>>() {

                                  public FunctionalRoutine<String, String> apply(
                                          final FunctionalRoutine<String, String> routine) {

                                      return JFunctional.routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                  }
                              })
                              .asyncCall("test1", null, "test2", null)
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testLiftNullPointerError() {

        try {

            JFunctional.routine().syncMap(PassingInvocation.factoryOf()).flatLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncMap(PassingInvocation.factoryOf()).lift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapConsumer() {

        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine()
                              .parallelMap(new BiConsumer<String, ResultChannel<String>>() {

                                  public void accept(final String s,
                                          final ResultChannel<String> result) {

                                      result.pass(s.toUpperCase());
                                  }
                              })
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsOnly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine()
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

            JFunctional.routine().asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .parallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .parallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .syncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFactory() {

        final InvocationFactory<String, String> factory = Invocations.factoryOf(UpperCase.class);
        assertThat(JFunctional.routine()
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .asyncMap(factory)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .parallelMap(factory)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsOnly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .syncMap(factory)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .asyncMap(factory)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .parallelMap(factory)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsOnly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
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

            JFunctional.routine().asyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().parallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .asyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .parallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .syncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFilter() {

        assertThat(JFunctional.routine()
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .asyncMap(new UpperCase())
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .parallelMap(new UpperCase())
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsOnly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .syncMap(new UpperCase())
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .asyncMap(new UpperCase())
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .parallelMap(new UpperCase())
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsOnly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
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

            JFunctional.routine().asyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().parallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .asyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .parallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .syncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFunction() {

        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine().parallelMap(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).asyncCall("test1", "test2").afterMax(seconds(3)).all()).containsOnly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .parallelMap(new Function<String, String>() {

                                  public String apply(final String s) {

                                      return s.toUpperCase();
                                  }
                              })
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsOnly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
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

            JFunctional.routine().asyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().parallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .asyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .parallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .syncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapRoutine() {

        final Routine<String, String> routine = JRoutine.on(new UpperCase()).buildRoutine();
        assertThat(JFunctional.routine()
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .asyncMap(routine)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .parallelMap(routine)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsOnly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .syncMap(routine)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .invocations()
                              .withOutputOrder(OrderType.BY_CALL)
                              .set()
                              .asyncMap(routine)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsExactly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .parallelMap(routine)
                              .asyncCall("test1", "test2")
                              .afterMax(seconds(3))
                              .all()).containsOnly("TEST1", "TEST2");
        assertThat(JFunctional.routine()
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

            JFunctional.routine().asyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().parallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .asyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .parallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .syncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReduceConsumer() {

        assertThat(JFunctional.routine()
                              .asyncReduce(
                                      new BiConsumer<List<? extends String>,
                                              ResultChannel<String>>() {

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
        assertThat(JFunctional.routine()
                              .syncReduce(
                                      new BiConsumer<List<? extends String>,
                                              ResultChannel<String>>() {

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
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .asyncReduce(
                                      new BiConsumer<List<? extends String>,
                                              ResultChannel<String>>() {

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
        assertThat(JFunctional.routine()
                              .syncMap(PassingInvocation.<String>factoryOf())
                              .syncReduce(
                                      new BiConsumer<List<? extends String>,
                                              ResultChannel<String>>() {

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

            JFunctional.routine()
                       .asyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .asyncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .syncReduce((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReduceFunction() {

        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine()
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
        assertThat(JFunctional.routine()
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

            JFunctional.routine().asyncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine().syncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
                       .syncMap(PassingInvocation.factoryOf())
                       .asyncReduce((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JFunctional.routine()
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
