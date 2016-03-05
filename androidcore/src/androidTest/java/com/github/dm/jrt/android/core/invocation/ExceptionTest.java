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

package com.github.dm.jrt.android.core.invocation;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.TestActivity;
import com.github.dm.jrt.android.core.service.ServiceDisconnectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exceptions unit tests.
 * <p/>
 * Created by davide-maestroni on 12/25/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ExceptionTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ExceptionTest() {

        super(TestActivity.class);
    }

    public void testLoaderException() {

        assertThat(new LoaderInvocationException().getId()).isEqualTo(-1);
        assertThat(new LoaderInvocationException(13).getId()).isEqualTo(13);
        assertThat(new InvocationClashException().getId()).isEqualTo(-1);
        assertThat(new InvocationClashException(13).getId()).isEqualTo(13);
        assertThat(new InvocationTypeException().getId()).isEqualTo(-1);
        assertThat(new InvocationTypeException(13).getId()).isEqualTo(13);
        assertThat(new MissingInvocationException().getId()).isEqualTo(-1);
        assertThat(new MissingInvocationException(13).getId()).isEqualTo(13);
        assertThat(new StaleResultException().getId()).isEqualTo(-1);
        assertThat(new StaleResultException(13).getId()).isEqualTo(13);
    }

    public void testLoaderExceptionSerialization() throws IOException, ClassNotFoundException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new ObjectOutputStream(outputStream).writeObject(new LoaderInvocationException(17));
        Object object = new ObjectInputStream(
                new ByteArrayInputStream(outputStream.toByteArray())).readObject();
        assertThat(object).isExactlyInstanceOf(LoaderInvocationException.class);
        assertThat(((LoaderInvocationException) object).getId()).isEqualTo(17);
        outputStream = new ByteArrayOutputStream();
        new ObjectOutputStream(outputStream).writeObject(new InvocationClashException(17));
        object = new ObjectInputStream(
                new ByteArrayInputStream(outputStream.toByteArray())).readObject();
        assertThat(object).isExactlyInstanceOf(InvocationClashException.class);
        assertThat(((InvocationClashException) object).getId()).isEqualTo(17);
        outputStream = new ByteArrayOutputStream();
        new ObjectOutputStream(outputStream).writeObject(new InvocationTypeException(17));
        object = new ObjectInputStream(
                new ByteArrayInputStream(outputStream.toByteArray())).readObject();
        assertThat(object).isExactlyInstanceOf(InvocationTypeException.class);
        assertThat(((InvocationTypeException) object).getId()).isEqualTo(17);
        outputStream = new ByteArrayOutputStream();
        new ObjectOutputStream(outputStream).writeObject(new MissingInvocationException(17));
        object = new ObjectInputStream(
                new ByteArrayInputStream(outputStream.toByteArray())).readObject();
        assertThat(object).isExactlyInstanceOf(MissingInvocationException.class);
        assertThat(((MissingInvocationException) object).getId()).isEqualTo(17);
        outputStream = new ByteArrayOutputStream();
        new ObjectOutputStream(outputStream).writeObject(new StaleResultException(17));
        object = new ObjectInputStream(
                new ByteArrayInputStream(outputStream.toByteArray())).readObject();
        assertThat(object).isExactlyInstanceOf(StaleResultException.class);
        assertThat(((StaleResultException) object).getId()).isEqualTo(17);
    }

    public void testServiceException() {

        assertThat(new ServiceDisconnectedException().getComponentName()).isNull();
        assertThat(new ServiceDisconnectedException(
                getActivity().getComponentName()).getComponentName()).isEqualTo(
                getActivity().getComponentName());
    }

    public void testServiceExceptionSerialization() throws IOException, ClassNotFoundException {

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new ObjectOutputStream(outputStream).writeObject(
                new ServiceDisconnectedException(getActivity().getComponentName()));
        final Object object = new ObjectInputStream(
                new ByteArrayInputStream(outputStream.toByteArray())).readObject();
        assertThat(object).isExactlyInstanceOf(ServiceDisconnectedException.class);
        assertThat(((ServiceDisconnectedException) object).getComponentName()).isEqualTo(
                getActivity().getComponentName());
    }
}
