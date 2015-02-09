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
package com.bmd.jrt.android.builder;

import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineChannelBuilder.OrderBy;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.routine.Routine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of routines linked to a context lifecycle.
 * <p/>
 * Routine invocations started through the returned objects can be safely restored after a change in
 * the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will always be dispatched in the main thread, no matter the calling one was,
 * so that waiting for the outputs right after the routine invocation will result in a deadlock.
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
public interface AndroidRoutineBuilder<INPUT, OUTPUT> {

    /**
     * Constant identifying a routine ID computed from the executor class and the input parameters.
     */
    public static final int AUTO = Integer.MIN_VALUE;

    /**
     * Applies the specified configuration to this builder.<br/>
     * Note that the configuration options not supported by this builder methods will be ignored.
     *
     * @param configuration the configuration.
     * @return this builder.
     * @throws java.lang.NullPointerException if the specified configuration is null.
     */
    @Nonnull
    public AndroidRoutineBuilder<INPUT, OUTPUT> apply(@Nonnull RoutineConfiguration configuration);

    /**
     * Builds and returns the routine.
     *
     * @return the newly created routine instance.
     */
    @Nonnull
    public Routine<INPUT, OUTPUT> buildRoutine();

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
     * Sets the order in which input data are collected from the input channel. A null value means
     * that it is up to the framework to chose a default order type.
     *
     * @param order the order type.
     * @return this builder.
     */
    @Nonnull
    public AndroidRoutineBuilder<INPUT, OUTPUT> withInputOrder(@Nullable OrderBy order);

    /**
     * Sets the log instance. A null value means that it is up to the framework to chose a default
     * implementation.
     *
     * @param log the log instance.
     * @return this builder.
     */
    @Nonnull
    public AndroidRoutineBuilder<INPUT, OUTPUT> withLog(@Nullable Log log);

    /**
     * Sets the log level. A null value means that it is up to the framework to chose a default
     * level.
     *
     * @param level the log level.
     * @return this builder.
     */
    @Nonnull
    public AndroidRoutineBuilder<INPUT, OUTPUT> withLogLevel(@Nullable LogLevel level);

    /**
     * Sets the order in which output data are collected from the result channel. A null value means
     * that it is up to the framework to chose a default order type.
     *
     * @param order the order type.
     * @return this builder.
     */
    @Nonnull
    public AndroidRoutineBuilder<INPUT, OUTPUT> withOutputOrder(@Nullable OrderBy order);

    /**
     * Sets the type of the synchronous runner to be used by the routine. A null value means that it
     * is up to the framework to chose a default runner type.
     *
     * @param type the runner type.
     * @return this builder.
     */
    @Nonnull
    public AndroidRoutineBuilder<INPUT, OUTPUT> withSyncRunner(@Nullable RunnerType type);

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
     * <li>the running invocation is restarted</li>
     * <li>the running invocation is retained, ignoring the input data</li>
     * <li>the current invocation is aborted</li>
     * <li>the running invocation is restarted only if the input data are different from the current
     * ones, and retained otherwise</li>
     * <li>the current invocation is aborted only if the input data are different from the current
     * ones, and retained otherwise</li>
     * </ul>
     */
    public enum ClashResolution {

        /**
         * The clash is resolved by restarting the running invocation.
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
         * The clash is resolved by restarting the running invocation, only in case its input data
         * are different from the current ones.
         */
        ABORT_THAT_INPUT,
        /**
         * The clash is resolved by aborting the invocation with an {@link InputClashException},
         * only in case its input data are different from the current ones.
         */
        ABORT_THIS_INPUT,
    }
}
