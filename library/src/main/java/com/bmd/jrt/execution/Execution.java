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
package com.bmd.jrt.execution;

import com.bmd.jrt.channel.ResultChannel;

/**
 * Interface defining the behavior of a routine execution.
 * <p/>
 * The typical lifecycle of an execution object is the following:
 * <pre>
 *     <code>
 *
 *         - onInit();
 *
 *           ...
 *
 *         - onInput(input, results);
 *
 *           ...
 *
 *         - onInput(input, results);
 *
 *           ...
 *
 *         - onResult(results);
 *
 *         - onReturn();
 *     </code>
 * </pre>
 * Note that the <b><code>onInput()</code></b> method will be called for each input passed to the
 * routine, so, in case no input is expected, the <b><code>onResult()</code></b> method will be
 * called soon after the initialization.
 * <p/>
 * Note also that <b><code>onAbort()</code></b> might be called at any time between
 * <b><code>onInit()</code></b> and <b><code>onReturn()</code></b> in case the execution is
 * aborted.<br/>
 * The only case in which the <b><code>onReturn()</code></b> method does not get call, is when an
 * exception escapes the <b><code>onAbort()</code></b> method invocation.
 * <p/>
 * The <b><code>onReturn()</code></b> method is meant to allow the clean up and reset operations
 * needed to prepare the execution object to be reused. In fact, when the method is not called or
 * does not complete successfully, the execution object is discarded.
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 */
public interface Execution<INPUT, OUTPUT> {

    /**
     * Called when the routine execution is aborted.<br/>
     * This method can be called at any time after the execution initialization.
     *
     * @param throwable the reason of the abortion.
     */
    public void onAbort(Throwable throwable);

    /**
     * Called when the routine execution is initialized.<br/>
     * This is always the first method in the execution lifecycle.
     */
    public void onInit();

    /**
     * Called when an input is passed to the routine.<br/>
     * This method is called once for each input object.
     *
     * @param input   the input.
     * @param results the result channel.
     */
    public void onInput(INPUT input, ResultChannel<OUTPUT> results);

    /**
     * Called when all the inputs has been passed to the routine.<br/>
     * This method is called once in the execution lifecycle to indicate that the final execution
     * results should be passed to the result channel.
     *
     * @param results the result channel.
     */
    public void onResult(ResultChannel<OUTPUT> results);

    /**
     * Called when the routine execution has completed.
     */
    public void onReturn();
}