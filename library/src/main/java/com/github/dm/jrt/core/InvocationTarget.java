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

package com.github.dm.jrt.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Class representing an invocation target.
 * <p/>
 * Created by davide-maestroni on 08/20/2015.
 *
 * @param <TYPE> the target object type.
 */
public abstract class InvocationTarget<TYPE> {

    /**
     * Avoid direct instantiation.
     */
    private InvocationTarget() {

    }

    /**
     * Returns a target based on the specified class.
     *
     * @param targetClass the target class.
     * @param <TYPE>      the target object type.
     * @return the invocation target.
     */
    @NotNull
    public static <TYPE> ClassInvocationTarget<TYPE> classOfType(
            @NotNull final Class<TYPE> targetClass) {

        return new ClassInvocationTarget<TYPE>(targetClass);
    }

    /**
     * Returns a target based on the specified instance.
     *
     * @param target the target instance.
     * @param <TYPE> the target object type.
     * @return the invocation target.
     */
    @NotNull
    public static <TYPE> InstanceInvocationTarget<TYPE> instance(@NotNull final TYPE target) {

        return new InstanceInvocationTarget<TYPE>(target);
    }

    /**
     * Returns the target of the invocation.
     *
     * @return the target.
     */
    @Nullable
    public abstract Object getTarget();

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @NotNull
    public abstract Class<? extends TYPE> getTargetClass();

    /**
     * Checks if this invocation target is assignable to the specified class.
     *
     * @param otherClass the other class.
     * @return whether the invocation target is assignable to the class.
     */
    public abstract boolean isAssignableTo(@NotNull Class<?> otherClass);

    /**
     * Checks if this invocation target is of the specified type.
     *
     * @param type the type class.
     * @return whether the invocation target is of the specified type.
     */
    public abstract boolean isSameTypeOf(@NotNull Class<?> type);

    /**
     * Invocation target wrapping a class.
     *
     * @param <TYPE> the target object type.
     */
    public static class ClassInvocationTarget<TYPE> extends InvocationTarget<TYPE> {

        private final Class<TYPE> mTargetClass;

        /**
         * Constructor.
         *
         * @param targetClass the target class.
         */
        @SuppressWarnings("ConstantConditions")
        private ClassInvocationTarget(@NotNull final Class<TYPE> targetClass) {

            if (targetClass == null) {
                throw new NullPointerException("the target class must not be null");
            }

            mTargetClass = targetClass;
        }

        @Nullable
        @Override
        public Object getTarget() {

            return mTargetClass;
        }

        @Override
        public int hashCode() {

            return mTargetClass.hashCode();
        }

        @Override
        public boolean isSameTypeOf(@NotNull final Class<?> type) {

            return (mTargetClass == type);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof ClassInvocationTarget)) {
                return false;
            }

            final ClassInvocationTarget that = (ClassInvocationTarget) o;
            return mTargetClass.equals(that.mTargetClass);
        }

        @NotNull
        @Override
        public Class<? extends TYPE> getTargetClass() {

            return mTargetClass;
        }

        @Override
        public boolean isAssignableTo(@NotNull final Class<?> otherClass) {

            return otherClass.isAssignableFrom(mTargetClass);
        }
    }

    /**
     * Invocation target wrapping an object instance.
     *
     * @param <TYPE> the target object type.
     */
    public static class InstanceInvocationTarget<TYPE> extends InvocationTarget<TYPE> {

        private final WeakReference<TYPE> mTarget;

        private final Class<? extends TYPE> mTargetClass;

        /**
         * Constructor.
         *
         * @param target the target instance.
         */
        @SuppressWarnings("unchecked")
        private InstanceInvocationTarget(@NotNull final TYPE target) {

            mTarget = new WeakReference<TYPE>(target);
            mTargetClass = (Class<? extends TYPE>) target.getClass();
        }

        @Override
        public boolean isSameTypeOf(@NotNull final Class<?> type) {

            return type.isInstance(mTarget.get());
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof InstanceInvocationTarget)) {
                return false;
            }

            final InstanceInvocationTarget that = (InstanceInvocationTarget) o;
            final Object referent = mTarget.get();
            return (referent != null) && referent.equals(that.mTarget.get()) && mTargetClass.equals(
                    that.mTargetClass);
        }

        @Override
        public int hashCode() {

            final Object referent = mTarget.get();
            int result = (referent != null) ? referent.hashCode() : 0;
            result = 31 * result + mTargetClass.hashCode();
            return result;
        }

        @Nullable
        @Override
        public Object getTarget() {

            return mTarget.get();
        }

        @NotNull
        @Override
        public Class<? extends TYPE> getTargetClass() {

            return mTargetClass;
        }

        @Override
        public boolean isAssignableTo(@NotNull final Class<?> otherClass) {

            return otherClass.isInstance(mTarget.get());
        }
    }
}
