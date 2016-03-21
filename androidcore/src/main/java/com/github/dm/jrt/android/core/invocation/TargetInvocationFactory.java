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

package com.github.dm.jrt.android.core.invocation;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class representing a context invocation factory target.
 * <p/>
 * Created by davide-maestroni on 08/20/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class TargetInvocationFactory<IN, OUT> implements Parcelable {

    /**
     * Avoid direct instantiation.
     */
    private TargetInvocationFactory() {

    }

    // TODO: 3/21/16 unit tests

    /**
     * Returns a target based on the specified invocation class.
     *
     * @param targetClass the target invocation class.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Class<? extends Invocation<IN, OUT>> targetClass) {

        return factoryFrom(targetClass, (Object[]) null);
    }

    /**
     * Returns a target based on the specified invocation class.
     *
     * @param targetClass the target invocation class.
     * @param factoryArgs the invocation factory arguments.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Class<? extends Invocation<IN, OUT>> targetClass,
            @Nullable final Object... factoryArgs) {

        if (targetClass == null) {
            throw new NullPointerException("the invocation class must not be null");
        }

        return new DefaultTargetInvocationFactory<IN, OUT>(ContextInvocationWrapper.class,
                asArgs(targetClass, factoryArgs));
    }

    /**
     * Returns a target based on the specified invocation token.
     *
     * @param targetToken the target invocation token.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> targetToken) {

        return factoryFrom(targetToken.getRawClass());
    }

    /**
     * Returns a target based on the specified invocation token.
     *
     * @param targetToken the target invocation token.
     * @param factoryArgs the invocation factory arguments.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> targetToken,
            @Nullable final Object... factoryArgs) {

        return factoryFrom(targetToken.getRawClass(), factoryArgs);
    }

    /**
     * Returns a target based on the specified invocation.
     *
     * @param targetInvocation the target invocation.
     * @param <IN>             the input data type.
     * @param <OUT>            the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Invocation<IN, OUT> targetInvocation) {

        return factoryFrom(tokenOf(targetInvocation));
    }

    /**
     * Returns a target based on the specified invocation class.
     *
     * @param targetClass the target invocation class.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends ContextInvocation<IN, OUT>> targetClass) {

        return factoryOf(targetClass, (Object[]) null);
    }

    /**
     * Returns a target based on the specified invocation class.
     *
     * @param targetClass the target invocation class.
     * @param factoryArgs the invocation factory arguments.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends ContextInvocation<IN, OUT>> targetClass,
            @Nullable final Object... factoryArgs) {

        return new DefaultTargetInvocationFactory<IN, OUT>(targetClass, factoryArgs);
    }

    /**
     * Returns a target based on the specified invocation token.
     *
     * @param targetToken the target invocation token.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends ContextInvocation<IN, OUT>> targetToken) {

        return factoryOf(targetToken.getRawClass());
    }

    /**
     * Returns a target based on the specified invocation token.
     *
     * @param targetToken the target invocation token.
     * @param factoryArgs the invocation factory arguments.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends ContextInvocation<IN, OUT>> targetToken,
            @Nullable final Object... factoryArgs) {

        return factoryOf(targetToken.getRawClass(), factoryArgs);
    }

    /**
     * Returns a target based on the specified invocation.
     *
     * @param targetInvocation the target invocation.
     * @param <IN>             the input data type.
     * @param <OUT>            the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ContextInvocation<IN, OUT> targetInvocation) {

        return factoryOf(tokenOf(targetInvocation));
    }

    /**
     * Returns a target based on the specified invocation.
     *
     * @param targetInvocation the target invocation.
     * @param factoryArgs      the invocation factory arguments.
     * @param <IN>             the input data type.
     * @param <OUT>            the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ContextInvocation<IN, OUT> targetInvocation,
            @Nullable final Object... factoryArgs) {

        return factoryOf(tokenOf(targetInvocation), factoryArgs);
    }

    /**
     * Returns the factory arguments.
     *
     * @return the factory arguments.
     */
    @NotNull
    public abstract Object[] getFactoryArgs();

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @NotNull
    public abstract Class<? extends ContextInvocation<IN, OUT>> getInvocationClass();

    /**
     * Invocation factory target implementation.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DefaultTargetInvocationFactory<IN, OUT>
            extends TargetInvocationFactory<IN, OUT> {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<DefaultTargetInvocationFactory> CREATOR =
                new Creator<DefaultTargetInvocationFactory>() {

                    public DefaultTargetInvocationFactory createFromParcel(final Parcel source) {

                        return new DefaultTargetInvocationFactory(source);
                    }

                    public DefaultTargetInvocationFactory[] newArray(final int size) {

                        return new DefaultTargetInvocationFactory[size];
                    }
                };

        private final Object[] mFactoryArgs;

        private final Class<? extends ContextInvocation<IN, OUT>> mTargetClass;

        /**
         * Constructor.
         *
         * @param source the source parcel.
         */
        @SuppressWarnings("unchecked")
        private DefaultTargetInvocationFactory(@NotNull final Parcel source) {

            this((Class<? extends ContextInvocation<IN, OUT>>) source.readSerializable(),
                    source.readArray(TargetInvocationFactory.class.getClassLoader()));
        }

        /**
         * Constructor.
         *
         * @param targetClass the target invocation class.
         * @param factoryArgs the invocation factory arguments.
         */
        @SuppressWarnings("ConstantConditions")
        private DefaultTargetInvocationFactory(
                @NotNull final Class<? extends ContextInvocation<IN, OUT>> targetClass,
                @Nullable final Object[] factoryArgs) {

            if (targetClass == null) {
                throw new NullPointerException("the target class must not be null");
            }

            mTargetClass = targetClass;
            mFactoryArgs = (factoryArgs != null) ? factoryArgs.clone() : Reflection.NO_ARGS;
        }

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
            dest.writeArray(mFactoryArgs);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof DefaultTargetInvocationFactory)) {
                return false;
            }

            final DefaultTargetInvocationFactory<?, ?> that =
                    (DefaultTargetInvocationFactory<?, ?>) o;
            return Arrays.deepEquals(mFactoryArgs, that.mFactoryArgs) && mTargetClass.equals(
                    that.mTargetClass);
        }

        @Override
        public int hashCode() {

            int result = Arrays.deepHashCode(mFactoryArgs);
            result = 31 * result + mTargetClass.hashCode();
            return result;
        }

        @NotNull
        @Override
        public Object[] getFactoryArgs() {

            return mFactoryArgs;
        }

        @NotNull
        @Override
        public Class<? extends ContextInvocation<IN, OUT>> getInvocationClass() {

            return mTargetClass;
        }
    }
}
