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
package com.bmd.wtf.xtr.rpd;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.flw.Fall;
import com.bmd.wtf.flw.Stream;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.Generator;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for rapid classes.
 * <p/>
 * Created by davide on 7/9/14.
 */
public class RapidTest extends TestCase {

    public void testCurrentGenerator() {

        assertThat(Rapid.currentGenerator(Current1.class).create(1)).isExactlyInstanceOf(
                Current1.class);
        assertThat(((Current2) Rapid.currentGenerator(Current2.class)
                                    .create(1)).getNumber()).isEqualTo(1);
        assertThat(((Current3) Rapid.currentGenerator(Current3.class)
                                    .create(1)).getNumber()).isEqualTo(2);
        assertThat(((Current4) Rapid.currentGenerator(Current4.class)
                                    .create(1)).getNumber()).isEqualTo(4);
        assertThat(((Current5) Rapid.currentGenerator(Current5.class)
                                    .create(1)).getNumber()).isEqualTo(77);
        assertThat(((Current6) Rapid.currentGenerator(Current6.class)
                                    .create(3)).getNumber()).isEqualTo(5);
        assertThat(((Current7) Rapid.currentGenerator(Current7.class)
                                    .create(3)).getNumber()).isEqualTo(4);
        assertThat(((Current8) Rapid.currentGenerator(Current8.class)
                                    .create(3)).getNumber()).isEqualTo(5);

        assertThat(Rapid.currentGenerator(Classification.ofType(Current1.class))
                        .create(1)).isExactlyInstanceOf(Current1.class);
        assertThat(
                ((Current2) Rapid.currentGenerator(Classification.ofType(Current2.class)).create(1))
                        .getNumber()
        ).isEqualTo(1);
        assertThat(
                ((Current3) Rapid.currentGenerator(Classification.ofType(Current3.class)).create(1))
                        .getNumber()
        ).isEqualTo(2);
        assertThat(
                ((Current4) Rapid.currentGenerator(Classification.ofType(Current4.class)).create(1))
                        .getNumber()
        ).isEqualTo(4);
        assertThat(
                ((Current5) Rapid.currentGenerator(Classification.ofType(Current5.class)).create(1))
                        .getNumber()
        ).isEqualTo(77);
        assertThat(
                ((Current6) Rapid.currentGenerator(Classification.ofType(Current6.class)).create(3))
                        .getNumber()
        ).isEqualTo(5);
        assertThat(
                ((Current7) Rapid.currentGenerator(Classification.ofType(Current7.class)).create(3))
                        .getNumber()
        ).isEqualTo(4);
        assertThat(
                ((Current8) Rapid.currentGenerator(Classification.ofType(Current8.class)).create(3))
                        .getNumber()
        ).isEqualTo(5);

        assertThat(Rapid.currentGenerator(new Current1()).create(1)).isExactlyInstanceOf(
                Current1.class);
        assertThat(((Current2) Rapid.currentGenerator(new Current2(22))
                                    .create(1)).getNumber()).isEqualTo(1);
        assertThat(((Current3) Rapid.currentGenerator(new Current3(33))
                                    .create(1)).getNumber()).isEqualTo(2);
        assertThat(((Current4) Rapid.currentGenerator(new Current4(44))
                                    .create(1)).getNumber()).isEqualTo(4);
        assertThat(((Current5) Rapid.currentGenerator(new Current5(55))
                                    .create(1)).getNumber()).isEqualTo(77);
        assertThat(((Current6) Rapid.currentGenerator(new Current6(66))
                                    .create(3)).getNumber()).isEqualTo(5);
        assertThat(((Current7) Rapid.currentGenerator(new Current7(77))
                                    .create(3)).getNumber()).isEqualTo(4);

        assertThat(((CurrentX) Rapid.currentGenerator(CurrentX.class, this,
                                                      new CopyOnWriteArrayList<Object>(), null,
                                                      null).create(1)).getNumber()).isEqualTo(33);
        assertThat(((CurrentX) Rapid.currentGenerator(new CurrentX(null, null, 11), this,
                                                      new CopyOnWriteArrayList<Object>(), null,
                                                      null).create(1)).getNumber()).isEqualTo(33);
        assertThat(((CurrentX) Rapid.currentGenerator(CurrentX.class, this,
                                                      new CopyOnWriteArrayList<Object>(), null)
                                    .create(1)).getNumber()).isEqualTo(12);
        assertThat(((CurrentX) Rapid.currentGenerator(CurrentX.class, this,
                                                      new CopyOnWriteArrayList<Object>(), null, 4)
                                    .create(1)).getNumber()).isEqualTo(37);
        assertThat(
                ((CurrentX) Rapid.currentGenerator(CurrentX.class, this, new LinkedList<Object>(),
                                                   null, 2).create(1)).getNumber()
        ).isEqualTo(35);
        assertThat(((CurrentX) Rapid.currentGenerator(CurrentX.class, this, new ArrayList<Object>(),
                                                      null, 2).create(1)).getNumber()
        ).isEqualTo(46);

        try {

            Rapid.currentGenerator(CurrentX1.class, this, new ArrayList<Object>(), null, 4);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.currentGenerator(CurrentX2.class, this, new ArrayList<Object>(), 2);

            fail();

        } catch (final Exception ignored) {

        }

        assertThat(Rapid.currentGenerator(new CurrentGenerator1(), Current.class)
                        .create(1)).isExactlyInstanceOf(Current1.class);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator2(), Current.class)
                                    .create(1)).getNumber()).isEqualTo(1);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator2(), Current2.class)
                                    .create(1)).getNumber()).isEqualTo(3);
        assertThat(Rapid.currentGenerator(new CurrentGenerator3(), Current.class)
                        .create(1)).isExactlyInstanceOf(Current1.class);
    }

    public static class Current1 implements Current {

        @Override
        public <DATA> void discharge(final Fall<DATA> fall, final Stream<DATA> origin) {

        }

        @Override
        public void forward(final Fall<?> fall, final Throwable throwable) {

        }

        @Override
        public <DATA> void push(final Fall<DATA> fall, final DATA drop) {

        }

        @Override
        public <DATA> void pushAfter(final Fall<DATA> fall, final long delay,
                final TimeUnit timeUnit, final DATA drop) {

        }

        @Override
        public <DATA> void pushAfter(final Fall<DATA> fall, final long delay,
                final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        }
    }

    public static class Current2 extends Current1 {

        private final int mNumber;

        public Current2(final int number) {

            mNumber = number;
        }

        public int getNumber() {

            return mNumber;
        }
    }

    public static class Current3 extends Current2 {

        public Current3(final Integer number) {

            super(number + 1);
        }

        public Current3(final String ignored, final Integer number) {

            super(number + 11);
        }
    }

    public static class Current4 extends Current2 {

        public Current4(final int number) {

            super(number + 3);
        }

        public Current4(final Integer number) {

            super(number + 2);
        }
    }

    public static class Current5 extends Current2 {

        @Generator
        public Current5() {

            super(77);
        }

        public Current5(final int number) {

            super(number + 2);
        }

        public Current5(final Integer number) {

            super(number + 1);
        }
    }

    public static class Current6 extends Current2 {

        public Current6() {

            super(77);
        }

        public Current6(final int number) {

            super(number + 2);
        }

        public Current6(final Integer number) {

            super(number + 1);
        }
    }

    public static class Current7 extends Current2 {

        public Current7() {

            super(77);
        }

        public Current7(final int number) {

            super(number + 2);
        }

        @Generator
        public Current7(final Integer number) {

            super(number + 1);
        }
    }

    public static class Current8 extends Current2 {

        @Generator
        public Current8() {

            super(88);
        }

        @Generator
        public Current8(final int number) {

            super(number + 2);
        }

        @Generator
        public Current8(final Integer number) {

            super(number + 1);
        }
    }

    public static class CurrentGenerator1 {

        public Current generate() {

            return new Current1();
        }
    }

    public static class CurrentGenerator2 {

        public Current generate() {

            return new Current1();
        }

        public Current generate(final int number) {

            return new Current2(number);
        }

        public Current2 generate(final Integer number) {

            return new Current2(number + 2);
        }
    }

    public static class CurrentGenerator3 {

        Current generate() {

            return new Current1();
        }
    }

    public class CurrentX extends Current2 {

        public CurrentX(List<?> list, String text, int number) {

            super(11 + number);
        }

        public CurrentX(LinkedList<?> list, String text, int number) {

            super(22 + number);
        }

        public CurrentX(List<?> list, String text, Integer number) {

            super(33 + ((number != null) ? number : 0));
        }

        public CurrentX(ArrayList<?> list, String text, Integer number) {

            super(44 + ((number != null) ? number : 0));
        }
    }

    public class CurrentX1 extends Current2 {

        public CurrentX1(List<?> list, String text, int number) {

            super(77 + number);
        }

        public CurrentX1(int number) {

            super(99 + number);
        }
    }

    public class CurrentX2 extends Current2 {

        @Generator
        public CurrentX2(List<?> list, String text, int number) {

            super(777 + number);
        }
    }
}