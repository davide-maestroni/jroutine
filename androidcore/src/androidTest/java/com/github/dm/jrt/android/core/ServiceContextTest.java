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
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.v4.core.TestActivity;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service context unit tests.
 * <p>
 * Created by davide-maestroni on 03/09/2016.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class ServiceContextTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ServiceContextTest() {

        super(TestActivity.class);
    }

    public void testEquals() {

        ServiceContext serviceContext = serviceFrom(getActivity());
        assertThat(serviceContext).isEqualTo(serviceContext);
        assertThat(serviceContext).isNotEqualTo(null);
        assertThat(serviceContext).isNotEqualTo("test");
        assertThat(serviceContext).isNotEqualTo(serviceFrom(getActivity().getApplicationContext()));
        assertThat(serviceContext).isEqualTo(serviceFrom(getActivity()));
        assertThat(serviceContext.hashCode()).isEqualTo(serviceFrom(getActivity()).hashCode());
        serviceContext = serviceFrom(getActivity(), TestService.class);
        assertThat(serviceContext).isEqualTo(serviceContext);
        assertThat(serviceContext).isNotEqualTo(null);
        assertThat(serviceContext).isNotEqualTo("test");
        assertThat(serviceContext).isNotEqualTo(
                serviceFrom(getActivity(), InvocationService.class));
        assertThat(serviceContext).isEqualTo(serviceFrom(getActivity(), TestService.class));
        assertThat(serviceContext.hashCode()).isEqualTo(
                serviceFrom(getActivity(), TestService.class).hashCode());
        final Intent intent = new Intent(getActivity(), TestService.class);
        serviceContext = serviceFrom(getActivity(), intent);
        assertThat(serviceContext).isEqualTo(serviceContext);
        assertThat(serviceContext).isNotEqualTo(null);
        assertThat(serviceContext).isNotEqualTo("test");
        assertThat(serviceContext).isNotEqualTo(
                serviceFrom(getActivity(), InvocationService.class));
        assertThat(serviceContext).isEqualTo(serviceFrom(getActivity(), intent));
        assertThat(serviceContext.hashCode()).isEqualTo(
                serviceFrom(getActivity(), intent).hashCode());
        final Intent intentExtra = new Intent(getActivity(), TestService.class);
        intentExtra.putExtra("test", true);
        serviceContext = serviceFrom(getActivity(), intentExtra);
        assertThat(serviceContext).isNotEqualTo(
                serviceFrom(getActivity(), InvocationService.class));
        final Intent intentExtra2 = new Intent(getActivity(), TestService.class);
        intentExtra2.putExtra("test", false);
        assertThat(serviceContext).isNotEqualTo(serviceFrom(getActivity(), intentExtra2));
        intentExtra2.putExtra("test", true);
        assertThat(serviceContext).isEqualTo(serviceFrom(getActivity(), intentExtra2));
        assertThat(serviceContext.hashCode()).isEqualTo(
                serviceFrom(getActivity(), intentExtra2).hashCode());
    }

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {
            serviceFrom(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            serviceFrom(getActivity(), (Class<InvocationService>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            serviceFrom(getActivity(), (Intent) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            serviceFrom(getActivity(), new Intent(getActivity(), Service.class));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }
}
