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

import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.invocation.Invocation;

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
 * channels, so that data are transferred asynchronously avoiding the need to block waiting for
 * them to be available.<br/>
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @see com.bmd.jrt.annotation.Async
 * @see com.bmd.jrt.annotation.AsyncParameters
 * @see com.bmd.jrt.annotation.AsyncResult
 */
public class JavaRoutine {

    /**
     * Avoid direct instantiation.
     */
    protected JavaRoutine() {

    }

    /**
     * Returns a routine builder wrapping the specified invocation class token.
     *
     * @param classToken the invocation class token.
     * @param <INPUT>    the input type.
     * @param <OUTPUT>   the output type.
     * @return the routine builder instance.
     * @throws NullPointerException if the class token is null.
     */
    @Nonnull
    public static <INPUT, OUTPUT> RoutineBuilder<INPUT, OUTPUT> on(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> classToken) {

        return new RoutineBuilder<INPUT, OUTPUT>(classToken);
    }

    /**
     * Returns a routine builder wrapping the specified target object.
     *
     * @param target the target object.
     * @return the routine builder instance.
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate name in the annotations is
     *                                  detected.
     */
    @Nonnull
    public static ObjectRoutineBuilder on(@Nonnull final Object target) {

        return new ObjectRoutineBuilder(target);
    }

    /**
     * Returns a routine builder wrapping the specified target class.
     *
     * @param target the target class.
     * @return the routine builder instance.
     * @throws NullPointerException     if the specified target is null.
     * @throws IllegalArgumentException if a duplicate name in the annotations is
     *                                  detected.
     */
    @Nonnull
    public static ClassRoutineBuilder on(@Nonnull final Class<?> target) {

        return new ClassRoutineBuilder(target);
    }
}