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

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.AbstractLeap;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.rpd.RapidAnnotations.FlowPath;

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
public class RapidLeapTest extends TestCase {

    public void testDeviateStream() {

        final Waterfall<Object, Object, Object> fall1 = fall().start();
        final Waterfall<Object, Object, Object> fall2 = fall1.chain(new RapidLeap<Object>() {

            @SuppressWarnings("UnusedDeclaration")
            public Integer dec(final Integer num) {

                if (num == 0) {

                    isolate();
                }

                return num - 1;
            }
        });
        final Waterfall<Object, Object, String> fall3 =
                fall2.chain(new AbstractLeap<Object, Object, String>() {

                    @Override
                    public void onPush(final River<Object, Object> upRiver,
                            final River<Object, String> downRiver, final int fallNumber,
                            final Object drop) {

                        downRiver.push(drop.toString());
                    }
                });
        final Waterfall<Object, String, String> fall4 = fall3.chain();

        assertThat(fall4.pull(1).now().next()).isEqualTo("0");
        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();

        fall1.chain(new RapidLeap<Object>() {

            @SuppressWarnings("UnusedDeclaration")
            public Integer same(final Integer num) {

                if (num == -1) {

                    dryUp();
                }

                return num;
            }
        }).chain(fall3);

        assertThat(fall4.pull(1).now().next()).isEqualTo("1");
        assertThat(fall4.pull(-1).now().all()).isEmpty();
        assertThat(fall4.pull(0).now().all()).isEmpty();

        fall1.chain().chain(fall3);

        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();
    }

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

