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
package com.bmd.wtf.xtr.rpd;

import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.OnData;

import junit.framework.TestCase;

import static com.bmd.wtf.fll.Waterfall.fall;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for rapid leap objects.
 * <p/>
 * Created by davide on 7/10/14.
 */
public class RapidLeapTest extends TestCase {

    public void testError() {

        try {

            fall().start(new RapidLeapError1());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(new RapidLeapError2());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(new RapidLeapError3());

            fail();

        } catch (final Exception ignored) {

        }

        final Waterfall<Object, Object, Object> fall1 =
                fall().start(new RapidLeapError4()).chain(new FreeLeap<Object, Object>() {

                    @Override
                    public void onUnhandled(final River<Object, Object> upRiver,
                            final River<Object, Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector1 = fall1.collect();

        fall1.source().push("11", null).forward(new IllegalArgumentException()).discharge();

        for (final Object e : collector1.now().all()) {

            assertThat(((RapidException) e).getCause()).isEqualTo(new MyException());
        }
    }

    public void testInherit() {

        assertThat(fall().start(new RapidLeapTest1())
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", "37.1", null);
        assertThat(fall().start(new RapidLeapTest2())
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(13, "-27", "37.1", "");
        assertThat(fall().start(new RapidLeapTest3())
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", 37.1, null, "test");

        final Waterfall<Object, Object, Object> fall1 =
                fall().start(new RapidLeapTest4()).chain(new FreeLeap<Object, Object>() {

                    @Override
                    public void onUnhandled(final River<Object, Object> upRiver,
                            final River<Object, Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector1 = fall1.collect();

        fall1.source().push("11", 27, 37.1, null);
        assertThat(collector1.all()).containsExactly(13, new MyException(), "37.1");

        final Collector<Object> collector2 = fall1.collect();

        fall1.source().forward(new IllegalArgumentException()).discharge();
        assertThat(collector2.all()).containsExactly(new MyException());

        final Collector<Object> collector3 = fall1.collect();

        fall1.source().forward(null);
        assertThat(collector3.all()).isEmpty();

        final Waterfall<Object, Object, Object> fall2 =
                fall().start(new RapidLeapTest5()).chain(new FreeLeap<Object, Object>() {

                    @Override
                    public void onUnhandled(final River<Object, Object> upRiver,
                            final River<Object, Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector4 = fall2.collect();

        fall2.source().forward(new IllegalArgumentException()).discharge();
        assertThat(collector4.next()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    public static class MyException extends Exception {

        @Override
        public int hashCode() {

            return 111;
        }

        @Override
        public boolean equals(final Object obj) {

            return (obj instanceof MyException);
        }
    }

    public static class RapidLeapError1 extends RapidLeap<Object> {

        public String method1(final String text) {

            return text;
        }

        public Integer method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidLeapError2 extends RapidLeap<Object> {

        @OnData
        public String method1(final String text) {

            return text;
        }

        @OnData
        public Integer method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidLeapError3 extends RapidLeap<Object> {

        @OnData
        public String method1(final String text, final int ignored) {

            return text;
        }
    }

    public static class RapidLeapError4 extends RapidLeap<Object> {

        public void error(final String text) throws MyException {

            throw new MyException();
        }

        public void error(final Void ignored) throws MyException {

            throw new MyException();
        }

        public void error(final Discharge ignored) throws MyException {

            throw new MyException();
        }

        public void error(final Throwable ignored) throws MyException {

            throw new MyException();
        }
    }

    public static class RapidLeapTest1 extends RapidLeap<Object> {

        public String onObject(final Object data) {

            return data.toString();
        }

        public Integer parse(final String text) {

            return Integer.parseInt(text);
        }

        public String serialize(final Integer integer) {

            return integer.toString();
        }
    }

    public static class RapidLeapTest2 extends RapidLeapTest1 {

        public String minusSerialize(final Integer integer) {

            return "-" + integer.toString();
        }

        public String onNull(final Void data) {

            return "";
        }

        @Override
        public Integer parse(final String text) {

            return Integer.parseInt(text) + 2;
        }
    }

    public static class RapidLeapTest3 extends RapidLeap<Object> {

        public RapidLeapTest3() {

            super(true);
        }

        @OnData
        public void discharge(final Discharge ignored) {

            downRiver().discharge("test");
        }

        public String onObject(final Object data) {

            return data.toString();
        }

        @OnData
        public Integer parse(final String text) {

            return Integer.parseInt(text);
        }

        @OnData
        public String serialize(final Integer integer) {

            return integer.toString();
        }
    }

    public static class RapidLeapTest4 extends RapidLeapTest1 {

        public MyException error(final Throwable ignored) {

            return new MyException();
        }

        public MyException error(final Integer ignored) {

            return new MyException();
        }

        public Discharge onNull(final Void ignored) {

            return null;
        }

        @Override
        public Integer parse(final String text) {

            return Integer.parseInt(text) + 2;
        }
    }

    public static class RapidLeapTest5 extends RapidLeap<Object> {

    }
}