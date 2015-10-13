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
package com.github.dm.jrt.invocation;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.RoutineException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface defining a routine invocation.
 * <p/>
 * The typical lifecycle of an invocation object is the one depicted below:
 * <pre>
 *     <code>
 *
 *                   &lt;init&gt;
 *
 *                   |     |
 *                   |     |-------------------------------
 *                   V     |                               |
 *            ----------------                             |
 *        --->|onInitialize()|-----------                  |
 *       |    ----------------           |                 |
 *       |           |      ------       |                 |
 *       |           |     |      |      |                 |
 *       |           V     V      |      |                 |
 *       |    ----------------    |      |                 |
 *       |    |  onInput()   |----       |                 |
 *       |    ----------------           V                 |
 *       |           |     |         ----------------      |
 *       |           |     |-------->|  onAbort()   |      |
 *       |           V     |         ----------------      |
 *       |    ----------------           |     |           |
 *       |    |  onResult()  |           |     |           |
 *       |    ----------------           |     |           |
 *       |           |      -------------      |           |
 *       |           |     |              (if exception is thrown)
 *       |           V     V                   |           |
 *       |    -----------------                |           |
 *        ----| onTerminate() |                |           |
 *            -----------------                |           |
 *                   |      -------------------------------
 *                   |     |
 *                   V     V
 *            -----------------
 *            |  onDestroy()  |
 *            -----------------
 *     </code>
 * </pre>
 * Note that the {@code onInput()} method will be called for each input passed to the routine, so,
 * in case no input is expected, the {@code onResult()} method will be called soon after the
 * initialization.
 * <p/>
 * Note also that {@code onAbort()} might be called at any time between {@code onInitialize()} and
 * {@code onTerminate()} in case the execution is aborted.
 * <br/>
 * The only case in which the {@code onTerminate()} method does not get call at all, is when an
 * exception escapes the {@code onAbort()} method invocation.
 * <p/>
 * The {@code onTerminate()} method is meant to allow the clean up and reset operations needed to
 * prepare the invocation object to be reused. When the method is not called or does not complete
 * successfully, the invocation object is discarded.<br/>
 * While the {@code onDestroy()} method is meant to indicate that the invocation object is no longer
 * needed, so any associated resource can be safely released. Note that this method may never get
 * called if the routine is automatically garbage collected.
 * <p/>
 * Be aware that there is no guarantee about how and when an invocation will produce a result. It
 * might return one or more output for each input, or no output at all.
 * <p/>
 * Any exception escaping the invocation methods, unless it extends the base
 * {@link com.github.dm.jrt.channel.RoutineException RoutineException}, will be wrapped as the cause
 * of an {@link com.github.dm.jrt.invocation.InvocationException InvocationException} instance.
 * <p/>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface Invocation<IN, OUT> {

    /**
     * Called when the routine execution is aborted.<br/>
     * This method may be called at any time after the invocation initialization.
     *
     * @param reason the reason of the abortion.
     */
    void onAbort(@Nullable RoutineException reason);

    /**
     * Called when the routine invocation is no longer needed.
     */
    void onDestroy();

    /**
     * Called when the routine invocation is initialized.<br/>
     * This is always the first method in the invocation lifecycle.
     */
    void onInitialize();

    /**
     * Called when an input is passed to the routine.<br/>
     * This method is called once for each input object.
     *
     * @param input  the input.
     * @param result the result channel.
     */
    void onInput(IN input, @NotNull ResultChannel<OUT> result);

    /**
     * Called when all the inputs has been passed to the routine.<br/>
     * This method is called once in the invocation lifecycle to indicate that the final invocation
     * results should be passed to the result channel.
     *
     * @param result the result channel.
     */
    void onResult(@NotNull ResultChannel<OUT> result);

    /**
     * Called when the invocation execution has completed.
     */
    void onTerminate();
}
