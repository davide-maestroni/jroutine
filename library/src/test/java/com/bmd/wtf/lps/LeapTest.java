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
package com.bmd.wtf.lps;

import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.WeakLeap.WhenVanished;

import junit.framework.TestCase;

import java.util.ArrayList;

import static com.bmd.wtf.fll.Waterfall.fall;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for leap classes.
 * <p/>
 * Created by davide on 6/27/14.
 */
public class LeapTest extends TestCase {

    public void testDecorator() {

        final Leap<Object, Object> dam1 = new FreeLeap<Object>();
        final Leap<Object, Object> dam2 = new FreeLeap<Object>();

        final LeapDecorator<Object, Object> decorator1 = new LeapDecorator<Object, Object>(dam1);
        final LeapDecorator<Object, Object> decorator2 = new LeapDecorator<Object, Object>(dam1);
        final LeapDecorator<Object, Object> decorator3 = new LeapDecorator<Object, Object>(dam2);

        assertThat(decorator1).isEqualTo(decorator1);
        assertThat(decorator1).isEqualTo(decorator2);
        assertThat(decorator1.hashCode()).isEqualTo(decorator2.hashCode());
        assertThat(decorator1).isNotEqualTo(new LeapDecorator<Object, Object>(decorator1));
        assertThat(decorator1).isNotEqualTo(null);
        assertThat(decorator1).isNotEqualTo(decorator3);

        try {

            new LeapDecorator<Object, Object>(null);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testWeak() {

        FreeLeap<Object> freeLeap = new FreeLeap<Object>();
        final Leap<Object, Object> weak1 = Leaps.weak(freeLeap);
        final Leap<Object, Object> weak2 = Leaps.weak(freeLeap);

        assertThat(weak1).isEqualTo(weak1);
        assertThat(weak1).isEqualTo(weak2);
        assertThat(weak1).isNotEqualTo(freeLeap);
        assertThat(weak1).isNotEqualTo(null);
        //noinspection EqualsBetweenInconvertibleTypes
        assertThat(weak1.equals("test")).isFalse();
        assertThat(weak1.hashCode()).isEqualTo(weak2.hashCode());

        final Leap<Object, Object> weak3 = Leaps.weak(freeLeap, WhenVanished.CLOSE);

        assertThat(weak1).isNotEqualTo(weak3);
        assertThat(weak2).isNotEqualTo(weak3);

        //noinspection UnusedAssignment
        freeLeap = null;

        System.gc();
        System.gc();

        assertThat(weak1).isEqualTo(weak2);
        assertThat(weak1.hashCode()).isEqualTo(weak2.hashCode());
        assertThat(weak1).isNotEqualTo(weak3);
        assertThat(weak2).isNotEqualTo(weak3);

        //noinspection UnusedAssignment
        freeLeap = new FreeLeap<Object>();

        assertThat(weak1).isNotEqualTo(weak3);
        assertThat(weak2).isNotEqualTo(weak3);

        //noinspection UnusedAssignment
        freeLeap = null;

        System.gc();
        System.gc();

        assertThat(weak1).isNotEqualTo(weak3);
        assertThat(weak2).isNotEqualTo(weak3);

        Leap<Object, Object> leap1 = new AbstractLeap<Object, Object>() {

            private Object mLast;

            @Override
            public void onFlush(final River<Object> upRiver, final River<Object> downRiver,
                    final int fallNumber) {

                if ("flush1".equals(mLast)) {

                    throw new IllegalArgumentException("flush1");
                }

                super.onFlush(upRiver, downRiver, fallNumber);
            }

            @Override
            public void onPush(final River<Object> upRiver, final River<Object> downRiver,
                    final int fallNumber, final Object drop) {

                mLast = drop;

                if ("discharge1".equals(drop)) {

                    throw new IllegalArgumentException("discharge1");
                }

                final String string = drop.toString();

                if (string.startsWith("push")) {

                    throw new IllegalStateException(string);
                }

                downRiver.push(drop);
            }

            @Override
            public void onUnhandled(final River<Object> upRiver, final River<Object> downRiver,
                    final int fallNumber, final Throwable throwable) {

                if ("push1".equals(throwable.getMessage())) {

                    throw new IllegalArgumentException("push1");
                }

                super.onUnhandled(upRiver, downRiver, fallNumber, throwable);
            }


        };
        final Waterfall<Object, Object, Object> waterfall1 =
                fall().start(Leaps.weak(leap1, WhenVanished.CLOSE));
        Leap<Object, Object> leap2 = new AbstractLeap<Object, Object>() {

            private Object mLast;

            @Override
            public void onFlush(final River<Object> upRiver, final River<Object> downRiver,
                    final int fallNumber) {

                if ("flush2".equals(mLast)) {

                    throw new IllegalArgumentException("flush2");
                }

                super.onFlush(upRiver, downRiver, fallNumber);
            }

            @Override
            public void onPush(final River<Object> upRiver, final River<Object> downRiver,
                    final int fallNumber, final Object drop) {

                mLast = drop;

                final String string = drop.toString();

                if ("discharge2".equals(string)) {

                    throw new IllegalArgumentException("discharge2");
                }

                if (string.startsWith("pull")) {

                    throw new IllegalStateException(string);

                } else {

                    downRiver.push(drop);
                }
            }


            @Override
            public void onUnhandled(final River<Object> upRiver, final River<Object> downRiver,
                    final int fallNumber, final Throwable throwable) {

                if ("push2".equals((throwable).getMessage())) {

                    throw new IllegalArgumentException("push2");
                }

                super.onUnhandled(upRiver, downRiver, fallNumber, throwable);
            }
        };
        final Waterfall<Object, Object, Object> waterfall2 =
                waterfall1.chain(Leaps.weak(leap2, WhenVanished.OPEN));

        freeLeap = new FreeLeap<Object>();

        final Waterfall<Object, Object, Object> waterfall3 =
                waterfall2.chain(Leaps.weak(freeLeap, WhenVanished.CLOSE));

        assertThat(waterfall1.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall2.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall3.pull("discharge").next()).isEqualTo("discharge");

        try {

            waterfall1.pull("discharge1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("discharge1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall3.pull("discharge1").allInto(new ArrayList<Object>());

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(waterfall1.pull("discharge2").all()).containsExactly("discharge2");
        try {

            waterfall2.pull("discharge2").next();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("discharge2").nextInto(new ArrayList<Object>());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            waterfall1.pull("flush1").next();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("flush1").next();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall3.pull("flush1").next();

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(waterfall1.pull("flush2").all()).containsExactly("flush2");
        try {

            waterfall2.pull("flush2").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall3.pull("flush2").next();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            waterfall1.pull("push").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("push").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall3.pull("push").all();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            waterfall1.pull("push1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("push1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall3.pull("push1").all();

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(waterfall1.pull("pull").next()).isEqualTo("pull");
        try {

            waterfall2.pull("pull").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall3.pull("pull").all();

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(waterfall1.pull("pull1").all()).containsExactly("pull1");
        try {

            waterfall2.pull("pull1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall3.pull("pull1").all();

            fail();

        } catch (final Exception ignored) {

        }

        //noinspection UnusedAssignment
        freeLeap = null;

        System.gc();
        System.gc();

        assertThat(waterfall1.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall2.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall3.pull("discharge").now().all()).isEmpty();

        try {

            waterfall1.pull("discharge1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("discharge1").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("discharge1").now().all()).isEmpty();

        assertThat(waterfall1.pull("discharge2").all()).containsExactly("discharge2");
        try {

            waterfall2.pull("discharge2").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("discharge2").now().all()).isEmpty();

        try {

            waterfall1.pull("flush1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("flush1").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("flush1").now().all()).isEmpty();

        assertThat(waterfall1.pull("flush2").all()).containsExactly("flush2");
        try {

            waterfall2.pull("flush2").next();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("flush2").now().all()).isEmpty();

        try {

            waterfall1.pull("push").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("push").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("push").now().all()).isEmpty();

        try {

            waterfall1.pull("push1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("push1").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("push1").now().all()).isEmpty();

        assertThat(waterfall1.pull("pull").next()).isEqualTo("pull");
        try {

            waterfall2.pull("pull").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("pull").now().all()).isEmpty();

        assertThat(waterfall1.pull("pull1").all()).containsExactly("pull1");
        try {

            waterfall2.pull("pull1").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("pull1").now().all()).isEmpty();

        //noinspection UnusedAssignment
        leap2 = null;

        System.gc();
        System.gc();

        assertThat(waterfall1.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall2.pull("discharge").all()).isEmpty();
        assertThat(waterfall3.pull("discharge").now().all()).isEmpty();

        try {

            waterfall1.pull("discharge1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("discharge1").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("discharge1").now().all()).isEmpty();

        assertThat(waterfall1.pull("discharge2").all()).containsExactly("discharge2");
        assertThat(waterfall2.pull("discharge2").all()).isEmpty();
        assertThat(waterfall3.pull("discharge2").now().all()).isEmpty();

        try {

            waterfall1.pull("flush1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("flush1").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("flush1").now().all()).isEmpty();

        assertThat(waterfall1.pull("flush2").all()).containsExactly("flush2");
        assertThat(waterfall2.pull("flush2").all()).isEmpty();
        assertThat(waterfall3.pull("flush2").now().all()).isEmpty();

        try {

            waterfall1.pull("push").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("push").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("push").now().all()).isEmpty();

        try {

            waterfall1.pull("push1").all();

            fail();

        } catch (final Exception ignored) {

        }
        try {

            waterfall2.pull("push1").all();

            fail();

        } catch (final Exception ignored) {

        }
        assertThat(waterfall3.pull("push1").now().all()).isEmpty();

        assertThat(waterfall1.pull("pull").next()).isEqualTo("pull");
        assertThat(waterfall2.pull("pull").all()).isEmpty();
        assertThat(waterfall3.pull("pull").now().all()).isEmpty();

        assertThat(waterfall1.pull("pull1").all()).containsExactly("pull1");
        assertThat(waterfall2.pull("pull1").all()).isEmpty();
        assertThat(waterfall3.pull("pull1").now().all()).isEmpty();

        //noinspection UnusedAssignment
        leap1 = null;

        System.gc();
        System.gc();

        assertThat(waterfall1.pull("discharge").now().all()).isEmpty();
        assertThat(waterfall2.pull("discharge").now().all()).isEmpty();
        assertThat(waterfall3.pull("discharge").now().all()).isEmpty();

        assertThat(waterfall1.pull("discharge1").now().all()).isEmpty();
        assertThat(waterfall2.pull("discharge1").now().all()).isEmpty();
        assertThat(waterfall3.pull("discharge1").now().all()).isEmpty();

        assertThat(waterfall1.pull("discharge2").now().all()).isEmpty();
        assertThat(waterfall2.pull("discharge2").now().all()).isEmpty();
        assertThat(waterfall3.pull("discharge2").now().all()).isEmpty();

        assertThat(waterfall1.pull("flush1").now().all()).isEmpty();
        assertThat(waterfall2.pull("flush1").now().all()).isEmpty();
        assertThat(waterfall3.pull("flush1").now().all()).isEmpty();

        assertThat(waterfall1.pull("flush2").now().all()).isEmpty();
        assertThat(waterfall2.pull("flush2").now().all()).isEmpty();
        assertThat(waterfall3.pull("flush2").now().all()).isEmpty();

        assertThat(waterfall1.pull("push").now().all()).isEmpty();
        assertThat(waterfall2.pull("push").now().all()).isEmpty();
        assertThat(waterfall3.pull("push").now().all()).isEmpty();

        assertThat(waterfall1.pull("push1").now().all()).isEmpty();
        assertThat(waterfall2.pull("push1").now().all()).isEmpty();
        assertThat(waterfall3.pull("push1").now().all()).isEmpty();

        assertThat(waterfall1.pull("push2").now().all()).isEmpty();
        assertThat(waterfall2.pull("push2").now().all()).isEmpty();
        assertThat(waterfall3.pull("push2").now().all()).isEmpty();

        assertThat(waterfall1.pull("pull").now().all()).isEmpty();
        assertThat(waterfall2.pull("pull").now().all()).isEmpty();
        assertThat(waterfall3.pull("pull").now().all()).isEmpty();

        assertThat(waterfall1.pull("pull1").now().all()).isEmpty();
        assertThat(waterfall2.pull("pull1").now().all()).isEmpty();
        assertThat(waterfall3.pull("pull1").now().all()).isEmpty();

        try {

            Leaps.weak(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Leaps.weak(null, WhenVanished.OPEN);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Leaps.weak(null, WhenVanished.CLOSE);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }
}