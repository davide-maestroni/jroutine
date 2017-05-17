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

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.v4.core.TestActivity;

import static com.github.dm.jrt.android.core.ServiceSource.serviceOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service context unit tests.
 * <p>
 * Created by davide-maestroni on 03/09/2016.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class ServiceSourceTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public ServiceSourceTest() {

    super(TestActivity.class);
  }

  public void testEquals() {

    ServiceSource serviceSource = ServiceSource.serviceOf(getActivity());
    assertThat(serviceSource).isEqualTo(serviceSource);
    assertThat(serviceSource).isNotEqualTo(null);
    assertThat(serviceSource).isNotEqualTo("test");
    assertThat(serviceSource).isNotEqualTo(
        ServiceSource.serviceOf(getActivity().getApplicationContext()));
    assertThat(serviceSource).isEqualTo(ServiceSource.serviceOf(getActivity()));
    assertThat(serviceSource.hashCode()).isEqualTo(
        ServiceSource.serviceOf(getActivity()).hashCode());
    serviceSource = ServiceSource.serviceOf(getActivity(), TestService.class);
    assertThat(serviceSource).isEqualTo(serviceSource);
    assertThat(serviceSource).isNotEqualTo(null);
    assertThat(serviceSource).isNotEqualTo("test");
    assertThat(serviceSource).isNotEqualTo(
        ServiceSource.serviceOf(getActivity(), InvocationService.class));
    assertThat(serviceSource).isEqualTo(ServiceSource.serviceOf(getActivity(), TestService.class));
    assertThat(serviceSource.hashCode()).isEqualTo(
        ServiceSource.serviceOf(getActivity(), TestService.class).hashCode());
    final Intent intent = new Intent(getActivity(), TestService.class);
    serviceSource = serviceOf(getActivity(), intent);
    assertThat(serviceSource).isEqualTo(serviceSource);
    assertThat(serviceSource).isNotEqualTo(null);
    assertThat(serviceSource).isNotEqualTo("test");
    assertThat(serviceSource).isNotEqualTo(
        ServiceSource.serviceOf(getActivity(), InvocationService.class));
    assertThat(serviceSource).isEqualTo(serviceOf(getActivity(), intent));
    assertThat(serviceSource.hashCode()).isEqualTo(serviceOf(getActivity(), intent).hashCode());
    final Intent intentExtra = new Intent(getActivity(), TestService.class);
    intentExtra.putExtra("test", true);
    serviceSource = serviceOf(getActivity(), intentExtra);
    assertThat(serviceSource).isNotEqualTo(
        ServiceSource.serviceOf(getActivity(), InvocationService.class));
    final Intent intentExtra2 = new Intent(getActivity(), TestService.class);
    intentExtra2.putExtra("test", false);
    assertThat(serviceSource).isNotEqualTo(serviceOf(getActivity(), intentExtra2));
    intentExtra2.putExtra("test", true);
    assertThat(serviceSource).isEqualTo(serviceOf(getActivity(), intentExtra2));
    assertThat(serviceSource.hashCode()).isEqualTo(
        serviceOf(getActivity(), intentExtra2).hashCode());
    intentExtra2.putExtra("testString", "test");
    assertThat(serviceSource).isNotEqualTo(serviceOf(getActivity(), intentExtra2));
    intentExtra2.putExtra("testString", (String) null);
    assertThat(serviceSource).isNotEqualTo(serviceOf(getActivity(), intentExtra2));
    intentExtra.putExtra("testString", (String) null);
    assertThat(serviceSource).isEqualTo(serviceOf(getActivity(), intentExtra2));
    intentExtra.putExtra("testString", "test");
    assertThat(serviceSource).isNotEqualTo(serviceOf(getActivity(), intentExtra2));
    intentExtra2.putExtra("testString", "test");
    intentExtra2.putExtra("testBundle", new Bundle());
    assertThat(serviceSource).isNotEqualTo(serviceOf(getActivity(), intentExtra2));
    intentExtra.putExtra("testBundle", new Bundle());
    assertThat(serviceSource).isNotEqualTo(serviceOf(getActivity(), intentExtra2));
  }

  @SuppressWarnings("ConstantConditions")
  public void testError() {

    try {
      ServiceSource.serviceOf(null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      ServiceSource.serviceOf(getActivity(), (Class<InvocationService>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      serviceOf(getActivity(), (Intent) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      serviceOf(getActivity(), new Intent(getActivity(), Service.class));
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      serviceOf(getActivity(), new Intent().setClassName(getActivity(), "not.existing.Class"));
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }
}
