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
        assertThat(ReflectionUtils.boxingClass(void.class).equals(Void.class)).isTrue();
        assertThat(ReflectionUtils.boxingClass(int.class).equals(Integer.class)).isTrue();
        assertThat(ReflectionUtils.boxingClass(byte.class).equals(Byte.class)).isTrue();
        assertThat(ReflectionUtils.boxingClass(boolean.class).equals(Boolean.class)).isTrue();
        assertThat(ReflectionUtils.boxingClass(char.class).equals(Character.class)).isTrue();
        assertThat(ReflectionUtils.boxingClass(short.class).equals(Short.class)).isTrue();
        assertThat(ReflectionUtils.boxingClass(long.class).equals(Long.class)).isTrue();
        assertThat(ReflectionUtils.boxingClass(float.class).equals(Float.class)).isTrue();
        assertThat(ReflectionUtils.boxingClass(double.class).equals(Double.class)).isTrue();
        assertThat(ReflectionUtils.boxingClass(TestCase.class).equals(TestCase.class)).isTrue();
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