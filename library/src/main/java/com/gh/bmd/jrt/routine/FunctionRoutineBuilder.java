/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.routine;

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
import com.gh.bmd.jrt.invocation.Invocations.Procedure0;
import com.gh.bmd.jrt.invocation.Invocations.Procedure1;
import com.gh.bmd.jrt.invocation.Invocations.Procedure2;
import com.gh.bmd.jrt.invocation.Invocations.Procedure3;
import com.gh.bmd.jrt.invocation.Invocations.Procedure4;
import com.gh.bmd.jrt.invocation.Invocations.ProcedureN;
import com.gh.bmd.jrt.invocation.SingleCallInvocation;
import com.gh.bmd.jrt.invocation.StatelessInvocation;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class implementing a builder of routine objects based on an invocation class token.
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
     * @throws NullPointerException if the class token is null.
     */
    @SuppressWarnings("ConstantConditions")
    private FunctionRoutineBuilder(@Nonnull final InvocationFactory<INPUT, OUTPUT> factory) {

        super(factory);
        super.withConfiguration(RoutineConfiguration.withInputOrder(OrderType.PASSING));
    }

    /**
     * Returns a new builder based on the specified function.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <OUTPUT> FunctionRoutineBuilder<Void, OUTPUT> newInstance(
            @Nonnull final Function0<OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<Void, OUTPUT>(new InvocationFactory<Void, OUTPUT>() {

            @Nonnull
            @Override
            public Invocation<Void, OUTPUT> newInvocation() {

                return new SingleCallInvocation<Void, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends Void> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

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
     * @throws NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, OUTPUT> FunctionRoutineBuilder<INPUT, OUTPUT> newInstance(
            @Nonnull final Function1<INPUT, OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new StatelessInvocation<INPUT, OUTPUT>() {

            @Override
            public void onInput(final INPUT input, @Nonnull final ResultChannel<OUTPUT> result) {

                result.pass(function.call(input));
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
     * @throws NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, OUTPUT>
    FunctionRoutineBuilder<INPUT, OUTPUT> newInstance(
            @Nonnull final Function2<INPUT1, INPUT2, OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            @Override
            public Invocation<INPUT, OUTPUT> newInvocation() {

                return new SingleCallInvocation<INPUT, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

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
     * @throws NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT, OUTPUT>
    FunctionRoutineBuilder<INPUT, OUTPUT> newInstance(
            @Nonnull final Function3<INPUT1, INPUT2, INPUT3, OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            @Override
            public Invocation<INPUT, OUTPUT> newInvocation() {

                return new SingleCallInvocation<INPUT, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

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
     * @throws NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT, INPUT4
            extends INPUT, OUTPUT> FunctionRoutineBuilder<INPUT, OUTPUT> newInstance(
            @Nonnull final Function4<INPUT1, INPUT2, INPUT3, INPUT4, OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            @Override
            public Invocation<INPUT, OUTPUT> newInvocation() {

                return new SingleCallInvocation<INPUT, OUTPUT>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<OUTPUT> result) {

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
     * @throws NullPointerException if the specified function is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, OUTPUT> FunctionRoutineBuilder<INPUT, OUTPUT> newInstance(
            @Nonnull final FunctionN<INPUT, OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            @Override
            public Invocation<INPUT, OUTPUT> newInvocation() {

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
     * Returns a new builder based on the specified procedure.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @return the builder instance.
     * @throws NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static FunctionRoutineBuilder<Void, Void> newInstance(@Nonnull final Procedure0 procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<Void, Void>(new InvocationFactory<Void, Void>() {

            @Nonnull
            @Override
            public Invocation<Void, Void> newInvocation() {

                return new SingleCallInvocation<Void, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends Void> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        procedure.execute();
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @return the builder instance.
     * @throws NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT> FunctionRoutineBuilder<INPUT, Void> newInstance(
            @Nonnull final Procedure1<INPUT> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new StatelessInvocation<INPUT, Void>() {

            @Override
            public void onInput(final INPUT input, @Nonnull final ResultChannel<Void> result) {

                procedure.execute(input);
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @param <INPUT1>  the first parameter type.
     * @param <INPUT2>  the second parameter type.
     * @return the builder instance.
     * @throws NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT> FunctionRoutineBuilder<INPUT,
            Void> newInstance(
            @Nonnull final Procedure2<INPUT1, INPUT2> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new InvocationFactory<INPUT, Void>() {

            @Nonnull
            @Override
            public Invocation<INPUT, Void> newInvocation() {

                return new SingleCallInvocation<INPUT, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        procedure.execute((INPUT1) inputs.get(0), (INPUT2) inputs.get(1));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @param <INPUT1>  the first parameter type.
     * @param <INPUT2>  the second parameter type.
     * @param <INPUT3>  the third parameter type.
     * @return the builder instance.
     * @throws NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT>
    FunctionRoutineBuilder<INPUT, Void> newInstance(
            @Nonnull final Procedure3<INPUT1, INPUT2, INPUT3> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new InvocationFactory<INPUT, Void>() {

            @Nonnull
            @Override
            public Invocation<INPUT, Void> newInvocation() {

                return new SingleCallInvocation<INPUT, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        procedure.execute((INPUT1) inputs.get(0), (INPUT2) inputs.get(1),
                                          (INPUT3) inputs.get(2));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.
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
     * @throws NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT, INPUT4
            extends INPUT> FunctionRoutineBuilder<INPUT, Void> newInstance(
            @Nonnull final Procedure4<INPUT1, INPUT2, INPUT3, INPUT4> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new InvocationFactory<INPUT, Void>() {

            @Nonnull
            @Override
            public Invocation<INPUT, Void> newInvocation() {

                return new SingleCallInvocation<INPUT, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        procedure.execute((INPUT1) inputs.get(0), (INPUT2) inputs.get(1),
                                          (INPUT3) inputs.get(2), (INPUT4) inputs.get(3));
                    }
                };
            }
        });
    }

    /**
     * Returns a new builder based on the specified procedure.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @return the builder instance.
     * @throws NullPointerException if the specified procedure is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static <INPUT> FunctionRoutineBuilder<INPUT, Void> newInstance(
            @Nonnull final ProcedureN<INPUT> procedure) {

        if (procedure == null) {

            throw new NullPointerException("the procedure must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, Void>(new InvocationFactory<INPUT, Void>() {

            @Nonnull
            @Override
            public Invocation<INPUT, Void> newInvocation() {

                return new SingleCallInvocation<INPUT, Void>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onCall(@Nonnull final List<? extends INPUT> inputs,
                            @Nonnull final ResultChannel<Void> result) {

                        procedure.execute(inputs);
                    }
                };
            }
        });
    }

    @Nonnull
    @Override
    public RoutineBuilder<INPUT, OUTPUT> withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        return super.withConfiguration(RoutineConfiguration.notNull(configuration)
                                                           .builderFrom()
                                                           .withInputOrder(OrderType.PASSING)
                                                           .buildConfiguration());
    }
}
