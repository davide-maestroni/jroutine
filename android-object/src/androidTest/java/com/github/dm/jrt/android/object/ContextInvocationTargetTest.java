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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.object.builder.FactoryContext;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.object.InvocationTarget;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.android.object.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.instanceOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context invocation target unit tests.
 * <p>
 * Created by davide-maestroni on 03/08/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ContextInvocationTargetTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public ContextInvocationTargetTest() {

    super(TestActivity.class);
  }

  public void testClass() throws Exception {

    final ContextInvocationTarget<TestClass> target = classOfType(TestClass.class);
    assertThat(target.getTargetClass()).isEqualTo(TestClass.class);
    assertThat(target.isAssignableTo(TestClass.class)).isTrue();
    assertThat(target.isAssignableTo(Object.class)).isTrue();
    assertThat(target.isOfType(TestClass.class)).isTrue();
    assertThat(target.isOfType(Object.class)).isFalse();
    final InvocationTarget<TestClass> invocationTarget = target.getInvocationTarget(getActivity());
    assertThat(invocationTarget.getTargetClass()).isEqualTo(TestClass.class);
    assertThat(invocationTarget.getTarget()).isEqualTo(TestClass.class);
  }

  public void testClassEquals() {

    final ContextInvocationTarget<TestClass> target = classOfType(TestClass.class);
    assertThat(target).isEqualTo(target);
    assertThat(target).isNotEqualTo(null);
    assertThat(target).isNotEqualTo("test");
    assertThat(target).isNotEqualTo(classOfType(Object.class));
    assertThat(target).isEqualTo(classOfType(TestClass.class));
    assertThat(target.hashCode()).isEqualTo(classOfType(TestClass.class).hashCode());
  }

  @SuppressWarnings("ConstantConditions")
  public void testClassError() {

    try {
      classOfType(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testClassParcelable() {

    final Parcel parcel = Parcel.obtain();
    final ContextInvocationTarget<TestClass> target = classOfType(TestClass.class);
    parcel.writeParcelable(target, 0);
    parcel.setDataPosition(0);
    final Parcelable parcelable =
        parcel.readParcelable(ContextInvocationTarget.class.getClassLoader());
    assertThat(parcelable).isEqualTo(target);
    parcel.recycle();
  }

  public void testInstance() throws Exception {

    final ContextInvocationTarget<TestClass> target = instanceOf(TestClass.class);
    assertThat(target.getTargetClass()).isEqualTo(TestClass.class);
    assertThat(target.isAssignableTo(TestClass.class)).isTrue();
    assertThat(target.isAssignableTo(Object.class)).isTrue();
    assertThat(target.isOfType(TestClass.class)).isTrue();
    assertThat(target.isOfType(Object.class)).isTrue();
    InvocationTarget<TestClass> invocationTarget = target.getInvocationTarget(getActivity());
    assertThat(invocationTarget.getTargetClass()).isEqualTo(TestClass.class);
    assertThat(invocationTarget.getTarget()).isExactlyInstanceOf(TestClass.class);
    invocationTarget = target.getInvocationTarget(new NullContext(getActivity()));
    assertThat(invocationTarget.getTargetClass()).isEqualTo(TestClass.class);
    assertThat(invocationTarget.getTarget()).isExactlyInstanceOf(TestClass.class);
  }

  public void testInstanceEquals() {

    final ContextInvocationTarget<TestClass> target = instanceOf(TestClass.class);
    assertThat(target).isEqualTo(target);
    assertThat(target).isNotEqualTo(null);
    assertThat(target).isNotEqualTo("test");
    assertThat(target).isNotEqualTo(classOfType(Object.class));
    assertThat(target).isEqualTo(instanceOf(TestClass.class));
    assertThat(target.hashCode()).isEqualTo(instanceOf(TestClass.class).hashCode());
  }

  @SuppressWarnings("ConstantConditions")
  public void testInstanceError() throws Exception {

    try {
      instanceOf(null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      instanceOf(null, "test");
      fail();

    } catch (final NullPointerException ignored) {

    }

    final ContextInvocationTarget<TestClass> target = instanceOf(TestClass.class);
    try {
      target.getInvocationTarget(new ObjectContext(getActivity()));
      fail();

    } catch (final RoutineException ignored) {

    }
  }

  public void testInstanceParcelable() {

    final Parcel parcel = Parcel.obtain();
    final ContextInvocationTarget<TestClass> target = instanceOf(TestClass.class);
    parcel.writeParcelable(target, 0);
    parcel.setDataPosition(0);
    final Parcelable parcelable =
        parcel.readParcelable(ContextInvocationTarget.class.getClassLoader());
    assertThat(parcelable).isEqualTo(target);
    parcel.recycle();
  }

  public void testPrimitiveClass() {
    try {
      classOfType(int.class);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  public static class TestClass {

  }

  private static class NullContext extends ContextWrapper implements FactoryContext {

    public NullContext(final Context base) {

      super(base);
    }

    @Nullable
    public <TYPE> TYPE geInstance(@NotNull final Class<? extends TYPE> type,
        @NotNull final Object... args) {

      return null;
    }
  }

  private static class ObjectContext extends ContextWrapper implements FactoryContext {

    public ObjectContext(final Context base) {

      super(base);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <TYPE> TYPE geInstance(@NotNull final Class<? extends TYPE> type,
        @NotNull final Object... args) {

      return (TYPE) new Object();
    }
  }
}
