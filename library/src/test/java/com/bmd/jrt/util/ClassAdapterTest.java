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
package com.bmd.jrt.util;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for classification objects.
 * <p/>
 * Created by davide on 6/15/14.
 */
public class ClassAdapterTest extends TestCase {

    public void testEquals() {

        final ClassAdapter<String> classAdapter1 = new ClassAdapter<String>() {};

        assertThat(classAdapter1).isEqualTo(classAdapter1);
        assertThat(classAdapter1).isEqualTo(new StringClassAdapter());
        assertThat(classAdapter1).isEqualTo(new SubStringClassAdapter());
        //noinspection ObjectEqualsNull
        assertThat(classAdapter1.equals(null)).isFalse();

        final ClassAdapter<List<String>> classAdapter2 = new ClassAdapter<List<String>>() {};

        assertThat(classAdapter2.isAssignableFrom(new ClassAdapter<List<String>>() {})).isTrue();
        assertThat(classAdapter2.isAssignableFrom(new ClassAdapter<List<Integer>>() {})).isTrue();
        assertThat(
                classAdapter2.isAssignableFrom(new ClassAdapter<ArrayList<String>>() {})).isTrue();

        assertThat(ClassAdapter.adapt(List.class)).isEqualTo(new ClassAdapter<List>() {});
        assertThat(ClassAdapter.classOf(new ArrayList())).isEqualTo(
                new ClassAdapter<ArrayList>() {});
        assertThat(ClassAdapter.classOf(new ArrayList<String>())).isEqualTo(
                new ClassAdapter<ArrayList<String>>() {});
    }

    public void testError() {

        try {

            new TestClassAdapter<List<Integer>>().getRawClass();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new SubTestClassAdapter().getRawClass();

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testType() {

        final ClassAdapter<String> classAdapter1 = new ClassAdapter<String>() {};

        assertThat(classAdapter1.getGenericType()).isEqualTo(String.class);
        assertThat(String.class.equals(classAdapter1.getRawClass())).isTrue();
        assertThat(classAdapter1.isInterface()).isFalse();

        final ClassAdapter<ArrayList<String>> classAdapter2 =
                new ClassAdapter<ArrayList<String>>() {};

        assertThat(classAdapter2.getGenericType()).isNotEqualTo(ArrayList.class);
        assertThat(ArrayList.class.equals(classAdapter2.getRawClass())).isTrue();
        assertThat(classAdapter1.isInterface()).isFalse();

        final ClassAdapter<List<String>> classAdapter3 = new ClassAdapter<List<String>>() {};

        assertThat(classAdapter3.getGenericType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(classAdapter3.getRawClass())).isTrue();
        assertThat(classAdapter3.isInterface()).isTrue();

        final ClassAdapter<List<ArrayList<String>>> classAdapter4 =
                new ClassAdapter<List<ArrayList<String>>>() {};

        assertThat(classAdapter4.getGenericType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(classAdapter4.getRawClass())).isTrue();
        assertThat(classAdapter4.isInterface()).isTrue();
    }

    private static class StringClassAdapter extends ClassAdapter<String> {

    }

    private static class SubStringClassAdapter extends StringClassAdapter {

    }

    private static class SubTestClassAdapter extends TestClassAdapter<List<Integer>> {

    }

    private static class TestClassAdapter<TEST extends List> extends ClassAdapter<TEST> {

    }
}