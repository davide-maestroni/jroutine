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

import com.bmd.jrt.common.RoutineException;

import java.lang.reflect.Method;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Exception indicating that an execution of an object method thrown an exception.
 * <p/>
 * Created by davide on 10/3/14.
 */
public class RoutineInvocationException extends RoutineException {

    private transient final Method mMethod;

    private final Object mTarget;

    /**
     * Constructor.
     *
     * @param cause  the wrapped exception.
     * @param target the target of the invocation.
     * @param method the method throwing the exception.
     * @throws NullPointerException if the specified method is null.
     */
    @SuppressWarnings("ConstantConditions")
    public RoutineInvocationException(@Nullable final Throwable cause,
            @Nullable final Object target, @NonNull final Method method) {

        super(cause);

        if (method == null) {

            throw new NullPointerException("the method must not be null");
        }

        mTarget = target;
        mMethod = method;
    }

    /**
     * The invoked method.
     *
     * @return the method.
     */
    @Nullable
    public Method getMethod() {

        return mMethod;
    }

    /**
     * The target instance or null if the invoked method is static.
     *
     * @return the target object.
     */
    @Nullable
    public Object getTarget() {

        return mTarget;
    }
}