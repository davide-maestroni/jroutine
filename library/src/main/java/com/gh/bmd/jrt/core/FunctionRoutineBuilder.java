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

import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.Invocations.Function0;
import com.gh.bmd.jrt.invocation.Invocations.Function1;
import com.gh.bmd.jrt.invocation.Invocations.Function2;
import com.gh.bmd.jrt.invocation.Invocations.Function3;
import com.gh.bmd.jrt.invocation.Invocations.Function4;
import com.gh.bmd.jrt.invocation.Invocations.FunctionN;
import com.gh.bmd.jrt.invocation.SingleCallInvocation;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Class implementing a builder of routine objects based on function or procedure instances.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class FunctionRoutineBuilder<INPUT, OUTPUT> extends DefaultRoutineBuilder<INPUT, OUTPUT> {

    /**
     * Constructor.
     *
     * @param factory the invocation factory.
     * @throws java.lang.NullPointerException if the factory is null.
     */
    private FunctionRoutineBuilder(@Nonnull final InvocationFactory<INPUT, OUTPUT> factory) {

        super(factory);
        routineConfiguration().withInputOrder(OrderType.PASSING_ORDER).applied();
    }

    /**
     * Returns a new builder based on the specified function.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <OUTPUT> FunctionRoutineBuilder<Void, OUTPUT> fromFunction(
            @Nonnull final Function0<? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<Void, OUTPUT>(new InvocationFactory<Void, OUTPUT>() {

            @Nonnull
            public Invocation<Void, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<Void, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends Void> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

                        if (inputs.size() != 0) {

                            throw new IllegalArgumentException(
                                    "[" + function.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 0");
                        }

                        result.pass(function.call());
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified function.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, OUTPUT> FunctionRoutineBuilder<INPUT, OUTPUT> fromFunction(
            @Nonnull final Function1<INPUT1, ? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

                        if (inputs.size() != 1) {

                            throw new IllegalArgumentException(
                                    "[" + function.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 1");
                        }

                        result.pass(function.call((INPUT1) inputs.get(0)));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified function.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, OUTPUT>
    FunctionRoutineBuilder<INPUT, OUTPUT> fromFunction(
            @Nonnull final Function2<INPUT1, INPUT2, ? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

                        if (inputs.size() != 2) {

                            throw new IllegalArgumentException(
                                    "[" + function.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 2");
                        }

                        result.pass(function.call((INPUT1) inputs.get(0), (INPUT2) inputs.get(1)));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified function.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <INPUT3> the third parameter type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT, OUTPUT>
    FunctionRoutineBuilder<INPUT, OUTPUT> fromFunction(
            @Nonnull final Function3<INPUT1, INPUT2, INPUT3, ? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

                        if (inputs.size() != 3) {

                            throw new IllegalArgumentException(
                                    "[" + function.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 3");
                        }

                        result.pass(function.call((INPUT1) inputs.get(0), (INPUT2) inputs.get(1),
                                                  (INPUT3) inputs.get(2)));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified function.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <INPUT3> the third parameter type.
     * @param <INPUT4> the fourth parameter type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT, INPUT4
            extends INPUT, OUTPUT> FunctionRoutineBuilder<INPUT, OUTPUT> fromFunction(
            @Nonnull final Function4<INPUT1, INPUT2, INPUT3, INPUT4, ? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

                        if (inputs.size() != 4) {

                            throw new IllegalArgumentException(
                                    "[" + function.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 4");
                        }

                        result.pass(function.call((INPUT1) inputs.get(0), (INPUT2) inputs.get(1),
                                                  (INPUT3) inputs.get(2), (INPUT4) inputs.get(3)));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified function.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, OUTPUT> FunctionRoutineBuilder<INPUT, OUTPUT> fromFunction(
            @Nonnull final FunctionN<INPUT, ? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

                        result.pass(function.call(inputs));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static FunctionRoutineBuilder<Void, Void> fromProcedure(
            @Nonnull final Function0<Void> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<Void, Void>(new InvocationFactory<Void, Void>() {

            @Nonnull
            public Invocation<Void, Void> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<Void, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends Void> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        if (inputs.size() != 0) {

                            throw new IllegalArgumentException(
                                    "[" + procedure.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 0");
                        }

                        procedure.call();
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT> FunctionRoutineBuilder<INPUT, Void> fromProcedure(
            @Nonnull final Function1<INPUT1, Void> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new InvocationFactory<INPUT, Void>() {

            @Nonnull
            public Invocation<INPUT, Void> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        if (inputs.size() != 1) {

                            throw new IllegalArgumentException(
                                    "[" + procedure.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 1");
                        }

                        procedure.call((INPUT1) inputs.get(0));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @param <INPUT1>  the first parameter type.
     * @param <INPUT2>  the second parameter type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT> FunctionRoutineBuilder<INPUT,
            Void> fromProcedure(
            @Nonnull final Function2<INPUT1, INPUT2, Void> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new InvocationFactory<INPUT, Void>() {

            @Nonnull
            public Invocation<INPUT, Void> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        if (inputs.size() != 2) {

                            throw new IllegalArgumentException(
                                    "[" + procedure.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 2");
                        }

                        procedure.call((INPUT1) inputs.get(0), (INPUT2) inputs.get(1));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @param <INPUT1>  the first parameter type.
     * @param <INPUT2>  the second parameter type.
     * @param <INPUT3>  the third parameter type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT>
    FunctionRoutineBuilder<INPUT, Void> fromProcedure(
            @Nonnull final Function3<INPUT1, INPUT2, INPUT3, Void> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new InvocationFactory<INPUT, Void>() {

            @Nonnull
            public Invocation<INPUT, Void> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        if (inputs.size() != 3) {

                            throw new IllegalArgumentException(
                                    "[" + procedure.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 3");
                        }

                        procedure.call((INPUT1) inputs.get(0), (INPUT2) inputs.get(1),
                                       (INPUT3) inputs.get(2));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @param <INPUT1>  the first parameter type.
     * @param <INPUT2>  the second parameter type.
     * @param <INPUT3>  the third parameter type.
     * @param <INPUT4>  the fourth parameter type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT, INPUT4
            extends INPUT> FunctionRoutineBuilder<INPUT, Void> fromProcedure(
            @Nonnull final Function4<INPUT1, INPUT2, INPUT3, INPUT4, Void> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new InvocationFactory<INPUT, Void>() {

            @Nonnull
            public Invocation<INPUT, Void> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        if (inputs.size() != 4) {

                            throw new IllegalArgumentException(
                                    "[" + procedure.getClass().getCanonicalName() +
                                            "] wrong number of input parameters: was "
                                            + inputs.size() + " while expected 4");
                        }

                        procedure.call((INPUT1) inputs.get(0), (INPUT2) inputs.get(1),
                                       (INPUT3) inputs.get(2), (INPUT4) inputs.get(3));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT> FunctionRoutineBuilder<INPUT, Void> fromProcedure(
            @Nonnull final FunctionN<INPUT, Void> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new InvocationFactory<INPUT, Void>() {

            @Nonnull
            public Invocation<INPUT, Void> newInvocation(@Nonnull final Object... args) {

                return new SingleCallInvocation<INPUT, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        procedure.call(inputs);
                    }
                };
            }
        });
    }

    @Nonnull
    @Override
    public RoutineBuilder<INPUT, OUTPUT> apply(@Nonnull final RoutineConfiguration configuration) {

        return super.apply(RoutineConfiguration.builderFrom(configuration)
                                               .withInputOrder(OrderType.PASSING_ORDER).applied());
    }
}
