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

import com.bmd.wtf.drp.Drops;
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.Gate.Action;
import com.bmd.wtf.flw.Gate.ConditionEvaluator;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.AbstractLeap;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for waterfall classes.
 * <p/>
 * Created by davide on 6/28/14.
 */
public class WaterfallTest extends TestCase {

    public void testBarrage() {

        final ArrayList<String> output = new ArrayList<String>();

        final Waterfall<String, List<String>, String> fall =
                Waterfall.create().start(new FreeLeap<String, String>() {

                    @Override
                    public void onPush(final River<String, String> upRiver,
                            final River<String, String> downRiver, final int fallNumber,
                            final String drop) {

                        if ("test".equals(drop)) {

                            throw new IllegalArgumentException();
                        }

                        super.onPush(upRiver, downRiver, fallNumber, drop);
                    }
                }).in(2).chain(new FreeLeap<String, String>() {

                    @Override
                    public void onPush(final River<String, String> upRiver,
                            final River<String, String> downRiver, final int fallNumber,
                            final String drop) {

                        if ((drop.length() == 0) || drop.toLowerCase().charAt(0) < 'm') {

                            if (fallNumber == 0) {

                                downRiver.push(drop);
                            }

                        } else if (fallNumber == 1) {

                            downRiver.push(drop);
                        }
                    }
                }).chain(new LeapGenerator<String, String, List<String>>() {

                    @Override
                    public Leap<String, String, List<String>> start(final int fallNumber) {

                        if (fallNumber == 0) {

                            return new AbstractLeap<String, String, List<String>>() {

                                private final ArrayList<String> mWords = new ArrayList<String>();

                                @Override
                                public void onPush(final River<String, String> upRiver,
                                        final River<String, List<String>> downRiver,
                                        final int fallNumber, final String drop) {

                                    if ("atest".equals(drop)) {

                                        throw new IllegalStateException();
                                    }

                                    mWords.add(drop);
                                }

                                @Override
                                public void onDischarge(final River<String, String> upRiver,
                                        final River<String, List<String>> downRiver,
                                        final int fallNumber) {

                                    Collections.sort(mWords);
                                    downRiver.push(new ArrayList<String>(mWords)).discharge();
                                    mWords.clear();
                                }
                            };
                        }

                        return new AbstractLeap<String, String, List<String>>() {

                            private final ArrayList<String> mWords = new ArrayList<String>();

                            @Override
                            public void onPush(final River<String, String> upRiver,
                                    final River<String, List<String>> downRiver,
                                    final int fallNumber, final String drop) {

                                mWords.add(drop);
                            }

                            @Override
                            public void onDischarge(final River<String, String> upRiver,
                                    final River<String, List<String>> downRiver,
                                    final int fallNumber) {

                                Collections.sort(mWords, Collections.reverseOrder());
                                downRiver.push(new ArrayList<String>(mWords)).discharge();
                                mWords.clear();
                            }
                        };
                    }
                }).in(1).chain(new AbstractLeap<String, List<String>, String>() {

                    private int mCount;

                    private ArrayList<String> mList = new ArrayList<String>();

                    @Override
                    public void onPush(final River<String, List<String>> upRiver,
                            final River<String, String> downRiver, final int fallNumber,
                            final List<String> drop) {

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

                            downRiver.push(new ArrayList<String>(mList)).discharge();
                            mList.clear();
                            mCount = 0;
                        }
                    }

                    @Override
                    public void onUnhandled(final River<String, List<String>> upRiver,
                            final River<String, String> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        //just ignore it
                    }
                });

        fall.pull("Ciao", "This", "zOO", null, "is", "a", "3", "test", "1111", "CAPITAL", "atest")
            .allInto(output);

        assertThat(output).containsExactly("1111", "3", "CAPITAL", "Ciao", "a", "is", "zOO",
                                           "This");

        assertThat(fall.pull("test").all()).isEmpty();

        final Waterfall<Integer, Integer, Integer> fall0 = Waterfall.create().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall1 = Waterfall.create().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall2 = Waterfall.create().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall3 = Waterfall.create().start(Integer.class);

        fall1.chain(fall0);
        fall2.chain(fall0);
        fall3.chain(fall0);

        final Collector<Integer> collector = fall0.collect();

        fall1.push(1).discharge();
        fall2.push(1).discharge();
        fall3.push(1).discharge();

        int i = 0;

        while (collector.hasNext()) {

            ++i;
            assertThat(collector.next()).isEqualTo(1);
        }

        assertThat(i).isEqualTo(3);

