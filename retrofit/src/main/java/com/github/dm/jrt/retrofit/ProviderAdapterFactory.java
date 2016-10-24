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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.retrofit.annotation.CallAdapterFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import retrofit2.CallAdapter;
import retrofit2.CallAdapter.Factory;
import retrofit2.Retrofit;

/**
 * Implementation of a call adapter factory selecting other factory instances.
 * <br>
 * The factory will scan the method annotations searching for {@code CallAdapterFactory} one. If
 * found and a factory with the corresponding name has been registered, it will be employed to
 * create the call adapter instance.
 * <br>
 * Special factory instances can be registered to cope with the cases in which no match is found or
 * the annotation is missing.
 * <p>
 * Created by davide-maestroni on 05/20/2016.
 */
public class ProviderAdapterFactory extends CallAdapter.Factory {

  private final Map<String, Factory> mFactories;

  private final Factory mMissingAnnotationFactory;

  private final Factory mMissingNameFactory;

  /**
   * Constructor.
   *
   * @param missingAnnotationFactory the factory to be used when the annotation is missing.
   * @param missingNameFactory       the factory to be used when matching name is missing.
   * @param factories                the registered factories.
   */
  private ProviderAdapterFactory(@Nullable final CallAdapter.Factory missingAnnotationFactory,
      @Nullable final CallAdapter.Factory missingNameFactory,
      @NotNull final Map<String, Factory> factories) {
    mMissingAnnotationFactory = missingAnnotationFactory;
    mMissingNameFactory = missingNameFactory;
    mFactories = factories;
  }

  /**
   * Returns a provider factory builder.
   *
   * @return the builder instance.
   */
  @NotNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a factory with a single registered instance.
   *
   * @param name    the registered factory name.
   * @param factory the registered factory instance.
   * @return the filter adapter factory.
   */
  @NotNull
  public static ProviderAdapterFactory withFactory(@NotNull final String name,
      @NotNull final CallAdapter.Factory factory) {
    return new ProviderAdapterFactory(null, null,
        Collections.singletonMap(ConstantConditions.notNull("factory name", name),
            ConstantConditions.notNull("factory instance", factory)));
  }

  @Override
  public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
      final Retrofit retrofit) {
    String factoryName = null;
    for (final Annotation annotation : annotations) {
      if (annotation.annotationType() == CallAdapterFactory.class) {
        factoryName = ((CallAdapterFactory) annotation).value();
      }
    }

    final Factory factory = mFactories.get(factoryName);
    return (factory != null) ? factory.get(returnType, annotations, retrofit)
        : getDefault(factoryName != null, returnType, annotations, retrofit);
  }

  @Nullable
  private CallAdapter<?> getDefault(final boolean hasAnnotation, final Type returnType,
      final Annotation[] annotations, final Retrofit retrofit) {
    final Factory factory = (hasAnnotation) ? mMissingNameFactory : mMissingAnnotationFactory;
    return (factory != null) ? factory.get(returnType, annotations, retrofit) : null;
  }

  /**
   * Builder of provider adapter factory instances.
   */
  public static class Builder {

    private final HashMap<String, Factory> mFactories = new HashMap<String, Factory>();

    private Factory mMissingAnnotationFactory;

    private Factory mMissingNameFactory;

    /**
     * Constructor.
     */
    private Builder() {
    }

    /**
     * Registers the specified factory instance with the specified name.
     * <p>
     * Note that the very same instance can be registered with different names.
     *
     * @param name    the registered factory name.
     * @param factory the registered factory instance.
     * @return this builder
     */
    @NotNull
    public Builder add(@NotNull final String name, @NotNull final CallAdapter.Factory factory) {
      mFactories.put(ConstantConditions.notNull("factory name", name),
          ConstantConditions.notNull("factory instance", factory));
      return this;
    }

    /**
     * Builds and return a new factory instance.
     *
     * @return the factory instance.
     */
    @NotNull
    public ProviderAdapterFactory buildFactory() {
      return new ProviderAdapterFactory(mMissingAnnotationFactory, mMissingNameFactory,
          new HashMap<String, CallAdapter.Factory>(mFactories));
    }

    /**
     * Sets the factory to be used when the annotation or a matching name is missing.
     *
     * @param factory the factory instance.
     * @return this builder.
     */
    @NotNull
    public Builder whenMissing(@Nullable final CallAdapter.Factory factory) {
      mMissingAnnotationFactory = factory;
      mMissingNameFactory = factory;
      return this;
    }

    /**
     * Sets the factory to be used when the annotation is missing.
     *
     * @param factory the factory instance.
     * @return this builder.
     */
    @NotNull
    public Builder whenMissingAnnotation(@Nullable final CallAdapter.Factory factory) {
      mMissingAnnotationFactory = factory;
      return this;
    }

    /**
     * Sets the factory to be used when matching name is missing.
     *
     * @param factory the factory instance.
     * @return this builder.
     */
    @NotNull
    public Builder whenMissingName(@Nullable final CallAdapter.Factory factory) {
      mMissingNameFactory = factory;
      return this;
    }
  }
}
