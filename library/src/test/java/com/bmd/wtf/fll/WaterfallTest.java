/**
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
package com.bmd.wtf.fll;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.crr.CurrentGenerator;
import com.bmd.wtf.crr.Currents;
import com.bmd.wtf.drp.Drops;
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.Dam.Action;
import com.bmd.wtf.flw.Dam.ConditionEvaluator;
import com.bmd.wtf.flw.Pump;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.flw.Stream.Direction;
import com.bmd.wtf.gts.AbstractGate;
import com.bmd.wtf.gts.Gate;
import com.bmd.wtf.gts.GateGenerator;
import com.bmd.wtf.gts.OpenGate;
import com.bmd.wtf.spr.Spring;
import com.bmd.wtf.spr.SpringGenerator;
import com.bmd.wtf.spr.Springs;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bmd.wtf.fll.Waterfall.fall;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for waterfall classes.
 * <p/>
 * Created by davide on 6/28/14.
 */
public class WaterfallTest extends TestCase {

    private static void testRiver(final River<Object> river) {

        river.push((Object) null)
             .push((Object[]) null)
             .push((Iterable<Object>) null)
             .push(new Object[0])
             .push(Arrays.asList())
             .push("push")
             .push(new Object[]{"push"})
             .push(new Object[]{"push", "push"})
             .push(Arrays.asList("push"))
             .push(Arrays.asList("push", "push"))
             .pushAfter(0, TimeUnit.MILLISECONDS, (Object) null)
             .pushAfter(0, TimeUnit.MILLISECONDS, (Object[]) null)
             .pushAfter(0, TimeUnit.MILLISECONDS, (Iterable<Object>) null)
             .pushAfter(0, TimeUnit.MILLISECONDS, new Object[0])
             .pushAfter(0, TimeUnit.MILLISECONDS, Arrays.asList())
             .pushAfter(0, TimeUnit.MILLISECONDS, "push")
             .pushAfter(0, TimeUnit.MILLISECONDS, new Object[]{"push"})
             .pushAfter(0, TimeUnit.MILLISECONDS, new Object[]{"push", "push"})
             .pushAfter(0, TimeUnit.MILLISECONDS, Arrays.asList("push"))
             .pushAfter(0, TimeUnit.MILLISECONDS, Arrays.asList("push", "push"))
             .forward(null)
             .forward(new RuntimeException("test"));

        river.flush((Object) null)
             .flush((Object[]) null)
             .flush((Iterable<Object>) null)
             .flush(new Object[0])
             .flush(Arrays.asList())
             .flush("push")
             .flush(new Object[]{"push"})
             .flush(new Object[]{"push", "push"})
             .flush(Arrays.asList("push"))
             .flush(Arrays.asList("push", "push"))
             .flushAfter(0, TimeUnit.MILLISECONDS, (Object) null)
             .flushAfter(0, TimeUnit.MILLISECONDS, (Object[]) null)
             .flushAfter(0, TimeUnit.MILLISECONDS, (Iterable<Object>) null)
             .flushAfter(0, TimeUnit.MILLISECONDS, new Object[0])
             .flushAfter(0, TimeUnit.MILLISECONDS, Arrays.asList())
             .flushAfter(0, TimeUnit.MILLISECONDS, "push")
             .flushAfter(0, TimeUnit.MILLISECONDS, new Object[]{"push"})
             .flushAfter(0, TimeUnit.MILLISECONDS, new Object[]{"push", "push"})
             .flushAfter(0, TimeUnit.MILLISECONDS, Arrays.asList("push"))
             .flushAfter(0, TimeUnit.MILLISECONDS, Arrays.asList("push", "push"))
             .flush();
    }

    private static void testRivers(final River<Object> upRiver, final River<Object> downRiver) {

        testRiver(downRiver);
        testStream(downRiver);
        testRiver(upRiver);
        testStream(upRiver);

        assertThat(downRiver.size()).isEqualTo(1);
        assertThat(upRiver.size()).isEqualTo(1);
    }

