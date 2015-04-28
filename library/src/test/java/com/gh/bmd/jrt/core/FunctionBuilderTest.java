/*
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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.invocation.Invocations.Function0;
import com.gh.bmd.jrt.invocation.Invocations.Function1;
import com.gh.bmd.jrt.invocation.Invocations.Function2;
import com.gh.bmd.jrt.invocation.Invocations.Function3;
import com.gh.bmd.jrt.invocation.Invocations.Function4;
import com.gh.bmd.jrt.invocation.Invocations.FunctionN;

import org.junit.Test;

import java.util.List;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Function routine builder unit tests.
 * <p/>
 * Created by davide on 3/27/15.
 */
public class FunctionBuilderTest {

    @Test
    public void testFunction0() {

        final Function0<String> function0 = new Function0<String>() {

            public String call() {

                return "test0";
            }
        };
        assertThat(JRoutine.on(function0).callAsync().eventually().readNext()).isEqualTo("test0");
        assertThat(JRoutine.on(function0)
                           .callAsync(null, null, null)
                           .eventually()
                           .readNext()).isEqualTo("test0");
    }

    @Test
    public void testFunction1() {

        final Function1<String, String> function1 = new Function1<String, String>() {

            public String call(final String param1) {

                return param1;
            }
        };
        assertThat(JRoutine.on(function1).callAsync("test1").eventually().readNext()).isEqualTo(
                "test1");
        assertThat(JRoutine.on(function1)
                           .callAsync("test1", "test2", "test3")
                           .eventually()
                           .readAll()).containsOnly("test1", "test2", "test3");
    }

    @Test
    public void testFunction2() {

        final Function2<String, String, String> function2 =
                new Function2<String, String, String>() {

                    public String call(final String param1, final String param2) {

                        return param1 + " " + param2;
                    }
                };
        assertThat(JRoutine.on(function2)
                           .callAsync("test1", "test2")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2");
        assertThat(JRoutine.on(function2)
                           .callAsync("test1", "test2", "test3")
                           .eventually()
                           .readAll()).containsOnly("test1 test2", "test3 null");
    }

    @Test
    public void testFunction3() {

        final Function3<String, String, String, String> function3 =
                new Function3<String, String, String, String>() {

                    public String call(final String param1, final String param2,
                            final String param3) {

                        return param1 + " " + param2 + " " + param3;
                    }
                };
        assertThat(
                JRoutine.on(function3).callAsync("test1", "test2", "test3").eventually().readNext())
                .isEqualTo("test1 test2 test3");
        assertThat(JRoutine.on(function3)
                           .callAsync("test1", "test2", "test3", "test4", "test5")
                           .eventually()
                           .readAll()).containsOnly("test1 test2 test3", "test4 test5 null");
        assertThat(JRoutine.on(function3)
                           .callAsync("test1", "test2", "test3", "test4", "test5", "test6")
                           .eventually()
                           .readAll()).containsOnly("test1 test2 test3", "test4 test5 test6");
    }

    @Test
    public void testFunction4() {

        final Function4<String, String, String, String, String> function4 =
                new Function4<String, String, String, String, String>() {

                    public String call(final String param1, final String param2,
                            final String param3, final String param4) {

                        return param1 + " " + param2 + " " + param3 + " " + param4;
                    }
                };
        assertThat(JRoutine.on(function4)
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2 test3 test4");
        assertThat(JRoutine.on(function4)
                           .callAsync("test1", "test2", "test3", "test4", "test5", "test6")
                           .eventually()
                           .readAll()).containsOnly("test1 test2 test3 test4",
                                                    "test5 test6 null null");
    }

    @Test
    public void testFunctionConfiguration() {

        final Function4<String, String, String, String, String> function4 =
                new Function4<String, String, String, String, String>() {

                    public String call(final String param1, final String param2,
                            final String param3, final String param4) {

                        return param1 + " " + param2 + " " + param3 + " " + param4;
                    }
                };
        assertThat(JRoutine.on(function4)
                           .routineConfiguration()
                           .withInputOrder(OrderType.NONE)
                           .applied()
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2 test3 test4");
    }

    @Test
    public void testFunctionN() {

        final FunctionN<String, String> functionN = new FunctionN<String, String>() {

            public String call(@Nonnull final List<? extends String> strings) {

                final StringBuilder builder = new StringBuilder();

                for (final String string : strings) {

                    builder.append(string);
                }

                return builder.toString();
            }
        };
        assertThat(JRoutine.on(functionN)
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readNext()).isEqualTo("test1test2test3test4");
    }

    @Test
    public void testFunctionParallel() {

        final Function1<String, String> function1 = new Function1<String, String>() {

            public String call(final String param1) {

                return param1;
            }
        };
        assertThat(JRoutine.on(function1)
                           .callParallel("test1", "test2", "test3")
                           .eventually()
                           .readAll()).containsOnly("test1", "test2", "test3");
    }

    @Test
    public void testFunctionSync() {

        final Function1<String, String> function1 = new Function1<String, String>() {

            public String call(final String param1) {

                return param1;
            }
        };
        assertThat(JRoutine.on(function1).callSync("test0", "test1").readAll()).containsExactly(
                "test0", "test1");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction0Error() {

        try {

            JRoutine.on((Function0<Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction1Error() {

        try {

            JRoutine.on((Function1<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction2Error() {

        try {

            JRoutine.on((Function2<Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction3Error() {

        try {

            JRoutine.on((Function3<Object, Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction4Error() {

        try {

            JRoutine.on((Function4<Object, Object, Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunctionNError() {

        try {

            JRoutine.on((FunctionN<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }
}
