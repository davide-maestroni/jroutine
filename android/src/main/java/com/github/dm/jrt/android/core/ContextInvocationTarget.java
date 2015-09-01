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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.android.builder.FactoryContext;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.InvocationTarget;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.util.Reflection;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.dm.jrt.util.Reflection.findConstructor;

/**
 * Class representing a context invocation target.
 * <p/>
 * Created by davide-maestroni on 08/21/2015.
 */
public abstract class ContextInvocationTarget implements Parcelable {

    /**
     * Avoid direct instantiation.
     */
    private ContextInvocationTarget() {

    }

    /**
     * Returns a target based on the specified class.
     *
     * @param targetClass the target class.
     * @return the context invocation target.
     */
    @Nonnull
    public static ContextInvocationTarget targetClass(@Nonnull final Class<?> targetClass) {

        return new ClassContextInvocationTarget(targetClass);
    }

    /**
     * Returns a target based on the specified instance.<br/>
     * No argument will be passed to the object factory.
     *
     * @param targetClass the target class.
     * @return the context invocation target.
     */
    @Nonnull
    public static ContextInvocationTarget targetObject(@Nonnull final Class<?> targetClass) {

        return targetObject(targetClass, (Object[]) null);
    }

    /**
     * Returns a target based on the specified instance.
     *
     * @param targetClass the target class.
     * @param factoryArgs the object factory arguments.
     * @return the context invocation target.
     */
    @Nonnull
    public static ContextInvocationTarget targetObject(@Nonnull final Class<?> targetClass,
            @Nullable final Object... factoryArgs) {

        return new ObjectContextInvocationTarget(targetClass, factoryArgs);
    }

    /**
     * Returns an invocation target based on the specified context.<br/>
     * Note that a new instance will be returned each time the method is invoked.
     *
     * @param context the target context.
     * @return the invocation target.
     */
    @Nonnull
    public abstract InvocationTarget getInvocationTarget(@Nonnull Context context);

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @Nonnull
    public abstract Class<?> getTargetClass();

    /**
     * Context invocation target wrapping a class.
     */
    private static class ClassContextInvocationTarget extends ContextInvocationTarget {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ClassContextInvocationTarget> CREATOR =
                new Creator<ClassContextInvocationTarget>() {

                    public ClassContextInvocationTarget createFromParcel(
                            @Nonnull final Parcel source) {

                        return new ClassContextInvocationTarget(
                                (Class<?>) source.readSerializable());
                    }

                    @Nonnull
                    public ClassContextInvocationTarget[] newArray(final int size) {

                        return new ClassContextInvocationTarget[size];
                    }
                };

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param targetClass the target class.
         */
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

        public void writeToParcel(@Nonnull final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
        }
    }

    /**
     * Context invocation target wrapping an object instance.
     */
    private static class ObjectContextInvocationTarget extends ContextInvocationTarget {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ObjectContextInvocationTarget> CREATOR =
                new Creator<ObjectContextInvocationTarget>() {

                    public ObjectContextInvocationTarget createFromParcel(
                            @Nonnull final Parcel source) {

                        return new ObjectContextInvocationTarget(
                                (Class<?>) source.readSerializable(),
                                source.readArray(InvocationFactoryTarget.class.getClassLoader()));
                    }

                    @Nonnull
                    public ObjectContextInvocationTarget[] newArray(final int size) {

                        return new ObjectContextInvocationTarget[size];
                    }
                };

        private final Object[] mFactoryArgs;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param targetClass the target class.
         * @param factoryArgs the object factory arguments.
         */
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

            Object target = null;
            final Class<?> targetClass = mTargetClass;
            final Object[] factoryArgs = mFactoryArgs;

            if (context instanceof FactoryContext) {

                // The only safe way is to synchronize the factory using the very same instance
                synchronized (context) {

                    target = ((FactoryContext) context).geInstance(targetClass, factoryArgs);
                }
            }

            if (target == null) {

                try {

                    target = findConstructor(targetClass, factoryArgs).newInstance(factoryArgs);

                } catch (final Throwable t) {

                    throw InvocationException.wrapIfNeeded(t);
                }

            } else if (!targetClass.isInstance(target)) {

                throw new RoutineException(
                        target + " is not an instance of " + targetClass.getName());
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

        public void writeToParcel(@Nonnull final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
            dest.writeArray(mFactoryArgs);
        }
    }
}
