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
package com.gh.bmd.jrt.android.builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of routines bound to a context lifecycle.
 * <p/>
 * Routine invocations started through the returned objects can be safely restored after a change in
 * the configuration, so to avoid duplicated calls and memory leaks. Be aware, though, that the
 * invocation results will always be dispatched in the main thread, no matter the calling one was,
 * so, waiting for the outputs right after the routine invocation, will result in a deadlock.<br/>
 * The local context of the invocations will always be the application one.
 * <p/>
 * Note that the <code>equals()</code> and <code>hashCode()</code> methods of the input parameter
 * and constructor argument objects might be employed to check for clashing of invocations or
 * compute the invocation ID.<br/>
 * In case the caller cannot guarantee the correct behavior of the aforementioned method
 * implementations, a user defined ID or the <code>ABORT_THAT</code> clash resolution should be used
 * to avoid unexpected results.
 * <p/>
 * Created by Davide on 4/6/2015.
 */
public interface ContextRoutineBuilder {

    /**
     * Constant identifying a routine ID computed from the executor class and the input parameters.
     */
    int AUTO = Integer.MIN_VALUE;

    /**
     * Tells the builder how to resolve clashes of invocations. A clash happens when an invocation
     * of the same type and with the same ID is still running. A null value means that it is up to
     * the framework to choose a default resolution type.
     *
     * @param resolutionType the type of resolution.
     * @return this builder.
     */
    @Nonnull
    ContextRoutineBuilder onClash(@Nullable ClashResolutionType resolutionType);

    /**
     * Tells the builder how to cache the invocation result after its completion. A null value means
     * that it is up to the framework to choose a default strategy.
     *
     * @param strategyType the cache strategy type.
     * @return this builder.
     */
    @Nonnull
    ContextRoutineBuilder onComplete(@Nullable CacheStrategyType strategyType);

    /**
     * Sets the arguments to be passed to the invocation constructor.
     * <p/>
     * Note that the <code>equals()</code> and <code>hashCode()</code> methods of the invocation
     * constructor arguments, might be employed to check for clashing of invocations or compute the
     * invocation ID.<br/>
     * Note also that, the specified objects will be retained, so, they should be immutable or never
     * change their internal state in order to avoid concurrency issues.
     *
     * @param args the arguments.
     * @return this builder.
     */
    @Nonnull
    ContextRoutineBuilder withArgs(@Nullable Object... args);

    /**
     * Tells the builder to identify the invocation with the specified ID.
     *
     * @param invocationId the invocation ID.
     * @return this builder.
     */
    @Nonnull
    ContextRoutineBuilder withId(int invocationId);

    /**
     * Result cache type enumeration.<br/>
     * The cache strategy type indicates what will happen to the result of an invocation after its
     * completion.
     */
    enum CacheStrategyType {

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
    enum ClashResolutionType {

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
