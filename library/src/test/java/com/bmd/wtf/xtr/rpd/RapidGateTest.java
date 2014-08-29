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

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.gts.AbstractGate;
import com.bmd.wtf.gts.OpenGate;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.DataFlow;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static com.bmd.wtf.fll.Waterfall.fall;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for rapid gate objects.
 * <p/>
 * Created by davide on 7/10/14.
 */
public class RapidGateTest extends TestCase {

    public void testBridge() {

        final Waterfall<Object, Object, Object> fall = fall().start(new TestGateBridge());

        assertThat(fall.chain(new RapidGate() {

            @SuppressWarnings("UnusedDeclaration")
            public Object obj(final Object obj) {

                assertThat(
                        Rapid.bridge(fall.bridge(GateBridge.class)).perform().getInt()).isEqualTo(
                        111);
                assertThat(Rapid.bridge(fall.bridge(Classification.ofType(GateBridge.class)))
                                .perform()
                                .getInt()).isEqualTo(111);

                return obj;
            }
        }).pull("test").next()).isEqualTo("test");
    }

    public void testDeviateStream() {

        final Waterfall<Object, Object, Object> fall1 = fall().start();
        final Waterfall<Object, Object, Object> fall2 = fall1.chain(new RapidGate() {

            @SuppressWarnings("UnusedDeclaration")
            public Integer dec(final Integer num) {

                if (num == 0) {

                    isolate();
                }

                return num - 1;
            }
        });
        final Waterfall<Object, Object, String> fall3 =
                fall2.chain(new AbstractGate<Object, String>() {

                    @Override
                    public void onPush(final River<Object> upRiver, final River<String> downRiver,
                            final int fallNumber, final Object drop) {

                        downRiver.push(drop.toString());
                    }
                });
        final Waterfall<Object, String, String> fall4 = fall3.chain();

        assertThat(fall4.pull(1).now().next()).isEqualTo("0");
        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();

        fall1.chain(new RapidGate() {

            @SuppressWarnings("UnusedDeclaration")
            public Integer same(final Integer num) {

                if (num == -1) {

                    dryUp();
                }

                return num;
            }
        }).feed(fall3);

        assertThat(fall4.pull(1).now().next()).isEqualTo("1");
        assertThat(fall4.pull(-1).now().all()).isEmpty();
        assertThat(fall4.pull(0).now().all()).isEmpty();

        fall1.chain().feed(fall3);

        assertThat(fall4.pull(0).now().all()).isEmpty();
        assertThat(fall4.pull(1).now().all()).isEmpty();
    }

