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

package com.github.dm.jrt.android.object;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.android.object.builder.FactoryContext;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.object.InvocationTarget;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.cloneArgs;
import static com.github.dm.jrt.core.util.Reflection.newInstanceOf;

/**
 * Class representing a Context invocation target.
 * <br>
 * The target identifies a class or an instance whose methods are to be called asynchronously.
 * <p>
 * Created by davide-maestroni on 08/21/2015.
 *
 * @param <TYPE> the target object type.
 */
public abstract class ContextInvocationTarget<TYPE> extends DeepEqualObject implements Parcelable {

  /**
   * Constructor.
   *
   * @param args the constructor arguments.
   */
  private ContextInvocationTarget(@Nullable final Object[] args) {
    super(args);
  }

  /**
   * Returns a target based on the specified class.
   *
   * @param targetClass the target class.
   * @param <TYPE>      the target object type.
   * @return the Context invocation target.
   */
  @NotNull
  public static <TYPE> ContextInvocationTarget<TYPE> classOfType(
      @NotNull final Class<TYPE> targetClass) {
    return new ClassContextInvocationTarget<TYPE>(targetClass);
  }

  /**
   * Returns a target based on the specified instance.
   * <br>
   * No argument will be passed to the object factory.
   *
   * @param targetClass the target class.
   * @param <TYPE>      the target object type.
   * @return the Context invocation target.
   */
  @NotNull
  public static <TYPE> ContextInvocationTarget<TYPE> instanceOf(
      @NotNull final Class<TYPE> targetClass) {
    return instanceOf(targetClass, (Object[]) null);
  }

  /**
   * Returns a target based on the specified instance.
   *
   * @param targetClass the target class.
   * @param factoryArgs the object factory arguments.
   * @param <TYPE>      the target object type.
   * @return the Context invocation target.
   */
  @NotNull
  public static <TYPE> ContextInvocationTarget<TYPE> instanceOf(
      @NotNull final Class<TYPE> targetClass, @Nullable final Object... factoryArgs) {
    return new ObjectContextInvocationTarget<TYPE>(targetClass, factoryArgs);
  }

  /**
   * Returns an invocation target based on the specified Context.
   * <p>
   * Note that a new instance will be returned each time the method is invoked.
   *
   * @param context the target Context.
   * @return the invocation target.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  @NotNull
  public abstract InvocationTarget<TYPE> getInvocationTarget(@NotNull Context context) throws
      Exception;

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
  public abstract boolean isOfType(@NotNull Class<?> type);

  /**
   * Context invocation target wrapping a class.
   *
   * @param <TYPE> the target object type.
   */
  private static class ClassContextInvocationTarget<TYPE> extends ContextInvocationTarget<TYPE> {

    /**
     * Creator instance needed by the parcelable protocol.
     */
    public static final Creator<ClassContextInvocationTarget> CREATOR =
        new Creator<ClassContextInvocationTarget>() {

          @Override
          @SuppressWarnings("unchecked")
          public ClassContextInvocationTarget createFromParcel(final Parcel source) {
            return new ClassContextInvocationTarget((Class<?>) source.readSerializable());
          }

          @Override
          public ClassContextInvocationTarget[] newArray(final int size) {
            return new ClassContextInvocationTarget[size];
          }
        };

    private final Class<TYPE> mTargetClass;

    /**
     * Constructor.
     *
     * @param targetClass the target class.
     */
    private ClassContextInvocationTarget(@NotNull final Class<TYPE> targetClass) {
      super(asArgs(targetClass));
      if (targetClass.isPrimitive()) {
        // The parceling of primitive classes is broken...
        throw new IllegalArgumentException("the target class cannot be primitive");
      }

      mTargetClass = targetClass;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @NotNull
    @Override
    public InvocationTarget<TYPE> getInvocationTarget(@NotNull final Context context) {
      return InvocationTarget.classOfType(mTargetClass);
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

    @Override
    public boolean isOfType(@NotNull final Class<?> type) {
      return (mTargetClass == type);
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      dest.writeSerializable(mTargetClass);
    }
  }

  /**
   * Context invocation target wrapping an object instance.
   *
   * @param <TYPE> the target object type.
   */
  private static class ObjectContextInvocationTarget<TYPE> extends ContextInvocationTarget<TYPE> {

    /**
     * Creator instance needed by the parcelable protocol.
     */
    public static final Creator<ObjectContextInvocationTarget> CREATOR =
        new Creator<ObjectContextInvocationTarget>() {

          @Override
          @SuppressWarnings("unchecked")
          public ObjectContextInvocationTarget createFromParcel(final Parcel source) {
            return new ObjectContextInvocationTarget((Class<?>) source.readSerializable(),
                source.readArray(ContextInvocationTarget.class.getClassLoader()));
          }

          @Override
          public ObjectContextInvocationTarget[] newArray(final int size) {
            return new ObjectContextInvocationTarget[size];
          }
        };

    private final Object[] mFactoryArgs;

    private final Class<TYPE> mTargetClass;

    /**
     * Constructor.
     *
     * @param targetClass the target class.
     * @param factoryArgs the object factory arguments.
     */
    private ObjectContextInvocationTarget(@NotNull final Class<TYPE> targetClass,
        @Nullable final Object[] factoryArgs) {
      super(asArgs(targetClass, cloneArgs(factoryArgs)));
      if (targetClass.isPrimitive()) {
        // The parceling of primitive classes is broken...
        throw new IllegalArgumentException("the target class cannot be primitive");
      }

      mTargetClass = targetClass;
      mFactoryArgs = cloneArgs(factoryArgs);
    }

    @NotNull
    @Override
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public InvocationTarget<TYPE> getInvocationTarget(@NotNull final Context context) throws
        Exception {
      TYPE target = null;
      final Class<TYPE> targetClass = mTargetClass;
      final Object[] factoryArgs = mFactoryArgs;
      if (context instanceof FactoryContext) {
        // The only safe way is to synchronize the factory using the very same instance
        synchronized (context) {
          target = ((FactoryContext) context).geInstance(targetClass, factoryArgs);
        }
      }

      if (target == null) {
        target = newInstanceOf(targetClass, factoryArgs);

      } else if (!targetClass.isInstance(target)) {
        throw new RoutineException(target + " is not an instance of " + targetClass.getName());
      }

      return InvocationTarget.instance(target);
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

    @Override
    public boolean isOfType(@NotNull final Class<?> type) {
      return isAssignableTo(type);
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      dest.writeSerializable(mTargetClass);
      dest.writeArray(mFactoryArgs);
    }
  }
}
