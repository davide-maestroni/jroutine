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
package com.bmd.jrt.android.invocator;

import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.invocation.Invocation;

import javax.annotation.Nonnull;

/**
 * Interface defining an helper class used to invoke routines linked to a context lifecycle.
 * <p/>
 * Routine invocations started through the implementing classes can be safely restored after a
 * change in the configuration, so to avoid duplicated calls and memory leaks.
 * <p/>
 * Note that the <code>equals()</code> and <code>hashCode()</code> methods of the input parameter
 * objects might be employed to check for clashing of invocations or compute the invocation ID.<br/>
 * In case the caller cannot guarantee the correct behavior of the aforementioned method
 * implementations, a user defined ID or the <code>RESET</code> clash resolution should be used to
 * avoid unexpected results.
 * <p/>
 * Created by davide on 12/9/14.
 */
public interface RoutineInvocator {

    /**
     * Constant identifying a routine ID computed from the executor class and the input parameters.
     */
    public static final int GENERATED_ID = Integer.MIN_VALUE;

    /**
     * Invokes the routine employing the specified executor.
     *
     * @param classToken the invocation class token.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the routine parameter channel.
     * @throws NullPointerException if the specified token is null.
     */
    @Nonnull
    public <INPUT, OUTPUT> ParameterChannel<INPUT, OUTPUT> invoke(
            @Nonnull ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken);

    /**
     * Tells the invocator how to resolve invocation clashes. A clash happens when an invocation
     * with the same ID is still running.
     *
     * @param resolution the type of resolution.
     * @return this invocator.
     * @throws NullPointerException if the specified resolution type is null.
     */
    @Nonnull
    public RoutineInvocator onClash(@Nonnull ClashResolution resolution);

    /**
     * Tells the invocator to identify the invocation with the specified ID.
     *
     * @param id the invocation ID.
     * @return this invocator.
     */
    @Nonnull
    public RoutineInvocator withId(int id);

    /**
     * Invocation clash resolution enumeration.<br/>
     * The clash of two invocation happens when the same ID is already in use at the time of the
     * routine execution. The possible outcomes are:
     * <ul>
     * <li>the running invocation is reset</li>
     * <li>the running invocation is restarted only if the input data are different from the current
     * ones, and retained otherwise</li>
     * <li>the running invocation is always retained (ignoring the input data)</li>
     * </ul>
     */
    public enum ClashResolution {

        /**
         * The clash is resolved by aborting and resetting the running invocation.
         */
        RESET,
        /**
         * The clash is resolved by restarting the running invocation, in case its input data are
         * different from the current ones.
         */
        RESTART,
        /**
         * The clash is resolved by keeping the running invocation, independently from its input
         * data.
         */
        KEEP,
        /**
         * The default resolution, that is, it is let to the framework decide the best strategy to
         * resolve the clash.
         */
        DEFAULT
    }
}
