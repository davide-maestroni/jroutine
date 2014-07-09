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
package com.bmd.wtf.fll;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for classification objects.
 * <p/>
 * Created by davide on 6/15/14.
 */
public class ClassificationTest extends TestCase {

    public void testEquals() {

        final Classification<String> classification1 = new Classification<String>() {};

        assertThat(classification1).isEqualTo(classification1);
        assertThat(classification1).isEqualTo(new StringClassification());
        assertThat(classification1).isEqualTo(new SubStringClassification());
        //noinspection ObjectEqualsNull
        assertThat(classification1.equals(null)).isFalse();

        final Classification<List<String>> classification2 = new Classification<List<String>>() {};

        assertThat(classification2).isNotEqualTo(new TestClassification<List<String>>());
        assertThat(classification2.equals(new TestClassification<List<Integer>>())).isFalse();
        assertThat(classification2.equals(new TestClassification<ArrayList<String>>())).isFalse();

        assertThat(
                classification2.isAssignableFrom(new Classification<List<String>>() {})).isTrue();
        assertThat(
                classification2.isAssignableFrom(new Classification<List<Integer>>() {})).isTrue();
        assertThat(classification2.isAssignableFrom(
                new Classification<ArrayList<String>>() {})).isTrue();

        assertThat(Classification.ofType(List.class)).isEqualTo(new Classification<List>() {});
    }

    public void testError() {

        try {

            new TestClassification<List<Integer>>().getRawType();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new SubTestClassification().getRawType();

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testType() {

        final Classification<String> classification1 = new Classification<String>() {};

        assertThat(classification1.getType()).isEqualTo(String.class);
        assertThat(String.class.equals(classification1.getRawType())).isTrue();
        assertThat(classification1.isInterface()).isFalse();

        final Classification<ArrayList<String>> classification2 =
                new Classification<ArrayList<String>>() {};

        assertThat(classification2.getType()).isNotEqualTo(ArrayList.class);
        assertThat(ArrayList.class.equals(classification2.getRawType())).isTrue();
        assertThat(classification1.isInterface()).isFalse();

        final Classification<List<String>> classification3 = new Classification<List<String>>() {};

        assertThat(classification3.getType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(classification3.getRawType())).isTrue();
        assertThat(classification3.isInterface()).isTrue();

        final Classification<List<ArrayList<String>>> classification4 =
                new Classification<List<ArrayList<String>>>() {};

        assertThat(classification4.getType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(classification4.getRawType())).isTrue();
        assertThat(classification4.isInterface()).isTrue();
    }

    private static class StringClassification extends Classification<String> {

    }

    private static class SubStringClassification extends StringClassification {

    }

    private static class SubTestClassification extends TestClassification<List<Integer>> {

    }

    private static class TestClassification<TEST extends List> extends Classification<TEST> {

    }
}