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
package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.ObjectRoutineBuilder;
import com.github.dm.jrt.builder.RoutineBuilder;
import com.github.dm.jrt.builder.TransportChannelBuilder;
import com.github.dm.jrt.invocation.InvocationFactory;

import javax.annotation.Nonnull;

/**
 * This utility class represents the entry point to the library by acting as a factory of routine
 * builders.
 * <p/>
 * There are mainly two ways to create a routine object:
 * <p/>
 * <b>Routine by invocation customization</b><br/>
 * The first approach consists in implementing an invocation object. Invocations mimic the scope of
 * a function call. Objects are instantiated when needed and recycled for successive invocations.
 * <p/>
 * <b>Routine by method invocation</b><br/>
 * The second approach is based on the asynchronous invocation of a method of an existing class or
 * object via reflection.<br/>
 * It is possible to annotate selected methods to be asynchronously invoked, or to simply select
 * a method through its signature. It is also possible to build a proxy object whose methods will
 * in turn asynchronously invoke the target object ones.<br/>
 * Note that a proxy object can be simply defined as an interface implemented by the target, but
 * also as a completely unrelated one mirroring the target methods. In this way it is possible to
 * apply the library functionality to objects defined by third party libraries which are not under
 * direct control.<br/>
 * A mirror interface adds the possibility to override input and output parameters with output
 * channels, so that data are transferred asynchronously, avoiding the need to block execution while
 * waiting for them to be available.<br/>
 * Finally, it also possible to create a wrapper class to enable asynchronous invocation of methods,
 * through annotation pre-processing and compile-time code generation. In order to activate the
 * processing of annotations, it is simply necessary to include the proxy artifact or module in the
 * project dependencies.
 * <p/>
 * This class provides also a way to build transport channel instances, which can be used to pass
 * data without the need to start a routine invocation.
 * <p/>
 * <b>Some usage examples</b>
 * <p/>
 * <b>Example 1:</b> Asynchronously merge the output of two routines.
 * <pre>
 *     <code>
 *
 *         final TransportChannel&lt;Result&gt; channel = JRoutine.transport().buildChannel();
 *         channel.pass(doSomething1.asyncCall())
 *                .pass(doSomething2.asyncCall())
 *                .close();
 *                .eventually()
 *                .allInto(results);
 *     </code>
 * </pre>
 * Or simply:
 * <pre>
 *     <code>
 *
 *         final OutputChannel&lt;Result&gt; output1 = doSomething1.asyncCall();
 *         final OutputChannel&lt;Result&gt; output2 = doSomething2.asyncCall();
 *         output1.eventually().allInto(results);
 *         output2.eventually().allInto(results);
 *     </code>
 * </pre>
 * (Note that, the order of the input or the output of the routine is not guaranteed unless properly
 * configured)
 * <p/>
 * <b>Example 2:</b> Asynchronously concatenate the output of two routines.
 * <pre>
 *     <code>
 *
 *         doSomething2.asyncCall(doSomething1.asyncCall())).eventually().allInto(results);
 *     </code>
 * </pre>
 * <p/>
 * <b>Example 3:</b> Asynchronously get the output of two routines.
 * <pre>
 *     <code>
 *
 *         public interface AsyncCallback {
 *
 *             public void onResults(
 *                  &#64;Input(Result.class) OutputChannel&lt;Result&gt; result1,
 *                  &#64;Input(Result.class) OutputChannel&lt;Result&gt; result2);
 *         }
 *
 *         final AsyncCallback callback = JRoutine.on(targetObject(myCallback))
 *                                                .buildProxy(AsyncCallback.class);
 *         callback.onResults(doSomething1.asyncCall(), doSomething2.asyncCall());
 *     </code>
 * </pre>
 * Where the object <code>myCallback</code> implements a method
 * <code>public void onResults(Result result1, Result result2)</code>.
 * <p/>
 * <b>Example 4:</b> Asynchronously feed a routine from a different thread.
 * <pre>
 *     <code>
 *
 *         final TransportChannel&lt;Result&gt; channel = JRoutine.transport().buildChannel();
 *
 *         new Thread() {
 *
 *             &#64;Override
 *             public void run() {
 *
 *                 channel.pass(new Result()).close();
 *             }
 *
 *         }.start();
 *
 *         final Routine&lt;Result, Result&gt; routine =
 *                  JRoutine.&lt;Result&gt;on(PassingInvocation.&lt;Result&gt;factoryOf())
 *                          .buildRoutine();
 *         routine.asyncCall(channel).eventually().allInto(results);
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @see com.github.dm.jrt.annotation.Alias Alias
 * @see com.github.dm.jrt.annotation.Input Input
 * @see com.github.dm.jrt.annotation.Inputs Inputs
 * @see com.github.dm.jrt.annotation.Output Output
 * @see com.github.dm.jrt.annotation.Priority Priority
 * @see com.github.dm.jrt.annotation.ShareGroup ShareGroup
 * @see com.github.dm.jrt.annotation.Timeout Timeout
 * @see com.github.dm.jrt.annotation.TimeoutAction TimeoutAction
 */
public class JRoutine {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutine() {

    }

    /**
     * Returns a routine builder based on the specified invocation factory.<br/>
     * In order to prevent undesired leaks, the class of the specified factory must be static.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the class of the specified factory is not
     *                                            static.
     */
    @Nonnull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @Nonnull final InvocationFactory<IN, OUT> factory) {

        return new DefaultRoutineBuilder<IN, OUT>(factory);
    }

    /**
     * Returns a routine builder wrapping the specified target object.<br/>
     * Note that it is responsibility of the caller to retain a strong reference to the target
     * instance to prevent it from being garbage collected.
     *
     * @param target the invocation target.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the specified object class represents an
     *                                            interface.
     */
    @Nonnull
    public static ObjectRoutineBuilder on(@Nonnull final InvocationTarget target) {

        return new DefaultObjectRoutineBuilder(target);
    }

    /**
     * Returns a transport channel builder.
     *
     * @return the transport channel builder instance.
     */
    @Nonnull
    public static TransportChannelBuilder transport() {

        return new DefaultTransportChannelBuilder();
    }
}