        assertThat(Waterfall.create().in(3).start(Integer.class).pull(1).all()).containsExactly(1,
                                                                                                1,
                                                                                                1);
        assertThat(
                Waterfall.create().in(3).start(Integer.class).in(1).pull(1).all()).containsExactly(
                1, 1, 1);

        assertThat(Waterfall.create()
                            .inBackground(3)
                            .start(Integer.class)
                            .pull(1)
                            .all()).containsExactly(1, 1, 1);
        assertThat(Waterfall.create()
                            .inBackground(3)
                            .start(Integer.class)
                            .in(1)
                            .pull(1)
                            .all()).containsExactly(1, 1, 1);
    }

    public void testCollect() {

        final Waterfall<String, String, String> fall =
                Waterfall.create().inBackground(1).start(String.class);
        Collector<String> collector = fall.collect();

        fall.source().push("test").discharge();

        assertThat(collector.next()).isEqualTo("test");

        collector = fall.collect();
        fall.source().pushAfter(2, TimeUnit.SECONDS, "test").discharge();

        try {

            collector.now().next();

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(collector.now().all()).isEmpty();
        assertThat(collector.eventually().all()).containsExactly("test");

        collector = fall.collect();
        fall.source().pushAfter(2, TimeUnit.SECONDS, "test").discharge();

        try {

            collector.afterMax(100, TimeUnit.MILLISECONDS).next();

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(collector.afterMax(100, TimeUnit.MILLISECONDS).all()).isEmpty();
        assertThat(collector.eventually().all()).containsExactly("test");

        collector = fall.collect();
        fall.source().pushAfter(2, TimeUnit.SECONDS, "test").discharge();

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
        fall.source().push("test").discharge();

        collector.next();

        try {

            collector.remove();

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testDeviate() {

        final Waterfall<Integer, Integer, Integer> fall1 = Waterfall.create().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall2 =
                fall1.chain(new AbstractLeap<Integer, Integer, Integer>() {

                    @Override
                    public void onPush(final River<Integer, Integer> upRiver,
                            final River<Integer, Integer> downRiver, final int fallNumber,
                            final Integer drop) {

                        downRiver.push(drop - 1);

                        if (drop == 0) {

                            upRiver.deviate();
                            downRiver.deviate();
                        }
                    }
                });
        final Waterfall<Integer, Integer, String> fall3 =
                fall2.chain(new AbstractLeap<Integer, Integer, String>() {

                    @Override
                    public void onPush(final River<Integer, Integer> upRiver,
                            final River<Integer, String> downRiver, final int fallNumber,
                            final Integer drop) {

                        downRiver.push(drop.toString());
                    }
                });
        final Waterfall<Integer, String, String> fall4 = fall3.chain();

        assertThat(fall4.pull(1).now().next()).isEqualTo("0");
        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();

        fall1.chain(new AbstractLeap<Integer, Integer, Integer>() {

            @Override
            public void onPush(final River<Integer, Integer> upRiver,
                    final River<Integer, Integer> downRiver, final int fallNumber,
                    final Integer drop) {

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

        fall1.chain(new AbstractLeap<Integer, Integer, Integer>() {

            @Override
            public void onPush(final River<Integer, Integer> upRiver,
                    final River<Integer, Integer> downRiver, final int fallNumber,
                    final Integer drop) {

                downRiver.push(drop);
            }
        }).chain(fall3);

        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();
    }

    public void testDeviateStream() {

        final Waterfall<Integer, Integer, Integer> fall1 = Waterfall.create().start(Integer.class);
        final Waterfall<Integer, Integer, Integer> fall2 =
                fall1.chain(new AbstractLeap<Integer, Integer, Integer>() {

                    @Override
                    public void onPush(final River<Integer, Integer> upRiver,
                            final River<Integer, Integer> downRiver, final int fallNumber,
                            final Integer drop) {

                        downRiver.push(drop - 1);

                        if (drop == 0) {

                            upRiver.deviate(0);
                            downRiver.deviate(0);
                        }
                    }
                });
        final Waterfall<Integer, Integer, String> fall3 =
                fall2.chain(new AbstractLeap<Integer, Integer, String>() {

                    @Override
                    public void onPush(final River<Integer, Integer> upRiver,
                            final River<Integer, String> downRiver, final int fallNumber,
                            final Integer drop) {

                        downRiver.push(drop.toString());
                    }
                });
        final Waterfall<Integer, String, String> fall4 = fall3.chain();

        assertThat(fall4.pull(1).now().next()).isEqualTo("0");
        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();

        fall1.chain(new AbstractLeap<Integer, Integer, Integer>() {

            @Override
            public void onPush(final River<Integer, Integer> upRiver,
                    final River<Integer, Integer> downRiver, final int fallNumber,
                    final Integer drop) {

                downRiver.push(drop);

                if (drop == -1) {

                    upRiver.drain(0);
                    downRiver.drain(0);
                }
            }
        }).chain(fall3);

        assertThat(fall4.pull(1).now().next()).isEqualTo("1");
        assertThat(fall4.pull(-1).now().all()).isEmpty();
        assertThat(fall4.pull(0).now().all()).isEmpty();

        fall1.chain(new AbstractLeap<Integer, Integer, Integer>() {

            @Override
            public void onPush(final River<Integer, Integer> upRiver,
                    final River<Integer, Integer> downRiver, final int fallNumber,
                    final Integer drop) {

                downRiver.push(drop);
            }
        }).chain(fall3);

        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();
    }

    public void testDistribute() {

        final TestLeap leap = new TestLeap();

        final Waterfall<String, String, ?> source =
                Waterfall.create().start(String.class).in(4).distribute().chain(leap).source();

        final ArrayList<String> data = new ArrayList<String>();

        for (int i = 0; i < 30; i++) {

            data.add(Integer.toString(i));
        }

        source.push(data);
        assertThat(leap.getData()).contains(data.toArray(new String[data.size()]));

        final IllegalStateException exception = new IllegalStateException();
        source.forward(exception);
        assertThat(leap.getUnhandled()).containsExactly(exception, exception, exception, exception);

        source.discharge();
        assertThat(leap.getDischarges()).isEqualTo(4);
    }

    public void testError() throws InterruptedException {

        try {

            Waterfall.create().start((Class) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().start((Classification) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().start((Leap) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().start((LeapGenerator) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().chain((Leap<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().chain((LeapGenerator<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().chain((Classification<Leap<Object, Object, Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().chain(new Classification<Leap<Object, Object, Object>>() {});

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().chain((Waterfall<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final FreeLeap<Object, Object> leap = new FreeLeap<Object, Object>();

            Waterfall.create().start(leap).chain(leap);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final FreeLeap<Object, Object> leap = new FreeLeap<Object, Object>();

            Waterfall.create().chain(leap).chain(leap);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final FreeLeap<Object, Object> leap = new FreeLeap<Object, Object>();

            Waterfall.create().start(leap).start((Class) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final FreeLeap<Object, Object> leap = new FreeLeap<Object, Object>();

            Waterfall.create().start(leap).start((Classification) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final FreeLeap<Object, Object> leap = new FreeLeap<Object, Object>();

            Waterfall.create().start(leap).start((Leap) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final FreeLeap<Object, Object> leap = new FreeLeap<Object, Object>();

            Waterfall.create().start(leap).start((LeapGenerator) null);

            fail();

        } catch (final Exception ignored) {

        }


        try {

            Waterfall.create().start().chain((Leap<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().start().chain((LeapGenerator<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().start().chain((Classification<Leap<Object, Object, Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().start().chain(new Classification<Leap<Object, Object, Object>>() {});

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().start().chain((Waterfall<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().as((Class) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().as((Classification) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().as(Integer.class).chain(new FreeLeap<Object, Object>());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.create().as(new Classification<Integer>() {})
                     .chain(new FreeLeap<Object, Object>());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = Waterfall.create().start();

            waterfall.chain(waterfall);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Waterfall<Object, Object, Object> waterfall = Waterfall.create().start();

            waterfall.chain().chain(waterfall);

            fail();

        } catch (final Exception ignored) {

        }

        final LatchLeap latchLeap = new LatchLeap();

        final FreeLeap<Object, Object> testLeap = new FreeLeap<Object, Object>() {

            public boolean mDischarged;

            public boolean mPushed;

            public boolean mThrown;

            @Override
            public void onDischarge(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber) {

                if (isFailed(upRiver) || mDischarged) {

                    return;
                }

                mDischarged = true;

                try {

                    test(upRiver, downRiver);

                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            try {

                                test(upRiver, downRiver);

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

            private void incCount(final River<Object, Object> river) {

                river.on(LatchLeap.class).immediately().perform(new Action<Void, LatchLeap>() {

                                                                    @Override
                                                                    public Void doOn(
                                                                            final LatchLeap leap,
                                                                            final Object... args) {

                                                                        leap.incCount();

                                                                        return null;
                                                                    }
                                                                }
                );
            }

            private boolean isFailed(final River<Object, Object> river) {

                return river.on(LatchLeap.class)
                            .immediately()
                            .perform(new Action<Boolean, LatchLeap>() {

                                         @Override
                                         public Boolean doOn(final LatchLeap leap,
                                                 final Object... args) {

                                             return leap.isFailed();
                                         }
                                     }
                            );
            }

            private void setFailed(final River<Object, Object> river) {

                river.on(LatchLeap.class).immediately().perform(new Action<Void, LatchLeap>() {

                                                                    @Override
                                                                    public Void doOn(
                                                                            final LatchLeap leap,
                                                                            final Object... args) {

                                                                        leap.setFailed();

                                                                        return null;
                                                                    }
                                                                }
                );
            }

            private void test(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver) {

                downRiver.push((Object) null)
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
                         .forward(new RuntimeException("test"))
                         .discharge();

                downRiver.push(0, (Object) null)
                         .push(0, (Object[]) null)
                         .push(0, (Iterable<Object>) null)
                         .push(0, new Object[0])
                         .push(0, Arrays.asList())
                         .push(0, "push")
                         .push(0, new Object[]{"push"})
                         .push(0, new Object[]{"push", "push"})
                         .push(0, Arrays.asList("push"))
                         .push(0, Arrays.asList("push", "push"))
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, (Object) null)
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, (Object[]) null)
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, (Iterable<Object>) null)
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, new Object[0])
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList())
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, "push")
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, new Object[]{"push"})
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, new Object[]{"push", "push"})
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList("push"))
                         .pushAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList("push", "push"))
                         .forward(0, null)
                         .forward(0, new RuntimeException("test"))
                         .discharge(0);

                upRiver.push((Object) null)
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
                       .forward(new RuntimeException("test"))
                       .discharge();

                upRiver.push(0, (Object) null)
                       .push(0, (Object[]) null)
                       .push(0, (Iterable<Object>) null)
                       .push(0, new Object[0])
                       .push(0, Arrays.asList())
                       .push(0, "push")
                       .push(0, new Object[]{"push"})
                       .push(0, new Object[]{"push", "push"})
                       .push(0, Arrays.asList("push"))
                       .push(0, Arrays.asList("push", "push"))
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, (Object) null)
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, (Object[]) null)
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, (Iterable<Object>) null)
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, new Object[0])
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList())
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, "push")
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, new Object[]{"push"})
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, new Object[]{"push", "push"})
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList("push"))
                       .pushAfter(0, 0, TimeUnit.MILLISECONDS, Arrays.asList("push", "push"))
                       .forward(0, null)
                       .forward(0, new RuntimeException("test"))
                       .discharge(0);

                assertThat(downRiver.source()).isNotNull();
                assertThat(downRiver.size()).isEqualTo(1);
                assertThat(upRiver.source()).isNotNull();
                assertThat(upRiver.size()).isEqualTo(1);
            }

            @Override
            public void onPush(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber,
                    final Object drop) {

                if (isFailed(downRiver) || mPushed) {

                    return;
                }

                mPushed = true;

                try {

                    test(upRiver, downRiver);

                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            try {

                                test(upRiver, downRiver);

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
            public void onUnhandled(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber,
                    final Throwable throwable) {

                if (isFailed(upRiver) || mThrown) {

                    return;
                }

                mThrown = true;

                try {

                    test(upRiver, downRiver);

                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            try {

                                test(upRiver, downRiver);

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
        };

        final Waterfall<Object, Object, Object> fall = Waterfall.create()
                                                                .asGate()
                                                                .start(latchLeap)
                                                                .chain(testLeap)
                                                                .chain(new FreeLeap<Object, Object>() {

                                                                    @Override
                                                                    public void onUnhandled(
                                                                            final River<Object, Object> upRiver,
                                                                            final River<Object, Object> downRiver,
                                                                            final int fallNumber,
                                                                            final Throwable throwable) {

                                                                        if ((throwable != null)
                                                                                && !"test".equals(
                                                                                throwable.getMessage())) {

                                                                            downRiver.on(
                                                                                    LatchLeap.class)
                                                                                     .immediately()
                                                                                     .perform(
                                                                                             new Action<Void, LatchLeap>() {

                                                                                                 @Override
                                                                                                 public Void doOn(
                                                                                                         final LatchLeap leap,
                                                                                                         final Object... args) {

                                                                                                     leap.setFailed();

                                                                                                     return null;
                                                                                                 }
                                                                                             }
                                                                                     );
                                                                        }
                                                                    }
                                                                });

        fall.source().push("test");

        fall.on(LatchLeap.class)
            .afterMax(3, TimeUnit.SECONDS)
            .eventuallyThrow(new IllegalStateException())
            .meeting(new ConditionEvaluator<LatchLeap>() {

                @Override
                public boolean isSatisfied(final LatchLeap leap) {

                    return (leap.getCount() == 3);
                }
            })
            .perform(new Action<Void, LatchLeap>() {

                @Override
                public Void doOn(final LatchLeap leap, final Object... args) {

                    if (leap.isFailed()) {

                        fail();
                    }

                    return null;
                }
            });
    }

    public void testJoin() {

        final Waterfall<Character, Integer, Integer> fall0 =
                Waterfall.create().start(new AbstractLeap<Character, Character, Integer>() {

                    private final StringBuffer mBuffer = new StringBuffer();

                    @Override
                    public void onPush(final River<Character, Character> upRiver,
                            final River<Character, Integer> downRiver, final int fallNumber,
                            final Character drop) {

                        mBuffer.append(drop);
                    }

                    @Override
                    public void onDischarge(final River<Character, Character> upRiver,
                            final River<Character, Integer> downRiver, final int fallNumber) {

                        downRiver.push(Integer.valueOf(mBuffer.toString())).discharge();

                        mBuffer.setLength(0);
                    }
                }).chain();

        final Waterfall<Character, Integer, Integer> fall1 =
                fall0.chain(new FreeLeap<Character, Integer>() {

                    private int mSum;

                    @Override
                    public void onPush(final River<Character, Integer> upRiver,
                            final River<Character, Integer> downRiver, final int fallNumber,
                            final Integer drop) {

                        mSum += drop;
                    }

                    @Override
                    public void onDischarge(final River<Character, Integer> upRiver,
                            final River<Character, Integer> downRiver, final int fallNumber) {

                        downRiver.push(new Integer[]{mSum}).discharge();

                        mSum = 0;
                    }
                });

        final Waterfall<Integer, Integer, Integer> fall2 = Waterfall.create().start(Integer.class);
        fall2.chain(fall0);
        fall2.source().push(Drops.asList(0, 1, 2, 3)).discharge();

        final ArrayList<Integer> output = new ArrayList<Integer>(1);

        fall1.pull('0', '1', '2', '3').nextInto(output);
        assertThat(output).containsExactly(129);

        fall2.source().drain();

        final Waterfall<Integer, Integer, Integer> fall3 = Waterfall.create().start(Integer.class);

        fall1.source().push(Drops.asList('0', '1', '2', '3'));
        fall3.chain(fall0);
        fall3.source().push(Drops.asList(4, 5, -4)).discharge();
        fall2.source().push(77).discharge();

        output.clear();
        fall1.pull().allInto(output);

        assertThat(output).containsExactly(128);

        final Waterfall<Integer, Integer, Integer> fall4 =
                Waterfall.create().start(new FreeLeap<Integer, Integer>() {

                    private int mAbsSum;

                    @Override
                    public void onPush(final River<Integer, Integer> upRiver,
                            final River<Integer, Integer> downRiver, final int fallNumber,
                            final Integer drop) {

                        mAbsSum += Math.abs(drop);
                    }

                    @Override
                    public void onDischarge(final River<Integer, Integer> upRiver,
                            final River<Integer, Integer> downRiver, final int fallNumber) {

                        downRiver.push(mAbsSum).discharge();
                    }
                });

        fall0.chain(fall4);

        fall3.source().push(Arrays.asList(4, 5, -4)).discharge();

        output.clear();
        fall1.pull(Drops.asList('0', '1', '2', '3')).allInto(output);
        fall4.pull().nextInto(output);

        assertThat(output).containsExactly(128, 136);
    }

    private static class LatchLeap extends FreeLeap<Object, Object> {

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

    private class TestLeap extends FreeLeap<String, String> {

        private final ArrayList<String> mData = new ArrayList<String>();

        private final ArrayList<Throwable> mThrows = new ArrayList<Throwable>();

        private int mDischargeCount;

        public List<String> getData() {

            return mData;
        }

        public int getDischarges() {

            return mDischargeCount;
        }

        public List<Throwable> getUnhandled() {

            return mThrows;
        }

        @Override
        public void onDischarge(final River<String, String> upRiver,
                final River<String, String> downRiver, final int fallNumber) {

            ++mDischargeCount;

            super.onDischarge(upRiver, downRiver, fallNumber);
        }

        @Override
        public void onPush(final River<String, String> upRiver,
                final River<String, String> downRiver, final int fallNumber, final String drop) {

            mData.add(drop);

            super.onPush(upRiver, downRiver, fallNumber, drop);
        }

        @Override
        public void onUnhandled(final River<String, String> upRiver,
                final River<String, String> downRiver, final int fallNumber,
                final Throwable throwable) {

            mThrows.add(throwable);

            super.onUnhandled(upRiver, downRiver, fallNumber, throwable);
        }
    }
}