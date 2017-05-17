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

package com.github.dm.jrt.android.v4.core;

import android.annotation.TargetApi;
import android.content.ContextWrapper;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.test.R;

import static com.github.dm.jrt.android.v4.core.LoaderSourceCompat.loaderOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader context unit tests.
 * <p>
 * Created by davide-maestroni on 03/09/2016.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class LoaderSourceCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public LoaderSourceCompatTest() {

    super(TestActivity.class);
  }

  public void testActivityEquals() {

    final ContextWrapper contextWrapper = new ContextWrapper(getActivity());
    LoaderSourceCompat loaderContext = LoaderSourceCompat.loaderOf(getActivity());
    assertThat(loaderContext).isEqualTo(loaderContext);
    assertThat(loaderContext).isNotEqualTo(null);
    assertThat(loaderContext).isNotEqualTo("test");
    assertThat(loaderContext).isNotEqualTo(loaderOf(getActivity(), contextWrapper));
    assertThat(loaderContext).isEqualTo(LoaderSourceCompat.loaderOf(getActivity()));
    assertThat(loaderContext.hashCode()).isEqualTo(
        LoaderSourceCompat.loaderOf(getActivity()).hashCode());
    loaderContext = loaderOf(getActivity(), contextWrapper);
    assertThat(loaderContext).isEqualTo(loaderContext);
    assertThat(loaderContext).isNotEqualTo(null);
    assertThat(loaderContext).isNotEqualTo("test");
    assertThat(loaderContext).isNotEqualTo(loaderOf(getActivity(), getActivity()));
    assertThat(loaderContext).isEqualTo(loaderOf(getActivity(), contextWrapper));
    assertThat(loaderContext.hashCode()).isEqualTo(
        loaderOf(getActivity(), contextWrapper).hashCode());
  }

  @SuppressWarnings("ConstantConditions")
  public void testActivityError() {

    try {
      LoaderSourceCompat.loaderOf((FragmentActivity) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      loaderOf(getActivity(), null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      loaderOf(getActivity(), new ContextWrapper(getActivity()) {});
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testFragmentEquals() {

    final ContextWrapper contextWrapper = new ContextWrapper(getActivity());
    final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                              .findFragmentById(R.id.test_fragment);
    LoaderSourceCompat loaderContext = LoaderSourceCompat.loaderOf(fragment);
    assertThat(loaderContext).isEqualTo(loaderContext);
    assertThat(loaderContext).isNotEqualTo(null);
    assertThat(loaderContext).isNotEqualTo("test");
    assertThat(loaderContext).isNotEqualTo(LoaderSourceCompat.loaderOf(fragment, contextWrapper));
    assertThat(loaderContext).isEqualTo(LoaderSourceCompat.loaderOf(fragment));
    assertThat(loaderContext.hashCode()).isEqualTo(
        LoaderSourceCompat.loaderOf(fragment).hashCode());
    loaderContext = LoaderSourceCompat.loaderOf(fragment, contextWrapper);
    assertThat(loaderContext).isEqualTo(loaderContext);
    assertThat(loaderContext).isNotEqualTo(null);
    assertThat(loaderContext).isNotEqualTo("test");
    assertThat(loaderContext).isNotEqualTo(LoaderSourceCompat.loaderOf(fragment, getActivity()));
    assertThat(loaderContext).isEqualTo(LoaderSourceCompat.loaderOf(fragment, contextWrapper));
    assertThat(loaderContext.hashCode()).isEqualTo(
        LoaderSourceCompat.loaderOf(fragment, contextWrapper).hashCode());
  }

  @SuppressWarnings("ConstantConditions")
  public void testFragmentError() {

    final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                              .findFragmentById(R.id.test_fragment);
    try {
      LoaderSourceCompat.loaderOf((Fragment) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      LoaderSourceCompat.loaderOf(fragment, null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      LoaderSourceCompat.loaderOf(fragment, new ContextWrapper(getActivity()) {});
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }
}