    public void testError() {

        try {

            fall().start(new RapidGateError1());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(new RapidGateError2());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(new RapidGateError3());

            fail();

        } catch (final Exception ignored) {

        }

        final Waterfall<Object, Object, Object> fall1 =
                fall().start(new RapidGateError4()).chain(new OpenGate<Object>() {

                    @Override
                    public void onException(final River<Object> upRiver,
                            final River<Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector1 = fall1.collect();

        fall1.source().push("11", null).exception(new IllegalArgumentException()).flush();

        for (final Object e : collector1.now().all()) {

            assertThat(((RapidException) e).getCause()).isEqualTo(new MyException());
        }

        try {

            fall().start(new RapidGateError5());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(new RapidGateError6());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(RapidGate.from(new RapidGateError1()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(RapidGate.from(new RapidGateError2()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(RapidGate.from(new RapidGateError3()));

            fail();

        } catch (final Exception ignored) {

        }

        final Waterfall<Object, Object, Object> fall2 =
                fall().start(RapidGate.from(new RapidGateError4())).chain(new OpenGate<Object>() {

                    @Override
                    public void onException(final River<Object> upRiver,
                            final River<Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector2 = fall2.collect();

        fall2.source().push("11", null).exception(new IllegalArgumentException()).flush();

        for (final Object e : collector2.now().all()) {

            assertThat(((RapidException) e).getCause()).isEqualTo(new MyException());
        }

        try {

            fall().start(RapidGate.from(new RapidGateError5()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start(RapidGate.from(new RapidGateError6()));

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testFlow() {

        assertThat(fall().start(new RapidGate() {

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

                upRiver().push(data.toString());
            }
        }).pull(11, 22f, 33d).all()).containsExactly("11", "22.0", "33.0");

        assertThat(fall().in(3)
                         .start(Rapid.gateGenerator(RapidGateFlow.class))
                         .pull("test")
                         .all()).containsExactly("test", "test", "test");
    }

    public void testInherit() {

        assertThat(fall().start(new RapidGateTest1())
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", "37.1", null);
        assertThat(fall().start(new RapidGateTest2())
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(13, "-27", "37.1", "");
        assertThat(fall().start(new RapidGateTest3())
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", 37.1, null, "test");

        final Waterfall<Object, Object, Object> fall1 =
                fall().start(new RapidGateTest4()).chain(new OpenGate<Object>() {

                    @Override
                    public void onException(final River<Object> upRiver,
                            final River<Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector1 = fall1.collect();

        fall1.source().push("11", 27, 37.1, null);
        assertThat(collector1.all()).containsExactly(13, new MyException(), "37.1");

        final Collector<Object> collector2 = fall1.collect();

        fall1.source().exception(new IllegalArgumentException()).flush();
        assertThat(collector2.all()).containsExactly(new MyException());

        final Collector<Object> collector3 = fall1.collect();

        fall1.source().exception(null);
        assertThat(collector3.all()).isEmpty();

        final Waterfall<Object, Object, Object> fall2 =
                fall().start(new RapidGateTest5()).chain(new OpenGate<Object>() {

                    @Override
                    public void onException(final River<Object> upRiver,
                            final River<Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector4 = fall2.collect();

        fall2.source().exception(new IllegalArgumentException()).flush();
        assertThat(collector4.next()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    public void testWrap() {

        assertThat(
                fall().start(RapidGate.from(new RapidGateTest1())).pull("11", 27, 37.1, null).all())
                .containsExactly(11, "27", "37.1", null);
        assertThat(
                fall().start(RapidGate.from(new RapidGateTest2())).pull("11", 27, 37.1, null).all())
                .containsExactly(13, "-27", "37.1", "");
        assertThat(fall().start(RapidGate.from(new RapidGateTest3())).chain(new OpenGate<Object>() {

            @Override
            public void onException(final River<Object> upRiver, final River<Object> downRiver,
                    final int fallNumber, final Throwable throwable) {

                downRiver.flush();
            }
        }).pull("11", 27, 37.1, null).all()).containsExactly(11, "27", "37.1", null);
        assertThat(fall().start(RapidGate.fromAnnotated(new RapidGateTest3()))
                         .chain(new OpenGate<Object>() {

                             @Override
                             public void onException(final River<Object> upRiver,
                                     final River<Object> downRiver, final int fallNumber,
                                     final Throwable throwable) {

                                 downRiver.flush();
                             }
                         })
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", 37.1, null);
        assertThat(fall().start(RapidGate.fromAnnotated(new RapidGateTest3()))
                         .chain(new OpenGate<Object>() {

                             @Override
                             public void onException(final River<Object> upRiver,
                                     final River<Object> downRiver, final int fallNumber,
                                     final Throwable throwable) {

                                 downRiver.flush();
                             }
                         })
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", 37.1, null);
        assertThat(fall().start(RapidGate.fromAnnotated(new RapidGateTest3()))
                         .chain(new OpenGate<Object>() {

                             @Override
                             public void onException(final River<Object> upRiver,
                                     final River<Object> downRiver, final int fallNumber,
                                     final Throwable throwable) {

                                 downRiver.flush();
                             }
                         })
                         .pull("11", 27, 37.1, null)
                         .all()).containsExactly(11, "27", 37.1, null);

        final Waterfall<Object, Object, Object> fall1 =
                fall().start(RapidGate.from(new RapidGateTest4())).chain(new OpenGate<Object>() {

                    @Override
                    public void onException(final River<Object> upRiver,
                            final River<Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector1 = fall1.collect();

        fall1.source().push("11", 27, 37.1, null);
        assertThat(collector1.all()).containsExactly(13, new MyException(), "37.1");

        final Collector<Object> collector2 = fall1.collect();

        fall1.source().exception(new IllegalArgumentException()).flush();
        assertThat(collector2.all()).containsExactly(new MyException());

        final Collector<Object> collector3 = fall1.collect();

        fall1.source().exception(null);
        assertThat(collector3.all()).isEmpty();

        final Waterfall<Object, Object, Object> fall2 =
                fall().start(RapidGate.from(new RapidGateTest5())).chain(new OpenGate<Object>() {

                    @Override
                    public void onException(final River<Object> upRiver,
                            final River<Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector4 = fall2.collect();

        fall2.source().exception(new IllegalArgumentException()).flush();
        assertThat(collector4.next()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    public interface GateBridge {

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

    public static class RapidGateError1 extends RapidGate {

        @SuppressWarnings("UnusedDeclaration")
        public String method1(final String text) {

            return text;
        }

        @SuppressWarnings("UnusedDeclaration")
        public Integer method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidGateError2 extends RapidGate {

        @DataFlow
        public String method1(final String text) {

            return text;
        }

        @DataFlow
        public Integer method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidGateError3 extends RapidGate {

        @DataFlow
        public String method1(final String text, final int ignored) {

            return text;
        }
    }

    public static class RapidGateError4 extends RapidGate {

        @SuppressWarnings("UnusedDeclaration")
        public void error(final String text) throws MyException {

            throw new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public void error(final Void ignored) throws MyException {

            throw new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public void error(final Flush ignored) throws MyException {

            throw new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public void error(final Throwable ignored) throws MyException {

            throw new MyException();
        }
    }

    public static class RapidGateError5 extends RapidGate {

        @DataFlow(Integer.class)
        public String method1(final String text) {

            return text;
        }
    }

    public static class RapidGateError6 extends RapidGate {

        @DataFlow
        public String method1(final ArrayList<?> list) {

            return list.toString();
        }

        @DataFlow(ArrayList.class)
        public String method2(final List<?> list) {

            return list.toString();
        }
    }

    public static class RapidGateFlow extends RapidGate {

        private final int mNumber;

        public RapidGateFlow(final int number) {

            mNumber = number;
        }

        @SuppressWarnings("UnusedDeclaration")
        public Object obj(final Object obj) {

            assertThat(fallNumber()).isEqualTo(mNumber);

            return obj;
        }
    }

    public static class RapidGateTest1 extends RapidGate {

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

    public static class RapidGateTest2 extends RapidGateTest1 {

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

    public static class RapidGateTest3 extends RapidGate {

        public RapidGateTest3() {

            super(ValidFlows.ANNOTATED_ONLY);
        }

        @DataFlow
        public void flush(final Flush ignored) {

            downRiver().flush("test");
        }

        @SuppressWarnings("UnusedDeclaration")
        public String onObject(final Object data) {

            return data.toString();
        }

        @DataFlow
        public Integer parse(final String text) {

            return Integer.parseInt(text);
        }

        @DataFlow
        public String serialize(final Integer integer) {

            return integer.toString();
        }
    }

    public static class RapidGateTest4 extends RapidGateTest1 {

        @SuppressWarnings("UnusedDeclaration")
        public MyException error(final Throwable ignored) {

            return new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public MyException error(final Integer ignored) {

            return new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Flush onNull(final Void ignored) {

            return null;
        }

        @Override
        public Integer parse(final String text) {

            return Integer.parseInt(text) + 2;
        }
    }

    public static class RapidGateTest5 extends RapidGate {

    }

    public static class TestGateBridge extends OpenGate<Object> implements GateBridge {

        @Override
        public int getInt() {

            return 111;
        }
    }
}