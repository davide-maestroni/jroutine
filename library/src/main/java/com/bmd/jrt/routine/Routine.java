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
 * TODO
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
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