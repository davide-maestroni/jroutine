/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.routine;

import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.InvocationChannel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface defines the main component of the library architecture.
 * <p/>
 * The interface takes inspiration from the Go routines, where functions are executed
 * asynchronously and communicate with the external world only through channels. Being Java a
 * strictly object oriented programming language, the routine itself must be an object based on
 * logic implemented in other objects.
 * <p/>
 * The library includes a routine class based on the implementation of an invocation interface.
 * Invocation objects are dynamically instantiated when needed, effectively mimicking the temporary
 * scope of a function call.<br/>
 * The paradigm is based on input, result and output channels. A routine can be invoked in
 * different ways (as explained below). Each routine invocation returns an input channel through
 * which the caller can pass the input parameters. When all the parameters has been passed, the
 * input channel is closed and returns the output channel from which to read the invocation
 * results. At the same time a result channel is passed to the invocation implementation, so that
 * the output computed from the input parameters can be published outside.<br/>
 * The advantage of this approach is that the invocation flow can be run in steps, allowing for
 * continuous streaming of the input data and for abortion in the middle of the execution, without
 * blocking the running thread for the whole duration of the asynchronous invocation.<br/>
 * In fact, each channel can abort the execution at any time.
 * <p/>
 * The implementing class must provides an automatic synchronization of the invocation member
 * fields, though, in order to avoid concurrency issues, data passed through the routine channels
 * should be immutable or, at least, never shared inside and outside the routine.<br/>
 * Moreover, it is possible to recursively call the same or another routine from inside a routine
 * invocation in a safe way. Nevertheless, it is not allow to perform blocking calls (such as
 * reading data from an output channel) in the middle of an execution when shared runner instances
 * are employed. Additionally, to prevent deadlock or starvation issues, it is encouraged the use of
 * finite timeouts when performing blocking calls.
 * <p/>
 * The routine object provides three different ways to invoke an execution:
 * <p/>
 * <b>Synchronous invocation</b><br/>
 * The routine starts an invocation employing a synchronous runner. The result is similar to a
 * classic method call where the processing completes as soon as the function exits.
 * <p/>
 * <b>Asynchronous invocation</b><br/>
 * The routine starts an invocation employing an asynchronous runner. In this case the processing
 * happens in a different thread, while the calling process may go on with its own execution and
 * perform other tasks. The invocation results can be collected at any time through the output
 * channel methods.
 * <p/>
 * <b>Parallel invocation</b><br/>
 * Processing parallelization is the key to leverage the power of multi-core machines. In order to
 * achieve it, the input data must be divided into subsets which are then processed on different
 * threads.<br/>
 * A routine object provides a convenient way to start an invocation which in turn spawns another
 * invocation for each input passed. This particular type of invocation obviously produces
 * meaningful results only for routines which takes a single input parameter and computes the
 * relative output results.
 * <p/>
 * It is worth noting how the library has been designed only through interfaces, so that, as far as
 * the implementation honors the specific contracts, it is possible to seamlessly combine different
 * routine instances, even the ones coming from third party libraries.
 * <p/>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface Routine<IN, OUT> {

    /**
     * Short for {@code asyncInvoke().result()}.
     *
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> asyncCall();

    /**
     * Short for {@code asyncInvoke().pass(input).result()}.
     *
     * @param input the input.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> asyncCall(@Nullable IN input);

    /**
     * Short for {@code asyncInvoke().pass(inputs).result()}.
     *
     * @param inputs the input data.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> asyncCall(@Nullable IN... inputs);

    /**
     * Short for {@code asyncInvoke().pass(inputs).result()}.
     *
     * @param inputs the iterable returning the input data.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> asyncCall(@Nullable Iterable<? extends IN> inputs);

    /**
     * Short for {@code asyncInvoke().pass(inputs).result()}.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> asyncCall(@Nullable OutputChannel<? extends IN> inputs);

    /**
     * Invokes the execution of this routine in asynchronous mode.
     *
     * @return the invocation channel.
     */
    @NotNull
    InvocationChannel<IN, OUT> asyncInvoke();

    /**
     * Short for {@code parallelInvoke().result()}.
     * <p/>
     * (This method actually makes little sense, and should never need to be called, thought it is
     * here for completeness)
     *
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> parallelCall();

    /**
     * Short for {@code parallelInvoke().pass(input).result()}.
     * <p/>
     * (This method actually makes little sense, and should never need to be called, thought it is
     * here for completeness)
     *
     * @param input the input.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> parallelCall(@Nullable IN input);

    /**
     * Short for {@code parallelInvoke().pass(inputs).result()}.
     *
     * @param inputs the input data.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> parallelCall(@Nullable IN... inputs);

    /**
     * Short for {@code parallelInvoke().pass(inputs).result()}.
     *
     * @param inputs the iterable returning the input data.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> parallelCall(@Nullable Iterable<? extends IN> inputs);

    /**
     * Short for {@code parallelInvoke().pass(inputs).result()}.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> parallelCall(@Nullable OutputChannel<? extends IN> inputs);

    /**
     * Invokes the execution of this routine in parallel mode.
     *
     * @return the invocation channel.
     */
    @NotNull
    InvocationChannel<IN, OUT> parallelInvoke();

    /**
     * Makes the routine destroy all the cached invocation instances.
     * <p/>
     * This method is useful to force the release of external resources when done with the routine.
     * Note however that the routine will still be usable after the method returns.
     * <p/>
     * Normally it is not needed to call this method.
     */
    void purge();

    /**
     * Short for {@code syncInvoke().result()}.
     *
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> syncCall();

    /**
     * Short for {@code syncInvoke().pass(input).result()}.
     *
     * @param input the input.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> syncCall(@Nullable IN input);

    /**
     * Short for {@code syncInvoke().pass(inputs).result()}.
     *
     * @param inputs the input data.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> syncCall(@Nullable IN... inputs);

    /**
     * Short for {@code syncInvoke().pass(inputs).result()}.
     *
     * @param inputs the iterable returning the input data.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> syncCall(@Nullable Iterable<? extends IN> inputs);

    /**
     * Short for {@code syncInvoke().pass(inputs).result()}.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    @NotNull
    OutputChannel<OUT> syncCall(@Nullable OutputChannel<? extends IN> inputs);

    /**
     * Invokes the execution of this routine in synchronous mode.
     *
     * @return the invocation channel.
     */
    @NotNull
    InvocationChannel<IN, OUT> syncInvoke();
}
