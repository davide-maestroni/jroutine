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

import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.AbstractLeap;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for waterfall classes.
 * <p/>
 * Created by davide on 6/28/14.
 */
public class WaterfallTest extends TestCase {

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

    public void testError() {

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

        final boolean[] fail = new boolean[1];

        Waterfall.create().start(new FreeLeap<Object, Object>() {

            public boolean mFlushed;

            @Override
            public void onDischarge(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber) {

                if (fail[0] || mFlushed) {

                    return;
                }

                mFlushed = true;

                fail[0] = true;

                downRiver.push((Object) null).push((Object[]) null).push((Iterable<Object>) null)
                         .push(new Object[0]).push(Arrays.asList()).push("push")
                         .push(new Object[]{"push"}).push(new Object[]{"push", "push"})
                         .push(Arrays.asList("push")).push(Arrays.asList("push", "push"))
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
                         .forward(null).forward(new RuntimeException()).discharge();

                downRiver.push(0, (Object) null).push(0, (Object[]) null)
                         .push(0, (Iterable<Object>) null).push(0, new Object[0])
                         .push(0, Arrays.asList()).push(0, "push").push(0, new Object[]{"push"})
                         .push(0, new Object[]{"push", "push"}).push(0, Arrays.asList("push"))
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
                         .forward(0, null).forward(0, new RuntimeException()).discharge(0);

                upRiver.push((Object) null).push((Object[]) null).push((Iterable<Object>) null)
                       .push(new Object[0]).push(Arrays.asList()).push("push")
                       .push(new Object[]{"push"}).push(new Object[]{"push", "push"})
                       .push(Arrays.asList("push")).push(Arrays.asList("push", "push"))
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
                       .forward(null).forward(new RuntimeException()).discharge();

                upRiver.push(0, (Object) null).push(0, (Object[]) null)
                       .push(0, (Iterable<Object>) null).push(0, new Object[0])
                       .push(0, Arrays.asList()).push(0, "push").push(0, new Object[]{"push"})
                       .push(0, new Object[]{"push", "push"}).push(0, Arrays.asList("push"))
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
                       .forward(0, null).forward(0, new RuntimeException()).discharge(0);

                fail[0] = false;
            }

            @Override
            public void onPush(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber,
                    final Object drop) {

                if (fail[0] || !"test".equals(drop)) {

                    return;
                }

                fail[0] = true;

                downRiver.push((Object) null).push((Object[]) null).push((Iterable<Object>) null)
                         .push(new Object[0]).push(Arrays.asList()).push("push")
                         .push(new Object[]{"push"}).push(new Object[]{"push", "push"})
                         .push(Arrays.asList("push")).push(Arrays.asList("push", "push"))
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
                         .forward(null).forward(new RuntimeException()).discharge();

                downRiver.push(0, (Object) null).push(0, (Object[]) null)
                         .push(0, (Iterable<Object>) null).push(0, new Object[0])
                         .push(0, Arrays.asList()).push(0, "push").push(0, new Object[]{"push"})
                         .push(0, new Object[]{"push", "push"}).push(0, Arrays.asList("push"))
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
                         .forward(0, null).forward(0, new RuntimeException()).discharge(0);

                upRiver.push((Object) null).push((Object[]) null).push((Iterable<Object>) null)
                       .push(new Object[0]).push(Arrays.asList()).push("push")
                       .push(new Object[]{"push"}).push(new Object[]{"push", "push"})
                       .push(Arrays.asList("push")).push(Arrays.asList("push", "push"))
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
                       .forward(null).forward(new RuntimeException()).discharge();

                upRiver.push(0, (Object) null).push(0, (Object[]) null)
                       .push(0, (Iterable<Object>) null).push(0, new Object[0])
                       .push(0, Arrays.asList()).push(0, "push").push(0, new Object[]{"push"})
                       .push(0, new Object[]{"push", "push"}).push(0, Arrays.asList("push"))
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
                       .forward(0, null).forward(0, new RuntimeException()).discharge(0);

                fail[0] = false;
            }

            @Override
            public void onUnhandled(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber,
                    final Throwable throwable) {

                if (fail[0] || !"test".equals(throwable.getMessage())) {

                    return;
                }

                fail[0] = true;

                downRiver.push((Object) null).push((Object[]) null).push((Iterable<Object>) null)
                         .push(new Object[0]).push(Arrays.asList()).push("push")
                         .push(new Object[]{"push"}).push(new Object[]{"push", "push"})
                         .push(Arrays.asList("push")).push(Arrays.asList("push", "push"))
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
                         .forward(null).forward(new RuntimeException()).discharge();

                downRiver.push(0, (Object) null).push(0, (Object[]) null)
                         .push(0, (Iterable<Object>) null).push(0, new Object[0])
                         .push(0, Arrays.asList()).push(0, "push").push(0, new Object[]{"push"})
                         .push(0, new Object[]{"push", "push"}).push(0, Arrays.asList("push"))
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
                         .forward(0, null).forward(0, new RuntimeException()).discharge(0);

                upRiver.push((Object) null).push((Object[]) null).push((Iterable<Object>) null)
                       .push(new Object[0]).push(Arrays.asList()).push("push")
                       .push(new Object[]{"push"}).push(new Object[]{"push", "push"})
                       .push(Arrays.asList("push")).push(Arrays.asList("push", "push"))
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
                       .forward(null).forward(new RuntimeException()).discharge();

                upRiver.push(0, (Object) null).push(0, (Object[]) null)
                       .push(0, (Iterable<Object>) null).push(0, new Object[0])
                       .push(0, Arrays.asList()).push(0, "push").push(0, new Object[]{"push"})
                       .push(0, new Object[]{"push", "push"}).push(0, Arrays.asList("push"))
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
                       .forward(0, null).forward(0, new RuntimeException()).discharge(0);

                fail[0] = false;
            }
        }).source().push("test", "push");

        if (fail[0]) {

            fail();
        }
    }
}