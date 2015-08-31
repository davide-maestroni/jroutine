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
package com.github.dm.jrt.android.core;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.android.invocation.ContextInvocation;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Reflection;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.dm.jrt.util.ClassToken.tokenOf;

/**
 * Class representing a context invocation factory target.
 * <p/>
 * Created by davide-maestroni on 20/08/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class InvocationFactoryTarget<INPUT, OUTPUT> implements Parcelable {

    /**
     * Avoid direct instantiation.
     */
    private InvocationFactoryTarget() {

    }

    /**
     * Returns a target based on the specified invocation class.
     *
     * @param targetClass the target invocation class.
     * @return the invocation factory target.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> targetClass) {

        return targetInvocation(targetClass, (Object[]) null);
    }

    /**
     * Returns a target based on the specified invocation class.
     *
     * @param targetClass the target invocation class.
     * @param factoryArgs the invocation factory arguments.
     * @return the invocation factory target.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> targetClass,
            @Nullable final Object... factoryArgs) {

        return new DefaultInvocationFactoryTarget<INPUT, OUTPUT>(targetClass, factoryArgs);
    }

    /**
     * Returns a target based on the specified invocation token.
     *
     * @param targetToken the target invocation token.
     * @return the invocation factory target.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> targetToken) {

        return targetInvocation(targetToken.getRawClass());
    }

    /**
     * Returns a target based on the specified invocation token.
     *
     * @param targetToken the target invocation token.
     * @param factoryArgs the invocation factory arguments.
     * @return the invocation factory target.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> targetToken,
            @Nullable final Object... factoryArgs) {

        return targetInvocation(targetToken.getRawClass(), factoryArgs);
    }

    /**
     * Returns a target based on the specified invocation.
     *
     * @param targetInvocation the target invocation.
     * @return the invocation factory target.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ContextInvocation<INPUT, OUTPUT> targetInvocation) {

        return targetInvocation(tokenOf(targetInvocation));
    }

    /**
     * Returns a target based on the specified invocation.
     *
     * @param targetInvocation the target invocation.
     * @param factoryArgs      the invocation factory arguments.
     * @return the invocation factory target.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ContextInvocation<INPUT, OUTPUT> targetInvocation,
            @Nullable final Object... factoryArgs) {

        return targetInvocation(tokenOf(targetInvocation), factoryArgs);
    }

    @Nonnull
    public abstract Object[] getFactoryArgs();

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @Nonnull
    public abstract Class<? extends ContextInvocation<INPUT, OUTPUT>> getInvocationClass();

    /**
     * Invocation factory target implementation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class DefaultInvocationFactoryTarget<INPUT, OUTPUT>
            extends InvocationFactoryTarget<INPUT, OUTPUT> {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<DefaultInvocationFactoryTarget> CREATOR =
                new Creator<DefaultInvocationFactoryTarget>() {

                    public DefaultInvocationFactoryTarget createFromParcel(
                            @Nonnull final Parcel source) {

                        return new DefaultInvocationFactoryTarget(source);
                    }

                    @Nonnull
                    public DefaultInvocationFactoryTarget[] newArray(final int size) {

                        return new DefaultInvocationFactoryTarget[size];
                    }
                };

        private final Object[] mFactoryArgs;

        private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mTargetClass;

        /**
         * Constructor.
         *
         * @param source the source parcel.
         */
        @SuppressWarnings("unchecked")
        private DefaultInvocationFactoryTarget(@Nonnull final Parcel source) {

            this((Class<? extends ContextInvocation<INPUT, OUTPUT>>) source.readSerializable(),
                 source.readArray(InvocationFactoryTarget.class.getClassLoader()));
        }

        /**
         * Constructor.
         *
         * @param targetClass the target invocation class.
         * @param factoryArgs the invocation factory arguments.
         */
        @SuppressWarnings("ConstantConditions")
        private DefaultInvocationFactoryTarget(
                @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> targetClass,
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

            // AUTO-GENERATED CODE
            if (this == o) {

                return true;
            }

            if (!(o instanceof DefaultInvocationFactoryTarget)) {

                return false;
            }

            final DefaultInvocationFactoryTarget<?, ?> that =
                    (DefaultInvocationFactoryTarget<?, ?>) o;
            return Arrays.equals(mFactoryArgs, that.mFactoryArgs) && mTargetClass.equals(
                    that.mTargetClass);
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            int result = Arrays.hashCode(mFactoryArgs);
            result = 31 * result + mTargetClass.hashCode();
            return result;
        }

        @Nonnull
        @Override
        public Object[] getFactoryArgs() {

            return mFactoryArgs;
        }

        @Nonnull
        @Override
        public Class<? extends ContextInvocation<INPUT, OUTPUT>> getInvocationClass() {

            return mTargetClass;
        }
    }
}