        try {

            fall().start(new RapidLeapError5());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(new RapidLeapError6());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(RapidLeap.from(new RapidLeapError1()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(RapidLeap.from(new RapidLeapError2()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(RapidLeap.from(new RapidLeapError3()));

            fail();

        } catch (final Exception ignored) {

        }

        final Waterfall<Object, Object, Object> fall2 =
                fall().start(RapidLeap.from(new RapidLeapError4()))
                      .chain(new FreeLeap<Object, Object>() {

                          @Override
                          public void onUnhandled(final River<Object, Object> upRiver,
                                  final River<Object, Object> downRiver, final int fallNumber,
                                  final Throwable throwable) {

                              downRiver.push(throwable);
                          }
                      });
        final Collector<Object> collector2 = fall2.collect();

        fall2.source().push("11", null).forward(new IllegalArgumentException()).discharge();

        for (final Object e : collector2.now().all()) {

            assertThat(((RapidException) e).getCause()).isEqualTo(new MyException());
        }

        try {

            fall().start(RapidLeap.from(new RapidLeapError5()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(RapidLeap.from(new RapidLeapError6()));

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testFlow() {

        assertThat(fall().start(new RapidLeap<Object>() {

            @SuppressWarnings("UnusedDeclaration")
            public void integerToString(final Integer data) {

                downRiver().push(data.toString());
            }

            @SuppressWarnings("UnusedDeclaration")
            public void floatToString(final Float data) {

                upRiver().push(data.toString());
            }

            @SuppressWarnings("UnusedDeclaration")
            public void doubleToStirng(final Double data) {

                source().push(data.toString());
            }
        }).pull(11, 22f, 33d).all()).containsExactly("11", "22.0", "33.0");

        assertThat(fall().in(3)
                         .start(Rapid.leapGenerator(RapidLeapFlow.class))
                         .pull("test")
                         .all()).containsExactly("test", "test", "test");
    }

    public void testGate() {

        assertThat(fall().asGate().start(new TestLeapGate()).chain(new RapidLeap<Object>() {

            @SuppressWarnings("UnusedDeclaration")
            public Object obj(final Object obj) {

                assertThat(on(LeapGate.class).perform().getInt()).isEqualTo(111);
                assertThat(on(Classification.ofType(LeapGate.class)).perform().getInt()).isEqualTo(
                        111);

                return obj;
            }
        }).pull("test").next()).isEqualTo("test");
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

    public void testWrap() {

        assertThat(fall().start(RapidLeap.from(new RapidLeapTest1(), Object.class))
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", "37.1", null);
        assertThat(fall().start(
                RapidLeap.from(new RapidLeapTest2(), Classification.ofType(Object.class)))
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(13, "-27", "37.1", "");
        assertThat(fall().start(RapidLeap.from(new RapidLeapTest3()))
                         .chain(new FreeLeap<Object, Object>() {

                             @Override
                             public void onUnhandled(final River<Object, Object> upRiver,
                                     final River<Object, Object> downRiver, final int fallNumber,
                                     final Throwable throwable) {

                                 downRiver.discharge();
                             }
                         })
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", "37.1", null);
        assertThat(fall().start(RapidLeap.fromAnnotated(new RapidLeapTest3()))
                         .chain(new FreeLeap<Object, Object>() {

                             @Override
                             public void onUnhandled(final River<Object, Object> upRiver,
                                     final River<Object, Object> downRiver, final int fallNumber,
                                     final Throwable throwable) {

                                 downRiver.discharge();
                             }
                         })
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", 37.1, null);
        assertThat(fall().start(RapidLeap.fromAnnotated(new RapidLeapTest3(), Object.class))
                         .chain(new FreeLeap<Object, Object>() {

                             @Override
                             public void onUnhandled(final River<Object, Object> upRiver,
                                     final River<Object, Object> downRiver, final int fallNumber,
                                     final Throwable throwable) {

                                 downRiver.discharge();
                             }
                         })
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", 37.1, null);
        assertThat(fall().start(
                RapidLeap.fromAnnotated(new RapidLeapTest3(), Classification.ofType(Object.class)))
                         .chain(new FreeLeap<Object, Object>() {

                             @Override
                             public void onUnhandled(final River<Object, Object> upRiver,
                                     final River<Object, Object> downRiver, final int fallNumber,
                                     final Throwable throwable) {

                                 downRiver.discharge();
                             }
                         })
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", 37.1, null);

        final Waterfall<Object, Object, Object> fall1 =
                fall().start(RapidLeap.from(new RapidLeapTest4()))
                      .chain(new FreeLeap<Object, Object>() {

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
                fall().start(RapidLeap.from(new RapidLeapTest5()))
                      .chain(new FreeLeap<Object, Object>() {

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

    public interface LeapGate {

        public int getInt();
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

        @SuppressWarnings("UnusedDeclaration")
        public String method1(final String text) {

            return text;
        }

        @SuppressWarnings("UnusedDeclaration")
        public Integer method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidLeapError2 extends RapidLeap<Object> {

        @FlowPath
        public String method1(final String text) {

            return text;
        }

        @FlowPath
        public Integer method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidLeapError3 extends RapidLeap<Object> {

        @FlowPath
        public String method1(final String text, final int ignored) {

            return text;
        }
    }

    public static class RapidLeapError4 extends RapidLeap<Object> {

        @SuppressWarnings("UnusedDeclaration")
        public void error(final String text) throws MyException {

            throw new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public void error(final Void ignored) throws MyException {

            throw new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public void error(final Discharge ignored) throws MyException {

            throw new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public void error(final Throwable ignored) throws MyException {

            throw new MyException();
        }
    }

    public static class RapidLeapError5 extends RapidLeap<Object> {

        @FlowPath(Integer.class)
        public String method1(final String text) {

            return text;
        }
    }

    public static class RapidLeapError6 extends RapidLeap<Object> {

        @FlowPath
        public String method1(final ArrayList<?> list) {

            return list.toString();
        }

        @FlowPath(ArrayList.class)
        public String method2(final List<?> list) {

            return list.toString();
        }
    }

    public static class RapidLeapFlow extends RapidLeap<Object> {

        private final int mNumber;

        public RapidLeapFlow(final int number) {

            mNumber = number;
        }

        @SuppressWarnings("UnusedDeclaration")
        public Object obj(final Object obj) {

            assertThat(fallNumber()).isEqualTo(mNumber);

            return obj;
        }
    }

    public static class RapidLeapTest1 extends RapidLeap<Object> {

        @SuppressWarnings("UnusedDeclaration")
        public String onObject(final Object data) {

            return data.toString();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Integer parse(final String text) {

            return Integer.parseInt(text);
        }

        @SuppressWarnings("UnusedDeclaration")
        public String serialize(final Integer integer) {

            return integer.toString();
        }
    }

    public static class RapidLeapTest2 extends RapidLeapTest1 {

        @SuppressWarnings("UnusedDeclaration")
        public String minusSerialize(final Integer integer) {

            return "-" + integer.toString();
        }

        @SuppressWarnings("UnusedDeclaration")
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

            super(ValidPaths.ANNOTATED_ONLY);
        }

        @FlowPath
        public void discharge(final Discharge ignored) {

            downRiver().discharge("test");
        }

        @SuppressWarnings("UnusedDeclaration")
        public String onObject(final Object data) {

            return data.toString();
        }

        @FlowPath
        public Integer parse(final String text) {

            return Integer.parseInt(text);
        }

        @FlowPath
        public String serialize(final Integer integer) {

            return integer.toString();
        }
    }

    public static class RapidLeapTest4 extends RapidLeapTest1 {

        @SuppressWarnings("UnusedDeclaration")
        public MyException error(final Throwable ignored) {

            return new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public MyException error(final Integer ignored) {

            return new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
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

    public static class TestLeapGate extends FreeLeap<Object, Object> implements LeapGate {

        @Override
        public int getInt() {

            return 111;
        }
    }
}