    private static void testStream(final River<Object> river) {

        river.pushStream(0, (Object) null)
             .pushStream(0, (Object[]) null)
             .pushStream(0, (Iterable<Object>) null)
             .pushStream(0)
             .pushStream(0, Arrays.asList())
             .pushStream(0, "push")
             .pushStream(0, new Object[]{"push"})
             .pushStream(0, "push", "push")
             .pushStream(0, Arrays.asList("push"))
             .pushStream(0, Arrays.asList("push", "push"))
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS, (Object) null)
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS, (Object[]) null)
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS, (Iterable<Object>) null)
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS)
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList())
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS, "push")
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS, new Object[]{"push"})
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS, "push", "push")
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList("push"))
             .pushStreamAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList("push", "push"))
             .forwardStream(0, null)
             .forwardStream(0, new RuntimeException("test"));

        river.flushStream(0, (Object) null)
             .flushStream(0, (Object[]) null)
             .flushStream(0, (Iterable<Object>) null)
             .flushStream(0)
             .flushStream(0, Arrays.asList())
             .flushStream(0, "push")
             .flushStream(0, new Object[]{"push"})
             .flushStream(0, "push", "push")
             .flushStream(0, Arrays.asList("push"))
             .flushStream(0, Arrays.asList("push", "push"))
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS, (Object) null)
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS, (Object[]) null)
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS, (Iterable<Object>) null)
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS)
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList())
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS, "push")
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS, new Object[]{"push"})
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS, "push", "push")
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList("push"))
             .flushStreamAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList("push", "push"))
             .flushStream(0);
    }

    public void testChain() {

        assertThat(fall().chain().pull("test").all()).containsExactly("test");

        assertThat(fall().chain(new OpenGate<Object>()).pull("test").all()).containsExactly("test");

        assertThat(fall().chain(new GateGenerator<Object, String>() {

            @Override
            public Gate<Object, String> create(final int fallNumber) {

                return new AbstractGate<Object, String>() {

                    @Override
                    public void onPush(final River<Object> upRiver, final River<String> downRiver,
                            final int fallNumber, final Object drop) {

                        downRiver.push(drop.toString());
                    }
                };
            }
        }).pull("test").all()).containsExactly("test");

        assertThat(fall().in(3).chain(new GateGenerator<Object, String>() {

            @Override
            public Gate<Object, String> create(final int fallNumber) {

                return new AbstractGate<Object, String>() {

                    @Override
                    public void onPush(final River<Object> upRiver, final River<String> downRiver,
                            final int fallNumber, final Object drop) {

                        downRiver.push(drop.toString());
                    }
                };
            }
        }).pull("test").all()).containsExactly("test", "test", "test");

        assertThat(fall().start().chain().pull("test").all()).containsExactly("test");

        assertThat(fall().start().chain(new OpenGate<Object>()).pull("test").all()).containsExactly(
                "test");

        assertThat(fall().start().chain(new GateGenerator<Object, String>() {

            @Override
            public Gate<Object, String> create(final int fallNumber) {

                return new AbstractGate<Object, String>() {

                    @Override
                    public void onPush(final River<Object> upRiver, final River<String> downRiver,
                            final int fallNumber, final Object drop) {

                        downRiver.push(drop.toString());
                    }
                };
            }
        }).pull("test").all()).containsExactly("test");

        assertThat(fall().start().in(3).chain(new GateGenerator<Object, String>() {

            @Override
            public Gate<Object, String> create(final int fallNumber) {

                return new AbstractGate<Object, String>() {

                    @Override
                    public void onPush(final River<Object> upRiver, final River<String> downRiver,
                            final int fallNumber, final Object drop) {

                        downRiver.push(drop.toString());
                    }
                };
            }
        }).pull("test").all()).containsExactly("test", "test", "test");

        assertThat(fall().in(2).start().chain().pull("test").all()).containsExactly("test", "test");

        assertThat(fall().in(2)
                         .start()
                         .chain(new OpenGate<Object>())
                         .pull("test")
                         .all()).containsExactly("test", "test");

        assertThat(fall().in(2).start().chain(new GateGenerator<Object, String>() {

            @Override
            public Gate<Object, String> create(final int fallNumber) {

                return new AbstractGate<Object, String>() {

                    @Override
                    public void onPush(final River<Object> upRiver, final River<String> downRiver,
                            final int fallNumber, final Object drop) {

                        downRiver.push(drop.toString());
                    }
                };
            }
        }).pull("test").all()).containsExactly("test", "test");

        assertThat(fall().in(2).start().in(3).chain(new GateGenerator<Object, String>() {

            @Override
            public Gate<Object, String> create(final int fallNumber) {

                return new AbstractGate<Object, String>() {

                    @Override
                    public void onPush(final River<Object> upRiver, final River<String> downRiver,
                            final int fallNumber, final Object drop) {

                        downRiver.push(drop.toString());
                    }
                };
            }
        }).pull("test").all()).containsExactly("test", "test", "test", "test", "test", "test");

        assertThat(fall().in(2)
                         .start()
                         .in(3)
                         .chain(new OpenGate<Object>())
                         .pull("test")
                         .all()).containsExactly("test", "test", "test", "test", "test", "test");

        assertThat(fall().in(2).start().in(3).chain().pull("test").all()).containsExactly("test",
                                                                                          "test",
                                                                                          "test",
                                                                                          "test",
                                                                                          "test",
                                                                                          "test");

        assertThat(fall().start().in(3).chain().pull("test").all()).containsExactly("test", "test",
                                                                                    "test");

        assertThat(fall().dam()
                         .chain()
                         .chain(new Classification<Gate<Object, Object>>() {})
                         .pull("test")
                         .all()).containsExactly("test");

        assertThat(fall().in(3)
                         .dam()
                         .chain()
                         .in(1)
                         .chain(new Classification<Gate<Object, Object>>() {})
                         .pull("test")
                         .all()).containsExactly("test", "test", "test");

        assertThat(fall().in(2)
                         .dam()
                         .chain()
                         .chain(new Classification<Gate<Object, Object>>() {})
                         .pull("test")
                         .all()).containsExactly("test", "test");

        assertThat(fall().in(2)
                         .dam()
                         .chain()
                         .in(3)
                         .chain(new Classification<Gate<Object, Object>>() {})
                         .pull("test")
                         .all()).containsExactly("test", "test", "test", "test", "test", "test");

        assertThat(fall().dam()
                         .chain()
                         .in(2)
                         .chain(new Classification<Gate<Object, Object>>() {})
                         .pull("test")
                         .all()).containsExactly("test", "test");

        final Waterfall<Object, Object, Object> fall0 = fall().start();
        final Waterfall<Object, Object, Object> fall1 = fall().in(2).start();
        fall1.chain(fall0);

        final Collector<Object> collector0 = fall0.collect();
        fall1.flush("test");
        assertThat(collector0.all()).containsExactly("test", "test");

        final Waterfall<Object, Object, Object> fall2 = fall().in(2).start();
        final Waterfall<Object, Object, Object> fall3 = fall().in(2).start();
        fall3.chain(fall2);

        final Collector<Object> collector2 = fall2.collect();
        fall3.flush("test");
        assertThat(collector2.all()).containsExactly("test", "test");

        final Waterfall<Object, Object, Object> fall4 = fall().in(2).start();
        final Waterfall<Object, Object, Object> fall5 = fall().in(3).start();
        fall5.chain(fall4);

        final Collector<Object> collector4 = fall4.collect();
        fall5.flush("test");
        assertThat(collector4.all()).containsExactly("test", "test", "test", "test", "test",
                                                     "test");

        final Waterfall<Object, Object, Object> fall6 = fall().in(2).start();
        final Waterfall<Object, Object, Object> fall7 = fall().start();
        fall7.chain(fall6);

        final Collector<Object> collector6 = fall6.collect();
        fall7.flush("test");
        assertThat(collector6.all()).containsExactly("test", "test");
    }

    public void testCollect() {

        final Waterfall<String, String, String> fall = fall().inBackground(1).start(String.class);
        Collector<String> collector = fall.collect();

        fall.source().flush("test");

        assertThat(collector.next()).isEqualTo("test");

        collector = fall.collect();
        fall.source().flushAfter(2, TimeUnit.SECONDS, "test");

        try {

            collector.now().next();

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(collector.now().all()).isEmpty();
        assertThat(collector.eventually().all()).containsExactly("test");

        collector = fall.collect();
        fall.source().flushAfter(2, TimeUnit.SECONDS, "test");

        try {

            collector.afterMax(100, TimeUnit.MILLISECONDS).next();

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(collector.afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();
        assertThat(collector.eventually().all()).containsExactly("test");

        collector = fall.collect();
        fall.source().flushAfter(2, TimeUnit.SECONDS, "test");

        try {

            collector.afterMax(100, TimeUnit.MILLISECONDS)
                     .eventuallyThrow(new IllegalStateException())
                     .next();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            collector.afterMax(100, TimeUnit.MILLISECONDS)
                     .eventuallyThrow(new IllegalStateException())
                     .all();

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(collector.eventually().all()).containsExactly("test");

        collector = fall.collect();
        fall.source().flush("test");

        collector.next();

        try {

            collector.remove();

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testDam() {

        final DamGate2 damGate = new DamGate2(1);

        final Waterfall<Object, Object, Object> fall = fall().dam(DamGate2.class).chain(damGate);

        assertThat(fall.on(DamGate.class).immediately().perform(new Action<Integer, DamGate>() {

            @Override
            public Integer doOn(final DamGate gate, final Object... args) {

                return gate.getId();
            }
        })).isEqualTo(1);

        assertThat(fall.on(Classification.ofType(DamGate2.class))
                       .immediately()
                       .perform(new Action<Integer, DamGate>() {

                           @Override
                           public Integer doOn(final DamGate gate, final Object... args) {

                               return gate.getId();
                           }
                       })).isEqualTo(1);

        assertThat(fall.on(damGate).immediately().perform(new Action<Integer, DamGate>() {

            @Override
            public Integer doOn(final DamGate gate, final Object... args) {

                return gate.getId();
            }
        })).isEqualTo(1);

        final Waterfall<Object, Object, Object> fall1 = fall.close((Gate) null)
                                                            .close(new OpenGate<Object>())
                                                            .close(damGate)
                                                            .dam(Classification.ofType(
                                                                    DamGate.class))
                                                            .chain(new DamGate());

        assertThat(fall1.on(DamGate.class).immediately().perform(new Action<Integer, DamGate>() {

            @Override
            public Integer doOn(final DamGate gate, final Object... args) {

                return gate.getId();
            }
        })).isEqualTo(0);

        try {

            fall1.on(DamGate2.class);

            fail();

        } catch (final Exception ignored) {

        }

        final Waterfall<Object, Object, Object> fall2 =
                fall1.close(Classification.ofType(String.class))
                     .close(Classification.ofType(DamGate.class))
                     .dam()
                     .chain(new DamGate2(2));

        assertThat(fall2.on(DamGate.class).immediately().perform(new Action<Integer, DamGate>() {

            @Override
            public Integer doOn(final DamGate gate, final Object... args) {

                return gate.getId();
            }
        })).isEqualTo(2);

        assertThat(fall2.on(Classification.ofType(DamGate2.class))
                        .immediately()
                        .perform(new Action<Integer, DamGate>() {

                            @Override
                            public Integer doOn(final DamGate gate, final Object... args) {

                                return gate.getId();
                            }
                        })).isEqualTo(2);

        assertThat(fall2.close(Classification.ofType(DamGate2.class))
                        .in(new CurrentGenerator() {

                            @Override
                            public Current create(final int fallNumber) {

                                return Currents.straight();
                            }
                        })
                        .in(3)
                        .dam()
                        .chain(new DamGate2(3))
                        .on(DamGate2.class)
                        .immediately()
                        .perform(new Action<Integer, DamGate>() {

                            @Override
                            public Integer doOn(final DamGate gate, final Object... args) {

                                return gate.getId();
                            }
                        })).isEqualTo(3);
    }

    public void testDeviate() {

        final Waterfall<Integer, Integer, Integer> fall1 = fall().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall2 =
                fall1.chain(new AbstractGate<Integer, Integer>() {

                    @Override
                    public void onPush(final River<Integer> upRiver, final River<Integer> downRiver,
                            final int fallNumber, final Integer drop) {

                        downRiver.push(drop - 1);

                        if (drop == 0) {

                            upRiver.deviate();
                            downRiver.deviate();
                        }
                    }
                });
        final Waterfall<Integer, Integer, String> fall3 =
                fall2.chain(new AbstractGate<Integer, String>() {

                    @Override
                    public void onPush(final River<Integer> upRiver, final River<String> downRiver,
                            final int fallNumber, final Integer drop) {

                        downRiver.push(drop.toString());
                    }
                });
        final Waterfall<Integer, String, String> fall4 = fall3.chain();

        assertThat(fall4.pull(1).now().next()).isEqualTo("0");
        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();

        fall1.chain(new AbstractGate<Integer, Integer>() {

            @Override
            public void onPush(final River<Integer> upRiver, final River<Integer> downRiver,
                    final int fallNumber, final Integer drop) {

                downRiver.push(drop);

                if (drop == -1) {

                    upRiver.drain();
                    downRiver.drain();
                }
            }
        }).chain(fall3);

        assertThat(fall4.pull(1).now().next()).isEqualTo("1");
        assertThat(fall4.pull(-1).now().all()).isEmpty();
        assertThat(fall4.pull(0).now().all()).isEmpty();

        fall1.chain(new AbstractGate<Integer, Integer>() {

            @Override
            public void onPush(final River<Integer> upRiver, final River<Integer> downRiver,
                    final int fallNumber, final Integer drop) {

                downRiver.push(drop);
            }
        }).chain(fall3);

        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();

        final Waterfall<Object, Object, Object> fall5 = fall().start();
        final Waterfall<Object, Object, Object> fall6 = fall5.chain();
        final Waterfall<Object, Object, Object> fall7 = fall6.chain();

        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.deviate();
        assertThat(fall6.pull("test").all()).containsExactly("test");
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall6.chain(fall7);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.deviate(Direction.DOWNSTREAM);
        assertThat(fall6.pull("test").all()).containsExactly("test");
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall6.chain(fall7);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.deviate(Direction.UPSTREAM);
        assertThat(fall6.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall5.chain(fall6);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.deviateStream(0);
        assertThat(fall6.pull("test").all()).containsExactly("test");
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall6.chain(fall7);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.deviateStream(0, Direction.DOWNSTREAM);
        assertThat(fall6.pull("test").all()).containsExactly("test");
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall6.chain(fall7);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.deviateStream(0, Direction.UPSTREAM);
        assertThat(fall6.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall5.chain(fall6);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.drain();
        assertThat(fall6.pull("test").all()).containsExactly("test");
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall6.chain(fall7);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.drain(Direction.DOWNSTREAM);
        assertThat(fall6.pull("test").all()).containsExactly("test");
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall6.chain(fall7);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.drain(Direction.UPSTREAM);
        assertThat(fall6.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall5.chain(fall6);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.drainStream(0);
        assertThat(fall6.pull("test").all()).containsExactly("test");
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall6.chain(fall7);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.drainStream(0, Direction.DOWNSTREAM);
        assertThat(fall6.pull("test").all()).containsExactly("test");
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall6.chain(fall7);
        assertThat(fall7.pull("test").all()).containsExactly("test");

        fall6.drainStream(0, Direction.UPSTREAM);
        assertThat(fall6.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();
        assertThat(fall7.pull("test").afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();

        fall5.chain(fall6);
        assertThat(fall7.pull("test").all()).containsExactly("test");
    }

    public void testDeviateStream() {

        final Waterfall<Integer, Integer, Integer> fall1 = fall().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall2 =
                fall1.chain(new AbstractGate<Integer, Integer>() {

                    @Override
                    public void onPush(final River<Integer> upRiver, final River<Integer> downRiver,
                            final int fallNumber, final Integer drop) {

                        downRiver.push(drop - 1);

                        if (drop == 0) {

                            upRiver.deviateStream(0);
                            downRiver.deviateStream(0);
                        }
                    }
                });
        final Waterfall<Integer, Integer, String> fall3 =
                fall2.chain(new AbstractGate<Integer, String>() {

                    @Override
                    public void onPush(final River<Integer> upRiver, final River<String> downRiver,
                            final int fallNumber, final Integer drop) {

                        downRiver.push(drop.toString());
                    }
                });
        final Waterfall<Integer, String, String> fall4 = fall3.chain();

        assertThat(fall4.pull(1).now().next()).isEqualTo("0");
        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();

        fall1.chain(new AbstractGate<Integer, Integer>() {

            @Override
            public void onPush(final River<Integer> upRiver, final River<Integer> downRiver,
                    final int fallNumber, final Integer drop) {

                downRiver.push(drop);

                if (drop == -1) {

                    upRiver.drainStream(0);
                    downRiver.drainStream(0);
                }
            }
        }).chain(0, fall3);

        assertThat(fall4.pull(1).now().next()).isEqualTo("1");
        assertThat(fall4.pull(-1).now().all()).isEmpty();
        assertThat(fall4.pull(0).now().all()).isEmpty();

        fall1.chain(new AbstractGate<Integer, Integer>() {

            @Override
            public void onPush(final River<Integer> upRiver, final River<Integer> downRiver,
                    final int fallNumber, final Integer drop) {

                downRiver.push(drop);
            }
        }).chain(0, fall3);

        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();
    }

    public void testDistribute() {

        final TraceGate gate1 = new TraceGate();

        final Waterfall<String, String, ?> source1 =
                fall().start(String.class).in(4).distribute().chain(gate1).source();

        final ArrayList<String> data = new ArrayList<String>();

        for (int i = 0; i < 30; ++i) {

            data.add(Integer.toString(i));
        }

        source1.push(data);
        assertThat(gate1.getData()).contains(data.toArray(new String[data.size()]));

        final IllegalStateException exception = new IllegalStateException();
        source1.forward(exception);
        assertThat(gate1.getUnhandled()).containsExactly(exception, exception, exception,
                                                         exception);

        source1.flush();
        assertThat(gate1.getFlushes()).isEqualTo(4);

        assertThat(fall().distribute().pull("test", "test").all()).containsExactly("test", "test");
        assertThat(fall().start().in(1).distribute().pull("test", "test").all()).containsExactly(
                "test", "test");
        assertThat(fall().start().in(3).distribute().pull("test", "test").all()).containsExactly(
                "test", "test");

        final TraceGate gate2 = new TraceGate();

        final Waterfall<String, String, ?> source2 =
                fall().start(String.class).in(4).distribute(new Pump<String>() {

                    @Override
                    public int onPush(final String drop) {

                        try {

                            return (Integer.parseInt(drop) % 4);

                        } catch (final NumberFormatException ignored) {

                        }

                        return DEFAULT_STREAM;
                    }
                }).chain(gate2).source();

        source2.push(data);
        assertThat(gate2.getData()).contains(data.toArray(new String[data.size()]));

        source2.forward(exception);
        assertThat(gate2.getUnhandled()).containsExactly(exception, exception, exception,
                                                         exception);

        source2.flush();
        assertThat(gate2.getFlushes()).isEqualTo(4);

        assertThat(fall().distribute(new Pump<Object>() {

            @Override
            public int onPush(final Object drop) {

                if (drop.equals("stop")) {

                    return NO_STREAM;
                }

                if (drop.equals("all")) {

                    return ALL_STREAMS;
                }

                return DEFAULT_STREAM;
            }
        }).pull("test", "stop").all()).containsExactly("test", "stop");
        assertThat(fall().start().in(1).distribute(new Pump<Object>() {

            @Override
            public int onPush(final Object drop) {

                if (drop.equals("stop")) {

                    return NO_STREAM;
                }

                if (drop.equals("all")) {

                    return ALL_STREAMS;
                }

                return DEFAULT_STREAM;
            }
        }).pull("test", "stop", "all").all()).containsExactly("test", "stop", "all");
        assertThat(fall().start().in(3).distribute(new Pump<Object>() {

            @Override
            public int onPush(final Object drop) {

                if (drop.equals("stop")) {

                    return NO_STREAM;
                }

                if (drop.equals("all")) {

                    return ALL_STREAMS;
                }

                return DEFAULT_STREAM;
            }
        }).pull("all", "test", "stop").all()).containsExactly("all", "all", "all", "test");
    }

    public void testError() throws InterruptedException {

        try {

            new DataStream<Object>(null,
                                   new DataFall<Object, Object>(fall().start(), Currents.straight(),
                                                                new OpenGate<Object>(), 0));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DataStream<Object>(new DataFall<Object, Object>(fall().start(), Currents.straight(),
                                                                new OpenGate<Object>(), 0), null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DataFall<Object, Object>(null, Currents.straight(), new OpenGate<Object>(), 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DataFall<Object, Object>(fall().start(), null, new OpenGate<Object>(), 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DataFall<Object, Object>(fall().start(), Currents.straight(), null, 0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new WaterfallRiver<Object>(null, Direction.DOWNSTREAM);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new WaterfallRiver<Object>(null, Direction.UPSTREAM);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new PumpGate<Object>(null, 1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new PumpGate<Object>(new Pump<Object>() {

                @Override
                public int onPush(final Object drop) {

                    return 0;
                }
            }, -1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start((Class<?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start((Classification<?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start((Gate<?, ?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start((GateGenerator<?, ?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start((Collection<Gate<Object, Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final List<Gate<Object, Object>> gates = Collections.emptyList();
            fall().start(gates);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().spring((SpringGenerator<?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().spring((Collection<Spring<Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final List<Spring<Object>> springs = Collections.emptyList();
            fall().spring(springs);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().chain((Gate<Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().chain((GateGenerator<Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            //noinspection unchecked
            fall().chain((Collection<Gate<Object, Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final List<Gate<Object, Object>> gates = Collections.emptyList();
            fall().chain(gates);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().chain((Classification<Gate<Object, Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().chain(new Classification<Gate<Object, Object>>() {});

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().chain((Waterfall<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().chain(0, null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().chain(1, fall().start());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final OpenGate<Object> gate = new OpenGate<Object>();

            fall().start(gate).chain(gate);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final OpenGate<Object> gate = new OpenGate<Object>();

            fall().chain(gate).chain(gate);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().distribute(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Pump<Object> pump = new Pump<Object>() {

                @Override
                public int onPush(final Object drop) {

                    return 0;
                }
            };

            fall().in(2).distribute(pump).distribute(pump);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final OpenGate<Object> gate = new OpenGate<Object>();

            fall().start(gate).start((Class<?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final OpenGate<Object> gate = new OpenGate<Object>();

            fall().start(gate).start((Classification<?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final OpenGate<Object> gate = new OpenGate<Object>();

            fall().start(gate).start((Gate<?, ?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final OpenGate<Object> gate = new OpenGate<Object>();

            fall().start(gate).start((GateGenerator<?, ?>) null);

            fail();

        } catch (final Exception ignored) {

        }


        try {

            fall().start().chain((Gate<Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().chain((GateGenerator<Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().chain((Classification<Gate<Object, Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().chain(new Classification<Gate<Object, Object>>() {});

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().chain((Waterfall<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().chain(fall());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().chain(0, fall());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().chain(1, fall());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().dam((Class) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().dam((Classification) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().dam(Integer.class).chain(new OpenGate<Object>());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().dam(new Classification<Integer>() {}).chain(new OpenGate<Object>());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().in(2).dam().start(new GateGenerator<Object, Object>() {

                @Override
                public Gate<Object, Object> create(final int fallNumber) {

                    return new OpenGate<Object>();
                }
            });

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().in(2).dam().chain(new GateGenerator<Object, Object>() {

                @Override
                public Gate<Object, Object> create(final int fallNumber) {

                    return new OpenGate<Object>();
                }
            });

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().in(2).start().dam().chain(new GateGenerator<Object, Object>() {

                @Override
                public Gate<Object, Object> create(final int fallNumber) {

                    return new OpenGate<Object>();
                }
            });

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.chain(waterfall);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.chain(0, waterfall);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.chain(1, waterfall);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.chain().chain(waterfall);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.chain().chain(0, waterfall);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.chain().chain(1, waterfall);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().in((CurrentGenerator) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().in((Current) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().in(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().inBackground(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.on((Class<?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.on((Classification<?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.on((Object) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.on(new OpenGate<Integer>());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = fall().start();

            waterfall.on(String.class);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().collect();

            fail();

        } catch (final Exception ignored) {

        }

        testRiver(fall());
        testRiver(fall().in(1));
        testRiver(fall().distribute());
        testRiver(fall().start());
        testStream(fall().start());
        testRiver(fall().chain());
        testStream(fall().chain());

        final Waterfall<Object, Object, Object> fall1 = fall().dam()
                                                              .start(new LatchGate())
                                                              .inBackground(1)
                                                              .chain(new TestGate())
                                                              .chain(new OpenGate<Object>() {

                                                                  @Override
                                                                  public void onUnhandled(
                                                                          final River<Object> upRiver,
                                                                          final River<Object> downRiver,
                                                                          final int fallNumber,
                                                                          final Throwable throwable) {

                                                                      if ((throwable != null)
                                                                              && !"test".equals(
                                                                              throwable.getMessage())) {

                                                                          downRiver.on(
                                                                                  LatchGate.class)
                                                                                   .immediately()
                                                                                   .perform(
                                                                                           new Action<Void, LatchGate>() {

                                                                                               @Override
                                                                                               public Void doOn(
                                                                                                       final LatchGate gate,
                                                                                                       final Object... args) {

                                                                                                   gate.setFailed();

                                                                                                   return null;
                                                                                               }
                                                                                           });
                                                                      }
                                                                  }
                                                              });

        fall1.source().push("test");

        fall1.on(LatchGate.class)
             .afterMax(30, TimeUnit.SECONDS)
             .eventuallyThrow(new IllegalStateException())
             .when(new ConditionEvaluator<LatchGate>() {

                 @Override
                 public boolean isSatisfied(final LatchGate gate) {

                     return (gate.getCount() == 3);
                 }
             })
             .perform(new Action<Void, LatchGate>() {

                 @Override
                 public Void doOn(final LatchGate gate, final Object... args) {

                     if (gate.isFailed()) {

                         fail();
                     }

                     return null;
                 }
             });

        final Waterfall<Object, Object, Object> fall2 = fall().dam()
                                                              .start(new LatchGate())
                                                              .inBackground(1)
                                                              .chain(new TestGate())
                                                              .inBackground(1)
                                                              .chain(new OpenGate<Object>() {

                                                                  @Override
                                                                  public void onUnhandled(
                                                                          final River<Object> upRiver,
                                                                          final River<Object> downRiver,
                                                                          final int fallNumber,
                                                                          final Throwable throwable) {

                                                                      if ((throwable != null)
                                                                              && !"test".equals(
                                                                              throwable.getMessage())) {

                                                                          downRiver.on(
                                                                                  LatchGate.class)
                                                                                   .immediately()
                                                                                   .perform(
                                                                                           new Action<Void, LatchGate>() {

                                                                                               @Override
                                                                                               public Void doOn(
                                                                                                       final LatchGate gate,
                                                                                                       final Object... args) {

                                                                                                   gate.setFailed();

                                                                                                   return null;
                                                                                               }
                                                                                           });
                                                                      }
                                                                  }
                                                              });

        fall2.source().push("test");

        fall2.on(LatchGate.class)
             .afterMax(3, TimeUnit.SECONDS)
             .eventuallyThrow(new IllegalStateException())
             .when(new ConditionEvaluator<LatchGate>() {

                 @Override
                 public boolean isSatisfied(final LatchGate gate) {

                     return (gate.getCount() == 3);
                 }
             })
             .perform(new Action<Void, LatchGate>() {

                 @Override
                 public Void doOn(final LatchGate gate, final Object... args) {

                     if (gate.isFailed()) {

                         fail();
                     }

                     return null;
                 }
             });
    }

    public void testIn() {

        assertThat(fall().in(1).in(Currents.straight()).chain().in(new CurrentGenerator() {

            @Override
            public Current create(final int fallNumber) {

                return Currents.straight();
            }
        }).chain().pull("test").all()).containsExactly("test");

        assertThat(fall().in(3).in(Currents.straight()).chain().in(new CurrentGenerator() {

            @Override
            public Current create(final int fallNumber) {

                return Currents.straight();
            }
        }).chain().pull("test").all()).containsExactly("test", "test", "test");

        assertThat(fall().in(3)
                         .in(Currents.straight())
                         .chain()
                         .inBackground(2)
                         .chain()
                         .pull("test")
                         .all()).containsExactly("test", "test", "test", "test", "test", "test");

        assertThat(fall().in(3).inBackground().chain().pull("test").all()).contains("test");
    }

    public void testJoin() {

        final Waterfall<Character, Integer, Integer> fall0 =
                fall().start(new AbstractGate<Character, Integer>() {

                    private final StringBuffer mBuffer = new StringBuffer();

                    @Override
                    public void onPush(final River<Character> upRiver,
                            final River<Integer> downRiver, final int fallNumber,
                            final Character drop) {

                        mBuffer.append(drop);
                    }

                    @Override
                    public void onFlush(final River<Character> upRiver,
                            final River<Integer> downRiver, final int fallNumber) {

                        downRiver.flush(Integer.valueOf(mBuffer.toString()));

                        mBuffer.setLength(0);
                    }
                }).chain();

        final Waterfall<Character, Integer, Integer> fall1 = fall0.chain(new OpenGate<Integer>() {

            private int mSum;

            @Override
            public void onPush(final River<Integer> upRiver, final River<Integer> downRiver,
                    final int fallNumber, final Integer drop) {

                mSum += drop;
            }

            @Override
            public void onFlush(final River<Integer> upRiver, final River<Integer> downRiver,
                    final int fallNumber) {

                downRiver.flush(new Integer[]{mSum});

                mSum = 0;
            }
        });

        final Waterfall<Integer, Integer, Integer> fall2 = fall().start(Integer.class);
        fall2.chain(fall0);
        fall2.source().flush(Drops.asList(0, 1, 2, 3));

        final ArrayList<Integer> output = new ArrayList<Integer>(1);

        fall1.pull('0', '1', '2', '3').nextInto(output);
        assertThat(output).containsExactly(129);

        fall2.source().drain();

        final Waterfall<Integer, Integer, Integer> fall3 = fall().start(Integer.class);

        fall1.source().push(Drops.asList('0', '1', '2', '3'));
        fall3.chain(fall0);
        fall3.source().flush(Drops.asList(4, 5, -4));
        fall2.source().flush(77);

        output.clear();
        fall1.pull().allInto(output);

        assertThat(output).containsExactly(128);

        final Waterfall<Integer, Integer, Integer> fall4 = fall().start(new OpenGate<Integer>() {

            private int mAbsSum;

            @Override
            public void onPush(final River<Integer> upRiver, final River<Integer> downRiver,
                    final int fallNumber, final Integer drop) {

                mAbsSum += Math.abs(drop);
            }

            @Override
            public void onFlush(final River<Integer> upRiver, final River<Integer> downRiver,
                    final int fallNumber) {

                downRiver.flush(mAbsSum);
            }
        });

        fall0.chain(fall4);

        fall3.source().flush(Arrays.asList(4, 5, -4));

        output.clear();
        fall1.pull(Drops.asList('0', '1', '2', '3')).allInto(output);
        fall4.pull().nextInto(output);

        assertThat(output).containsExactly(128, 136);

        try {

            final Waterfall<Object, Object, Object> fall = fall().start();

            fall().chain(fall);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testPump() {

        final ArrayList<String> output = new ArrayList<String>();

        //noinspection unchecked
        final Waterfall<String, List<String>, String> fall = fall().start(new OpenGate<String>() {

            @Override
            public void onPush(final River<String> upRiver, final River<String> downRiver,
                    final int fallNumber, final String drop) {

                if ("test".equals(drop)) {

                    throw new IllegalArgumentException();
                }

                super.onPush(upRiver, downRiver, fallNumber, drop);
            }
        }).in(2).chain(new OpenGate<String>() {

            @Override
            public void onPush(final River<String> upRiver, final River<String> downRiver,
                    final int fallNumber, final String drop) {

                if ((drop.length() == 0) || drop.toLowerCase().charAt(0) < 'm') {

                    if (fallNumber == 0) {

                        downRiver.push(drop);
                    }

                } else if (fallNumber == 1) {

                    downRiver.push(drop);
                }
            }
        }).chain(Arrays.asList(new AbstractGate<String, List<String>>() {

                                   private final ArrayList<String> mWords = new ArrayList<String>();

                                   @Override
                                   public void onPush(final River<String> upRiver,
                                           final River<List<String>> downRiver,
                                           final int fallNumber, final String drop) {

                                       if ("atest".equals(drop)) {

                                           throw new IllegalStateException();
                                       }

                                       mWords.add(drop);
                                   }

                                   @Override
                                   public void onFlush(final River<String> upRiver,
                                           final River<List<String>> downRiver,
                                           final int fallNumber) {

                                       Collections.sort(mWords);
                                       downRiver.flush(new ArrayList<String>(mWords));
                                       mWords.clear();
                                   }
                               },

                               new AbstractGate<String, List<String>>() {

                                   private final ArrayList<String> mWords = new ArrayList<String>();

                                   @Override
                                   public void onPush(final River<String> upRiver,
                                           final River<List<String>> downRiver,
                                           final int fallNumber, final String drop) {

                                       mWords.add(drop);
                                   }

                                   @Override
                                   public void onFlush(final River<String> upRiver,
                                           final River<List<String>> downRiver,
                                           final int fallNumber) {

                                       Collections.sort(mWords, Collections.reverseOrder());
                                       downRiver.flush(new ArrayList<String>(mWords));
                                       mWords.clear();
                                   }
                               })).in(1).chain(new AbstractGate<List<String>, String>() {

            private int mCount;

            private ArrayList<String> mList = new ArrayList<String>();

            @Override
            public void onPush(final River<List<String>> upRiver, final River<String> downRiver,
                    final int fallNumber, final List<String> drop) {

                if (mList.isEmpty() || drop.isEmpty()) {

                    mList.addAll(drop);

                } else {

                    final String first = drop.get(0);

                    if ((first.length() == 0) || first.toLowerCase().charAt(0) < 'm') {

                        mList.addAll(0, drop);

                    } else {

                        mList.addAll(drop);
                    }
                }

                if (++mCount == 2) {

                    downRiver.flush(new ArrayList<String>(mList));
                    mList.clear();
                    mCount = 0;
                }
            }

            @Override
            public void onUnhandled(final River<List<String>> upRiver,
                    final River<String> downRiver, final int fallNumber,
                    final Throwable throwable) {

                // just ignore it
            }
        });

        fall.pull("Ciao", "This", "zOO", null, "is", "a", "3", "test", "1111", "CAPITAL", "atest")
            .allInto(output);

        assertThat(output).containsExactly("1111", "3", "CAPITAL", "Ciao", "a", "is", "zOO",
                                           "This");

        assertThat(fall.pull("test").all()).isEmpty();

        final Waterfall<Integer, Integer, Integer> fall0 = fall().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall1 = fall().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall2 = fall().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall3 = fall().start(Integer.class);

        fall1.chain(fall0);
        fall2.chain(fall0);
        fall3.chain(fall0);

        final Collector<Integer> collector = fall0.collect();

        fall1.flush(1);
        fall2.flush(1);
        fall3.flush(1);

        int i = 0;

        while (collector.hasNext()) {

            ++i;
            assertThat(collector.next()).isEqualTo(1);
        }

        assertThat(i).isEqualTo(3);

        assertThat(fall().in(3).start(Integer.class).pull(1).all()).containsExactly(1, 1, 1);
        assertThat(fall().in(3).start(Integer.class).in(1).pull(1).all()).containsExactly(1, 1, 1);

        assertThat(fall().inBackground(3).start(Integer.class).pull(1).all()).containsExactly(1, 1,
                                                                                              1);
        assertThat(fall().inBackground(3).start(Integer.class).in(1).pull(1).all()).containsExactly(
                1, 1, 1);
    }

    public void testSpring() {

        //noinspection unchecked
        assertThat(fall().spring(Arrays.asList(Springs.sequence(0, 2), Springs.sequence(3, 2)))
                         .pull()
                         .all()).containsExactly(0, 1, 2, 3, 4, 5);
        //noinspection unchecked
        assertThat(fall().in(3)
                         .spring(Arrays.asList(Springs.sequence(0, 2), Springs.sequence(3, 2)))
                         .pull()
                         .all()).containsExactly(0, 1, 2, 3, 4, 5);
        assertThat(fall().spring(new SpringGenerator<Integer>() {

            @Override
            public Spring<Integer> create(final int fallNumber) {

                return Springs.sequence(fallNumber, 2);
            }
        }).pull().all()).containsExactly(0, 1, 2);
        assertThat(fall().in(2).spring(new SpringGenerator<Integer>() {

            @Override
            public Spring<Integer> create(final int fallNumber) {

                return Springs.sequence(fallNumber, 2);
            }
        }).pull().all()).containsExactly(0, 1, 2, 1, 2, 3);
    }

    public void testStart() {

        assertThat(fall().start().pull("test").all()).containsExactly("test");

        assertThat(fall().start(String.class).pull("test").all()).containsExactly("test");

        assertThat(
                fall().start(new Classification<String>() {}).pull("test").all()).containsExactly(
                "test");

        assertThat(fall().start(new OpenGate<String>()).pull("test").all()).containsExactly("test");

        assertThat(fall().start(new GateGenerator<String, Object>() {

            @Override
            public Gate<String, Object> create(final int fallNumber) {

                return new AbstractGate<String, Object>() {

                    @Override
                    public void onPush(final River<String> upRiver, final River<Object> downRiver,
                            final int fallNumber, final String drop) {

                        downRiver.push(drop);
                    }
                };
            }
        }).pull("test").all()).containsExactly("test");

        assertThat(fall().in(3).start(new GateGenerator<String, Object>() {

            @Override
            public Gate<String, Object> create(final int fallNumber) {

                return new AbstractGate<String, Object>() {

                    @Override
                    public void onPush(final River<String> upRiver, final River<Object> downRiver,
                            final int fallNumber, final String drop) {

                        downRiver.push(drop);
                    }
                };
            }
        }).pull("test").all()).containsExactly("test", "test", "test");
    }

    private static class DamGate extends OpenGate<Object> {

        public int getId() {

            return 0;
        }
    }

    private static class DamGate2 extends DamGate {

        private int mId;

        public DamGate2(final int id) {

            mId = id;
        }

        @Override
        public int getId() {

            return mId;
        }
    }

    private static class LatchGate extends OpenGate<Object> {

        private int mCount;

        private boolean mFailed;

        public int getCount() {

            return mCount;
        }

        public void incCount() {

            ++mCount;
        }

        public boolean isFailed() {

            return mFailed;
        }

        public void setFailed() {

            mFailed = true;
        }
    }

    private static class TestGate extends OpenGate<Object> {

        public boolean mFlushed;

        public boolean mPushed;

        public boolean mThrown;

        @Override
        public void onFlush(final River<Object> upRiver, final River<Object> downRiver,
                final int fallNumber) {

            if (isFailed(upRiver) || mFlushed) {

                return;
            }

            mFlushed = true;

            try {

                testRivers(upRiver, downRiver);

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        try {

                            testRivers(upRiver, downRiver);

                        } catch (final Throwable ignored) {

                            setFailed(downRiver);

                        } finally {

                            incCount(downRiver);
                        }
                    }
                }).start();

            } catch (final Throwable ignored) {

                setFailed(downRiver);
            }
        }

        private void incCount(final River<Object> river) {

            river.on(LatchGate.class).immediately().perform(new Action<Void, LatchGate>() {

                @Override
                public Void doOn(final LatchGate gate, final Object... args) {

                    gate.incCount();

                    return null;
                }
            });
        }

        private boolean isFailed(final River<Object> river) {

            return river.on(LatchGate.class)
                        .immediately()
                        .perform(new Action<Boolean, LatchGate>() {

                            @Override
                            public Boolean doOn(final LatchGate gate, final Object... args) {

                                return gate.isFailed();
                            }
                        });
        }

        private void setFailed(final River<Object> river) {

            river.on(LatchGate.class).immediately().perform(new Action<Void, LatchGate>() {

                @Override
                public Void doOn(final LatchGate gate, final Object... args) {

                    gate.setFailed();

                    return null;
                }
            });
        }

        @Override
        public void onPush(final River<Object> upRiver, final River<Object> downRiver,
                final int fallNumber, final Object drop) {

            if (isFailed(downRiver) || mPushed) {

                return;
            }

            mPushed = true;

            try {

                testRivers(upRiver, downRiver);

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        try {

                            testRivers(upRiver, downRiver);

                        } catch (final Throwable ignored) {

                            setFailed(upRiver);

                        } finally {

                            incCount(upRiver);
                        }
                    }
                }).start();

            } catch (final Throwable ignored) {

                setFailed(downRiver);
            }
        }

        @Override
        public void onUnhandled(final River<Object> upRiver, final River<Object> downRiver,
                final int fallNumber, final Throwable throwable) {

            if (isFailed(upRiver) || mThrown) {

                return;
            }

            mThrown = true;

            try {

                testRivers(upRiver, downRiver);

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        try {

                            testRivers(upRiver, downRiver);

                        } catch (final Throwable ignored) {

                            setFailed(downRiver);

                        } finally {

                            incCount(downRiver);
                        }
                    }
                }).start();

            } catch (final Throwable ignored) {

                setFailed(upRiver);
            }
        }
    }

    private class TraceGate extends OpenGate<String> {

        private final ArrayList<String> mData = new ArrayList<String>();

        private final ArrayList<Throwable> mThrows = new ArrayList<Throwable>();

        private int mFlushCount;

        public List<String> getData() {

            return mData;
        }

        public int getFlushes() {

            return mFlushCount;
        }

        public List<Throwable> getUnhandled() {

            return mThrows;
        }

        @Override
        public void onFlush(final River<String> upRiver, final River<String> downRiver,
                final int fallNumber) {

            ++mFlushCount;

            super.onFlush(upRiver, downRiver, fallNumber);
        }

        @Override
        public void onPush(final River<String> upRiver, final River<String> downRiver,
                final int fallNumber, final String drop) {

            mData.add(drop);

            super.onPush(upRiver, downRiver, fallNumber, drop);
        }

        @Override
        public void onUnhandled(final River<String> upRiver, final River<String> downRiver,
                final int fallNumber, final Throwable throwable) {

            mThrows.add(throwable);

            super.onUnhandled(upRiver, downRiver, fallNumber, throwable);
        }
    }
}