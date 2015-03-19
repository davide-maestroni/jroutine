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
package com.gh.bmd.jrt.invocation;

import com.gh.bmd.jrt.channel.ResultChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining the behavior of a routine invocation.
 * <p/>
 * The typical lifecycle of an invocation object is the following:
 * <pre>
 *     <code>
 *
 *                  |
 *                  V
 *            --------------
 *        --->|  onInit()  |
 *       |    --------------
 *       |          |     ------
 *       |          |    |      |
 *       |          V    V      |
 *       |    --------------    |
 *       |    | onInput()  |----
 *       |    --------------
 *       |          |               --------------
 *       |          |        ...--->| onAbort()  |
 *       |          V               --------------
 *       |    --------------            |    |
 *       |    | onResult() |            |    |
 *       |    --------------            |    |
 *       |          |     --------------     |
 *       |          |    |                   | (if exception is thrown)
 *       |          V    V                   |
 *       |    --------------                 |
 *        ----| onReturn() |                 |
 *            --------------                 |
 *                  |     -------------------
 *                  |    |
 *                  V    V
 *            --------------
 *            |onDestroy() |
 *            --------------
 *     </code>
 * </pre>
 * Note that the <b><code>onInput()</code></b> method will be called for each input passed to the
 * routine, so, in case no input is expected, the <b><code>onResult()</code></b> method will be
 * called soon after the initialization.
 * <p/>
 * Note also that <b><code>onAbort()</code></b> might be called at any time between
 * <b><code>onInit()</code></b> and <b><code>onReturn()</code></b> in case the execution is
 * aborted.<br/>
 * The only case in which the <b><code>onReturn()</code></b> method does not get call at all, is
 * when an exception escapes the <b><code>onAbort()</code></b> method invocation.
 * <p/>
 * The <b><code>onReturn()</code></b> method is meant to allow the clean up and reset operations
 * needed to prepare the invocation object to be reused. When the method is not called or does not
 * complete successfully, the invocation object is discarded.<br/>
 * The <b><code>onDestroy()</code></b> method is meant to indicate that the invocation object is no
 * longer needed, so any associated resource can be safely released.
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface Invocation<INPUT, OUTPUT> {

    /**
     * Called when the routine execution is aborted.<br/>
     * This method may be called at any time after the invocation initialization.
     *
     * @param reason the reason of the abortion.
     */
    void onAbort(@Nullable Throwable reason);

    /**
     * Called when the routine invocation is no longer needed.
     */
    void onDestroy();

    /**
     * Called when the routine invocation is initialized.<br/>
     * This is always the first method in the invocation lifecycle.
     */
    void onInit();

    /**
     * Called when an input is passed to the routine.<br/>
     * This method is called once for each input object.
     *
     * @param input  the input.
     * @param result the result channel.
     */
    void onInput(INPUT input, @Nonnull ResultChannel<OUTPUT> result);

    /**
     * Called when all the inputs has been passed to the routine.<br/>
     * This method is called once in the invocation lifecycle to indicate that the final invocation
     * results should be passed to the result channel.
     *
     * @param result the result channel.
     */
    void onResult(@Nonnull ResultChannel<OUTPUT> result);

    /**
     * Called when the routine execution has completed.
     */
    void onReturn();
}
