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
import com.gh.bmd.jrt.invocation.StatelessInvocation;
import com.gh.bmd.jrt.invocation.TemplateInvocation;

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
        routineConfiguration().withInputOrder(OrderType.PASSING_ORDER).apply();
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
    static <OUTPUT> FunctionRoutineBuilder<Void, OUTPUT> from(
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
    static <INPUT, INPUT1 extends INPUT, OUTPUT> FunctionRoutineBuilder<INPUT, OUTPUT> from(
            @Nonnull final Function1<INPUT1, ? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new StatelessInvocation<INPUT, OUTPUT>() {

            @SuppressWarnings("unchecked")
            public void onInput(final INPUT input, @Nonnull final ResultChannel<OUTPUT> result) {

                result.pass(function.call((INPUT1) input));
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
    FunctionRoutineBuilder<INPUT, OUTPUT> from(
            @Nonnull final Function2<INPUT1, INPUT2, ? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new TemplateInvocation<INPUT, OUTPUT>() {

                    private final Object[] mInputs = new Object[2];

                    private int mIndex;

                    @Override
                    public void onInit() {

                        mIndex = 0;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onInput(final INPUT input,
                            @Nonnull final ResultChannel<OUTPUT> result) {

                        final Object[] inputs = mInputs;
                        inputs[mIndex] = input;

                        if (++mIndex > 1) {

                            mIndex = 0;
                            result.pass(function.call((INPUT1) inputs[0], (INPUT2) inputs[1]));
                        }
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onResult(@Nonnull final ResultChannel<OUTPUT> result) {

                        final int index = mIndex;
                        final Object[] inputs = mInputs;

                        if (index > 0) {

                            for (int i = index; i < inputs.length; i++) {

                                inputs[i] = null;
                            }
                        }

                        result.pass(function.call((INPUT1) inputs[0], (INPUT2) inputs[1]));
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
    FunctionRoutineBuilder<INPUT, OUTPUT> from(
            @Nonnull final Function3<INPUT1, INPUT2, INPUT3, ? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new TemplateInvocation<INPUT, OUTPUT>() {

                    private final Object[] mInputs = new Object[3];

                    private int mIndex;

                    @Override
                    public void onInit() {

                        mIndex = 0;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onInput(final INPUT input,
                            @Nonnull final ResultChannel<OUTPUT> result) {

                        final Object[] inputs = mInputs;
                        inputs[mIndex] = input;

                        if (++mIndex > 2) {

                            mIndex = 0;
                            result.pass(function.call((INPUT1) inputs[0], (INPUT2) inputs[1],
                                                      (INPUT3) inputs[2]));
                        }
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onResult(@Nonnull final ResultChannel<OUTPUT> result) {

                        final int index = mIndex;
                        final Object[] inputs = mInputs;

                        if (index > 0) {

                            for (int i = index; i < inputs.length; i++) {

                                inputs[i] = null;
                            }
                        }

                        result.pass(function.call((INPUT1) inputs[0], (INPUT2) inputs[1],
                                                  (INPUT3) inputs[2]));
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
            extends INPUT, OUTPUT> FunctionRoutineBuilder<INPUT, OUTPUT> from(
            @Nonnull final Function4<INPUT1, INPUT2, INPUT3, INPUT4, ? extends OUTPUT> function) {

        if (function == null) {

            throw new NullPointerException("the function must not be null");
        }

        return new FunctionRoutineBuilder<INPUT, OUTPUT>(new InvocationFactory<INPUT, OUTPUT>() {

            @Nonnull
            public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

                return new TemplateInvocation<INPUT, OUTPUT>() {

                    private final Object[] mInputs = new Object[4];

                    private int mIndex;

                    @Override
                    public void onInit() {

                        mIndex = 0;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onInput(final INPUT input,
                            @Nonnull final ResultChannel<OUTPUT> result) {

                        final Object[] inputs = mInputs;
                        inputs[mIndex] = input;

                        if (++mIndex > 3) {

                            mIndex = 0;
                            result.pass(function.call((INPUT1) inputs[0], (INPUT2) inputs[1],
                                                      (INPUT3) inputs[2], (INPUT4) inputs[3]));
                        }
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onResult(@Nonnull final ResultChannel<OUTPUT> result) {

                        final int index = mIndex;
                        final Object[] inputs = mInputs;

                        if (index > 0) {

                            for (int i = index; i < inputs.length; i++) {

                                inputs[i] = null;
                            }
                        }

                        result.pass(function.call((INPUT1) inputs[0], (INPUT2) inputs[1],
                                                  (INPUT3) inputs[2], (INPUT4) inputs[3]));
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
    static <INPUT, OUTPUT> FunctionRoutineBuilder<INPUT, OUTPUT> from(
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

    @Nonnull
    @Override
    public RoutineBuilder<INPUT, OUTPUT> apply(@Nonnull final RoutineConfiguration configuration) {

        return super.apply(
                configuration.builderFrom().withInputOrder(OrderType.PASSING_ORDER).apply());
    }
}
