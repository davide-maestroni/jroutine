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
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Exception indicating that an invocation of an object method thrown an exception.
 * <p/>
 * Created by davide on 10/3/14.
 */
public class RoutineInvocationException extends RoutineException {

    private final String mMethodName;

    private final Class<?>[] mMethodParameterTypes;

    private final Object mTarget;

    private final Class<?> mTargetClass;

    /**
     * Constructor.
     *
     * @param cause          the wrapped exception.
     * @param target         the target of the invocation.
     * @param targetClass    the target class.
     * @param methodName     the method name.
     * @param parameterTypes the method parameter types.
     * @throws NullPointerException if the specified method is null.
     */
    @SuppressWarnings("ConstantConditions")
    public RoutineInvocationException(@Nullable final Throwable cause,
            @Nullable final Object target, @Nonnull final Class<?> targetClass,
            @Nonnull final String methodName, @Nonnull final Class<?>... parameterTypes) {

        super(cause);

        if (targetClass == null) {

            throw new NullPointerException("the target class must not be null");
        }

        if (methodName == null) {

            throw new NullPointerException("the method name must not be null");
        }

        if (parameterTypes == null) {

            throw new NullPointerException("the list of parameter types must not be null");
        }

        mTarget = target;
        mTargetClass = targetClass;
        mMethodName = methodName;
        mMethodParameterTypes = parameterTypes;
    }

    /**
     * Gets the invoked method.
     *
     * @return the method.
     * @throws NoSuchMethodException if the method is not found.
     */
    @Nullable
    public Method getMethod() throws NoSuchMethodException {

        return mTargetClass.getMethod(mMethodName, mMethodParameterTypes);
    }

    /**
     * Gets the invoked method name.
     *
     * @return the method name.
     */
    @Nonnull
    public String getMethodName() {

        return mMethodName;
    }

    /**
     * Gets the list of the invoked method parameter types.
     *
     * @return the list of parameter types.
     */
    @Nonnull
    public List<Class<?>> getMethodParameterTypes() {

        return Arrays.asList(mMethodParameterTypes);
    }

    /**
     * Gets the target instance or null if the invoked method is static.
     *
     * @return the target object.
     */
    @Nullable
    public Object getTarget() {

        return mTarget;
    }

    /**
     * Gets the target class.
     *
     * @return the target class.
     */
    @Nonnull
    public Class<?> getTargetClass() {

        return mTargetClass;
    }
}