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

package com.github.dm.jrt.object;

import com.github.dm.jrt.object.builder.ObjectRoutineBuilder;

import org.jetbrains.annotations.NotNull;

/**
 * This utility class extends the base factory of routine builders.
 * <p/>
 * The class provides an additional way to build a routine, based on the asynchronous invocation of
 * a method of an existing class or object via reflection.<br/>
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
 * <b>Some usage examples</b>
 * <p/>
 * <b>Example 1:</b> Asynchronously get the output of two routines.
 * <pre>
 *     <code>
 *
 *         public interface AsyncCallback {
 *
 *             public void onResults(
 *                  &#64;AsyncIn(Result.class) OutputChannel&lt;Result&gt; result1,
 *                  &#64;AsyncIn(Result.class) OutputChannel&lt;Result&gt; result2);
 *         }
 *
 *         final AsyncCallback callback = JRoutineObject.on(instance(myCallback))
 *                                                      .buildProxy(AsyncCallback.class);
 *         callback.onResults(doSomething1.asyncCall(), doSomething2.asyncCall());
 *     </code>
 * </pre>
 * Where the object <code>myCallback</code> implements a method
 * <code>public void onResults(Result result1, Result result2)</code>.
 * <p/>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @see <a href='{@docRoot}/com/github/dm/jrt/object/annotation/package-summary.html'>
 * Annotations</a>
 */
public class JRoutineObject {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineObject() {

    }

    /**
     * Returns a routine builder wrapping the specified target object.
     * <p/>
     * Note that it is responsibility of the caller to retain a strong reference to the target
     * instance to prevent it from being garbage collected.<br/>
     * Note also that the invocation input data will be cached, and the results will be produced
     * only after the invocation channel is closed, so be sure to avoid streaming inputs in
     * order to prevent starvation or out of memory errors.
     *
     * @param target the invocation target.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the specified object class represents an
     *                                            interface.
     */
    @NotNull
    public static ObjectRoutineBuilder on(@NotNull final InvocationTarget<?> target) {

        return new DefaultObjectRoutineBuilder(target);
    }
}
