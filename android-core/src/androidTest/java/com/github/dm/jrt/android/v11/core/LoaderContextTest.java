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

package com.github.dm.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContextWrapper;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.R;

import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader context unit tests.
 * <p>
 * Created by davide-maestroni on 03/09/2016.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class LoaderContextTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public LoaderContextTest() {

    super(TestActivity.class);
  }

  public void testActivityEquals() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ContextWrapper contextWrapper = new ContextWrapper(getActivity());
    LoaderContext loaderContext = loaderFrom(getActivity());
    assertThat(loaderContext).isEqualTo(loaderContext);
    assertThat(loaderContext).isNotEqualTo(null);
    assertThat(loaderContext).isNotEqualTo("test");
    assertThat(loaderContext).isNotEqualTo(loaderFrom(getActivity(), contextWrapper));
    assertThat(loaderContext).isEqualTo(loaderFrom(getActivity()));
    assertThat(loaderContext.hashCode()).isEqualTo(loaderFrom(getActivity()).hashCode());
    loaderContext = loaderFrom(getActivity(), contextWrapper);
    assertThat(loaderContext).isEqualTo(loaderContext);
    assertThat(loaderContext).isNotEqualTo(null);
    assertThat(loaderContext).isNotEqualTo("test");
    assertThat(loaderContext).isNotEqualTo(loaderFrom(getActivity(), getActivity()));
    assertThat(loaderContext).isEqualTo(loaderFrom(getActivity(), contextWrapper));
    assertThat(loaderContext.hashCode()).isEqualTo(
        loaderFrom(getActivity(), contextWrapper).hashCode());
  }

  @SuppressWarnings("ConstantConditions")
  public void testActivityError() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {
      loaderFrom((Activity) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      loaderFrom(getActivity(), null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      loaderFrom(getActivity(), new ContextWrapper(getActivity()) {});
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testFragmentEquals() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ContextWrapper contextWrapper = new ContextWrapper(getActivity());
    final TestFragment fragment =
        (TestFragment) getActivity().getFragmentManager().findFragmentById(R.id.test_fragment);
    LoaderContext loaderContext = loaderFrom(fragment);
    assertThat(loaderContext).isEqualTo(loaderContext);
    assertThat(loaderContext).isNotEqualTo(null);
    assertThat(loaderContext).isNotEqualTo("test");
    assertThat(loaderContext).isNotEqualTo(loaderFrom(fragment, contextWrapper));
    assertThat(loaderContext).isEqualTo(loaderFrom(fragment));
    assertThat(loaderContext.hashCode()).isEqualTo(loaderFrom(fragment).hashCode());
    loaderContext = loaderFrom(fragment, contextWrapper);
    assertThat(loaderContext).isEqualTo(loaderContext);
    assertThat(loaderContext).isNotEqualTo(null);
    assertThat(loaderContext).isNotEqualTo("test");
    assertThat(loaderContext).isNotEqualTo(loaderFrom(fragment, getActivity()));
    assertThat(loaderContext).isEqualTo(loaderFrom(fragment, contextWrapper));
    assertThat(loaderContext.hashCode()).isEqualTo(loaderFrom(fragment, contextWrapper).hashCode());
  }

  @SuppressWarnings("ConstantConditions")
  public void testFragmentError() {

    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final TestFragment fragment =
        (TestFragment) getActivity().getFragmentManager().findFragmentById(R.id.test_fragment);
    try {
      loaderFrom((Fragment) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      loaderFrom(fragment, null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      loaderFrom(fragment, new ContextWrapper(getActivity()) {});
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }
}
