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
import com.bmd.wtf.gts.OpenGate;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.DataFlow;
import com.bmd.wtf.xtr.rpd.RapidGate.ValidFlows;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static com.bmd.wtf.fll.Waterfall.fall;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for rapid pump objects.
 * <p/>
 * Created by davide on 7/10/14.
 */
public class RapidPumpTest extends TestCase {

    public void testError() {

        try {

            fall().start().distribute(new RapidPumpError1());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(new RapidPumpError2());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(new RapidPumpError3());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(new RapidPumpError4());

            fail();

        } catch (final Exception ignored) {

        }

        final Waterfall<Object, Object, Object> fall1 = fall().start()
                                                              .in(2)
                                                              .distribute(new RapidPumpError5())
                                                              .chain(new OpenGate<Object>() {

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

        fall1.source().push("11", null).flush();

        for (final Object e : collector1.now().all()) {

            assertThat(((RapidException) e).getCause()).isEqualTo(new MyException());
        }

        try {

            fall().start().distribute(new RapidPumpError6());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(new RapidPumpError7());

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidPump.from(new RapidPumpError1()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidPump.from(new RapidPumpError2()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidPump.from(new RapidPumpError3()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidPump.from(new RapidPumpError4()));

            fail();

        } catch (final Exception ignored) {

        }

        final Waterfall<Object, Object, Object> fall2 = fall().start()
                                                              .in(2)
                                                              .distribute(RapidPump.from(
                                                                      new RapidPumpError5()))
                                                              .chain(new OpenGate<Object>() {

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

        fall2.source().push("11", null).flush();

        for (final Object e : collector2.now().all()) {

            assertThat(((RapidException) e).getCause()).isEqualTo(new MyException());
        }

        try {

            fall().start().distribute(RapidPump.from(new RapidPumpError6()));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            fall().start().distribute(RapidPump.from(new RapidPumpError7()));

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testFlow() {

        assertThat(fall().start().in(3).distribute(new RapidPump() {

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

        assertThat(
                fall().start().in(4).distribute(new RapidPumpTest1()).pull("1", 2, 3.0, null).all())
                .containsExactly("1", 2, 3.0, null);
        assertThat(
                fall().start().in(4).distribute(new RapidPumpTest2()).pull("1", 2, 3.0, null).all())
                .containsExactly("1", 2, 3.0, null);
        assertThat(
                fall().start().in(4).distribute(new RapidPumpTest3()).pull("1", 2, 3.0, null).all())
                .containsExactly("1", 2, 3.0, null);

        final Waterfall<Object, Object, Object> fall1 =
                fall().start().in(4).distribute(new RapidPumpTest4()).chain(new OpenGate<Object>() {

                    @Override
                    public void onUnhandled(final River<Object> upRiver,
                            final River<Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector1 = fall1.collect();

        fall1.source().flush("1", 2, 3.0, null);
        assertThat(collector1.all()).containsExactly("1", 2, 3.0, null);

        final Collector<Object> collector2 = fall1.collect();

        fall1.source().forward(new MyException()).flush();
        assertThat(collector2.all()).containsExactly(new MyException(), new MyException(),
                                                     new MyException(), new MyException());

        final Waterfall<Object, Object, Object> fall2 =
                fall().start().in(4).distribute(new RapidPumpTest5()).chain(new OpenGate<Object>() {

                    @Override
                    public void onUnhandled(final River<Object> upRiver,
                            final River<Object> downRiver, final int fallNumber,
                            final Throwable throwable) {

                        downRiver.push(throwable);
                    }
                });
        final Collector<Object> collector3 = fall2.collect();

        fall2.source().flush(new IllegalArgumentException()).flush();
        assertThat(collector3.next()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    public void testWrap() {

        assertThat(fall().start()
                         .in(4)
                         .distribute(RapidPump.from(new RapidPumpTest1()))
                         .pull("1", 2, 3.0, null)
                         .all()).containsExactly("1", 2, 3.0, null);
        assertThat(fall().start()
                         .in(4)
                         .distribute(RapidPump.from(new RapidPumpTest2()))
                         .pull("1", 2, 3.0, null)
                         .all()).containsExactly("1", 2, 3.0, null);
        assertThat(fall().start()
                         .in(4)
                         .distribute(RapidPump.fromAnnotated(new RapidPumpTest3()))
                         .pull("1", 2, 3.0, null)
                         .all()).containsExactly("1", 2, 3.0, null);

        final Waterfall<Object, Object, Object> fall1 = fall().start()
                                                              .in(4)
                                                              .distribute(RapidPump.from(
                                                                      new RapidPumpTest4()))
                                                              .chain(new OpenGate<Object>() {

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

        fall1.source().flush("1", 2, 3.0, null);
        assertThat(collector1.all()).containsExactly("1", 2, 3.0, null);

        final Collector<Object> collector2 = fall1.collect();

        fall1.source().forward(new MyException()).flush();
        assertThat(collector2.all()).containsExactly(new MyException(), new MyException(),
                                                     new MyException(), new MyException());

        final Waterfall<Object, Object, Object> fall2 = fall().start()
                                                              .in(4)
                                                              .distribute(RapidPump.from(
                                                                      new RapidPumpTest5()))
                                                              .chain(new OpenGate<Object>() {

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

        fall2.source().flush(new IllegalArgumentException()).flush();
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

    public static class RapidPumpError1 extends RapidPump {

        @SuppressWarnings("UnusedDeclaration")
        public int method1(final String text) {

            return 0;
        }

        @SuppressWarnings("UnusedDeclaration")
        public int method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidPumpError2 extends RapidPump {

        @DataFlow
        public int method1(final String text) {

            return 0;
        }

        @DataFlow
        public int method2(final String text) {

            return Integer.parseInt(text);
        }
    }

    public static class RapidPumpError3 extends RapidPump {

        @DataFlow
        public int method1(final String text, final int number) {

            return number;
        }
    }

    public static class RapidPumpError4 extends RapidPump {

        @DataFlow
        public String method1(final String text) {

            return text;
        }
    }

    public static class RapidPumpError5 extends RapidPump {

        @SuppressWarnings("UnusedDeclaration")
        public int error(final String text) throws MyException {

            throw new MyException();
        }

        @SuppressWarnings("UnusedDeclaration")
        public int error(final Void ignored) throws MyException {

            throw new MyException();
        }
    }

    public static class RapidPumpError6 extends RapidPump {

        @DataFlow(Integer.class)
        public int method1(final String text) {

            return 0;
        }
    }

    public static class RapidPumpError7 extends RapidPump {

        @DataFlow
        public int method1(final ArrayList<?> list) {

            return 0;
        }

        @DataFlow(ArrayList.class)
        public int method2(final List<?> list) {

            return 0;
        }
    }

    public static class RapidPumpTest1 extends RapidPump {

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

    public static class RapidPumpTest2 extends RapidPumpTest1 {

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

    public static class RapidPumpTest3 extends RapidPump {

        public RapidPumpTest3() {

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

    public static class RapidPumpTest4 extends RapidPumpTest1 {

        @SuppressWarnings("UnusedDeclaration")
        public int onNull(final Void ignored) {

            return 0;
        }

        @Override
        public int onText(final String text) {

            return Integer.parseInt(text) + 2;
        }
    }

    public static class RapidPumpTest5 extends RapidPump {

    }
}