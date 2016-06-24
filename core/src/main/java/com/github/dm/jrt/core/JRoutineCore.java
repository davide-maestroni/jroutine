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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * This utility class represents the entry point to the library by acting as a factory of routine
 * builders.
 * <p>
 * The main way of creating a routine is to implement an invocation object. Invocations mimic the
 * scope of a function call. Objects are instantiated when needed and recycled for successive
 * invocations.
 * <p>
 * This class provides also a way to build channel instances, which can be used to pass data without
 * the need to start a routine invocation.
 * <p>
 * <b>Some usage examples</b>
 * <p>
 * <b>Example 1:</b> Configure and build a routine.
 * <pre>
 *     <code>
 *
 *         final Routine&lt;Input, Result&gt; routine =
 *                 JRoutineCore.with(myFactory)
 *                             .invocationConfiguration()
 *                             .withLogLevel(Level.WARNING)
 *                             .applied()
 *                             .buildRoutine();
 *     </code>
 * </pre>
 * <p>
 * <b>Example 2:</b> Asynchronously merge the output of two routines.
 * <pre>
 *     <code>
 *
 *         final Channel&lt;Result, Result&gt; channel = JRoutineCore.io().buildChannel();
 *         channel.pass(routine1.asyncCall().close())
 *                .pass(routine2.asyncCall().close())
 *                .close();
 *                .after(seconds(20))
 *                .allInto(results);
 *     </code>
 * </pre>
 * Or simply:
 * <pre>
 *     <code>
 *
 *         final Channel&lt;Void, Result&gt; output1 = routine1.asyncCall().close();
 *         final Channel&lt;Void, Result&gt; output2 = routine2.asyncCall().close();
 *         output1.after(seconds(20)).allInto(results);
 *         output2.after(seconds(20)).allInto(results);
 *     </code>
 * </pre>
 * (Note that, the order of the input or the output of the routine is not guaranteed unless properly
 * configured)
 * <p>
 * <b>Example 3:</b> Asynchronously concatenate the output of two routines.
 * <pre>
 *     <code>
 *
 *         routine2.asyncCall(routine1.asyncCall().close()).after(seconds(20).all();
 *     </code>
 * </pre>
 * Or, in an equivalent way:
 * <pre>
 *     <code>
 *
 *         routine1.asyncCall().close().bind(routine2.asyncCall()).close().after(seconds(20).all();
 *     </code>
 * </pre>
 * <p>
 * <b>Example 4:</b> Asynchronously feed a routine from a different thread.
 * <pre>
 *     <code>
 *
 *         final Routine&lt;Result, Result&gt; routine =
 *                  JRoutineCore.with(IdentityInvocation.&lt;Result&gt;factoryOf())
 *                              .buildRoutine();
 *         final Channel&lt;Result, Result&gt; channel = routine.asyncCall();
 *
 *         new Thread() {
 *
 *             &#64;Override
 *             public void run() {
 *                 channel.pass(new Result()).close();
 *             }
 *         }.start();
 *
 *         channel.after(seconds(20)).allInto(results);
 *     </code>
 * </pre>
 * <p>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @see com.github.dm.jrt.core.routine.Routine Routine
 */
public class JRoutineCore {

    /**
     * Avoid explicit instantiation.
     */
    protected JRoutineCore() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a channel builder.
     *
     * @return the channel builder instance.
     */
    @NotNull
    public static ChannelBuilder io() {
        return new DefaultChannelBuilder();
    }

    /**
     * Returns a routine builder based on the specified invocation factory.
     * <br>
     * In order to prevent undesired leaks, the class of the specified factory should have a static
     * scope.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> with(
            @NotNull final InvocationFactory<IN, OUT> factory) {
        return new DefaultRoutineBuilder<IN, OUT>(factory);
    }
}
