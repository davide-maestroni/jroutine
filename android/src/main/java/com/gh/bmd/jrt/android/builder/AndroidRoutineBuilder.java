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
package com.gh.bmd.jrt.android.builder;

import com.gh.bmd.jrt.android.routine.AndroidRoutine;
import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of routines bound to a context lifecycle.
 * <p/>
 * Routine invocations started through the returned objects can be safely restored after a change in
 * the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will always be dispatched in the main thread, no matter the calling one was,
 * so waiting for the outputs right after the routine invocation will result in a deadlock.<br/>
 * The context of the invocations will be always the application one.
 * <p/>
 * Note that the <code>equals()</code> and <code>hashCode()</code> methods of the input parameter
 * objects might be employed to check for clashing of invocations or compute the invocation ID.<br/>
 * In case the caller cannot guarantee the correct behavior of the aforementioned method
 * implementations, a user defined ID or the <code>ABORT_THAT</code> clash resolution should be used
 * to avoid unexpected results.
 * <p/>
 * Created by davide on 12/9/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface AndroidRoutineBuilder<INPUT, OUTPUT> extends RoutineBuilder<INPUT, OUTPUT> {

    /**
     * Constant identifying a routine ID computed from the executor class and the input parameters.
     */
    public static final int AUTO = Integer.MIN_VALUE;

    @Nonnull
    @Override
    public AndroidRoutine<INPUT, OUTPUT> buildRoutine();

    /**
     * Note that all the options related to the output and input channels size and timeout will be
     * ignored.
     *
     * @param configuration the routine configuration.
     * @return this builder.
     */
    @Nonnull
    @Override
    public AndroidRoutineBuilder<INPUT, OUTPUT> withConfiguration(
            @Nullable RoutineConfiguration configuration);

    /**
     * Tells the builder how to resolve clashes of invocations. A clash happens when an invocation
     * of the same type and with the same ID is still running. A null value means that it is up to
     * the framework to chose a default resolution type.
     *
     * @param resolution the type of resolution.
     * @return this builder.
     */
    @Nonnull
    public AndroidRoutineBuilder<INPUT, OUTPUT> onClash(@Nullable ClashResolution resolution);

    /**
     * Tells the builder how to cache the invocation result after its completion. A null value means
     * that it is up to the framework to chose a default strategy.
     *
     * @param cacheStrategy the cache type.
     * @return this builder.
     */
    @Nonnull
    public AndroidRoutineBuilder<INPUT, OUTPUT> onComplete(@Nullable CacheStrategy cacheStrategy);

    /**
     * Tells the builder to identify the invocation with the specified ID.
     *
     * @param id the invocation ID.
     * @return this builder.
     */
    @Nonnull
    public AndroidRoutineBuilder<INPUT, OUTPUT> withId(int id);

    /**
     * Result cache type enumeration.<br/>
     * The cache type indicates what will happen to the result of an invocation after its
     * completion.
     */
    public enum CacheStrategy {

        /**
         * On completion the invocation results are cleared.
         */
        CLEAR,
        /**
         * Only in case of error the results are cleared, otherwise they are retained.
         */
        CACHE_IF_SUCCESS,
        /**
         * Only in case of successful completion the results are cleared, otherwise they are
         * retained.
         */
        CACHE_IF_ERROR,
        /**
         * On completion the invocation results are retained.
         */
        CACHE,
    }

    /**
     * Invocation clash resolution enumeration.<br/>
     * The clash of two invocation happens when the same ID is already in use at the time of the
     * routine execution. The possible outcomes are:
     * <ul>
     * <li>the running invocation is aborted</li>
     * <li>the running invocation is retained, ignoring the input data</li>
     * <li>the current invocation is aborted</li>
     * <li>the running invocation is aborted only if the input data are different from the current
     * ones, and retained otherwise</li>
     * <li>the current invocation is aborted only if the input data are different from the current
     * ones, and retained otherwise</li>
     * </ul>
     */
    public enum ClashResolution {

        /**
         * The clash is resolved by aborting the running invocation.
         */
        ABORT_THAT,
        /**
         * The clash is resolved by keeping the running invocation.
         */
        KEEP_THAT,
        /**
         * The clash is resolved by aborting the invocation with an {@link InputClashException}.
         */
        ABORT_THIS,
        /**
         * The clash is resolved by aborting the running invocation, only in case its input data are
         * different from the current ones.
         */
        ABORT_THAT_INPUT,
        /**
         * The clash is resolved by aborting the invocation with an {@link InputClashException},
         * only in case its input data are different from the current ones.
         */
        ABORT_THIS_INPUT,
    }
}
