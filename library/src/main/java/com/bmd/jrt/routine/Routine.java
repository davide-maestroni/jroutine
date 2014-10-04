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
package com.bmd.jrt.routine;

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ParameterChannel;

import java.util.List;

/**
 * This interface defines the main component of this framework.
 * <p/>
 * The interface takes inspiration from the Go routines, where functions are executed
 * asynchronously and communicate with the external world only through channels. Being Java a
 * strictly non-functional programming language, the routine itself must be an object based on
 * logic implemented in other objects.
 * <p/>
 * The framework includes a routine class based on the implementation of an execution interface.
 * Execution objects are dynamically instantiated when needed, effectively mimicking the temporary
 * scope of a function call.<br/>
 * The paradigm is based on input, result and output channels. The invocation of a routine returns
 * an input channel through which the caller can pass the input parameters. When the input is
 * closed, it returns the output channel from which to read the invocation results. At the same
 * time a result channel is passed to the execution implementation, so that the output computed
 * from the input parameters can be transferred outside.<br/>
 * The advantage of this approach is that the execution flow can be run in steps, allowing for
 * continuous streaming of the input data and for abortion in the middle of the execution, without
 * blocking the running thread for the whole duration of the asynchronous invocation.<br/>
 * In fact, each channel can be abort the execution at any time by calling the exposed method.
 * <p/>
 * The class implementation provides an automatic synchronization of the execution member fields,
 * though, in order to avoid concurrency issues, data passed through the routine channels should
 * be immutable or, at least, never shared inside and outside the routine.<br/>
 * Moreover, it is possible to recursively call the same or another routine from inside a routine
 * execution in a safe way. Nevertheless, it is always advisable to never perform blocking calls
 * (such as: reading data from an output channel) in the middle of an execution, since the use
 * of shared runner instances may lead to unexpected deadlocks. In facts, to prevent deadlock or
 * starvation issues, it is encouraged the use of finite timeouts when performing blocking calls.
 * <p/>
 * The routine object provides three different ways to invoke an execution:
 * <p/>
 * <b>Synchronous invocation</b><br/>
 * The routine starts an execution employing a synchronous runner. The result is similar to a
 * classic method call where the processing completes as soon as the function exits.
 * <p/>
 * <b>Asynchronous invocation</b><br/>
 * The routine starts an execution employing an asynchronous runner. In this case the processing
 * happens in a different thread, while the calling process may go on with its own execution and
 * perform other tasks. The invocation results can be collected at any time through the output
 * channel methods.
 * <p/>
 * <b>Parallel invocation</b><br/>
 * Processing parallelization is the key to leverage the power of multi-core machines. In order to
 * achieve it, the input data must be divided into subsets which are then processed on different
 * threads.<br/>
 * A routine object provides a convenient way to start an execution which in turn spawns another
 * invocation for each input passed. This particular type of invocation obviously produces
 * meaningful results only for routines which takes a single input parameter and computes the
 * relative output results.
 * <p/>
 * TODO: interface integration
 * TODO: explain synchronization
 * TODO: examples
 * TODO: call via reflection in real life (verify security issues...)
 * TODO: @Nonnull
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 * @see com.bmd.jrt.execution.Execution
 * @see com.bmd.jrt.runner.Runner
 */
public interface Routine<INPUT, OUTPUT> {

    /**
     * Short for <code>run().readAll()</code>.
     *
     * @return the list of output data.
     */
    public List<OUTPUT> call();

    /**
     * Short for <code>run(input).readAll()</code>.
     *
     * @param input the input.
     * @return the list of output data.
     */
    public List<OUTPUT> call(INPUT input);

    /**
     * Short for <code>run(inputs).readAll()</code>.
     *
     * @param inputs the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> call(INPUT... inputs);

    /**
     * Short for <code>run(inputs).readAll()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> call(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>run(inputs).readAll()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> call(OutputChannel<? extends INPUT> inputs);

    /**
     * Short for <code>runAsyn().readAll()</code>.
     *
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn();

    /**
     * Short for <code>runAsyn(input).readAll()</code>.
     *
     * @param input the input.
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn(INPUT input);

    /**
     * Short for <code>runAsyn(inputs).readAll()</code>.
     *
     * @param inputs the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn(INPUT... inputs);

    /**
     * Short for <code>runAsyn(inputs).readAll()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>runAsyn(inputs).readAll()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn(OutputChannel<? extends INPUT> inputs);

    /**
     * Short for <code>runParall().readAll()</code>.
     *
     * @return the list of output data.
     */
    public List<OUTPUT> callParall();

    /**
     * Short for <code>runParall(input).readAll()</code>.
     *
     * @param input the input.
     * @return the list of output data.
     */
    public List<OUTPUT> callParall(INPUT input);

    /**
     * Short for <code>runParall(inputs).readAll()</code>.
     *
     * @param inputs the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callParall(INPUT... inputs);

    /**
     * Short for <code>runParall(inputs).readAll()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callParall(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>runParall(inputs).readAll()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callParall(OutputChannel<? extends INPUT> inputs);

    /**
     * Invokes the execution of this routine in synchronous mode.
     *
     * @return the routine parameter channel.
     */
    public ParameterChannel<INPUT, OUTPUT> invoke();

    /**
     * Invokes the execution of this routine in asynchronous mode.
     *
     * @return the routine parameter channel.
     */
    public ParameterChannel<INPUT, OUTPUT> invokeAsyn();

    /**
     * Invokes the execution of this routine in parallel mode.
     *
     * @return the routine parameter channel.
     */
    public ParameterChannel<INPUT, OUTPUT> invokeParall();

    /**
     * Short for <code>invoke().results()</code>.
     *
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> run();

    /**
     * Short for <code>invoke(input).results()</code>.
     *
     * @param input the input.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> run(INPUT input);

    /**
     * Short for <code>invoke(inputs).results()</code>.
     *
     * @param inputs the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> run(INPUT... inputs);

    /**
     * Short for <code>invoke(inputs).results()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> run(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>invoke(inputs).results()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> run(OutputChannel<? extends INPUT> inputs);

    /**
     * Short for <code>invokeAsyn().results()</code>.
     *
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runAsyn();

    /**
     * Short for <code>invokeAsyn(input).results()</code>.
     *
     * @param input the input.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runAsyn(INPUT input);

    /**
     * Short for <code>invokeAsyn(inputs).results()</code>.
     *
     * @param inputs the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runAsyn(INPUT... inputs);

    /**
     * Short for <code>invokeAsyn(inputs).results()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runAsyn(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>invokeAsyn(inputs).results()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runAsyn(OutputChannel<? extends INPUT> inputs);

    /**
     * Short for <code>invokeParall(input).results()</code>.
     *
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runParall();

    /**
     * Short for <code>invokeParall(input).results()</code>.
     *
     * @param input the input.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runParall(INPUT input);

    /**
     * Short for <code>invokeParall(inputs).results()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runParall(INPUT... inputs);

    /**
     * Short for <code>invokeParall(inputs).results()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runParall(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>invokeParall(inputs).results()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> runParall(OutputChannel<? extends INPUT> inputs);
}