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
package com.bmd.wtf.spr;

import junit.framework.TestCase;

import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for gate classes.
 * <p/>
 * Created by davide on 6/27/14.
 */
public class SpringTest extends TestCase {

    private static <DATA> ArrayList<DATA> readData(final Spring<DATA> spring) {

        final ArrayList<DATA> list = new ArrayList<DATA>();

        while (spring.hasDrops()) {

            list.add(spring.nextDrop());
        }

        return list;
    }

    public void testDec() {

        assertThat(readData(Springs.dec(Byte.MIN_VALUE, (byte) 0))).isEmpty();
        assertThat(readData(Springs.dec(Byte.MIN_VALUE, (byte) 1))).containsExactly(Byte.MIN_VALUE);
        assertThat(Springs.dec((byte) -1, Byte.MAX_VALUE).nextDrop()).isEqualTo((byte) -1);
        assertThat(Springs.dec(Byte.MAX_VALUE, Byte.MAX_VALUE).nextDrop()).isEqualTo(
                Byte.MAX_VALUE);
    }

    public void testInc() {

        assertThat(readData(Springs.inc(Byte.MAX_VALUE, (byte) 0))).isEmpty();
        assertThat(readData(Springs.inc(Byte.MAX_VALUE, (byte) 1))).containsExactly(Byte.MAX_VALUE);
        assertThat(Springs.inc((byte) 0, Byte.MAX_VALUE).nextDrop()).isEqualTo((byte) 0);
        assertThat(Springs.inc(Byte.MIN_VALUE, Byte.MAX_VALUE).nextDrop()).isEqualTo(
                Byte.MIN_VALUE);
    }
}