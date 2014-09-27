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
 * The framework provides a routine class which is based on the implementation of an execution
 * interface. Execution objects are dynamically instantiated when needed, effectively mimicking
 * the temporary scope of a function call.<br/>
 * The advantage of this approach is that the execution flow can be run in steps, allowing for
 * continuous streaming of the input data and for abortion in the middle of the execution, without
 * blocking the running thread for the whole duration of the asynchronous invocation.
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
 * The routine object provides three different ways to launch an execution:
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
 * Processing parallelization is the key to leverage the processing power of the modern multi-core
 * machines. In order to achieve it, the input data must be...
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
     * Short for <code>invoke().readAll()</code>.
     *
     * @return the list of output data.
     */
    public List<OUTPUT> call();

    /**
     * Short for <code>invoke(input).readAll()</code>.
     *
     * @param input the input.
     * @return the list of output data.
     */
    public List<OUTPUT> call(INPUT input);

    /**
     * Short for <code>invoke(inputs).readAll()</code>.
     *
     * @param inputs the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> call(INPUT... inputs);

    /**
     * Short for <code>invoke(inputs).readAll()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> call(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>invoke(inputs).readAll()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> call(OutputChannel<? extends INPUT> inputs);

    /**
     * Short for <code>invokeAsyn().readAll()</code>.
     *
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn();

    /**
     * Short for <code>invokeAsyn(input).readAll()</code>.
     *
     * @param input the input.
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn(INPUT input);

    /**
     * Short for <code>invokeAsyn(inputs).readAll()</code>.
     *
     * @param inputs the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn(INPUT... inputs);

    /**
     * Short for <code>invokeAsyn(inputs).readAll()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>invokeAsyn(inputs).readAll()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callAsyn(OutputChannel<? extends INPUT> inputs);

    /**
     * Short for <code>invokeParall().readAll()</code>.
     *
     * @return the list of output data.
     */
    public List<OUTPUT> callParall();

    /**
     * Short for <code>invokeParall(input).readAll()</code>.
     *
     * @param input the input.
     * @return the list of output data.
     */
    public List<OUTPUT> callParall(INPUT input);

    /**
     * Short for <code>invokeParall(inputs).readAll()</code>.
     *
     * @param inputs the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callParall(INPUT... inputs);

    /**
     * Short for <code>invokeParall(inputs).readAll()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callParall(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>invokeParall(inputs).readAll()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the list of output data.
     */
    public List<OUTPUT> callParall(OutputChannel<? extends INPUT> inputs);

    /**
     * Short for <code>launch().close()</code>.
     *
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invoke();

    /**
     * Short for <code>launch(input).close()</code>.
     *
     * @param input the input.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invoke(INPUT input);

    /**
     * Short for <code>launch(inputs).close()</code>.
     *
     * @param inputs the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invoke(INPUT... inputs);

    /**
     * Short for <code>launch(inputs).close()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invoke(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>launch(inputs).close()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invoke(OutputChannel<? extends INPUT> inputs);

    /**
     * Short for <code>launchAsyn().close()</code>.
     *
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeAsyn();

    /**
     * Short for <code>launchAsyn(input).close()</code>.
     *
     * @param input the input.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeAsyn(INPUT input);

    /**
     * Short for <code>launchAsyn(inputs).close()</code>.
     *
     * @param inputs the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeAsyn(INPUT... inputs);

    /**
     * Short for <code>launchAsyn(inputs).close()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeAsyn(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>launchAsyn(inputs).close()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeAsyn(OutputChannel<? extends INPUT> inputs);

    /**
     * Short for <code>launchParall(input).close()</code>.
     *
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeParall();

    /**
     * Short for <code>launchParall(input).close()</code>.
     *
     * @param input the input.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeParall(INPUT input);

    /**
     * Short for <code>launchParall(inputs).close()</code>.
     *
     * @param inputs the iterable returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeParall(INPUT... inputs);

    /**
     * Short for <code>launchParall(inputs).close()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeParall(Iterable<? extends INPUT> inputs);

    /**
     * Short for <code>launchParall(inputs).close()</code>.
     *
     * @param inputs the output channel returning the input data.
     * @return the output channel.
     */
    public OutputChannel<OUTPUT> invokeParall(OutputChannel<? extends INPUT> inputs);

    /**
     * Launches the execution of this routine in synchronous mode.
     *
     * @return the routine parameter channel.
     */
    public ParameterChannel<INPUT, OUTPUT> launch();

    /**
     * Launches the execution of this routine in asynchronous mode.
     *
     * @return the routine parameter channel.
     */
    public ParameterChannel<INPUT, OUTPUT> launchAsyn();

    /**
     * Launches the execution of this routine in parallel mode.
     *
     * @return the routine parameter channel.
     */
    public ParameterChannel<INPUT, OUTPUT> launchParall();
}