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
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.JRoutine;
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
    public void testBuilder() {

        assertThat(Streams.routine().asyncCall("test").afterMax(seconds(3)).all()).containsExactly(
                "test");
        assertThat(Streams.of().asyncCall().afterMax(seconds(3)).all()).isEmpty();
        assertThat(Streams.of((Object[]) null).asyncCall().afterMax(seconds(3)).all()).isEmpty();
        assertThat(Streams.of((List<?>) null).asyncCall().afterMax(seconds(3)).all()).isEmpty();
        assertThat(Streams.of(Collections.emptyList())
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.of("test").asyncCall().afterMax(seconds(3)).all()).containsExactly(
                "test");
        assertThat(Streams.of("test1", "test2", "test3")
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2", "test3");
        assertThat(Streams.of(Arrays.asList("test1", "test2", "test3"))
                          .asyncCall()
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2", "test3");
    }

    @Test
    public void testCollectConsumer() {

        assertThat(Streams.<String>routine()
                          .asyncCollect(
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
        assertThat(Streams.<String>routine()
                          .syncCollect(
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
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncCollect(
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
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncCollect(
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
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .asyncCollect(
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
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .syncCollect(
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
    public void testCollectConsumerNullPointerError() {

        try {

            Streams.routine().asyncCollect((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncCollect((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncCollect((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncCollect((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .asyncCollect((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .syncCollect((BiConsumer<List<?>, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testCollectFunction() {

        assertThat(Streams.<String>routine()
                          .asyncCollect(new Function<List<? extends String>, String>() {

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
        assertThat(Streams.<String>routine()
                          .syncCollect(new Function<List<? extends String>, String>() {

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
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncCollect(new Function<List<? extends String>, String>() {

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
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncCollect(new Function<List<? extends String>, String>() {

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
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .asyncCollect(new Function<List<? extends String>, String>() {

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
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .syncCollect(new Function<List<? extends String>, String>() {

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
    public void testCollectFunctionNullPointerError() {

        try {

            Streams.routine().asyncCollect((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncCollect((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncCollect((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncCollect((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .asyncCollect((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .syncCollect((Function<List<?>, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFilter() {

        assertThat(Streams.routine()
                          .asyncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.routine()
                          .parallelFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.routine()
                          .syncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.routine()
                          .syncMap(PassingInvocation.factoryOf())
                          .asyncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.routine()
                          .syncMap(PassingInvocation.factoryOf())
                          .parallelFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.routine()
                          .syncMap(PassingInvocation.factoryOf())
                          .syncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.routine()
                          .tryCatch(new Function<RoutineException, Object>() {

                              public Object apply(final RoutineException e) {

                                  return e.getMessage();
                              }
                          })
                          .asyncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.routine()
                          .tryCatch(new Function<RoutineException, Object>() {

                              public Object apply(final RoutineException e) {

                                  return e.getMessage();
                              }
                          })
                          .parallelFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
        assertThat(Streams.routine()
                          .tryCatch(new Function<RoutineException, Object>() {

                              public Object apply(final RoutineException e) {

                                  return e.getMessage();
                              }
                          })
                          .syncFilter(Functions.notNull())
                          .asyncCall(null, "test")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {

        try {

            Streams.routine().asyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().parallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).asyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).parallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).syncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).asyncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).parallelFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).syncFilter(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testForEach() {

        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(Streams.<String>routine().syncForEach(new Consumer<String>() {

            public void accept(final String s) {

                list.add(s);
            }
        }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(Streams.<String>routine().asyncForEach(new Consumer<String>() {

            public void accept(final String s) {

                list.add(s);
            }
        }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
        list.clear();
        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine()
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
        list.clear();
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
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
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
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

            Streams.routine().syncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().asyncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).syncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).asyncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).syncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).asyncForEach(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testGenerate() {

        assertThat(Streams.<String>routine().syncGenerate(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> resultChannel) {

                resultChannel.pass("TEST2");
            }
        }).asyncCall("test1").afterMax(seconds(3)).all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine().syncGenerate(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).asyncCall("test1").afterMax(seconds(3)).all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine().asyncGenerate(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).asyncCall("test1").afterMax(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                                                                          "TEST2");
        assertThat(Streams.<String>routine().asyncGenerate(new Consumer<ResultChannel<String>>() {

            public void accept(final ResultChannel<String> resultChannel) {

                resultChannel.pass("TEST2");
            }
        }).asyncCall("test1").afterMax(seconds(3)).all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine().asyncGenerate(new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).asyncCall("test1").afterMax(seconds(3)).all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine().asyncGenerate(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).asyncCall("test1").afterMax(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                                                                          "TEST2");
        assertThat(Streams.<String>routine().parallelGenerate(3, new Supplier<String>() {

            public String get() {

                return "TEST2";
            }
        }).asyncCall("test1").afterMax(seconds(3)).all()).containsExactly("TEST2", "TEST2",
                                                                          "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncGenerate(new Consumer<ResultChannel<String>>() {

                              public void accept(final ResultChannel<String> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncGenerate(new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncGenerate(3, new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncGenerate(new Consumer<ResultChannel<String>>() {

                              public void accept(final ResultChannel<String> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncGenerate(new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncGenerate(3, new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelGenerate(3, new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .syncGenerate(new Consumer<ResultChannel<String>>() {

                              public void accept(final ResultChannel<String> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .syncGenerate(new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .syncGenerate(3, new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .asyncGenerate(new Consumer<ResultChannel<String>>() {

                              public void accept(final ResultChannel<String> resultChannel) {

                                  resultChannel.pass("TEST2");
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .asyncGenerate(new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .asyncGenerate(3, new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .parallelGenerate(3, new Supplier<String>() {

                              public String get() {

                                  return "TEST2";
                              }
                          })
                          .asyncCall("test1")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST2", "TEST2", "TEST2");
    }

    @Test
    public void testGenerateNegativeCount() {

        try {

            Streams.routine().syncGenerate(-1, SupplierWrapper.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.routine().asyncGenerate(0, SupplierWrapper.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.routine().parallelGenerate(-1, SupplierWrapper.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncGenerate(0, SupplierWrapper.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncGenerate(-1, SupplierWrapper.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelGenerate(0, SupplierWrapper.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .syncGenerate(-1, SupplierWrapper.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .asyncGenerate(0, SupplierWrapper.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .parallelGenerate(-1, SupplierWrapper.constant(null));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testGenerateNullPointerError() {

        try {

            Streams.routine().syncGenerate((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncGenerate((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().asyncGenerate((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().asyncGenerate((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().asyncGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().parallelGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncGenerate((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncGenerate((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncGenerate((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncGenerate((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .syncGenerate((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).syncGenerate((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).syncGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .asyncGenerate((Consumer<ResultChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).asyncGenerate((Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).asyncGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .parallelGenerate(3, (Supplier<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testLift() {

        assertThat(Streams.<String>routine()
                          .flatLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.<String>routine()
                          .asyncMap(new UpperCase())
                          .flatLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .flatLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.<String>routine()
                          .syncLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.<String>routine()
                          .asyncMap(new UpperCase())
                          .syncLift(
                                  new Function<StreamRoutine<String, String>, Routine<String,
                                          String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .syncLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.<String>routine()
                          .asyncLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.<String>routine()
                          .asyncMap(new UpperCase())
                          .asyncLift(
                                  new Function<StreamRoutine<String, String>, Routine<String,
                                          String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .asyncLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1", "test2");
        assertThat(Streams.<String>routine()
                          .parallelLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2");
        assertThat(Streams.<String>routine()
                          .asyncMap(new UpperCase())
                          .parallelLift(
                                  new Function<StreamRoutine<String, String>, Routine<String,
                                          String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .parallelLift(
                                  new Function<StreamRoutine<String, String>,
                                          StreamRoutine<String, String>>() {

                                      public StreamRoutine<String, String> apply(
                                              final StreamRoutine<String, String> routine) {

                                          return Streams.<String>routine()
                                                        .syncFilter(Functions.<String>notNull())
                                                        .asyncMap(routine);
                                      }
                                  })
                          .asyncCall("test1", null, "test2", null)
                          .afterMax(seconds(3))
                          .all()).containsOnly("test1", "test2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testLiftNullPointerError() {

        try {

            Streams.routine().flatLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().asyncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().parallelLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).flatLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).syncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).asyncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).parallelLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).flatLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).syncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).asyncLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).parallelLift(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapConsumer() {

        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine()
                          .parallelMap(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
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
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .parallelMap(new BiConsumer<String, ResultChannel<String>>() {

                              public void accept(final String s,
                                      final ResultChannel<String> result) {

                                  result.pass(s.toUpperCase());
                              }
                          })
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
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

            Streams.routine().asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().parallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .asyncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .parallelMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .syncMap((BiConsumer<Object, ResultChannel<Object>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFactory() {

        final InvocationFactory<String, String> factory = Invocations.factoryOf(UpperCase.class);
        assertThat(Streams.<String>routine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .parallelMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .parallelMap(factory)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
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

            Streams.routine().asyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().parallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .asyncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .parallelMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .syncMap((InvocationFactory<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFilter() {

        assertThat(Streams.<String>routine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .parallelMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .parallelMap(new UpperCase())
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
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

            Streams.routine().asyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().parallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .asyncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .parallelMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .syncMap((FilterInvocation<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapFunction() {

        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine().parallelMap(new Function<String, String>() {

            public String apply(final String s) {

                return s.toUpperCase();
            }
        }).asyncCall("test1", "test2").afterMax(seconds(3)).all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelMap(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
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
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
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
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .parallelMap(new Function<String, String>() {

                              public String apply(final String s) {

                                  return s.toUpperCase();
                              }
                          })
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
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

            Streams.routine().asyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().parallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .asyncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .parallelMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .syncMap((Function<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMapRoutine() {

        final Routine<String, String> routine = JRoutine.on(new UpperCase()).buildRoutine();
        assertThat(Streams.<String>routine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .parallelMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .parallelMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .syncMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .invocations()
                          .withOutputOrder(OrderType.BY_CALL)
                          .set()
                          .asyncMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .parallelMap(routine)
                          .asyncCall("test1", "test2")
                          .afterMax(seconds(3))
                          .all()).containsOnly("TEST1", "TEST2");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
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

            Streams.routine().asyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().parallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .asyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .parallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .syncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .asyncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .parallelMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .syncMap((Routine<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testReduce() {

        assertThat(Streams.<String>routine().asyncReduce(new BiFunction<String, String, String>() {

            public String apply(final String s, final String s2) {

                return s + s2;
            }
        }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(Streams.<String>routine().syncReduce(new BiFunction<String, String, String>() {

            public String apply(final String s, final String s2) {

                return s + s2;
            }
        }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .asyncReduce(new BiFunction<String, String, String>() {

                              public String apply(final String s, final String s2) {

                                  return s + s2;
                              }
                          })
                          .asyncCall("test1", "test2", "test3")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.<String>routine()
                          .syncMap(PassingInvocation.<String>factoryOf())
                          .syncReduce(new BiFunction<String, String, String>() {

                              public String apply(final String s, final String s2) {

                                  return s + s2;
                              }
                          })
                          .asyncCall("test1", "test2", "test3")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .asyncReduce(new BiFunction<String, String, String>() {

                              public String apply(final String s, final String s2) {

                                  return s + s2;
                              }
                          })
                          .asyncCall("test1", "test2", "test3")
                          .afterMax(seconds(3))
                          .all()).containsExactly("test1test2test3");
        assertThat(Streams.<String>routine()
                          .tryCatch(ConsumerWrapper.sink())
                          .syncReduce(new BiFunction<String, String, String>() {

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
    public void testReduceNullPointerError() {

        try {

            Streams.routine().asyncReduce(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncReduce(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).asyncReduce(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().syncMap(PassingInvocation.factoryOf()).syncReduce(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).asyncReduce(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch(ConsumerWrapper.sink()).syncReduce(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testRetry() {

        final Function<StreamRoutine<Object, String>, StreamRoutine<Object, String>> retryFunction =
                new Function<StreamRoutine<Object, String>, StreamRoutine<Object, String>>() {

                    public StreamRoutine<Object, String> apply(
                            final StreamRoutine<Object, String> routine) {

                        return Streams.routine()
                                      .syncCollect(
                                              new BiConsumer<List<?>, ResultChannel<String>>() {

                                                  public void accept(final List<?> inputs,
                                                          final ResultChannel<String> result) {

                                                      final int[] count = {0};
                                                      routine.tryCatch(
                                                              new BiConsumer<RoutineException,
                                                                      InputChannel<String>>() {

                                                                  public void accept(
                                                                          final RoutineException e,
                                                                          final
                                                                          InputChannel<String>
                                                                                  channel) {

                                                                      if (++count[0] < 3) {

                                                                          routine.tryCatch(this)
                                                                                 .syncCall(inputs)
                                                                                 .passTo(channel);

                                                                      } else {

                                                                          throw e;
                                                                      }
                                                                  }
                                                              }).syncCall(inputs).passTo(result);
                                                  }
                                              });
                    }
                };

        try {

            Streams.routine().syncMap(new Function<Object, String>() {

                public String apply(final Object o) {

                    return o.toString();
                }
            }).flatLift(retryFunction).asyncCall((Object) null).afterMax(seconds(3)).all();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testTryCatch() {

        assertThat(Streams.routine().syncMap(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).tryCatch(new BiConsumer<RoutineException, InputChannel<Object>>() {

            public void accept(final RoutineException e, final InputChannel<Object> channel) {

                channel.pass("exception");
            }
        }).asyncCall("test").afterMax(seconds(3)).next()).isEqualTo("exception");

        try {

            Streams.routine().syncMap(new Function<Object, Object>() {

                public Object apply(final Object o) {

                    throw new NullPointerException();
                }
            }).tryCatch(new Consumer<RoutineException>() {

                public void accept(final RoutineException e) {

                    throw new IllegalArgumentException();
                }
            }).asyncCall("test").afterMax(seconds(3)).next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(Streams.routine().syncMap(new Function<Object, Object>() {

            public Object apply(final Object o) {

                throw new NullPointerException();
            }
        }).tryCatch(new Function<RoutineException, Object>() {

            public Object apply(final RoutineException e) {

                return "exception";
            }
        }).asyncCall("test").afterMax(seconds(3)).next()).isEqualTo("exception");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testTryCatchNullPointerError() {

        try {

            Streams.routine().tryCatch((BiConsumer<RoutineException, InputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch((Consumer<RoutineException>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine().tryCatch((Function<RoutineException, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .tryCatch((BiConsumer<RoutineException, InputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .tryCatch((Consumer<RoutineException>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .syncMap(PassingInvocation.factoryOf())
                   .tryCatch((Function<RoutineException, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .tryCatch((BiConsumer<RoutineException, InputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .tryCatch((Consumer<RoutineException>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.routine()
                   .tryCatch(ConsumerWrapper.sink())
                   .tryCatch((Function<RoutineException, ?>) null);

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
