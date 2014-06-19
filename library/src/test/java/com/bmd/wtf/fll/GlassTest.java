package com.bmd.wtf.fll;

import com.bmd.wtf.flg.Gate;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by davide on 6/15/14.
 */
public class GlassTest extends TestCase {

    public void testType() {

        final Gate<String> gate1 = new Gate<String>() {};

        assertThat(gate1.getType()).isEqualTo(String.class);
        assertThat(String.class.equals(gate1.getRawType())).isTrue();
        assertThat(gate1.isInterface()).isFalse();

        final Gate<ArrayList<String>> gate2 = new Gate<ArrayList<String>>() {};

        assertThat(gate2.getType()).isNotEqualTo(ArrayList.class);
        assertThat(ArrayList.class.equals(gate2.getRawType())).isTrue();
        assertThat(gate1.isInterface()).isFalse();

        final Gate<List<String>> gate3 = new Gate<List<String>>() {};

        assertThat(gate3.getType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(gate3.getRawType())).isTrue();
        assertThat(gate3.isInterface()).isTrue();

        final Gate<List<ArrayList<String>>> gate4 = new Gate<List<ArrayList<String>>>() {};

        assertThat(gate4.getType()).isNotEqualTo(List.class);
        assertThat(List.class.equals(gate4.getRawType())).isTrue();
        assertThat(gate4.isInterface()).isTrue();
    }
}