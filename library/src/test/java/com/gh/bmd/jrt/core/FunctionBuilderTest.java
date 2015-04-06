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
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.invocation.Invocations.Function0;
import com.gh.bmd.jrt.invocation.Invocations.Function1;
import com.gh.bmd.jrt.invocation.Invocations.Function2;
import com.gh.bmd.jrt.invocation.Invocations.Function3;
import com.gh.bmd.jrt.invocation.Invocations.Function4;
import com.gh.bmd.jrt.invocation.Invocations.FunctionN;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.builder.RoutineConfiguration.withInputOrder;
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
        assertThat(JRoutine.onFunction(function0).callAsync().eventually().readNext()).isEqualTo(
                "test0");
    }

    @Test
    public void testFunction1() {

        final Function1<String, String> function1 = new Function1<String, String>() {

            public String call(final String param1) {

                return param1;
            }
        };
        assertThat(JRoutine.onFunction(function1)
                           .callAsync("test1")
                           .eventually()
                           .readNext()).isEqualTo("test1");
    }

    @Test
    public void testFunction2() {

        final Function2<String, String, String> function2 =
                new Function2<String, String, String>() {

                    public String call(final String param1, final String param2) {

                        return param1 + " " + param2;
                    }
                };
        assertThat(JRoutine.onFunction(function2)
                           .callAsync("test1", "test2")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2");
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
        assertThat(JRoutine.onFunction(function3)
                           .callAsync("test1", "test2", "test3")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2 test3");
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
        assertThat(JRoutine.onFunction(function4)
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2 test3 test4");
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
        assertThat(JRoutine.onFunction(function4).withConfiguration(withInputOrder(OrderType.NONE))
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
        assertThat(JRoutine.onFunction(functionN)
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
        assertThat(JRoutine.onFunction(function1)
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
        assertThat(JRoutine.onFunction(function1).callSync("test0").readNext()).isEqualTo("test0");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction0Error() {

        try {

            JRoutine.onFunction((Function0<Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction1Error() {

        try {

            JRoutine.onFunction((Function1<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction2Error() {

        try {

            JRoutine.onFunction((Function2<Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction3Error() {

        try {

            JRoutine.onFunction((Function3<Object, Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunction4Error() {

        try {

            JRoutine.onFunction((Function4<Object, Object, Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullFunctionNError() {

        try {

            JRoutine.onFunction((FunctionN<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullProcedure0Error() {

        try {

            JRoutine.onProcedure((Function0<Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullProcedure1Error() {

        try {

            JRoutine.onProcedure((Function1<Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullProcedure2Error() {

        try {

            JRoutine.onProcedure((Function2<Object, Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullProcedure3Error() {

        try {

            JRoutine.onProcedure((Function3<Object, Object, Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullProcedure4Error() {

        try {

            JRoutine.onProcedure((Function4<Object, Object, Object, Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullProcedureNError() {

        try {

            JRoutine.onProcedure((FunctionN<Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testProcedure0() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final Function0<Void> procedure0 = new Function0<Void>() {

            public Void call() {

                semaphore.release();
                return null;
            }
        };
        assertThat(JRoutine.onProcedure(procedure0).callAsync().eventually().readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testProcedure1() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final Function1<String, Void> procedure1 = new Function1<String, Void>() {

            public Void call(final String param1) {

                semaphore.release();
                return null;
            }
        };
        assertThat(JRoutine.onProcedure(procedure1)
                           .callAsync("test1")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testProcedure2() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final Function2<String, String, Void> procedure2 = new Function2<String, String, Void>() {

            public Void call(final String param1, final String param2) {

                semaphore.release();
                return null;
            }
        };
        assertThat(
                JRoutine.onProcedure(procedure2).callAsync("test1", "test2").eventually().readAll())
                .isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testProcedure3() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final Function3<String, String, String, Void> procedure3 =
                new Function3<String, String, String, Void>() {

                    public Void call(final String param1, final String param2,
                            final String param3) {

                        semaphore.release();
                        return null;
                    }
                };
        assertThat(JRoutine.onProcedure(procedure3)
                           .callAsync("test1", "test2", "test3")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testProcedure4() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final Function4<String, String, String, String, Void> procedure4 =
                new Function4<String, String, String, String, Void>() {

                    public Void call(final String param1, final String param2, final String param3,
                            final String param4) {

                        semaphore.release();
                        return null;
                    }
                };
        assertThat(JRoutine.onProcedure(procedure4)
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testProcedureN() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final FunctionN<String, Void> procedureN = new FunctionN<String, Void>() {

            public Void call(@Nonnull final List<? extends String> strings) {

                semaphore.release();
                return null;
            }
        };
        assertThat(JRoutine.onProcedure(procedureN)
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testProcedureParallel() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final Function1<String, Void> procedure1 = new Function1<String, Void>() {

            public Void call(final String param1) {

                semaphore.release();
                return null;
            }
        };
        assertThat(JRoutine.onProcedure(procedure1)
                           .callParallel("test0", "test1", "test2")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testProcedureSync() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final Function1<String, Void> procedure1 = new Function1<String, Void>() {

            public Void call(final String param1) {

                semaphore.release();
                return null;
            }
        };
        assertThat(JRoutine.onProcedure(procedure1)
                           .callSync("test0")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testWrongParamFunction0Error() {

        try {

            JRoutine.onFunction(new Function0<Object>() {

                public Object call() {

                    return null;
                }
            }).callAsync((Void) null).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testWrongParamFunction1Error() {

        try {

            JRoutine.onFunction(new Function1<Object, Object>() {

                public Object call(final Object param1) {

                    return null;
                }
            }).callAsync().eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.onFunction(new Function1<Object, Object>() {

                public Object call(final Object param1) {

                    return null;
                }
            }).callAsync(1, 2).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testWrongParamFunction2Error() {

        try {

            JRoutine.onFunction(new Function2<Object, Object, Object>() {

                public Object call(final Object param1, final Object param2) {

                    return null;
                }
            }).callAsync(1).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.onFunction(new Function2<Object, Object, Object>() {

                public Object call(final Object param1, final Object param2) {

                    return null;
                }
            }).callAsync(1, 2, 3).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testWrongParamFunction3Error() {

        try {

            JRoutine.onFunction(new Function3<Object, Object, Object, Object>() {

                public Object call(final Object param1, final Object param2, final Object param3) {

                    return null;
                }
            }).callAsync(1, 2).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.onFunction(new Function3<Object, Object, Object, Object>() {

                public Object call(final Object param1, final Object param2, final Object param3) {

                    return null;
                }
            }).callAsync(1, 2, 3, 4).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testWrongParamFunction4Error() {

        try {

            JRoutine.onFunction(new Function4<Object, Object, Object, Object, Object>() {

                public Object call(final Object param1, final Object param2, final Object param3,
                        final Object param4) {

                    return null;
                }
            }).callAsync(1, 2, 3).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.onFunction(new Function4<Object, Object, Object, Object, Object>() {

                public Object call(final Object param1, final Object param2, final Object param3,
                        final Object param4) {

                    return null;
                }
            }).callAsync(1, 2, 3, 4, 5).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testWrongParamProcedure0Error() {

        try {

            JRoutine.onProcedure(new Function0<Void>() {

                public Void call() {

                    return null;
                }
            }).callAsync((Void) null).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testWrongParamProcedure1Error() {

        try {

            JRoutine.onProcedure(new Function1<Object, Void>() {

                public Void call(final Object param1) {

                    return null;
                }
            }).callAsync().eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.onProcedure(new Function1<Object, Void>() {

                public Void call(final Object param1) {

                    return null;
                }
            }).callAsync(1, 2).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testWrongParamProcedure2Error() {

        try {

            JRoutine.onProcedure(new Function2<Object, Object, Void>() {

                public Void call(final Object param1, final Object param2) {

                    return null;
                }
            }).callAsync(1).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.onProcedure(new Function2<Object, Object, Void>() {

                public Void call(final Object param1, final Object param2) {

                    return null;
                }
            }).callAsync(1, 2, 3).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testWrongParamProcedure3Error() {

        try {

            JRoutine.onProcedure(new Function3<Object, Object, Object, Void>() {

                public Void call(final Object param1, final Object param2, final Object param3) {

                    return null;
                }
            }).callAsync(1, 2).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.onProcedure(new Function3<Object, Object, Object, Void>() {

                public Void call(final Object param1, final Object param2, final Object param3) {

                    return null;
                }
            }).callAsync(1, 2, 3, 4).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testWrongParamProcedure4Error() {

        try {

            JRoutine.onProcedure(new Function4<Object, Object, Object, Object, Void>() {

                public Void call(final Object param1, final Object param2, final Object param3,
                        final Object param4) {

                    return null;
                }
            }).callAsync(1, 2, 3).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.onProcedure(new Function4<Object, Object, Object, Object, Void>() {

                public Void call(final Object param1, final Object param2, final Object param3,
                        final Object param4) {

                    return null;
                }
            }).callAsync(1, 2, 3, 4, 5).eventually().readAll();

            fail();

        } catch (final InvocationException ignored) {

        }
    }
}
