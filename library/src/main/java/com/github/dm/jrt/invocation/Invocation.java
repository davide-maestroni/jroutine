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

package com.github.dm.jrt.invocation;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.common.RoutineException;

import org.jetbrains.annotations.NotNull;

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
 * The routine invocation interface is designed so to allow recycling of instantiated objects.
 * <p/>
 * Note that the {@code onInput()} method will be called for each input passed to the routine, so,
 * in case no input is expected, the {@code onResult()} method will be called soon after the
 * initialization.
 * <p/>
 * Note also that {@code onAbort()} might be called at any time between {@code onInitialize()} and
 * {@code onTerminate()} in case the execution is aborted.<br/>
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
 * Keep in mind, when implementing an invocation class, that the result channel passed to the
 * {@code onInput()} and {@code onResult()} methods will be closed as soon as the latter exits.<br/>
 * Any further attempt to post results will generate an exception.<br/>
 * It is anyway possible to keep on generating results by passing to the result channel an output or
 * an I/O channel.
 * <p/>
 * Be aware that there is no guarantee about how and when an invocation will produce a result. It
 * might return one or more output for each input, or no output at all.
 * <p/>
 * Any exception escaping the invocation methods, unless it extends the base
 * {@link com.github.dm.jrt.common.RoutineException RoutineException}, will be wrapped as the cause
 * of an {@link com.github.dm.jrt.invocation.InvocationException InvocationException} instance.
 * <p/>
 * The class {@link com.github.dm.jrt.invocation.TemplateInvocation TemplateInvocation} provides
 * an abstract empty implementation of the invocation interface.
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
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    void onAbort(@NotNull RoutineException reason) throws Exception;

    /**
     * Called when the routine invocation is no longer needed.
     *
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    void onDestroy() throws Exception;

    /**
     * Called when the routine invocation is initialized.<br/>
     * This is always the first method in the invocation lifecycle.
     *
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    void onInitialize() throws Exception;

    /**
     * Called when an input is passed to the routine.<br/>
     * This method is called once for each input object.
     *
     * @param input  the input.
     * @param result the result channel.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    void onInput(IN input, @NotNull ResultChannel<OUT> result) throws Exception;

    /**
     * Called when all the inputs has been passed to the routine.<br/>
     * This method is called once in the invocation lifecycle to indicate that the final invocation
     * results should be passed to the result channel.
     *
     * @param result the result channel.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    void onResult(@NotNull ResultChannel<OUT> result) throws Exception;

    /**
     * Called when the invocation execution has completed.
     *
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    void onTerminate() throws Exception;
}
