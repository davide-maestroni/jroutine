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

package com.github.dm.jrt.android.processor;

import com.github.dm.jrt.processor.RoutineProcessor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Annotation processor used to generate proxy classes enabling method asynchronous invocations,
 * by leveraging Android platform specific classes.
 * <p>
 * Created by davide-maestroni on 05/06/2015.
 */
public class ContextRoutineProcessor extends RoutineProcessor {

  private TypeMirror mCacheAnnotationType;

  private TypeMirror mClashAnnotationType;

  private TypeMirror mExecutorClassAnnotationType;

  private String mHeaderService;

  private String mHeaderV11;

  private String mHeaderV4;

  private TypeMirror mInputClashAnnotationType;

  private TypeMirror mInvocationIdAnnotationType;

  private TypeMirror mLoaderIdAnnotationType;

  private TypeElement mLoaderProxyCompatElement;

  private TypeElement mLoaderProxyElement;

  private TypeMirror mLogClassAnnotationType;

  private String mMethodHeader;

  private String mMethodHeaderV1;

  private String mMethodInvocationHeader;

  private TypeElement mServiceProxyElement;

  private TypeMirror mStaleTimeAnnotationType;

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    final HashSet<String> types = new HashSet<String>();
    types.add("com.github.dm.jrt.android.proxy.annotation.ServiceProxy");
    types.add("com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat");
    types.add("com.github.dm.jrt.android.proxy.annotation.LoaderProxy");
    return types;
  }

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    mCacheAnnotationType =
        getMirrorFromName("com.github.dm.jrt.android.reflect.annotation.CacheStrategy");
    mClashAnnotationType =
        getMirrorFromName("com.github.dm.jrt.android.reflect.annotation.ClashResolution");
    mInvocationIdAnnotationType =
        getMirrorFromName("com.github.dm.jrt.android.reflect.annotation.InvocationId");
    mLoaderIdAnnotationType =
        getMirrorFromName("com.github.dm.jrt.android.reflect.annotation.LoaderId");
    mInputClashAnnotationType =
        getMirrorFromName("com.github.dm.jrt.android.reflect.annotation.MatchResolution");
    mStaleTimeAnnotationType =
        getMirrorFromName("com.github.dm.jrt.android.reflect.annotation.ResultStaleTime");
    mExecutorClassAnnotationType =
        getMirrorFromName("com.github.dm.jrt.android.reflect.annotation.ServiceExecutor");
    mLogClassAnnotationType =
        getMirrorFromName("com.github.dm.jrt.android.reflect.annotation.ServiceLog");
    final Types typeUtils = processingEnv.getTypeUtils();
    mServiceProxyElement = (TypeElement) typeUtils.asElement(
        getMirrorFromName("com.github.dm.jrt.android.proxy.annotation.ServiceProxy"));
    mLoaderProxyCompatElement = (TypeElement) typeUtils.asElement(
        getMirrorFromName("com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat"));
    mLoaderProxyElement = (TypeElement) typeUtils.asElement(
        getMirrorFromName("com.github.dm.jrt.android.proxy.annotation.LoaderProxy"));
  }

  @NotNull
  @Override
  protected String buildRoutineFieldsInit(@NotNull final TypeElement annotationElement,
      @NotNull final TypeElement element, @NotNull final Element targetElement, final int size) {
    final TypeElement serviceProxyElement = mServiceProxyElement;
    final StringBuilder builder = new StringBuilder();
    for (int i = 1; i <= size; ++i) {
      builder.append("mRoutine").append(i).append(" = ").append("initRoutine").append(i);
      if (annotationElement == serviceProxyElement) {
        builder.append(
            "(target, serviceSource, invocationConfiguration, wrapperConfiguration, "
                + "serviceConfiguration);");

      } else {
        builder.append(
            "(target, invocationConfiguration, wrapperConfiguration, loaderConfiguration);");
      }

      builder.append(NEW_LINE);
    }

    return builder.toString();
  }

  @NotNull
  @Override
  protected String getHeaderTemplate(@NotNull final TypeElement annotationElement,
      @NotNull final TypeElement element, @NotNull final Element targetElement) throws IOException {
    final TypeElement serviceProxyElement = mServiceProxyElement;
    final TypeElement loaderProxyCompatElement = mLoaderProxyCompatElement;
    final TypeElement loaderProxyElement = mLoaderProxyElement;
    if (annotationElement == serviceProxyElement) {
      if (mHeaderService == null) {
        mHeaderService = parseTemplate("/templates/android/header.txt");
      }

      return mHeaderService;
    }

    if (annotationElement == loaderProxyCompatElement) {
      if (mHeaderV4 == null) {
        mHeaderV4 = parseTemplate("/templates/android/v4/header.txt");
      }

      return mHeaderV4;
    }

    if (annotationElement == loaderProxyElement) {
      if (mHeaderV11 == null) {
        mHeaderV11 = parseTemplate("/templates/android/v11/header.txt");
      }

      return mHeaderV11;
    }

    return super.getHeaderTemplate(annotationElement, element, targetElement);
  }

  @NotNull
  @Override
  protected String getMethodHeaderTemplate(@NotNull final TypeElement annotationElement,
      @NotNull final TypeElement element, @NotNull final Element targetElement,
      @NotNull final ExecutableElement methodElement, final int count) throws IOException {
    if (annotationElement != mServiceProxyElement) {
      if (mMethodHeaderV1 == null) {
        mMethodHeaderV1 = parseTemplate("/templates/android/v1/method_header.txt");
      }

      return mMethodHeaderV1.replace("${loaderBuilderOptions}", buildLoaderOptions(methodElement));
    }

    if (mMethodHeader == null) {
      mMethodHeader = parseTemplate("/templates/android/method_header.txt");
    }

    return mMethodHeader.replace("${serviceBuilderOptions}", buildServiceOptions(methodElement));
  }

  @NotNull
  @Override
  protected String getMethodInvocationHeaderTemplate(@NotNull final TypeElement annotationElement,
      @NotNull final TypeElement element, @NotNull final Element targetElement,
      @NotNull final ExecutableElement methodElement, final int count) throws IOException {
    if (mMethodInvocationHeader == null) {
      mMethodInvocationHeader = parseTemplate("/templates/android/method_invocation_header.txt");
    }

    return mMethodInvocationHeader;
  }

  @NotNull
  private String buildLoaderOptions(@NotNull final ExecutableElement methodElement) {
    // We need to avoid explicit dependency on the android module...
    final StringBuilder builder = new StringBuilder();
    final Integer loaderId =
        (Integer) getAnnotationValue(methodElement, mLoaderIdAnnotationType, "value");
    if (loaderId != null) {
      builder.append(".withLoaderId(").append(loaderId).append(")");
    }

    final Integer invocationId =
        (Integer) getAnnotationValue(methodElement, mInvocationIdAnnotationType, "value");
    if (invocationId != null) {
      builder.append(".withInvocationId(").append(invocationId).append(")");
    }

    final Object resolutionType = getAnnotationValue(methodElement, mClashAnnotationType, "value");
    if (resolutionType != null) {
      builder.append(
          ".withClashResolution(com.github.dm.jrt.android.core.config.LoaderConfiguration"
              + ".ClashResolutionType.").append(resolutionType).append(")");
    }

    final Object inputResolutionType =
        getAnnotationValue(methodElement, mInputClashAnnotationType, "value");
    if (inputResolutionType != null) {
      builder.append(".withMatchResolution(com.github.dm.jrt.android.core.config"
          + ".LoaderConfiguration.ClashResolutionType.").append(resolutionType).append(")");
    }

    final Object strategyType = getAnnotationValue(methodElement, mCacheAnnotationType, "value");
    if (strategyType != null) {
      builder.append(".withCacheStrategy(com.github.dm.jrt.android.core.config.LoaderConfiguration"
          + ".CacheStrategyType.").append(strategyType).append(")");
    }

    final TypeMirror staleTimeAnnotationType = mStaleTimeAnnotationType;
    final Object staleTime = getAnnotationValue(methodElement, staleTimeAnnotationType, "value");
    if (staleTime != null) {
      final Object staleTimeUnit =
          getAnnotationValue(methodElement, staleTimeAnnotationType, "unit");
      builder.append(".withResultStaleTime(")
             .append(staleTime)
             .append(", ")
             .append(TimeUnit.class.getCanonicalName())
             .append(".")
             .append((staleTimeUnit != null) ? staleTimeUnit : TimeUnit.MILLISECONDS)
             .append(")");
    }

    return builder.toString();
  }

  @NotNull
  private String buildServiceOptions(@NotNull final ExecutableElement methodElement) {
    // We need to avoid explicit dependency on the android module...
    final StringBuilder builder = new StringBuilder();
    final Object executorClass =
        getAnnotationValue(methodElement, mExecutorClassAnnotationType, "value");
    if (executorClass != null) {
      builder.append(".withExecutorClass(").append(executorClass).append(")");
    }

    final Object logClass = getAnnotationValue(methodElement, mLogClassAnnotationType, "value");
    if (logClass != null) {
      builder.append(".withLogClass(").append(logClass).append(")");
    }

    return builder.toString();
  }
}
