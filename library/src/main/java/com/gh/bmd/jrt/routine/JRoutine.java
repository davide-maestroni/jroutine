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
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.builder.ClassRoutineBuilder;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.StandaloneChannelBuilder;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.Invocations;
import com.gh.bmd.jrt.invocation.Invocations.Function0;
import com.gh.bmd.jrt.invocation.Invocations.Function1;
import com.gh.bmd.jrt.invocation.Invocations.Function2;
import com.gh.bmd.jrt.invocation.Invocations.Function3;
import com.gh.bmd.jrt.invocation.Invocations.Function4;
import com.gh.bmd.jrt.invocation.Invocations.FunctionN;

import javax.annotation.Nonnull;

/**
 * This utility class represents the entry point to the framework functionalities by acting as a
 * factory of routine builders.
 * <p/>
 * There are mainly two ways to create a routine object:
 * <p/>
 * <b>Routine by invocation customization</b><br/>
 * The first approach consists in implementing an invocation object.
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
 * channels, so that data are transferred asynchronously, avoiding the need to block execution while
 * waiting for them to be available.<br/>
 * Finally, it also possible to create a wrapper class to enable asynchronous invocation of methods,
 * through annotation pre-processing and compile-time code generation. In order to activate the
 * processing of annotations, it is simply necessary to include the "jroutine-processor" artifact
 * or module in the project dependencies.
 * <p/>
 * This class provides also a way to build standalone channel instances, which can be used to pass
 * data without the need to start a routine invocation.
 * <p/>
 * <b>Some usage examples</b>
 * <p/>
 * <b>Example 1:</b> Asynchronously merge the output of two routines.
 * <pre>
 *     <code>
 *
 *         final StandaloneChannel&lt;Result&gt; channel = JRoutine.standalone().buildChannel();
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
 * Where the object <code>myCallback</code> implements a method
 * <code>public void onResults(Result result1, Result result2)</code>.
 * <p/>
 * <b>Example 4:</b> Asynchronously feed a routine from a different thread.
 * <pre>
 *     <code>
 *
 *         final StandaloneChannel&lt;Result&gt; channel = JRoutine.standalone().buildChannel();
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
 *                  JRoutine.&lt;Result&gt;on(PassingInvocation.&lt;Result&gt;factoryOf())
 *                          .buildRoutine();
 *
 *         routine.callAsync(channel.output()).eventually().readAllInto(results);
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @see com.gh.bmd.jrt.annotation.Bind
 * @see com.gh.bmd.jrt.annotation.Pass
 * @see com.gh.bmd.jrt.annotation.Share
 * @see com.gh.bmd.jrt.annotation.Timeout
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

        return new DefaultClassRoutineBuilder(target);
    }

    /**
     * Returns a routine builder based on the specified invocation factory.
     * <p/>
     * The invocation instance is created only when needed, by passing the specified arguments to
     * the constructor. Note that the arguments objects should be immutable or, at least, never
     * shared inside and outside the routine in order to avoid concurrency issues.
     *
     * @param factory  the invocation factory.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine builder instance.
     * @throws java.lang.NullPointerException if the specified factory is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> RoutineBuilder<INPUT, OUTPUT> on(
            @Nonnull final InvocationFactory<INPUT, OUTPUT> factory) {

        return new DefaultRoutineBuilder<INPUT, OUTPUT>(factory);
    }

    /**
     * Returns a routine builder based on the specified invocation class token.
     * <p/>
     * The invocation instance is created through reflection only when needed.
     *
     * @param token    the invocation class token.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine builder instance.
     * @throws java.lang.NullPointerException if the specified factory is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> RoutineBuilder<INPUT, OUTPUT> on(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> token) {

        return on(Invocations.factoryOf(token));
    }

    /**
     * Returns a routine builder wrapping a weak reference to the specified target object.<br/>
     * Note that it is responsibility of the caller to retain a strong reference to the target
     * instance to prevent it from being garbage collected.
     *
     * @param target the target object.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    @Nonnull
    public static ObjectRoutineBuilder on(@Nonnull final Object target) {

        return new DefaultObjectRoutineBuilder(target);
    }

    /**
     * Returns a routine builder based on the specified function.<br/>
     * The inputs will be passed to the function only when the invocation completes, so, it is up to
     * the caller to verify that the correct number of parameters is passed to the input channel in
     * order to avoid unexpected behaviors.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    public static <OUTPUT> RoutineBuilder<Void, OUTPUT> onFunction(
            @Nonnull final Function0<? extends OUTPUT> function) {

        return FunctionRoutineBuilder.fromFunction(function);
    }

    /**
     * Returns a routine builder based on the specified function.<br/>
     * The inputs will be passed to the function only when the invocation completes, so, it is up to
     * the caller to verify that the correct number of parameters is passed to the input channel in
     * order to avoid unexpected behaviors.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> RoutineBuilder<INPUT, OUTPUT> onFunction(
            @Nonnull final Function1<INPUT, ? extends OUTPUT> function) {

        return FunctionRoutineBuilder.fromFunction(function);
    }

    /**
     * Returns a routine builder based on the specified function.<br/>
     * The inputs will be passed to the function only when the invocation completes, so, it is up to
     * the caller to verify that the correct number of parameters is passed to the input channel in
     * order to avoid unexpected behaviors.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    public static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, OUTPUT>
    RoutineBuilder<INPUT, OUTPUT> onFunction(
            @Nonnull final Function2<INPUT1, INPUT2, ? extends OUTPUT> function) {

        return FunctionRoutineBuilder.fromFunction(function);
    }

    /**
     * Returns a routine builder based on the specified function.<br/>
     * The inputs will be passed to the function only when the invocation completes, so, it is up to
     * the caller to verify that the correct number of parameters is passed to the input channel in
     * order to avoid unexpected behaviors.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <INPUT3> the third parameter type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    public static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT,
            OUTPUT> RoutineBuilder<INPUT, OUTPUT> onFunction(
            @Nonnull final Function3<INPUT1, INPUT2, INPUT3, ? extends OUTPUT> function) {

        return FunctionRoutineBuilder.fromFunction(function);
    }

    /**
     * Returns a routine builder based on the specified function.<br/>
     * The inputs will be passed to the function only when the invocation completes, so, it is up to
     * the caller to verify that the correct number of parameters is passed to the input channel in
     * order to avoid unexpected behaviors.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <INPUT3> the third parameter type.
     * @param <INPUT4> the fourth parameter type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    public static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT,
            INPUT4 extends INPUT, OUTPUT> RoutineBuilder<INPUT, OUTPUT> onFunction(
            @Nonnull final Function4<INPUT1, INPUT2, INPUT3, INPUT4, ? extends OUTPUT> function) {

        return FunctionRoutineBuilder.fromFunction(function);
    }

    /**
     * Returns a routine builder based on the specified function.<br/>
     * The inputs will be passed to the function only when the invocation completes, so, it is up to
     * the caller to verify that the correct number of parameters is passed to the input channel in
     * order to avoid unexpected behaviors.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified function is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> RoutineBuilder<INPUT, OUTPUT> onFunction(
            @Nonnull final FunctionN<INPUT, ? extends OUTPUT> function) {

        return FunctionRoutineBuilder.fromFunction(function);
    }

    /**
     * Returns a routine builder based on the specified procedure.<br/>
     * The inputs will be passed to the procedure only when the invocation completes, so, it is up
     * to the caller to verify that the correct number of parameters is passed to the input channel
     * in order to avoid unexpected behaviors.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    public static RoutineBuilder<Void, Void> onProcedure(@Nonnull final Function0<Void> procedure) {

        return FunctionRoutineBuilder.fromProcedure(procedure);
    }

    /**
     * Returns a routine builder based on the specified procedure.<br/>
     * The inputs will be passed to the procedure only when the invocation completes, so, it is up
     * to the caller to verify that the correct number of parameters is passed to the input channel
     * in order to avoid unexpected behaviors.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @param <INPUT1>  the first parameter type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    public static <INPUT, INPUT1 extends INPUT> RoutineBuilder<INPUT, Void> onProcedure(
            @Nonnull final Function1<INPUT1, Void> procedure) {

        return FunctionRoutineBuilder.fromProcedure(procedure);
    }

    /**
     * Returns a routine builder based on the specified procedure.<br/>
     * The inputs will be passed to the procedure only when the invocation completes, so, it is up
     * to the caller to verify that the correct number of parameters is passed to the input channel
     * in order to avoid unexpected behaviors.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @param <INPUT1>  the first parameter type.
     * @param <INPUT2>  the second parameter type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    public static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT> RoutineBuilder<INPUT, Void>
    onProcedure(
            @Nonnull final Function2<INPUT1, INPUT2, Void> procedure) {

        return FunctionRoutineBuilder.fromProcedure(procedure);
    }

    /**
     * Returns a routine builder based on the specified procedure.<br/>
     * The inputs will be passed to the procedure only when the invocation completes, so, it is up
     * to the caller to verify that the correct number of parameters is passed to the input channel
     * in order to avoid unexpected behaviors.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @param <INPUT1>  the first parameter type.
     * @param <INPUT2>  the second parameter type.
     * @param <INPUT3>  the third parameter type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    public static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT>
    RoutineBuilder<INPUT, Void> onProcedure(
            @Nonnull final Function3<INPUT1, INPUT2, INPUT3, Void> procedure) {

        return FunctionRoutineBuilder.fromProcedure(procedure);
    }

    /**
     * Returns a routine builder based on the specified procedure.<br/>
     * The inputs will be passed to the procedure only when the invocation completes, so, it is up
     * to the caller to verify that the correct number of parameters is passed to the input channel
     * in order to avoid unexpected behaviors.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @param <INPUT1>  the first parameter type.
     * @param <INPUT2>  the second parameter type.
     * @param <INPUT3>  the third parameter type.
     * @param <INPUT4>  the fourth parameter type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    public static <INPUT, INPUT1 extends INPUT, INPUT2 extends INPUT, INPUT3 extends INPUT,
            INPUT4 extends INPUT> RoutineBuilder<INPUT, Void> onProcedure(
            @Nonnull final Function4<INPUT1, INPUT2, INPUT3, INPUT4, Void> procedure) {

        return FunctionRoutineBuilder.fromProcedure(procedure);
    }

    /**
     * Returns a routine builder based on the specified procedure.<br/>
     * The inputs will be passed to the procedure only when the invocation completes, so, it is up
     * to the caller to verify that the correct number of parameters is passed to the input channel
     * in order to avoid unexpected behaviors.<br/>
     * The procedure output will be discarded.
     * <p/>
     * Note that the procedure object must be stateless in order to avoid concurrency issues.
     *
     * @param procedure the procedure instance.
     * @param <INPUT>   the input data type.
     * @return the builder instance.
     * @throws java.lang.NullPointerException if the specified procedure is null.
     */
    @Nonnull
    public static <INPUT> RoutineBuilder<INPUT, Void> onProcedure(
            @Nonnull final FunctionN<INPUT, Void> procedure) {

        return FunctionRoutineBuilder.fromProcedure(procedure);
    }

    /**
     * Returns a standalone channel builder.
     *
     * @return the standalone channel builder instance.
     */
    @Nonnull
    public static StandaloneChannelBuilder standalone() {

        return new DefaultStandaloneChannelBuilder();
    }
}
