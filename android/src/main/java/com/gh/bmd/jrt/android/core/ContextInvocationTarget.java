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
package com.gh.bmd.jrt.android.core;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.gh.bmd.jrt.android.builder.FactoryContext;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.core.InvocationTarget;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.util.Reflection;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.util.Reflection.findConstructor;

/**
 * // TODO: 21/08/15 javadoc
 * <p/>
 * Created by davide-maestroni on 21/08/15.
 */
public abstract class ContextInvocationTarget implements Parcelable {

    private ContextInvocationTarget() {

    }

    @Nonnull
    public static ContextInvocationTarget targetClass(@Nonnull final Class<?> targetClass) {

        return new ClassContextInvocationTarget(targetClass);
    }

    @Nonnull
    public static ContextInvocationTarget targetObject(@Nonnull final Class<?> targetClass,
            @Nullable final Object... factoryArgs) {

        return new ObjectContextInvocationTarget(targetClass, factoryArgs);
    }

    @Nonnull
    public static ContextInvocationTarget targetObject(@Nonnull final Class<?> targetClass) {

        return new ObjectContextInvocationTarget(targetClass, null);
    }

    @Nonnull
    public abstract InvocationTarget getInvocationTarget(@Nonnull final Context context);

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @Nonnull
    public abstract Class<?> getTargetClass();

    private static class ClassContextInvocationTarget extends ContextInvocationTarget {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ClassContextInvocationTarget> CREATOR =
                new Creator<ClassContextInvocationTarget>() {

                    public ClassContextInvocationTarget createFromParcel(
                            @Nonnull final Parcel source) {

                        return new ClassContextInvocationTarget(source);
                    }

                    @Nonnull
                    public ClassContextInvocationTarget[] newArray(final int size) {

                        return new ClassContextInvocationTarget[size];
                    }
                };

        private final Class<?> mTargetClass;

        private ClassContextInvocationTarget(@Nonnull final Parcel source) {

            this((Class<?>) source.readSerializable());
        }

        @SuppressWarnings("ConstantConditions")
        private ClassContextInvocationTarget(@Nonnull final Class<?> targetClass) {

            if (targetClass.isPrimitive()) {

                // The parceling of primitive classes is broken...
                throw new IllegalArgumentException("the target class cannot be primitive");
            }

            mTargetClass = targetClass;
        }

        public int describeContents() {

            return 0;
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {

                return true;
            }

            if (!(o instanceof ClassContextInvocationTarget)) {

                return false;
            }

            final ClassContextInvocationTarget that = (ClassContextInvocationTarget) o;
            return mTargetClass.equals(that.mTargetClass);
        }

        @Nonnull
        @Override
        public InvocationTarget getInvocationTarget(@Nonnull final Context context) {

            return InvocationTarget.targetClass(mTargetClass);
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            return mTargetClass.hashCode();
        }


        @Nonnull
        @Override
        public Class<?> getTargetClass() {

            return mTargetClass;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
        }
    }

    private static class ObjectContextInvocationTarget extends ContextInvocationTarget {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ObjectContextInvocationTarget> CREATOR =
                new Creator<ObjectContextInvocationTarget>() {

                    public ObjectContextInvocationTarget createFromParcel(
                            @Nonnull final Parcel source) {

                        return new ObjectContextInvocationTarget(source);
                    }

                    @Nonnull
                    public ObjectContextInvocationTarget[] newArray(final int size) {

                        return new ObjectContextInvocationTarget[size];
                    }
                };

        private final Object[] mFactoryArgs;

        private final Object mMutex = new Object();

        private final Class<?> mTargetClass;

        private Object mTarget;

        private ObjectContextInvocationTarget(@Nonnull final Parcel source) {

            this((Class<?>) source.readSerializable(),
                 source.readArray(InvocationFactoryTarget.class.getClassLoader()));
        }

        @SuppressWarnings("ConstantConditions")
        private ObjectContextInvocationTarget(@Nonnull final Class<?> targetClass,
                @Nullable final Object[] factoryArgs) {

            if (targetClass.isPrimitive()) {

                // The parceling of primitive classes is broken...
                throw new IllegalArgumentException("the target class cannot be primitive");
            }

            mTargetClass = targetClass;
            mFactoryArgs = (factoryArgs != null) ? factoryArgs.clone() : Reflection.NO_ARGS;
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {

                return true;
            }

            if (!(o instanceof ObjectContextInvocationTarget)) {

                return false;
            }

            final ObjectContextInvocationTarget that = (ObjectContextInvocationTarget) o;
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
        @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
        public InvocationTarget getInvocationTarget(@Nonnull final Context context) {

            Object target;

            synchronized (mMutex) {

                target = mTarget;

                if (target == null) {

                    final Class<?> targetClass = mTargetClass;
                    final Object[] factoryArgs = mFactoryArgs;

                    if (context instanceof FactoryContext) {

                        // The only safe way is to synchronize the factory using the very same
                        // instance
                        synchronized (context) {

                            target =
                                    ((FactoryContext) context).geInstance(targetClass, factoryArgs);
                        }
                    }

                    if (target == null) {

                        try {

                            target = findConstructor(targetClass, factoryArgs).newInstance(
                                    factoryArgs);

                        } catch (final Throwable t) {

                            throw InvocationException.wrapIfNeeded(t);
                        }

                    } else if (!targetClass.isInstance(target)) {

                        throw new RoutineException(
                                target + " is not an instance of " + targetClass.getName());
                    }

                    mTarget = target;
                }
            }

            return InvocationTarget.targetObject(target);
        }

        @Nonnull
        @Override
        public Class<?> getTargetClass() {

            return mTargetClass;
        }

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
            dest.writeArray(mFactoryArgs);
        }
    }
}
