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
package com.bmd.wtf.rpd;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.flw.Fall;
import com.bmd.wtf.flw.Stream;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.rpd.RapidAnnotations.Generator;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for rapid generators classes.
 * <p/>
 * Created by davide on 7/9/14.
 */
public class RapidGeneratorsTest extends TestCase {

    public void testClashing() {

        try {

            Rapid.leapGenerator(LeapError1.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(LeapError2.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(LeapError3.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(LeapError4.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(LeapError5.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(LeapError6.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(new LeapGeneratorError1(),
                                new Classification<Leap<Object, Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(new LeapGeneratorError2(),
                                new Classification<Leap<Object, Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(new LeapGeneratorError3(),
                                new Classification<Leap<Object, Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(new LeapGeneratorError4(),
                                new Classification<Leap<Object, Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(new LeapGeneratorError5(),
                                new Classification<Leap<Object, Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(new LeapGeneratorError6(),
                                new Classification<Leap<Object, Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }
    }

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
        assertThat(
                ((CurrentX) Rapid.currentGenerator(CurrentX.class, this, new LinkedList<Object>(),
                                                   null, 2).create(1)).getNumber()
        ).isEqualTo(24);
        assertThat(((CurrentX) Rapid.currentGenerator(CurrentX.class, this, new ArrayList<Object>(),
                                                      null, 2).create(1)).getNumber()
        ).isEqualTo(46);
        assertThat(
                ((Current2) Rapid.currentGenerator(CurrentX1.class, this, new ArrayList<Object>(),
                                                   null, 4).create(1)).getNumber()
        ).isEqualTo(81);

        try {

            Rapid.currentGenerator(CurrentX.class, this, new CopyOnWriteArrayList<Object>(), null,
                                   4);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.currentGenerator(CurrentX1.class, this, new ArrayList<Object>(), null, 4, "test");

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
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator4(), Current.class,
                                                      new LinkedList<Object>(), null, 2)
                                    .create(1)).getNumber()).isEqualTo(2);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator4(), Current.class,
                                                      new LinkedList<Object>(), null)
                                    .create(1)).getNumber()).isEqualTo(1);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator4(), Current.class,
                                                      new ArrayList<Object>(), null)
                                    .create(1)).getNumber()).isEqualTo(3);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator5(), Current.class,
                                                      new LinkedList<Object>(), null, 2)
                                    .create(1)).getNumber()).isEqualTo(2);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator5(), Current.class,
                                                      new LinkedList<Object>(), null)
                                    .create(1)).getNumber()).isEqualTo(1);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator5(), Current.class,
                                                      new ArrayList<Object>(), null)
                                    .create(1)).getNumber()).isEqualTo(3);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator6(), Current.class,
                                                      new LinkedList<Object>(), null, 2)
                                    .create(1)).getNumber()).isEqualTo(2);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator6(), Current.class,
                                                      new LinkedList<Object>(), null)
                                    .create(1)).getNumber()).isEqualTo(44);
        assertThat(((Current2) Rapid.currentGenerator(new CurrentGenerator6(), Current.class,
                                                      new ArrayList<Object>(), null)
                                    .create(1)).getNumber()).isEqualTo(4);

        try {

            Rapid.currentGenerator(new CurrentGenerator6(), Current.class, new ArrayList<Object>(),
                                   2);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testLeapGenerator() {

        assertThat(Rapid.leapGenerator(Leap1.class).start(1)).isExactlyInstanceOf(Leap1.class);
        assertThat(((Leap2) Rapid.leapGenerator(Leap2.class).start(1)).getNumber()).isEqualTo(1);
        assertThat(((Leap3) Rapid.leapGenerator(Leap3.class).start(1)).getNumber()).isEqualTo(2);
        assertThat(((Leap4) Rapid.leapGenerator(Leap4.class).start(1)).getNumber()).isEqualTo(4);
        assertThat(((Leap5) Rapid.leapGenerator(Leap5.class).start(1)).getNumber()).isEqualTo(77);
        assertThat(((Leap6) Rapid.leapGenerator(Leap6.class).start(3)).getNumber()).isEqualTo(5);
        assertThat(((Leap7) Rapid.leapGenerator(Leap7.class).start(3)).getNumber()).isEqualTo(4);
        assertThat(((Leap8) Rapid.leapGenerator(Leap8.class).start(3)).getNumber()).isEqualTo(5);

        assertThat(Rapid.leapGenerator(Classification.ofType(Leap1.class))
                        .start(1)).isExactlyInstanceOf(Leap1.class);
        assertThat(((Leap2) Rapid.leapGenerator(Classification.ofType(Leap2.class))
                                 .start(1)).getNumber()
        ).isEqualTo(1);
        assertThat(((Leap3) Rapid.leapGenerator(Classification.ofType(Leap3.class))
                                 .start(1)).getNumber()
        ).isEqualTo(2);
        assertThat(((Leap4) Rapid.leapGenerator(Classification.ofType(Leap4.class))
                                 .start(1)).getNumber()
        ).isEqualTo(4);
        assertThat(((Leap5) Rapid.leapGenerator(Classification.ofType(Leap5.class))
                                 .start(1)).getNumber()
        ).isEqualTo(77);
        assertThat(((Leap6) Rapid.leapGenerator(Classification.ofType(Leap6.class))
                                 .start(3)).getNumber()
        ).isEqualTo(5);
        assertThat(((Leap7) Rapid.leapGenerator(Classification.ofType(Leap7.class))
                                 .start(3)).getNumber()
        ).isEqualTo(4);
        assertThat(((Leap8) Rapid.leapGenerator(Classification.ofType(Leap8.class))
                                 .start(3)).getNumber()
        ).isEqualTo(5);

        assertThat(Rapid.leapGenerator(new Leap1()).start(1)).isExactlyInstanceOf(Leap1.class);
        assertThat(((Leap2) Rapid.leapGenerator(new Leap2(22)).start(1)).getNumber()).isEqualTo(1);
        assertThat(((Leap3) Rapid.leapGenerator(new Leap3(33)).start(1)).getNumber()).isEqualTo(2);
        assertThat(((Leap4) Rapid.leapGenerator(new Leap4(44)).start(1)).getNumber()).isEqualTo(4);
        assertThat(((Leap5) Rapid.leapGenerator(new Leap5(55)).start(1)).getNumber()).isEqualTo(77);
        assertThat(((Leap6) Rapid.leapGenerator(new Leap6(66)).start(3)).getNumber()).isEqualTo(5);
        assertThat(((Leap7) Rapid.leapGenerator(new Leap7(77)).start(3)).getNumber()).isEqualTo(4);

        assertThat(
                ((LeapX) Rapid.leapGenerator(LeapX.class, this, new CopyOnWriteArrayList<Object>(),
                                             null, null).start(1)).getNumber()
        ).isEqualTo(33);
        assertThat(((LeapX) Rapid.leapGenerator(new LeapX(null, null, 11), this,
                                                new CopyOnWriteArrayList<Object>(), null, null)
                                 .start(1)).getNumber()).isEqualTo(33);
        assertThat(
                ((LeapX) Rapid.leapGenerator(LeapX.class, this, new CopyOnWriteArrayList<Object>(),
                                             null).start(1)).getNumber()
        ).isEqualTo(12);
        assertThat(
                ((LeapX) Rapid.leapGenerator(LeapX.class, this, new LinkedList<Object>(), null, 2)
                              .start(1)).getNumber()
        ).isEqualTo(24);
        assertThat(((LeapX) Rapid.leapGenerator(LeapX.class, this, new ArrayList<Object>(), null, 2)
                                 .start(1)).getNumber()
        ).isEqualTo(46);
        assertThat(
                ((Leap2) Rapid.leapGenerator(LeapX1.class, this, new ArrayList<Object>(), null, 4)
                              .start(1)).getNumber()
        ).isEqualTo(81);

        try {

            Rapid.leapGenerator(LeapX.class, this, new CopyOnWriteArrayList<Object>(), null, 4);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(LeapX1.class, this, new ArrayList<Object>(), null, 4, "test");

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.leapGenerator(LeapX2.class, this, new ArrayList<Object>(), 2);

            fail();

        } catch (final Exception ignored) {

        }

        final Classification<Leap<Object, Object, Object>> classification =
                new Classification<Leap<Object, Object, Object>>() {};

        assertThat(Rapid.leapGenerator(new LeapGenerator1(), classification)
                        .start(1)).isExactlyInstanceOf(Leap1.class);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator2(), classification)
                                 .start(1)).getNumber()).isEqualTo(1);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator2(),
                                                Classification.ofType(Leap2.class))
                                 .start(1)).getNumber()).isEqualTo(3);
        assertThat(Rapid.leapGenerator(new LeapGenerator3(), classification)
                        .start(1)).isExactlyInstanceOf(Leap1.class);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator4(), classification,
                                                new LinkedList<Object>(), null, 2)
                                 .start(1)).getNumber()).isEqualTo(2);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator4(), classification,
                                                new LinkedList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(1);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator4(), classification,
                                                new ArrayList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(3);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator5(), classification,
                                                new LinkedList<Object>(), null, 2)
                                 .start(1)).getNumber()).isEqualTo(2);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator5(), classification,
                                                new LinkedList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(1);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator5(), classification,
                                                new ArrayList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(3);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator6(), classification,
                                                new LinkedList<Object>(), null, 2)
                                 .start(1)).getNumber()).isEqualTo(2);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator6(), classification,
                                                new LinkedList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(44);
        assertThat(((Leap2) Rapid.leapGenerator(new LeapGenerator6(), classification,
                                                new ArrayList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(4);

        try {

            Rapid.leapGenerator(new LeapGenerator6(), classification, new ArrayList<Object>(), 2);

            fail();

        } catch (final Exception ignored) {

        }
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

        @SuppressWarnings("UnusedDeclaration")
        public Current3(final String ignored, final Integer number) {

            super(number + 11);
        }
    }

    public static class Current4 extends Current2 {

        public Current4(final int number) {

            super(number + 3);
        }

        @SuppressWarnings("UnusedDeclaration")
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

        @SuppressWarnings("UnusedDeclaration")
        public Current5(final Integer number) {

            super(number + 1);
        }
    }

    public static class Current6 extends Current2 {

        @SuppressWarnings("UnusedDeclaration")
        public Current6() {

            super(77);
        }

        public Current6(final int number) {

            super(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Current6(final Integer number) {

            super(number + 1);
        }
    }

    public static class Current7 extends Current2 {

        @SuppressWarnings("UnusedDeclaration")
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

        @SuppressWarnings("UnusedDeclaration")
        public Current generate() {

            return new Current1();
        }
    }

    public static class CurrentGenerator2 {

        @SuppressWarnings("UnusedDeclaration")
        public Current generate() {

            return new Current1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Current generate(final int number) {

            return new Current2(number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Current2 generate(final Integer number) {

            return new Current2(number + 2);
        }
    }

    public static class CurrentGenerator3 {

        @SuppressWarnings("UnusedDeclaration")
        Current generate() {

            return new Current1();
        }
    }

    public static class CurrentGenerator4 {

        @SuppressWarnings("UnusedDeclaration")
        public Current generate() {

            return new Current1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Current generate(ArrayList<?> list, int ignored, Integer number) {

            return new Current2(number + 3);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Current generate(ArrayList<?> list, String text, Integer number) {

            return new Current2(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Current generate1(List<?> list, String text, Integer number) {

            return new Current2(number);
        }
    }

    public static class CurrentGenerator5 {

        @Generator
        public Current generate() {

            return new Current1();
        }

        @Generator
        public Current generate(ArrayList<?> list, int ignored, Integer number) {

            return new Current2(number + 3);
        }

        @Generator
        public Current generate(ArrayList<?> list, String text, Integer number) {

            return new Current2(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Current generate1(List<?> list, String text, Integer number) {

            return new Current2(number);
        }
    }

    public static class CurrentGenerator6 {

        @Generator
        public Current generate(List<?> list, String text) {

            return new Current2(44);
        }

        @Generator
        public Current generate(ArrayList<?> list, String text, int number) {

            return new Current2(number + 3);
        }

        @Generator
        public Current generate(ArrayList<?> list, String text, Integer number) {

            return new Current2(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Current generate1(List<?> list, String text, Integer number) {

            return new Current2(number);
        }
    }

    public static class Leap1 extends FreeLeap<Object, Object> {

    }

    public static class Leap2 extends Leap1 {

        private final int mNumber;

        public Leap2(final int number) {

            mNumber = number;
        }

        public int getNumber() {

            return mNumber;
        }
    }

    public static class Leap3 extends Leap2 {

        public Leap3(final Integer number) {

            super(number + 1);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap3(final String ignored, final Integer number) {

            super(number + 11);
        }
    }

    public static class Leap4 extends Leap2 {

        public Leap4(final int number) {

            super(number + 3);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap4(final Integer number) {

            super(number + 2);
        }
    }

    public static class Leap5 extends Leap2 {

        @Generator
        public Leap5() {

            super(77);
        }

        public Leap5(final int number) {

            super(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap5(final Integer number) {

            super(number + 1);
        }
    }

    public static class Leap6 extends Leap2 {

        @SuppressWarnings("UnusedDeclaration")
        public Leap6() {

            super(77);
        }

        public Leap6(final int number) {

            super(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap6(final Integer number) {

            super(number + 1);
        }
    }

    public static class Leap7 extends Leap2 {

        @SuppressWarnings("UnusedDeclaration")
        public Leap7() {

            super(77);
        }

        public Leap7(final int number) {

            super(number + 2);
        }

        @Generator
        public Leap7(final Integer number) {

            super(number + 1);
        }
    }

    public static class Leap8 extends Leap2 {

        @Generator
        public Leap8() {

            super(88);
        }

        @Generator
        public Leap8(final int number) {

            super(number + 2);
        }

        @Generator
        public Leap8(final Integer number) {

            super(number + 1);
        }
    }

    public static class LeapError1 extends Leap1 {

        @Generator
        public LeapError1(final int ignored) {

        }

        @Generator
        public LeapError1(final Integer ignored) {

        }
    }

    public static class LeapError2 extends Leap1 {

        @Generator
        public LeapError2(final int ignored, final int number) {

        }

        @Generator
        public LeapError2(final Integer ignored, final int number) {

        }
    }

    public static class LeapError3 extends Leap1 {

        @Generator
        public LeapError3(final int ignored, final Integer number) {

        }

        @Generator
        public LeapError3(final Integer ignored, final Integer number) {

        }
    }

    public static class LeapError4 extends Leap1 {

        @SuppressWarnings("UnusedDeclaration")
        public LeapError4(final int ignored) {

        }

        @SuppressWarnings("UnusedDeclaration")
        public LeapError4(final Integer ignored) {

        }
    }

    public static class LeapError5 extends Leap1 {

        @SuppressWarnings("UnusedDeclaration")
        public LeapError5(final int ignored, final int number) {

        }

        @SuppressWarnings("UnusedDeclaration")
        public LeapError5(final Integer ignored, final int number) {

        }
    }

    public static class LeapError6 extends Leap1 {

        @SuppressWarnings("UnusedDeclaration")
        public LeapError6(final int ignored, final Integer number) {

        }

        @SuppressWarnings("UnusedDeclaration")
        public LeapError6(final Integer ignored, final Integer number) {

        }
    }

    public static class LeapGenerator1 {

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate() {

            return new Leap1();
        }
    }

    public static class LeapGenerator2 {

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate() {

            return new Leap1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate(final int number) {

            return new Leap2(number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap2 generate(final Integer number) {

            return new Leap2(number + 2);
        }
    }

    public static class LeapGenerator3 {

        @SuppressWarnings("UnusedDeclaration")
        Leap generate() {

            return new Leap1();
        }
    }

    public static class LeapGenerator4 {

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate() {

            return new Leap1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate(ArrayList<?> list, int ignored, Integer number) {

            return new Leap2(number + 3);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate(ArrayList<?> list, String text, Integer number) {

            return new Leap2(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate1(List<?> list, String text, Integer number) {

            return new Leap2(number);
        }
    }

    public static class LeapGenerator5 {

        @Generator
        public Leap generate() {

            return new Leap1();
        }

        @Generator
        public Leap generate(ArrayList<?> list, int ignored, Integer number) {

            return new Leap2(number + 3);
        }

        @Generator
        public Leap generate(ArrayList<?> list, String text, Integer number) {

            return new Leap2(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate1(List<?> list, String text, Integer number) {

            return new Leap2(number);
        }
    }

    public static class LeapGenerator6 {

        @Generator
        public Leap generate(List<?> list, String text) {

            return new Leap2(44);
        }

        @Generator
        public Leap generate(ArrayList<?> list, String text, int number) {

            return new Leap2(number + 3);
        }

        @Generator
        public Leap generate(ArrayList<?> list, String text, Integer number) {

            return new Leap2(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate1(List<?> list, String text, Integer number) {

            return new Leap2(number);
        }
    }

    public static class LeapGeneratorError1 {

        @Generator
        public Leap generate(final int ignored) {

            return new Leap1();
        }

        @Generator
        public Leap generate(final Integer ignored) {

            return new Leap1();
        }
    }

    public static class LeapGeneratorError2 {

        @Generator
        public Leap generate(final int ignored, final int number) {

            return new Leap1();
        }

        @Generator
        public Leap generate(final Integer ignored, final int number) {

            return new Leap1();
        }
    }

    public static class LeapGeneratorError3 {

        @Generator
        public Leap generate(final int ignored, final Integer number) {

            return new Leap1();
        }

        @Generator
        public Leap generate(final Integer ignored, final Integer number) {

            return new Leap1();
        }
    }

    public static class LeapGeneratorError4 {

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate(final int ignored) {

            return new Leap1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate(final Integer ignored) {

            return new Leap1();
        }
    }

    public static class LeapGeneratorError5 {

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate(final int ignored, final int number) {

            return new Leap1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate(final Integer ignored, final int number) {

            return new Leap1();
        }
    }

    public static class LeapGeneratorError6 {

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate(final int ignored, final Integer number) {

            return new Leap1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Leap generate(final Integer ignored, final Integer number) {

            return new Leap1();
        }
    }

    public class CurrentX extends Current2 {

        @SuppressWarnings("UnusedDeclaration")
        public CurrentX(List<?> list, String text, int number) {

            super(11 + number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public CurrentX(LinkedList<?> list, String text, int number) {

            super(22 + number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public CurrentX(List<?> list, String text, Integer number) {

            super(33 + ((number != null) ? number : 0));
        }

        @SuppressWarnings("UnusedDeclaration")
        public CurrentX(ArrayList<?> list, String text, Integer number) {

            super(44 + ((number != null) ? number : 0));
        }
    }

    public class CurrentX1 extends Current2 {

        @SuppressWarnings("UnusedDeclaration")
        public CurrentX1(List<?> list, String text, int number) {

            super(77 + number);
        }

        @SuppressWarnings("UnusedDeclaration")
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

    public class LeapX extends Leap2 {

        @SuppressWarnings("UnusedDeclaration")
        public LeapX(List<?> list, String text, int number) {

            super(11 + number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public LeapX(LinkedList<?> list, String text, int number) {

            super(22 + number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public LeapX(List<?> list, String text, Integer number) {

            super(33 + ((number != null) ? number : 0));
        }

        @SuppressWarnings("UnusedDeclaration")
        public LeapX(ArrayList<?> list, String text, Integer number) {

            super(44 + ((number != null) ? number : 0));
        }
    }

    public class LeapX1 extends Leap2 {

        @SuppressWarnings("UnusedDeclaration")
        public LeapX1(List<?> list, String text, int number) {

            super(77 + number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public LeapX1(int number) {

            super(99 + number);
        }
    }

    public class LeapX2 extends Leap2 {

        @Generator
        public LeapX2(List<?> list, String text, int number) {

            super(777 + number);
        }
    }
}