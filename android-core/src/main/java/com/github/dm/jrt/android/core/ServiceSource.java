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

package com.github.dm.jrt.android.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Class representing an Android Service source.
 * <p>
 * No strong reference to the wrapped Context will be retained by this class implementations.
 * <p>
 * Created by davide-maestroni on 07/11/2015.
 */
public abstract class ServiceSource {

  /**
   * Avoid explicit instantiation.
   */
  private ServiceSource() {
  }

  /**
   * Returns a Context based on the specified instance.
   * <br>
   * The default {@link com.github.dm.jrt.android.core.service.InvocationService InvocationService}
   * class will be employed.
   *
   * @param context the Context.
   * @return the Service context.
   */
  @NotNull
  public static ServiceSource serviceOf(@NotNull final Context context) {
    return serviceOf(context, InvocationService.class);
  }

  /**
   * Returns a Context based on the specified instance, employing a Service of the specified type.
   *
   * @param context      the Context.
   * @param serviceClass the Service type.
   * @return the Service context.
   */
  @NotNull
  public static ServiceSource serviceOf(@NotNull final Context context,
      @NotNull final Class<? extends InvocationService> serviceClass) {
    return new IntentServiceSource(context, new Intent(context, serviceClass));
  }

  /**
   * Returns a Context based on the specified instance, employing the specified Intent to start
   * the Service.
   *
   * @param context the Context.
   * @param service the Service Intent.
   * @return the Service context.
   * @throws java.lang.IllegalArgumentException if the component of the specified Intent does not
   *                                            inherit from {@link InvocationService}.
   */
  @NotNull
  public static ServiceSource serviceOf(@NotNull final Context context,
      @NotNull final Intent service) {
    final ComponentName component = service.getComponent();
    try {
      if ((component == null) || !InvocationService.class.isAssignableFrom(
          Class.forName(component.getClassName()))) {
        throw new IllegalArgumentException(
            "Service class must inherit from " + InvocationService.class.getName());
      }

    } catch (final ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }

    return new IntentServiceSource(context, service);
  }

  private static boolean bundleEquals(@Nullable final Bundle bundle1,
      @Nullable final Bundle bundle2) {
    if (bundle1 == bundle2) {
      return true;
    }

    if ((bundle1 == null) || (bundle2 == null)) {
      return false;
    }

    if (bundle1.size() != bundle2.size()) {
      return false;
    }

    for (final String key : bundle1.keySet()) {
      final Object value1 = bundle1.get(key);
      final Object value2 = bundle2.get(key);
      if ((value1 instanceof Bundle) && (value2 instanceof Bundle) && !bundleEquals((Bundle) value1,
          (Bundle) value2)) {
        return false;

      } else if (value1 == null) {
        if ((value2 != null) || !bundle2.containsKey(key)) {
          return false;
        }

      } else if (!value1.equals(value2)) {
        return false;
      }
    }

    return true;
  }

  private static int bundleHashCode(@Nullable final Bundle bundle) {
    if (bundle == null) {
      return 0;
    }

    int result = 0;
    for (final String key : bundle.keySet()) {
      final Object value = bundle.get(key);
      if (value instanceof Bundle) {
        result = 31 * result + bundleHashCode((Bundle) value);

      } else if (value == null) {
        result = 31 * result;

      } else {
        result = 31 * result + value.hashCode();
      }
    }

    return result;
  }

  /**
   * Returns the Service context.
   *
   * @return the Context.
   */
  @Nullable
  public abstract Context getContext();

  /**
   * Returns the Service Intent.
   *
   * @return the Intent.
   */
  @NotNull
  public abstract Intent getIntent();

  /**
   * Service context wrapping a Service Intent.
   */
  private static class IntentServiceSource extends ServiceSource {

    private final WeakReference<Context> mContext;

    private final Intent mIntent;

    /**
     * Constructor.
     *
     * @param context the Context.
     * @param service the Service Intent.
     */
    private IntentServiceSource(@NotNull final Context context, @NotNull final Intent service) {
      mContext = new WeakReference<Context>(ConstantConditions.notNull("Service context", context));
      mIntent = ConstantConditions.notNull("Service Intent", service);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof IntentServiceSource)) {
        return false;
      }

      final IntentServiceSource that = (IntentServiceSource) o;
      final Context referent = mContext.get();
      return (referent != null) && referent.equals(that.mContext.get()) && mIntent.filterEquals(
          that.mIntent) && bundleEquals(mIntent.getExtras(), that.mIntent.getExtras());
    }

    @Override
    public int hashCode() {
      final Context referent = mContext.get();
      int result = (referent != null) ? referent.hashCode() : 0;
      result = 31 * result + mIntent.filterHashCode();
      result = 31 * result + bundleHashCode(mIntent.getExtras());
      return result;
    }

    @Nullable
    @Override
    public Context getContext() {
      return mContext.get();
    }

    @NotNull
    @Override
    public Intent getIntent() {
      return mIntent;
    }
  }
}
