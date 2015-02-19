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
package com.bmd.jrt.routine;

import com.bmd.jrt.invocation.InvocationFactory;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

/**
 * This utility class represents the entry point to the framework functionalities by acting as a
 * factory of routine builders.
 * <p/>
 * There are mainly two ways to create a routine object:
 * <p/>
 * <b>Routine by invocation customization</b><br/>
 * The first approach consists in implementing an invocation object and build on its class token
 * a routine instance.
 * <p/>
 * <b>Routine by method invocation</b><br/>
 * The second approach is based on the asynchronous invocation of a method of an existing class or
 * object via reflection.<br/>
 * It is possible to annotate selected methods to be asynchronously invoked, or to simply select
 * a method through its signature. It is also possible to build a proxy object whose methods will
 * in turn asynchronously invoke the target object ones.<br/>
 * Note that a proxy object can be simply defined as an interface implemented by the target, but
 * also as a completely unrelated one mirroring the target methods. In this way it is possible to
 * apply the framework functionalities to objects defined by third party libraries which are not
 * under direct control.<br/>
 * A mirror interface adds the possibility to override input and output parameters with output
 * channels, so that data are transferred asynchronously avoiding the need to block execution while
 * waiting for them to be available.<br/>
 * Finally, it also possible to create a wrapper class to enable asynchronous invocation of methods,
 * through annotation pre-processing and compile time code generation. In order to activate the
 * processing of annotations, it is simply necessary to include the "jroutine-processor" artifact
 * or module in the project dependencies.
 * <p/>
 * <b>Some usage examples</b>
 * <p/>
 * <b>Example 1:</b> Asynchronously merge the output of two routines.
 * <pre>
 *     <code>
 *
 *         final StandaloneChannel&lt;Result&gt; channel =
 *                 JRoutine.on().&lt;Result&gt;buildChannel();
 *
 *         channel.input()
 *                .pass(doSomething1.callAsync())
 *                .pass(doSomething2.callAsync())
 *                .close();
 *         channel.output()
 *                .eventually()
 *                .readAllInto(results);
 *     </code>
 * </pre>
 * Or simply:
 * <pre>
 *     <code>
 *
 *         final OutputChannel&lt;Result&gt; output1 = doSomething1.callAsync();
 *         final OutputChannel&lt;Result&gt; output2 = doSomething2.callAsync();
 *
 *         output1.eventually().readAllInto(results);
 *         output2.eventually().readAllInto(results);
 *     </code>
 * </pre>
 * (Note that, the order of the input or the output of the routine is not guaranteed unless the
 * proper builder methods are called)
 * <p/>
 * <b>Example 2:</b> Asynchronously concatenate the output of two routines.
 * <pre>
 *     <code>
 *
 *         doSomething1.callAsync(doSomething2.callAsync())).eventually().readAllInto(results);
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
 *                  &#64;Pass(Result.class) OutputChannel&lt;Result&gt; result1,
 *                  &#64;Pass(Result.class) OutputChannel&lt;Result&gt; result2);
 *         }
 *
 *         final AsyncCallback callback = JRoutine.on(myCallback)
 *                                                .buildProxy(AsyncCallback.class);
 *
 *         callback.onResults(doSomething1.callAsync(), doSomething2.callAsync());
 *     </code>
 * </pre>
 * Where the object <code>myCallback</code> implements a method <code>public void onResults(Result
 * result1, Result result2)</code>.
 * <p/>
 * <b>Example 4:</b> Asynchronously feed a routine from a different thread.
 * <pre>
 *     <code>
 *
 *         final StandaloneChannel&lt;Result&gt; channel =
 *                 JRoutine.on().&lt;Result&gt;buildChannel();
 *
 *         new Thread() {
 *
 *             &#64;Override
 *             public void run() {
 *
 *                 channel.input().pass(new Result()).close();
 *             }
 *
 *         }.start();
 *
 *         final Routine&lt;Result, Result&gt; routine =
 *                  JRoutine.&lt;Result&gt;on().buildRoutine();
 *
 *         routine.callAsync(channel.output()).eventually().readAllInto(results);
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @see com.bmd.jrt.annotation.Bind
 * @see com.bmd.jrt.annotation.Pass
 * @see com.bmd.jrt.annotation.Share
 * @see com.bmd.jrt.annotation.Timeout
 * @see com.bmd.jrt.annotation.Wrap
 */
public class JRoutine {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutine() {

    }

    /**
     * Returns a routine builder wrapping the specified target class.
     *
     * @param target the target class.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    @Nonnull
    public static ClassRoutineBuilder on(@Nonnull final Class<?> target) {

        return new ClassRoutineBuilder(target);
    }

    /**
     * Returns a routine builder wrapping the specified invocation class token.
     *
     * @param invocationFactory the invocation factory.
     * @param <INPUT>           the input data type.
     * @param <OUTPUT>          the output data type.
     * @return the routine builder instance.
     * @throws java.lang.NullPointerException if the factory is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationRoutineBuilder<INPUT, OUTPUT> on(
            @Nonnull final InvocationFactory<INPUT, OUTPUT> invocationFactory) {

        return new InvocationRoutineBuilder<INPUT, OUTPUT>(invocationFactory);
    }

    /**
     * Returns a routine builder wrapping the specified target object.
     *
     * @param target the target object.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    @Nonnull
    public static ObjectRoutineBuilder on(@Nonnull final Object target) {

        return new ObjectRoutineBuilder(target);
    }

    /**
     * Returns a routine builder wrapping a weak reference to the specified target object.
     *
     * @param target the target object.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    @Nonnull
    public static ObjectRoutineBuilder onWeak(@Nonnull final Object target) {

        return onWeak(new WeakReference<Object>(target));
    }

    /**
     * Returns a routine builder wrapping a weak reference to the specified target object.
     *
     * @param target the reference to the target object.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    @Nonnull
    public static ObjectRoutineBuilder onWeak(@Nonnull final WeakReference<?> target) {

        return new ObjectRoutineBuilder(target);
    }

    /**
     * Returns a standalone channel builder.
     *
     * @return the standalone channel builder instance.
     */
    @Nonnull
    public static StandaloneChannelBuilder standalone() {

        return new StandaloneChannelBuilder();
    }
}
