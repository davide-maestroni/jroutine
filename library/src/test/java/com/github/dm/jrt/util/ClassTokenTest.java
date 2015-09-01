/*
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
package com.github.dm.jrt.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Class token unit tests.
 * <p/>
 * Created by davide-maestroni on 06/15/14.
 */
public class ClassTokenTest {

    @Test
    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    public void testArrayListType() {

        final ClassToken<String> classToken1 = new ClassToken<String>() {};
        final ClassToken<ArrayList<String>> classToken2 = new ClassToken<ArrayList<String>>() {};

        assertThat(classToken2.getGenericType()).isNotEqualTo(ArrayList.class);
        assertThat(ArrayList.class.equals(classToken2.getRawClass())).isTrue();
        assertThat(classToken1.isInterface()).isFalse();
    }

    @Test
    public void testCast() {

        final ClassToken<List<Integer>> classToken = new ClassToken<List<Integer>>() {};

        classToken.cast(new ArrayList<Integer>());

        try {

            final ArrayList<String> list = new ArrayList<String>();
            list.add("test");
            assertThat(classToken.cast(list).get(0) + 1).isNotZero();

            fail();

        } catch (final Exception ignored) {

        }
    }

    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() {

        final ClassToken<String> classToken1 = new ClassToken<String>() {};

        assertThat(classToken1).isEqualTo(classToken1);
        assertThat(classToken1).isEqualTo(new StringClassToken());
        assertThat(classToken1.hashCode()).isEqualTo(new StringClassToken().hashCode());
        assertThat(classToken1).isEqualTo(new SubStringClassToken());
        assertThat(classToken1.hashCode()).isEqualTo(new SubStringClassToken().hashCode());
        assertThat(classToken1.equals(null)).isFalse();
    }

    @Test
    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    public void testListArrayListType() {

        final ClassToken<List<ArrayList<String>>> classToken4 =
                new ClassToken<List<ArrayList<String>>>() {};

        assertThat(classToken4.getGenericType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(classToken4.getRawClass())).isTrue();
        assertThat(classToken4.isInterface()).isTrue();
    }

    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testListEquals() {

        final ClassToken<List<String>> classToken2 = new ClassToken<List<String>>() {};

        assertThat(classToken2.isAssignableFrom(new ClassToken<List<String>>() {})).isTrue();
        assertThat(classToken2.isAssignableFrom(new ClassToken<List<Integer>>() {})).isTrue();
        assertThat(classToken2.isAssignableFrom(new ClassToken<ArrayList<String>>() {})).isTrue();

        assertThat(ClassToken.tokenOf(List.class)).isEqualTo(new ClassToken<List>() {});
        assertThat(ClassToken.tokenOf(new ArrayList())).isEqualTo(new ClassToken<ArrayList>() {});
        assertThat(ClassToken.tokenOf(new ArrayList<String>())).isEqualTo(
                new ClassToken<ArrayList<String>>() {});
    }

    @Test
    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    public void testListType() {

        final ClassToken<List<String>> classToken3 = new ClassToken<List<String>>() {};

        assertThat(classToken3.getGenericType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(classToken3.getRawClass())).isTrue();
        assertThat(classToken3.isInterface()).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullClassError() {

        try {

            ClassToken.tokenOf((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullObjectError() {

        try {

            ClassToken.tokenOf(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSubClassError() {

        try {

            new TestClassToken<List<Integer>>().getRawClass();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            new SubTestClassToken().getRawClass();

            fail();

        } catch (final IllegalStateException ignored) {

        }
    }

    @Test
    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    public void testType() {

        final ClassToken<String> classToken1 = new ClassToken<String>() {};

        assertThat(classToken1.getGenericType()).isEqualTo(String.class);
        assertThat(String.class.equals(classToken1.getRawClass())).isTrue();
        assertThat(classToken1.isInterface()).isFalse();
    }

    private static class StringClassToken extends ClassToken<String> {

    }

    private static class SubStringClassToken extends StringClassToken {

    }

    private static class SubTestClassToken extends TestClassToken<List<Integer>> {

    }

    private static class TestClassToken<TEST extends List> extends ClassToken<TEST> {

    }
}
