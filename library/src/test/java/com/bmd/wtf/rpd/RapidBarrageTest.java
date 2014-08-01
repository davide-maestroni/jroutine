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
package com.bmd.wtf.rpd;

import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.rpd.RapidAnnotations.DataFlow;
import com.bmd.wtf.rpd.RapidLeap.ValidFlows;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static com.bmd.wtf.fll.Waterfall.fall;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for rapid leap objects.
 * <p/>
 * Created by davide on 7/10/14.
 */
public class RapidBarrageTest extends TestCase {

    public void testError() {

        try {

            fall().start().distribute(new RapidBarrageError1());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(new RapidBarrageError2());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(new RapidBarrageError3());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(new RapidBarrageError4());

            fail();

        } catch (final Exception ignored) {

        }

        final Waterfall<Object, Object, Object> fall1 = fall().start()
                                                              .in(2)
                                                              .distribute(new RapidBarrageError5())
                                                              .chain(new FreeLeap<Object>() {

                                                                  @Override
                                                                  public void onUnhandled(
                                                                          final River<Object> upRiver,
                                                                          final River<Object> downRiver,
                                                                          final int fallNumber,
                                                                          final Throwable throwable) {

                                                                      downRiver.push(throwable);
                                                                  }
                                                              });
        final Collector<Object> collector1 = fall1.collect();

        fall1.source().push("11", null).discharge();

        for (final Object e : collector1.now().all()) {

            assertThat(((RapidException) e).getCause()).isEqualTo(new MyException());
        }

        try {

            fall().start().distribute(new RapidBarrageError6());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(new RapidBarrageError7());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidBarrage.from(new RapidBarrageError1()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidBarrage.from(new RapidBarrageError2()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidBarrage.from(new RapidBarrageError3()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidBarrage.from(new RapidBarrageError4()));

            fail();

        } catch (final Exception ignored) {

        }

        final Waterfall<Object, Object, Object> fall2 = fall().start()
                                                              .in(2)
                                                              .distribute(RapidBarrage.from(
                                                                      new RapidBarrageError5()))
                                                              .chain(new FreeLeap<Object>() {

                                                                  @Override
                                                                  public void onUnhandled(
                                                                          final River<Object> upRiver,
                                                                          final River<Object> downRiver,
                                                                          final int fallNumber,
                                                                          final Throwable throwable) {

                                                                      downRiver.push(throwable);
                                                                  }
                                                              });
        final Collector<Object> collector2 = fall2.collect();

        fall2.source().push("11", null).discharge();

        for (final Object e : collector2.now().all()) {

            assertThat(((RapidException) e).getCause()).isEqualTo(new MyException());
        }

        try {

            fall().start().distribute(RapidBarrage.from(new RapidBarrageError6()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidBarrage.from(new RapidBarrageError7()));

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testFlow() {

        assertThat(fall().start().in(3).distribute(new RapidBarrage() {

            @SuppressWarnings("UnusedDeclaration")
            public int onShort(final Short data) {

                return data;
            }

            @SuppressWarnings("UnusedDeclaration")
            public int onInteger(final Integer data) {

                return DEFAULT_STREAM;
            }

            @SuppressWarnings("UnusedDeclaration")
            public int onFloat(final Float data) {

                return ALL_STREAMS;
            }

            @SuppressWarnings("UnusedDeclaration")
            public int onDouble(final Double data) {

                return NO_STREAM;
            }
        }).pull(11, 22f, 33d, (short) 2).all()).containsExactly(11, 22f, 22f, 22f, (short) 2);
    }

    public void testInherit() {

        assertThat(fall().start()
                         .in(4)
                         .distribute(new RapidBarrageTest1())
                         .pull("1", 2, 3.0, null)
                         .all()).containsExactly("1", 2, 3.0, null);
        assertThat(fall().start()
                         .in(4)
                         .distribute(new RapidBarrageTest2())
                         .pull("1", 2, 3.0, null)
                         .all()).containsExactly("1", 2, 3.0, null);
        assertThat(fall().start()
                         .in(4)
                         .distribute(new RapidBarrageTest3())
                         .pull("1", 2, 3.0, null)
                         .all()).containsExactly("1", 2, 3.0, null);

        final Waterfall<Object, Object, Object> fall1 = fall().start()
                                                              .in(4)
                                                              .distribute(new RapidBarrageTest4())
                                                              .chain(new FreeLeap<Object>() {

                                                                  @Override
                                                                  public void onUnhandled(
                                                                          final River<Object> upRiver,
                                                                          final River<Object> downRiver,
                                                                          final int fallNumber,
                                                                          final Throwable throwable) {

                                                                      downRiver.push(throwable);
                                                                  }
                                                              });
        final Collector<Object> collector1 = fall1.collect();

        fall1.source().discharge("1", 2, 3.0, null);
        assertThat(collector1.all()).containsExactly("1", 2, 3.0, null);

        final Collector<Object> collector2 = fall1.collect();

        fall1.source().forward(new MyException()).discharge();
        assertThat(collector2.all()).containsExactly(new MyException(), new MyException(),
                                                     new MyException(), new MyException());

        final Waterfall<Object, Object, Object> fall2 = fall().start()
                                                              .in(4)
                                                              .distribute(new RapidBarrageTest5())
                                                              .chain(new FreeLeap<Object>() {

                                                                  @Override
                                                                  public void onUnhandled(
                                                                          final River<Object> upRiver,
                                                                          final River<Object> downRiver,
                                                                          final int fallNumber,
                                                                          final Throwable throwable) {

                                                                      downRiver.push(throwable);
                                                                  }
                                                              });
        final Collector<Object> collector3 = fall2.collect();

        fall2.source().discharge(new IllegalArgumentException()).discharge();
        assertThat(collector3.next()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    public void testWrap() {

        assertThat(fall().start()
                         .in(4)
                         .distribute(RapidBarrage.from(new RapidBarrageTest1()))
                         .pull("1", 2, 3.0, null)
                         .all()).containsExactly("1", 2, 3.0, null);
        assertThat(fall().start()
                         .in(4)
                         .distribute(RapidBarrage.from(new RapidBarrageTest2()))
                         .pull("1", 2, 3.0, null)
                         .all()).containsExactly("1", 2, 3.0, null);
        assertThat(fall().start()
                         .in(4)
                         .distribute(RapidBarrage.fromAnnotated(new RapidBarrageTest3()))
                         .pull("1", 2, 3.0, null)
                         .all()).containsExactly("1", 2, 3.0, null);

        final Waterfall<Object, Object, Object> fall1 = fall().start()
                                                              .in(4)
                                                              .distribute(RapidBarrage.from(
                                                                      new RapidBarrageTest4()))
                                                              .chain(new FreeLeap<Object>() {

                                                                  @Override
                                                                  public void onUnhandled(
                                                                          final River<Object> upRiver,
                                                                          final River<Object> downRiver,
                                                                          final int fallNumber,
                                                                          final Throwable throwable) {

                                                                      downRiver.push(throwable);
                                                                  }
                                                              });
        final Collector<Object> collector1 = fall1.collect();

        fall1.source().discharge("1", 2, 3.0, null);
        assertThat(collector1.all()).containsExactly("1", 2, 3.0, null);

        final Collector<Object> collector2 = fall1.collect();

        fall1.source().forward(new MyException()).discharge();
        assertThat(collector2.all()).containsExactly(new MyException(), new MyException(),
                                                     new MyException(), new MyException());

        final Waterfall<Object, Object, Object> fall2 = fall().start()
                                                              .in(4)
                                                              .distribute(RapidBarrage.from(
                                                                      new RapidBarrageTest5()))
                                                              .chain(new FreeLeap<Object>() {

                                                                  @Override
                                                                  public void onUnhandled(
                                                                          final River<Object> upRiver,
                                                                          final River<Object> downRiver,
                                                                          final int fallNumber,
                                                                          final Throwable throwable) {

                                                                      downRiver.push(throwable);
                                                                  }
                                                              });
        final Collector<Object> collector3 = fall2.collect();

        fall2.source().discharge(new IllegalArgumentException()).discharge();
        assertThat(collector3.next()).isExactlyInstanceOf(IllegalArgumentException.class);
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

    public static class RapidBarrageError1 extends RapidBarrage {

        @SuppressWarnings("UnusedDeclaration")
        public int method1(final String text) {

            return 0;
        }

        @SuppressWarnings("UnusedDeclaration")
        public int method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidBarrageError2 extends RapidBarrage {

        @DataFlow
        public int method1(final String text) {

            return 0;
        }

        @DataFlow
        public int method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidBarrageError3 extends RapidBarrage {

        @DataFlow
        public int method1(final String text, final int number) {

            return number;
        }
    }

    public static class RapidBarrageError4 extends RapidBarrage {

        @DataFlow
        public String method1(final String text) {

            return text;
        }
    }

    public static class RapidBarrageError5 extends RapidBarrage {

        @SuppressWarnings("UnusedDeclaration")
        public int error(final String text) throws MyException {

            throw new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public int error(final Void ignored) throws MyException {

            throw new MyException();
        }
    }

    public static class RapidBarrageError6 extends RapidBarrage {

        @DataFlow(Integer.class)
        public int method1(final String text) {

            return 0;
        }
    }

    public static class RapidBarrageError7 extends RapidBarrage {

        @DataFlow
        public int method1(final ArrayList<?> list) {

            return 0;
        }

        @DataFlow(ArrayList.class)
        public int method2(final List<?> list) {

            return 0;
        }
    }

    public static class RapidBarrageTest1 extends RapidBarrage {

        @SuppressWarnings("UnusedDeclaration")
        public int onInt(final Integer integer) {

            return integer;
        }

        @SuppressWarnings("UnusedDeclaration")
        public int onNumber(final Number data) {

            return data.intValue();
        }

        @SuppressWarnings("UnusedDeclaration")
        public int onText(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidBarrageTest2 extends RapidBarrageTest1 {

        @SuppressWarnings("UnusedDeclaration")
        public int minusInt(final Integer integer) {

            return integer - 1;
        }

        @SuppressWarnings("UnusedDeclaration")
        public int onNull(final Void data) {

            return 0;
        }

        @Override
        public int onText(final String text) {

            return Integer.parseInt(text) + 2;
        }
    }

    public static class RapidBarrageTest3 extends RapidBarrage {

        public RapidBarrageTest3() {

            super(ValidFlows.ANNOTATED_ONLY);
        }

        @DataFlow
        public int onInt(final Integer integer) {

            return integer;
        }

        @SuppressWarnings("UnusedDeclaration")
        public int onObject(final Object data) {

            return onText(data.toString());
        }

        @DataFlow
        public int onText(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidBarrageTest4 extends RapidBarrageTest1 {

        @SuppressWarnings("UnusedDeclaration")
        public int onNull(final Void ignored) {

            return 0;
        }

        @Override
        public int onText(final String text) {

            return Integer.parseInt(text) + 2;
        }
    }

    public static class RapidBarrageTest5 extends RapidBarrage {

    }
}