package com.bmd.wtf.fll;

import com.bmd.wtf.flw.Glass;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by davide on 6/15/14.
 */
public class GlassTest extends TestCase {

    public void testType() {

        final Glass<String> glass1 = new Glass<String>() {};

        assertThat(glass1.getType()).isEqualTo(String.class);
        assertThat(String.class.equals(glass1.getRawType())).isTrue();
        assertThat(glass1.isInterface()).isFalse();

        final Glass<ArrayList<String>> glass2 = new Glass<ArrayList<String>>() {};

        assertThat(glass2.getType()).isNotEqualTo(ArrayList.class);
        assertThat(ArrayList.class.equals(glass2.getRawType())).isTrue();
        assertThat(glass1.isInterface()).isFalse();

        final Glass<List<String>> glass3 = new Glass<List<String>>() {};

        assertThat(glass3.getType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(glass3.getRawType())).isTrue();
        assertThat(glass3.isInterface()).isTrue();

        final Glass<List<ArrayList<String>>> glass4 = new Glass<List<ArrayList<String>>>() {};

        assertThat(glass4.getType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(glass4.getRawType())).isTrue();
        assertThat(glass4.isInterface()).isTrue();
    }
}