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

package com.github.dm.jrt.channel;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.common.TimeoutException;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Future channel unit tests.
 * <p>
 * Created by davide-maestroni on 08/31/2016.
 */
public class FutureChannelTest {

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testAbort() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 1, TimeUnit.SECONDS);
    final DurationMeasure timeout = seconds(1);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    channel.abort(new IllegalStateException());
    try {
      channel.in(timeout).throwError();

    } catch (final AbortException ex) {
      assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testAbort2() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 1, TimeUnit.SECONDS);
    final DurationMeasure timeout = seconds(1);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future, true);
    channel.abort(new IllegalStateException());
    try {
      channel.in(timeout).throwError();

    } catch (final AbortException ex) {
      assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testAbortDelay() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    final ArrayList<String> results = new ArrayList<String>();
    channel.in(10, TimeUnit.MILLISECONDS).allInto(results);
    assertThat(results).isEmpty();
    assertThat(channel.inNoTime().eventuallyContinue().getComplete()).isFalse();
    assertThat(channel.afterNoDelay().abort()).isTrue();
    try {
      channel.next();
      fail();

    } catch (final AbortException ignored) {
    }

    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void testAbortDelay2() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future, true);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    final ArrayList<String> results = new ArrayList<String>();
    channel.in(10, TimeUnit.MILLISECONDS).allInto(results);
    assertThat(results).isEmpty();
    assertThat(channel.inNoTime().eventuallyContinue().getComplete()).isFalse();
    assertThat(channel.afterNoDelay().abort()).isTrue();
    try {
      channel.next();
      fail();

    } catch (final AbortException ignored) {
    }

    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void testAllIntoTimeout() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyAbort().eventuallyFail();
    try {
      channel.allInto(new ArrayList<String>());
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testAllIntoTimeout2() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.allInto(new ArrayList<String>());
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testAllTimeout() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail();
    try {
      channel.all();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testAllTimeout2() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.all();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testAsynchronousInput() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 500, TimeUnit.MILLISECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    final Channel<String, String> outputChannel = JRoutineCore.routine()
                                                              .of(IdentityInvocation
                                                                  .<String>factory())
                                                              .invoke()
                                                              .pass(channel)
                                                              .close();
    assertThat(outputChannel.in(seconds(1)).next()).isEqualTo("test");
    assertThat(outputChannel.getComplete()).isTrue();
  }

  @Test
  public void testBind() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).submit(new Callable<String>() {

          public String call() {
            return "test";
          }
        });
    final Channel<?, String> channel = JRoutineChannels.channelHandler()
                                                       .channelOf(future)
                                                       .pipe(
                                                           JRoutineCore.channel().<String>ofType());
    assertThat(channel.in(seconds(1)).next()).isEqualTo("test");
    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void testBindAbort() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.isBound()).isFalse();
    final Channel<?, String> outputChannel = channel.pipe(JRoutineCore.channel().<String>ofType());
    assertThat(channel.isBound()).isTrue();
    channel.abort();
    assertThat(outputChannel.in(seconds(1)).getError()).isExactlyInstanceOf(AbortException.class);
    assertThat(outputChannel.isOpen()).isFalse();
  }

  @Test
  public void testBindError() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    channel.pipe(JRoutineCore.channel().<String>ofType());
    try {
      channel.pipe(JRoutineCore.channel().<String>ofType());
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.in(seconds(10)).next();
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  @Test
  public void testEmpty() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 100, TimeUnit.MILLISECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.isEmpty()).isTrue();
    channel.in(seconds(1)).next();
    assertThat(channel.isEmpty()).isTrue();
  }

  @Test
  public void testEmpty2() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 100, TimeUnit.MILLISECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.isEmpty()).isTrue();
    assertThat(channel.close().in(seconds(10)).getComplete()).isTrue();
    assertThat(channel.isEmpty()).isFalse();
  }

  @Test
  public void testEmptyAbort() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 100, TimeUnit.MILLISECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.isEmpty()).isTrue();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.isEmpty()).isFalse();
  }

  @Test
  public void testHasNext() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).submit(new Callable<String>() {

          public String call() {
            return "test";
          }
        });
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.in(seconds(1)).hasNext()).isTrue();
  }

  @Test
  public void testHasNextIteratorTimeout() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.isEmpty()).isTrue();
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail();
    try {
      channel.iterator().hasNext();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testHasNextIteratorTimeout2() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.iterator().hasNext();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testIterator() {
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    final Future<String> future = executor.submit(new Callable<String>() {

      public String call() {
        return "test";
      }
    });
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    final Iterator<String> iterator = channel.in(seconds(1)).iterator();
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo("test");
    assertThat(iterator.hasNext()).isFalse();
    try {
      iterator.remove();
      fail();

    } catch (final UnsupportedOperationException ignored) {
    }
  }

  @Test
  public void testNextIteratorTimeout() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail();
    try {
      channel.iterator().next();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testNextIteratorTimeout2() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.iterator().next();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testNextList() {
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    final Future<String> future = executor.submit(new Callable<String>() {

      public String call() {
        return "test";
      }
    });
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.next(0)).isEmpty();
    assertThat(channel.eventuallyContinue().in(seconds(1)).next(2)).containsExactly("test");
    try {
      JRoutineChannels.channelHandler().channelOf(executor.schedule(new Callable<String>() {

        public String call() {
          return "test";
        }
      }, 1, TimeUnit.SECONDS)).eventuallyAbort().next(2);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineChannels.channelHandler().channelOf(executor.schedule(new Callable<String>() {

        public String call() {
          return "test";
        }
      }, 1, TimeUnit.SECONDS)).eventuallyAbort(new IllegalStateException()).next(2);
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineChannels.channelHandler().channelOf(executor.schedule(new Callable<String>() {

        public String call() {
          return "test";
        }
      }, 1, TimeUnit.SECONDS)).eventuallyFail().next(2);
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testNextOr() {
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    assertThat(JRoutineChannels.channelHandler().channelOf(executor.submit(new Callable<Object>() {

      public Object call() {
        return "test1";
      }
    })).in(seconds(1)).nextOrElse(2)).isEqualTo("test1");
    assertThat(
        JRoutineChannels.channelHandler().channelOf(executor.schedule(new Callable<Object>() {

          public Object call() {
            return "test1";
          }
        }, 3, TimeUnit.SECONDS))

                        .eventuallyContinue().in(seconds(1)).nextOrElse(2)).isEqualTo(2);
    try {
      JRoutineChannels.channelHandler().channelOf(executor.schedule(new Callable<Object>() {

        public Object call() {
          return "test1";
        }
      }, 3, TimeUnit.SECONDS)).eventuallyAbort().in(millis(100)).nextOrElse("test2");
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineChannels.channelHandler()
                      .channelOf(executor.schedule(new Callable<Object>() {

                        public Object call() {
                          return "test1";
                        }
                      }, 3, TimeUnit.SECONDS))
                      .eventuallyAbort(new IllegalStateException())
                      .in(millis(100))
                      .nextOrElse("test2");
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineChannels.channelHandler().channelOf(executor.schedule(new Callable<Object>() {

        public Object call() {
          return "test1";
        }
      }, 3, TimeUnit.SECONDS)).eventuallyFail().in(millis(100)).nextOrElse("test2");
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testNextTimeout() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail();
    try {
      channel.next();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testNextTimeout2() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inNoTime().eventuallyContinue().all()).isEmpty();
    channel.eventuallyFail().in(millis(10));
    try {
      channel.next();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }

    assertThat(channel.getComplete()).isFalse();
  }

  @Test
  public void testPass() {
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    final Future<String> future = executor.submit(new Callable<String>() {

      public String call() {
        return "test";
      }
    });
    @SuppressWarnings("unchecked") final Channel<Object, String> channel =
        (Channel<Object, String>) JRoutineChannels.channelHandler().channelOf(future);
    try {
      channel.sorted().pass("test");
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.unsorted().pass("test", "test");
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.unsorted().pass(Arrays.asList("test", "test"));
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.unsorted().pass(JRoutineCore.channel().ofType());
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  @Test
  public void testPass2() {
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    final Future<String> future = executor.schedule(new Callable<String>() {

      public String call() {
        return "test";
      }
    }, 3, TimeUnit.SECONDS);
    @SuppressWarnings("unchecked") final Channel<Object, String> channel =
        (Channel<Object, String>) JRoutineChannels.channelHandler().channelOf(future);
    channel.abort();
    try {
      channel.sorted().pass("test");
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.unsorted().pass("test", "test");
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.unsorted().pass(Arrays.asList("test", "test"));
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.unsorted().pass(JRoutineCore.channel().ofType());
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  @Test
  public void testPassTimeout() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler()
                                                       .withChannel()
                                                       .withOutputTimeout(millis(10))
                                                       .withOutputTimeoutAction(
                                                           TimeoutActionType.CONTINUE)
                                                       .configuration()
                                                       .channelOf(future);
    assertThat(channel.all()).isEmpty();
  }

  @Test
  public void testPassTimeout2() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler()
                                                       .withChannel()
                                                       .withOutputTimeout(millis(10))
                                                       .withOutputTimeoutAction(
                                                           TimeoutActionType.ABORT)
                                                       .configuration()
                                                       .channelOf(future);
    try {
      channel.all();
      fail();

    } catch (final AbortException ignored) {
    }
  }

  @Test
  public void testPassTimeout3() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 3, TimeUnit.SECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler()
                                                       .withChannel()
                                                       .withOutputTimeout(millis(10))
                                                       .withOutputTimeoutAction(
                                                           TimeoutActionType.FAIL)
                                                       .configuration()
                                                       .channelOf(future);
    try {
      channel.all();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }
  }

  @Test
  public void testSize() throws InterruptedException {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

          public String call() {
            return "test";
          }
        }, 100, TimeUnit.MILLISECONDS);
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    assertThat(channel.inputSize()).isEqualTo(0);
    assertThat(channel.outputSize()).isEqualTo(0);
    millis(500).sleepAtLeast();
    assertThat(channel.inputSize()).isEqualTo(1);
    assertThat(channel.outputSize()).isEqualTo(1);
    channel.close();
    assertThat(channel.in(seconds(1)).getComplete()).isTrue();
    assertThat(channel.inputSize()).isEqualTo(1);
    assertThat(channel.outputSize()).isEqualTo(1);
    assertThat(channel.size()).isEqualTo(1);
    assertThat(channel.skipNext(1).outputSize()).isEqualTo(0);
  }

  @Test
  public void testSkip() {
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    assertThat(JRoutineChannels.channelHandler().channelOf(executor.submit(new Callable<String>() {

      public String call() {
        return "test";
      }
    })).in(seconds(1)).skipNext(0).all()).containsExactly("test");
    assertThat(JRoutineChannels.channelHandler().channelOf(executor.submit(new Callable<String>() {

      public String call() {
        return "test";
      }
    })).eventuallyContinue().in(seconds(1)).skipNext(2).all()).isEmpty();
    assertThat(JRoutineChannels.channelHandler().channelOf(executor.submit(new Callable<String>() {

      public String call() {
        return "test";
      }
    })).in(seconds(1)).skipNext(1).all()).isEmpty();
    try {
      JRoutineChannels.channelHandler().channelOf(executor.submit(new Callable<String>() {

        public String call() {
          return "test";
        }
      })).eventuallyAbort().in(seconds(1)).skipNext(2);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineChannels.channelHandler().channelOf(executor.submit(new Callable<String>() {

        public String call() {
          return "test";
        }
      })).eventuallyAbort(new IllegalStateException()).in(seconds(1)).skipNext(2);
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineChannels.channelHandler().channelOf(executor.submit(new Callable<String>() {

        public String call() {
          return "test";
        }
      })).eventuallyFail().in(seconds(1)).skipNext(2);
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testThrowError() {
    final Future<String> future =
        Executors.newScheduledThreadPool(1).submit(new Callable<String>() {

          public String call() {
            return "test";
          }
        });
    final Channel<?, String> channel = JRoutineChannels.channelHandler().channelOf(future);
    channel.in(seconds(1)).throwError();
  }
}
