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
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.error.TimeoutException;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.util.UnitDuration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
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
        final UnitDuration timeout = seconds(1);
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        channel.abort(new IllegalStateException());
        try {
            channel.after(timeout).throwError();

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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
        final ArrayList<String> results = new ArrayList<String>();
        channel.after(10, TimeUnit.MILLISECONDS).allInto(results);
        assertThat(results).isEmpty();
        assertThat(channel.now().eventuallyContinue().getComplete()).isFalse();
        assertThat(channel.now().abort()).isTrue();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
        channel.eventuallyFail().after(millis(10));
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
        channel.eventuallyFail().after(millis(10));
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        final Channel<String, String> outputChannel =
                JRoutineCore.with(IdentityInvocation.<String>factoryOf()).call(channel);
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo("test");
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
        final Channel<? super String, String> channel = Channels.fromFuture(future)
                                                                .buildChannels()
                                                                .bind(JRoutineCore.io()
                                                                        .<String>buildChannel());
        assertThat(channel.after(seconds(1)).next()).isEqualTo("test");
        assertThat(channel.isOpen()).isTrue();
        assertThat(channel.now().close().isOpen()).isFalse();
    }

    @Test
    public void testBindAbort() {
        final Future<String> future =
                Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

                    public String call() {
                        return "test";
                    }
                }, 3, TimeUnit.SECONDS);
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.isBound()).isFalse();
        final Channel<? super String, String> outputChannel =
                channel.bind(JRoutineCore.io().<String>buildChannel());
        assertThat(channel.isBound()).isTrue();
        channel.abort();
        assertThat(outputChannel.after(seconds(1)).getError()).isExactlyInstanceOf(
                AbortException.class);
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        channel.bind(JRoutineCore.io().<String>buildChannel());
        try {
            channel.bind(JRoutineCore.io().<String>buildChannel());
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            channel.after(seconds(10)).next();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.isEmpty()).isTrue();
        channel.after(seconds(1)).next();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(channel.close().after(seconds(10)).getComplete()).isTrue();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.after(seconds(1)).hasNext()).isTrue();
    }

    @Test
    public void testHasNextIteratorTimeout() {
        final Future<String> future =
                Executors.newScheduledThreadPool(1).schedule(new Callable<String>() {

                    public String call() {
                        return "test";
                    }
                }, 3, TimeUnit.SECONDS);
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
        channel.eventuallyFail().after(millis(10));
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        final Iterator<String> iterator = channel.after(seconds(1)).iterator();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
        channel.eventuallyFail().after(millis(10));
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.next(0)).isEmpty();
        assertThat(channel.eventuallyContinue().after(seconds(1)).next(2)).containsExactly("test");
        try {
            Channels.fromFuture(executor.schedule(new Callable<String>() {

                public String call() {
                    return "test";
                }
            }, 1, TimeUnit.SECONDS)).buildChannels().eventuallyAbort().next(2);
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            Channels.fromFuture(executor.schedule(new Callable<String>() {

                public String call() {
                    return "test";
                }
            }, 1, TimeUnit.SECONDS))
                    .buildChannels()
                    .eventuallyAbort(new IllegalStateException())
                    .next(2);
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {
            Channels.fromFuture(executor.schedule(new Callable<String>() {

                public String call() {
                    return "test";
                }
            }, 1, TimeUnit.SECONDS)).buildChannels().eventuallyFail().next(2);
            fail();

        } catch (final TimeoutException ignored) {
        }
    }

    @Test
    public void testNextOr() {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        assertThat(Channels.fromFuture(executor.submit(new Callable<Object>() {

            public Object call() {
                return "test1";
            }
        })).buildChannels().after(seconds(1)).nextOrElse(2)).isEqualTo("test1");
        assertThat(Channels.fromFuture(executor.schedule(new Callable<Object>() {

            public Object call() {
                return "test1";
            }
        }, 3, TimeUnit.SECONDS))
                           .buildChannels()
                           .eventuallyContinue()
                           .after(seconds(1))
                           .nextOrElse(2)).isEqualTo(2);
        try {
            Channels.fromFuture(executor.schedule(new Callable<Object>() {

                public Object call() {
                    return "test1";
                }
            }, 3, TimeUnit.SECONDS))
                    .buildChannels()
                    .eventuallyAbort()
                    .after(millis(100))
                    .nextOrElse("test2");
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            Channels.fromFuture(executor.schedule(new Callable<Object>() {

                public Object call() {
                    return "test1";
                }
            }, 3, TimeUnit.SECONDS))
                    .buildChannels()
                    .eventuallyAbort(new IllegalStateException())
                    .after(millis(100))
                    .nextOrElse("test2");
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {
            Channels.fromFuture(executor.schedule(new Callable<Object>() {

                public Object call() {
                    return "test1";
                }
            }, 3, TimeUnit.SECONDS))
                    .buildChannels()
                    .eventuallyFail()
                    .after(millis(100))
                    .nextOrElse("test2");
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.now().eventuallyContinue().all()).isEmpty();
        channel.eventuallyFail().after(millis(10));
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
                (Channel<Object, String>) Channels.fromFuture(future).buildChannels();
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
            channel.unsorted().pass(JRoutineCore.io().buildChannel());
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
                (Channel<Object, String>) Channels.fromFuture(future).buildChannels();
        channel.abort();
        try {
            channel.sorted().pass("test");
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            channel.unsorted().pass("test", "test");
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            channel.unsorted().pass(Arrays.asList("test", "test"));
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            channel.unsorted().pass(JRoutineCore.io().buildChannel());
            fail();

        } catch (final AbortException ignored) {
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
        final Channel<?, String> channel = Channels.fromFuture(future)
                                                   .applyChannelConfiguration()
                                                   .withOutputTimeout(millis(10))
                                                   .withOutputTimeoutAction(
                                                           TimeoutActionType.CONTINUE)
                                                   .configured()
                                                   .buildChannels();
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
        final Channel<?, String> channel = Channels.fromFuture(future)
                                                   .applyChannelConfiguration()
                                                   .withOutputTimeout(millis(10))
                                                   .withOutputTimeoutAction(TimeoutActionType.ABORT)
                                                   .configured()
                                                   .buildChannels();
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
        final Channel<?, String> channel = Channels.fromFuture(future)
                                                   .applyChannelConfiguration()
                                                   .withOutputTimeout(millis(10))
                                                   .withOutputTimeoutAction(TimeoutActionType.FAIL)
                                                   .configured()
                                                   .buildChannels();
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        assertThat(channel.inputCount()).isEqualTo(0);
        assertThat(channel.outputCount()).isEqualTo(0);
        millis(500).sleepAtLeast();
        assertThat(channel.inputCount()).isEqualTo(1);
        assertThat(channel.outputCount()).isEqualTo(1);
        channel.close();
        assertThat(channel.after(seconds(1)).getComplete()).isTrue();
        assertThat(channel.inputCount()).isEqualTo(1);
        assertThat(channel.outputCount()).isEqualTo(1);
        assertThat(channel.size()).isEqualTo(1);
        assertThat(channel.skipNext(1).outputCount()).isEqualTo(0);
    }

    @Test
    public void testSkip() {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        assertThat(Channels.fromFuture(executor.submit(new Callable<String>() {

            public String call() {
                return "test";
            }
        })).buildChannels().after(seconds(1)).skipNext(0).all()).containsExactly("test");
        assertThat(Channels.fromFuture(executor.submit(new Callable<String>() {

            public String call() {
                return "test";
            }
        })).buildChannels().eventuallyContinue().after(seconds(1)).skipNext(2).all()).isEmpty();
        assertThat(Channels.fromFuture(executor.submit(new Callable<String>() {

            public String call() {
                return "test";
            }
        })).buildChannels().after(seconds(1)).skipNext(1).all()).isEmpty();
        try {
            Channels.fromFuture(executor.submit(new Callable<String>() {

                public String call() {
                    return "test";
                }
            })).buildChannels().eventuallyAbort().after(seconds(1)).skipNext(2);
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            Channels.fromFuture(executor.submit(new Callable<String>() {

                public String call() {
                    return "test";
                }
            }))
                    .buildChannels()
                    .eventuallyAbort(new IllegalStateException())
                    .after(seconds(1))
                    .skipNext(2);
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {
            Channels.fromFuture(executor.submit(new Callable<String>() {

                public String call() {
                    return "test";
                }
            })).buildChannels().eventuallyFail().after(seconds(1)).skipNext(2);
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
        final Channel<?, String> channel = Channels.fromFuture(future).buildChannels();
        channel.after(seconds(1)).throwError();
    }
}
