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

package com.github.dm.jrt.core.util;

import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Reflection utils unit tests.
 * <p>
 * Created by davide-maestroni on 10/04/2014.
 */
public class ReflectionTest {

    @Test
    public void testBoxingClass() {
        assertThat(Void.class.equals(Reflection.boxingClass(void.class))).isTrue();
        assertThat(Integer.class.equals(Reflection.boxingClass(int.class))).isTrue();
        assertThat(Byte.class.equals(Reflection.boxingClass(byte.class))).isTrue();
        assertThat(Boolean.class.equals(Reflection.boxingClass(boolean.class))).isTrue();
        assertThat(Character.class.equals(Reflection.boxingClass(char.class))).isTrue();
        assertThat(Short.class.equals(Reflection.boxingClass(short.class))).isTrue();
        assertThat(Long.class.equals(Reflection.boxingClass(long.class))).isTrue();
        assertThat(Float.class.equals(Reflection.boxingClass(float.class))).isTrue();
        assertThat(Double.class.equals(Reflection.boxingClass(double.class))).isTrue();
        assertThat(Reflection.class.equals(Reflection.boxingClass(Reflection.class))).isTrue();
    }

    @Test
    public void testBoxingDefault() {
        assertThat(Reflection.boxingDefault(void.class)).isNull();
        assertThat(Reflection.boxingDefault(int.class)).isEqualTo(0);
        assertThat(Reflection.boxingDefault(byte.class)).isEqualTo((byte) 0);
        assertThat(Reflection.boxingDefault(boolean.class)).isEqualTo(false);
        assertThat(Reflection.boxingDefault(char.class)).isEqualTo((char) 0);
        assertThat(Reflection.boxingDefault(short.class)).isEqualTo((short) 0);
        assertThat(Reflection.boxingDefault(long.class)).isEqualTo((long) 0);
        assertThat(Reflection.boxingDefault(float.class)).isEqualTo((float) 0);
        assertThat(Reflection.boxingDefault(double.class)).isEqualTo((double) 0);
        assertThat(Reflection.boxingDefault(Reflection.class)).isNull();
    }

    @Test
    public void testConstructor() {
        boolean failed = false;
        try {
            new Reflection();
            failed = true;

        } catch (final Throwable ignored) {
        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testFindConstructor() {
        assertThat(Reflection.findBestMatchingConstructor(TestClass.class)).isNotNull();
        assertThat(Reflection.findBestMatchingConstructor(TestClass.class, "test")).isNotNull();
        assertThat(Reflection.findBestMatchingConstructor(TestClass.class, new ArrayList<String>()))
                .isNotNull();
    }

    @Test
    public void testFindConstructorError() {
        try {
            Reflection.findBestMatchingConstructor(TestClass.class, 4);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testFindConstructorNullParamError() {
        try {
            Reflection.findBestMatchingConstructor(TestClass.class, (Object) null);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testFindConstructorParamNumberError() {
        try {
            Reflection.findBestMatchingConstructor(TestClass.class, "test", 4);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testFindMethod() {
        assertThat(Reflection.findBestMatchingMethod(TestClass.class)).isNotNull();
        assertThat(Reflection.findBestMatchingMethod(TestClass.class, "test")).isNotNull();
        assertThat(Reflection.findBestMatchingMethod(TestClass.class,
                new ArrayList<String>())).isNotNull();
        assertThat(Reflection.findBestMatchingMethod(TestClass.class, new Exception())).isNotNull();
    }

    @Test
    public void testFindMethodError() {
        try {
            Reflection.findBestMatchingMethod(TestClass.class, 4);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testFindMethodNullParamError() {
        try {
            Reflection.findBestMatchingMethod(TestClass.class, (Object) null);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testFindMethodParamNumberError() {
        try {
            Reflection.findBestMatchingMethod(TestClass.class, "test", 4);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testNewInstance() {
        assertThat(Reflection.newInstanceOf(String.class)).isExactlyInstanceOf(String.class);
        try {
            Reflection.newInstanceOf(Integer.class);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            Reflection.newInstanceOf(InputStream.class);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @SuppressWarnings("unused")
    public static class TestClass {

        public TestClass() {
        }

        public TestClass(final String ignored) {
        }

        public TestClass(final int ignored) {
        }

        public TestClass(final Integer ignored) {
        }

        private TestClass(final LinkedList<String> ignored) {
        }

        private TestClass(final ArrayList<String> ignored) {
        }

        private TestClass(final List<String> ignored) {
        }

        public void run(final int ignore) {
        }

        public void run(final Integer ignored) {
        }

        public void run(final String ignored) {
        }

        protected void run() {
        }

        void run(final Exception ignored) {
        }

        private void run(final LinkedList<String> ignored) {
        }

        private void run(final ArrayList<String> ignored) {
        }

        private void run(final List<String> ignored) {
        }
    }
}
