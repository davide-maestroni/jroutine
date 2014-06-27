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
}