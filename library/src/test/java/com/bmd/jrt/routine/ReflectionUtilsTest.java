/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.jrt.routine;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Reflection utils unit tests.
 * <p/>
 * Created by davide on 10/4/14.
 */
public class ReflectionUtilsTest extends TestCase {

    public void testBoxingClass() {

        assertThat(ReflectionUtils.boxingClass(null)).isNull();
        assertThat(Void.class.equals(ReflectionUtils.boxingClass(void.class))).isTrue();
        assertThat(Integer.class.equals(ReflectionUtils.boxingClass(int.class))).isTrue();
        assertThat(Byte.class.equals(ReflectionUtils.boxingClass(byte.class))).isTrue();
        assertThat(Boolean.class.equals(ReflectionUtils.boxingClass(boolean.class))).isTrue();
        assertThat(Character.class.equals(ReflectionUtils.boxingClass(char.class))).isTrue();
        assertThat(Short.class.equals(ReflectionUtils.boxingClass(short.class))).isTrue();
        assertThat(Long.class.equals(ReflectionUtils.boxingClass(long.class))).isTrue();
        assertThat(Float.class.equals(ReflectionUtils.boxingClass(float.class))).isTrue();
        assertThat(Double.class.equals(ReflectionUtils.boxingClass(double.class))).isTrue();
        assertThat(TestCase.class.equals(ReflectionUtils.boxingClass(TestCase.class))).isTrue();
    }

    public void testConstructor() {

        assertThat(ReflectionUtils.findConstructor(TestClass.class)).isNotNull();
        assertThat(ReflectionUtils.findConstructor(TestClass.class, "test")).isNotNull();

        try {

            ReflectionUtils.findConstructor(TestClass.class, 4);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            ReflectionUtils.findConstructor(TestClass.class, "test", 4);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        assertThat(ReflectionUtils.findConstructor(TestClass.class,
                                                   new ArrayList<String>())).isNotNull();

        try {

            ReflectionUtils.findConstructor(TestClass.class, (Object) null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("UnusedDeclaration")
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
    }
}