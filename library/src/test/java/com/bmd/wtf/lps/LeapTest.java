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

import junit.framework.TestCase;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for leap classes.
 * <p/>
 * Created by davide on 6/27/14.
 */
public class LeapTest extends TestCase {

    public void testDecorator() {

        final Leap<Object, Object, Object> dam1 = new FreeLeap<Object, Object>();
        final Leap<Object, Object, Object> dam2 = new FreeLeap<Object, Object>();

        final LeapDecorator<Object, Object, Object> decorator1 =
                new LeapDecorator<Object, Object, Object>(dam1);
        final LeapDecorator<Object, Object, Object> decorator2 =
                new LeapDecorator<Object, Object, Object>(dam1);
        final LeapDecorator<Object, Object, Object> decorator3 =
                new LeapDecorator<Object, Object, Object>(dam2);

        assertThat(decorator1).isEqualTo(decorator1);
        assertThat(decorator1).isEqualTo(decorator2);
        assertThat(decorator1.hashCode()).isEqualTo(decorator2.hashCode());
        assertThat(decorator1).isNotEqualTo(new LeapDecorator<Object, Object, Object>(decorator1));
        assertThat(decorator1).isNotEqualTo(null);
        assertThat(decorator1).isNotEqualTo(decorator3);

        try {

            new LeapDecorator<Object, Object, Object>(null);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testWeak() {

        FreeLeap<Object, Object> freeLeap = new FreeLeap<Object, Object>();
        final Leap<Object, Object, Object> weak1 = Leaps.weak(freeLeap);
        final Leap<Object, Object, Object> weak2 = Leaps.weak(freeLeap);

        assertThat(weak1).isEqualTo(weak1);
        assertThat(weak1).isEqualTo(weak2);
        assertThat(weak1).isNotEqualTo(freeLeap);
        assertThat(weak1).isNotEqualTo(null);
        //noinspection EqualsBetweenInconvertibleTypes
        assertThat(weak1.equals("test")).isFalse();
        assertThat(weak1.hashCode()).isEqualTo(weak2.hashCode());

        final Leap<Object, Object, Object> weak3 = Leaps.weak(freeLeap, false);

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

        freeLeap = new FreeLeap<Object, Object>();

        final Leap<Object, Object, Object> weak4 = Leaps.weak(freeLeap);

        assertThat(weak1).isNotEqualTo(weak3);
        assertThat(weak2).isNotEqualTo(weak3);

        //noinspection UnusedAssignment
        freeLeap = null;

        System.gc();
        System.gc();

        assertThat(weak1).isNotEqualTo(weak3);
        assertThat(weak2).isNotEqualTo(weak3);

        Leap<Object, Object, Object> leap1 = new AbstractLeap<Object, Object, Object>() {

            private Object mLast;

            @Override
            public void onDischarge(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber) {

                if ("flush1".equals(mLast)) {

                    throw new IllegalArgumentException("flush1");
                }

                super.onDischarge(upRiver, downRiver, fallNumber);
            }

            @Override
            public void onPush(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber,
                    final Object drop) {

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
            public void onUnhandled(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber,
                    final Throwable throwable) {

                if ("push1".equals(throwable.getMessage())) {

                    throw new IllegalArgumentException("push1");
                }

                super.onUnhandled(upRiver, downRiver, fallNumber, throwable);
            }


        };
        final Waterfall<Object, Object, Object> waterfall1 =
                Waterfall.create().start(Leaps.weak(leap1, false));
        Leap<Object, Object, Object> leap2 = new AbstractLeap<Object, Object, Object>() {

            private Object mLast;

            @Override
            public void onDischarge(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber) {

                if ("flush2".equals(mLast)) {

                    throw new IllegalArgumentException("flush2");
                }

                super.onDischarge(upRiver, downRiver, fallNumber);
            }

            @Override
            public void onPush(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber,
                    final Object drop) {

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
            public void onUnhandled(final River<Object, Object> upRiver,
                    final River<Object, Object> downRiver, final int fallNumber,
                    final Throwable throwable) {

                if ("push2".equals((throwable).getMessage())) {

                    throw new IllegalArgumentException("push2");
                }

                super.onUnhandled(upRiver, downRiver, fallNumber, throwable);
            }
        };
        final Waterfall<Object, Object, Object> waterfall2 =
                waterfall1.chain(Leaps.weak(leap2, true));

        freeLeap = new FreeLeap<Object, Object>();

        final Waterfall<Object, Object, Object> waterfall3 =
                waterfall2.chain(Leaps.weak(freeLeap, false));

        assertThat(waterfall1.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall2.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall3.pull("discharge").next()).isEqualTo("discharge");

        assertThat(waterfall1.pull("discharge1").all()).isEmpty();
        assertThat(waterfall2.pull("discharge1").all()).isEmpty();
        assertThat(waterfall3.pull("discharge1").all()).isEmpty();

        assertThat(waterfall1.pull("discharge2").all()).containsExactly("discharge2");
        assertThat(waterfall2.pull("discharge2").all()).isEmpty();
        assertThat(waterfall3.pull("discharge2").all()).isEmpty();

        assertThat(waterfall1.pull("flush1").next()).isEqualTo("flush1");
        assertThat(waterfall2.pull("flush1").all()).containsExactly("flush1");
        assertThat(waterfall3.pull("flush1").all()).containsExactly("flush1");

        assertThat(waterfall1.pull("flush2").all()).containsExactly("flush2");
        assertThat(waterfall2.pull("flush2").next()).isEqualTo("flush2");
        assertThat(waterfall3.pull("flush2").next()).isEqualTo("flush2");

        assertThat(waterfall1.pull("push").all()).isEmpty();
        assertThat(waterfall2.pull("push").all()).isEmpty();
        assertThat(waterfall3.pull("push").all()).isEmpty();

        assertThat(waterfall1.pull("push1").all()).isEmpty();
        assertThat(waterfall2.pull("push1").all()).isEmpty();
        assertThat(waterfall3.pull("push1").all()).isEmpty();

        assertThat(waterfall1.pull("push2").all()).isEmpty();
        assertThat(waterfall2.pull("push2").all()).isEmpty();
        assertThat(waterfall3.pull("push2").all()).isEmpty();

        assertThat(waterfall1.pull("pull").next()).isEqualTo("pull");
        assertThat(waterfall2.pull("pull").all()).isEmpty();
        assertThat(waterfall3.pull("pull").all()).isEmpty();

        assertThat(waterfall1.pull("pull1").all()).containsExactly("pull1");
        assertThat(waterfall2.pull("pull1").all()).isEmpty();
        assertThat(waterfall3.pull("pull1").all()).isEmpty();

        //noinspection UnusedAssignment
        freeLeap = null;

        System.gc();
        System.gc();

        assertThat(waterfall1.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall2.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall3.pull("discharge").now().all()).isEmpty();

        assertThat(waterfall1.pull("discharge1").all()).isEmpty();
        assertThat(waterfall2.pull("discharge1").all()).isEmpty();
        assertThat(waterfall3.pull("discharge1").now().all()).isEmpty();

        assertThat(waterfall1.pull("discharge2").all()).containsExactly("discharge2");
        assertThat(waterfall2.pull("discharge2").all()).isEmpty();
        assertThat(waterfall3.pull("discharge2").now().all()).isEmpty();

        assertThat(waterfall1.pull("flush1").next()).isEqualTo("flush1");
        assertThat(waterfall2.pull("flush1").all()).containsExactly("flush1");
        assertThat(waterfall3.pull("flush1").now().all()).isEmpty();

        assertThat(waterfall1.pull("flush2").all()).containsExactly("flush2");
        assertThat(waterfall2.pull("flush2").next()).isEqualTo("flush2");
        assertThat(waterfall3.pull("flush2").now().all()).isEmpty();

        assertThat(waterfall1.pull("push").all()).isEmpty();
        assertThat(waterfall2.pull("push").all()).isEmpty();
        assertThat(waterfall3.pull("push").now().all()).isEmpty();

        assertThat(waterfall1.pull("push1").all()).isEmpty();
        assertThat(waterfall2.pull("push1").all()).isEmpty();
        assertThat(waterfall3.pull("push1").now().all()).isEmpty();

        assertThat(waterfall1.pull("push2").all()).isEmpty();
        assertThat(waterfall2.pull("push2").all()).isEmpty();
        assertThat(waterfall3.pull("push2").now().all()).isEmpty();

        assertThat(waterfall1.pull("pull").next()).isEqualTo("pull");
        assertThat(waterfall2.pull("pull").all()).isEmpty();
        assertThat(waterfall3.pull("pull").now().all()).isEmpty();

        assertThat(waterfall1.pull("pull1").all()).containsExactly("pull1");
        assertThat(waterfall2.pull("pull1").all()).isEmpty();
        assertThat(waterfall3.pull("pull1").now().all()).isEmpty();

        //noinspection UnusedAssignment
        leap2 = null;

        System.gc();
        System.gc();

        assertThat(waterfall1.pull("discharge").next()).isEqualTo("discharge");
        assertThat(waterfall2.pull("discharge").all()).isEmpty();
        assertThat(waterfall3.pull("discharge").now().all()).isEmpty();

        assertThat(waterfall1.pull("discharge1").all()).isEmpty();
        assertThat(waterfall2.pull("discharge1").all()).isEmpty();
        assertThat(waterfall3.pull("discharge1").now().all()).isEmpty();

        assertThat(waterfall1.pull("discharge2").all()).containsExactly("discharge2");
        assertThat(waterfall2.pull("discharge2").all()).isEmpty();
        assertThat(waterfall3.pull("discharge2").now().all()).isEmpty();

        assertThat(waterfall1.pull("flush1").next()).isEqualTo("flush1");
        assertThat(waterfall2.pull("flush1").all()).isEmpty();
        assertThat(waterfall3.pull("flush1").now().all()).isEmpty();

        assertThat(waterfall1.pull("flush2").all()).containsExactly("flush2");
        assertThat(waterfall2.pull("flush2").all()).isEmpty();
        assertThat(waterfall3.pull("flush2").now().all()).isEmpty();

        assertThat(waterfall1.pull("push").all()).isEmpty();
        assertThat(waterfall2.pull("push").all()).isEmpty();
        assertThat(waterfall3.pull("push").now().all()).isEmpty();

        assertThat(waterfall1.pull("push1").all()).isEmpty();
        assertThat(waterfall2.pull("push1").all()).isEmpty();
        assertThat(waterfall3.pull("push1").now().all()).isEmpty();

        assertThat(waterfall1.pull("push2").all()).isEmpty();
        assertThat(waterfall2.pull("push2").all()).isEmpty();
        assertThat(waterfall3.pull("push2").now().all()).isEmpty();

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

        } catch (final IllegalArgumentException ignore) {

        }

        try {

            Leaps.weak(null, true);

            fail();

        } catch (final IllegalArgumentException ignore) {

        }

        try {

            Leaps.weak(null, false);

            fail();

        } catch (final IllegalArgumentException ignore) {

        }
    }
}