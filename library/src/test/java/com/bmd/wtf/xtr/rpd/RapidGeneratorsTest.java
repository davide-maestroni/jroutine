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
import com.bmd.wtf.gts.Gate;
import com.bmd.wtf.gts.OpenGate;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.Generator;

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

            Rapid.gateGenerator(GateError1.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(GateError2.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(GateError3.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(GateError4.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(GateError5.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(GateError6.class, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(new GateGeneratorError1(),
                                new Classification<Gate<Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(new GateGeneratorError2(),
                                new Classification<Gate<Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(new GateGeneratorError3(),
                                new Classification<Gate<Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(new GateGeneratorError4(),
                                new Classification<Gate<Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(new GateGeneratorError5(),
                                new Classification<Gate<Object, Object>>() {}, 2);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(new GateGeneratorError6(),
                                new Classification<Gate<Object, Object>>() {}, 2);

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
                        .getNumber()).isEqualTo(1);
        assertThat(
                ((Current3) Rapid.currentGenerator(Classification.ofType(Current3.class)).create(1))
                        .getNumber()).isEqualTo(2);
        assertThat(
                ((Current4) Rapid.currentGenerator(Classification.ofType(Current4.class)).create(1))
                        .getNumber()).isEqualTo(4);
        assertThat(
                ((Current5) Rapid.currentGenerator(Classification.ofType(Current5.class)).create(1))
                        .getNumber()).isEqualTo(77);
        assertThat(
                ((Current6) Rapid.currentGenerator(Classification.ofType(Current6.class)).create(3))
                        .getNumber()).isEqualTo(5);
        assertThat(
                ((Current7) Rapid.currentGenerator(Classification.ofType(Current7.class)).create(3))
                        .getNumber()).isEqualTo(4);
        assertThat(
                ((Current8) Rapid.currentGenerator(Classification.ofType(Current8.class)).create(3))
                        .getNumber()).isEqualTo(5);

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
                                                   null, 2).create(1)).getNumber()).isEqualTo(24);
        assertThat(((CurrentX) Rapid.currentGenerator(CurrentX.class, this, new ArrayList<Object>(),
                                                      null, 2).create(1)).getNumber()).isEqualTo(
                46);
        assertThat(
                ((Current2) Rapid.currentGenerator(CurrentX1.class, this, new ArrayList<Object>(),
                                                   null, 4).create(1)).getNumber()).isEqualTo(81);

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

    public void testGateGenerator() {

        assertThat(Rapid.gateGenerator(Gate1.class).start(1)).isExactlyInstanceOf(Gate1.class);
        assertThat(((Gate2) Rapid.gateGenerator(Gate2.class).start(1)).getNumber()).isEqualTo(1);
        assertThat(((Gate3) Rapid.gateGenerator(Gate3.class).start(1)).getNumber()).isEqualTo(2);
        assertThat(((Gate4) Rapid.gateGenerator(Gate4.class).start(1)).getNumber()).isEqualTo(4);
        assertThat(((Gate5) Rapid.gateGenerator(Gate5.class).start(1)).getNumber()).isEqualTo(77);
        assertThat(((Gate6) Rapid.gateGenerator(Gate6.class).start(3)).getNumber()).isEqualTo(5);
        assertThat(((Gate7) Rapid.gateGenerator(Gate7.class).start(3)).getNumber()).isEqualTo(4);
        assertThat(((Gate8) Rapid.gateGenerator(Gate8.class).start(3)).getNumber()).isEqualTo(5);

        assertThat(Rapid.gateGenerator(Classification.ofType(Gate1.class))
                        .start(1)).isExactlyInstanceOf(Gate1.class);
        assertThat(((Gate2) Rapid.gateGenerator(Classification.ofType(Gate2.class))
                                 .start(1)).getNumber()).isEqualTo(1);
        assertThat(((Gate3) Rapid.gateGenerator(Classification.ofType(Gate3.class))
                                 .start(1)).getNumber()).isEqualTo(2);
        assertThat(((Gate4) Rapid.gateGenerator(Classification.ofType(Gate4.class))
                                 .start(1)).getNumber()).isEqualTo(4);
        assertThat(((Gate5) Rapid.gateGenerator(Classification.ofType(Gate5.class))
                                 .start(1)).getNumber()).isEqualTo(77);
        assertThat(((Gate6) Rapid.gateGenerator(Classification.ofType(Gate6.class))
                                 .start(3)).getNumber()).isEqualTo(5);
        assertThat(((Gate7) Rapid.gateGenerator(Classification.ofType(Gate7.class))
                                 .start(3)).getNumber()).isEqualTo(4);
        assertThat(((Gate8) Rapid.gateGenerator(Classification.ofType(Gate8.class))
                                 .start(3)).getNumber()).isEqualTo(5);

        assertThat(Rapid.gateGenerator(new Gate1()).start(1)).isExactlyInstanceOf(Gate1.class);
        assertThat(((Gate2) Rapid.gateGenerator(new Gate2(22)).start(1)).getNumber()).isEqualTo(1);
        assertThat(((Gate3) Rapid.gateGenerator(new Gate3(33)).start(1)).getNumber()).isEqualTo(2);
        assertThat(((Gate4) Rapid.gateGenerator(new Gate4(44)).start(1)).getNumber()).isEqualTo(4);
        assertThat(((Gate5) Rapid.gateGenerator(new Gate5(55)).start(1)).getNumber()).isEqualTo(77);
        assertThat(((Gate6) Rapid.gateGenerator(new Gate6(66)).start(3)).getNumber()).isEqualTo(5);
        assertThat(((Gate7) Rapid.gateGenerator(new Gate7(77)).start(3)).getNumber()).isEqualTo(4);

        assertThat(
                ((GateX) Rapid.gateGenerator(GateX.class, this, new CopyOnWriteArrayList<Object>(),
                                             null, null).start(1)).getNumber()).isEqualTo(33);
        assertThat(((GateX) Rapid.gateGenerator(new GateX(null, null, 11), this,
                                                new CopyOnWriteArrayList<Object>(), null, null)
                                 .start(1)).getNumber()).isEqualTo(33);
        assertThat(
                ((GateX) Rapid.gateGenerator(GateX.class, this, new CopyOnWriteArrayList<Object>(),
                                             null).start(1)).getNumber()).isEqualTo(12);
        assertThat(
                ((GateX) Rapid.gateGenerator(GateX.class, this, new LinkedList<Object>(), null, 2)
                              .start(1)).getNumber()).isEqualTo(24);
        assertThat(((GateX) Rapid.gateGenerator(GateX.class, this, new ArrayList<Object>(), null, 2)
                                 .start(1)).getNumber()).isEqualTo(46);
        assertThat(
                ((Gate2) Rapid.gateGenerator(GateX1.class, this, new ArrayList<Object>(), null, 4)
                              .start(1)).getNumber()).isEqualTo(81);

        try {

            Rapid.gateGenerator(GateX.class, this, new CopyOnWriteArrayList<Object>(), null, 4);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(GateX1.class, this, new ArrayList<Object>(), null, 4, "test");

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gateGenerator(GateX2.class, this, new ArrayList<Object>(), 2);

            fail();

        } catch (final Exception ignored) {

        }

        final Classification<Gate<Object, Object>> classification =
                new Classification<Gate<Object, Object>>() {};

        assertThat(Rapid.gateGenerator(new GateGenerator1(), classification)
                        .start(1)).isExactlyInstanceOf(Gate1.class);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator2(), classification)
                                 .start(1)).getNumber()).isEqualTo(1);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator2(),
                                                Classification.ofType(Gate2.class))
                                 .start(1)).getNumber()).isEqualTo(3);
        assertThat(Rapid.gateGenerator(new GateGenerator3(), classification)
                        .start(1)).isExactlyInstanceOf(Gate1.class);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator4(), classification,
                                                new LinkedList<Object>(), null, 2)
                                 .start(1)).getNumber()).isEqualTo(2);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator4(), classification,
                                                new LinkedList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(1);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator4(), classification,
                                                new ArrayList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(3);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator5(), classification,
                                                new LinkedList<Object>(), null, 2)
                                 .start(1)).getNumber()).isEqualTo(2);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator5(), classification,
                                                new LinkedList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(1);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator5(), classification,
                                                new ArrayList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(3);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator6(), classification,
                                                new LinkedList<Object>(), null, 2)
                                 .start(1)).getNumber()).isEqualTo(2);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator6(), classification,
                                                new LinkedList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(44);
        assertThat(((Gate2) Rapid.gateGenerator(new GateGenerator6(), classification,
                                                new ArrayList<Object>(), null)
                                 .start(1)).getNumber()).isEqualTo(4);

        try {

            Rapid.gateGenerator(new GateGenerator6(), classification, new ArrayList<Object>(), 2);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public static class Current1 implements Current {

        @Override
        public <DATA> void flush(final Fall<DATA> fall, final Stream<DATA> origin) {

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

    public static class Gate1 extends OpenGate<Object> {

    }

    public static class Gate2 extends Gate1 {

        private final int mNumber;

        public Gate2(final int number) {

            mNumber = number;
        }

        public int getNumber() {

            return mNumber;
        }
    }

    public static class Gate3 extends Gate2 {

        public Gate3(final Integer number) {

            super(number + 1);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate3(final String ignored, final Integer number) {

            super(number + 11);
        }
    }

    public static class Gate4 extends Gate2 {

        public Gate4(final int number) {

            super(number + 3);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate4(final Integer number) {

            super(number + 2);
        }
    }

    public static class Gate5 extends Gate2 {

        @Generator
        public Gate5() {

            super(77);
        }

        public Gate5(final int number) {

            super(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate5(final Integer number) {

            super(number + 1);
        }
    }

    public static class Gate6 extends Gate2 {

        @SuppressWarnings("UnusedDeclaration")
        public Gate6() {

            super(77);
        }

        public Gate6(final int number) {

            super(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate6(final Integer number) {

            super(number + 1);
        }
    }

    public static class Gate7 extends Gate2 {

        @SuppressWarnings("UnusedDeclaration")
        public Gate7() {

            super(77);
        }

        public Gate7(final int number) {

            super(number + 2);
        }

        @Generator
        public Gate7(final Integer number) {

            super(number + 1);
        }
    }

    public static class Gate8 extends Gate2 {

        @Generator
        public Gate8() {

            super(88);
        }

        @Generator
        public Gate8(final int number) {

            super(number + 2);
        }

        @Generator
        public Gate8(final Integer number) {

            super(number + 1);
        }
    }

    public static class GateError1 extends Gate1 {

        @Generator
        public GateError1(final int ignored) {

        }

        @Generator
        public GateError1(final Integer ignored) {

        }
    }

    public static class GateError2 extends Gate1 {

        @Generator
        public GateError2(final int ignored, final int number) {

        }

        @Generator
        public GateError2(final Integer ignored, final int number) {

        }
    }

    public static class GateError3 extends Gate1 {

        @Generator
        public GateError3(final int ignored, final Integer number) {

        }

        @Generator
        public GateError3(final Integer ignored, final Integer number) {

        }
    }

    public static class GateError4 extends Gate1 {

        @SuppressWarnings("UnusedDeclaration")
        public GateError4(final int ignored) {

        }

        @SuppressWarnings("UnusedDeclaration")
        public GateError4(final Integer ignored) {

        }
    }

    public static class GateError5 extends Gate1 {

        @SuppressWarnings("UnusedDeclaration")
        public GateError5(final int ignored, final int number) {

        }

        @SuppressWarnings("UnusedDeclaration")
        public GateError5(final Integer ignored, final int number) {

        }
    }

    public static class GateError6 extends Gate1 {

        @SuppressWarnings("UnusedDeclaration")
        public GateError6(final int ignored, final Integer number) {

        }

        @SuppressWarnings("UnusedDeclaration")
        public GateError6(final Integer ignored, final Integer number) {

        }
    }

    public static class GateGenerator1 {

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate() {

            return new Gate1();
        }
    }

    public static class GateGenerator2 {

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate() {

            return new Gate1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate(final int number) {

            return new Gate2(number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate2 generate(final Integer number) {

            return new Gate2(number + 2);
        }
    }

    public static class GateGenerator3 {

        @SuppressWarnings("UnusedDeclaration")
        Gate generate() {

            return new Gate1();
        }
    }

    public static class GateGenerator4 {

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate() {

            return new Gate1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate(ArrayList<?> list, int ignored, Integer number) {

            return new Gate2(number + 3);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate(ArrayList<?> list, String text, Integer number) {

            return new Gate2(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate1(List<?> list, String text, Integer number) {

            return new Gate2(number);
        }
    }

    public static class GateGenerator5 {

        @Generator
        public Gate generate() {

            return new Gate1();
        }

        @Generator
        public Gate generate(ArrayList<?> list, int ignored, Integer number) {

            return new Gate2(number + 3);
        }

        @Generator
        public Gate generate(ArrayList<?> list, String text, Integer number) {

            return new Gate2(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate1(List<?> list, String text, Integer number) {

            return new Gate2(number);
        }
    }

    public static class GateGenerator6 {

        @Generator
        public Gate generate(List<?> list, String text) {

            return new Gate2(44);
        }

        @Generator
        public Gate generate(ArrayList<?> list, String text, int number) {

            return new Gate2(number + 3);
        }

        @Generator
        public Gate generate(ArrayList<?> list, String text, Integer number) {

            return new Gate2(number + 2);
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate1(List<?> list, String text, Integer number) {

            return new Gate2(number);
        }
    }

    public static class GateGeneratorError1 {

        @Generator
        public Gate generate(final int ignored) {

            return new Gate1();
        }

        @Generator
        public Gate generate(final Integer ignored) {

            return new Gate1();
        }
    }

    public static class GateGeneratorError2 {

        @Generator
        public Gate generate(final int ignored, final int number) {

            return new Gate1();
        }

        @Generator
        public Gate generate(final Integer ignored, final int number) {

            return new Gate1();
        }
    }

    public static class GateGeneratorError3 {

        @Generator
        public Gate generate(final int ignored, final Integer number) {

            return new Gate1();
        }

        @Generator
        public Gate generate(final Integer ignored, final Integer number) {

            return new Gate1();
        }
    }

    public static class GateGeneratorError4 {

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate(final int ignored) {

            return new Gate1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate(final Integer ignored) {

            return new Gate1();
        }
    }

    public static class GateGeneratorError5 {

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate(final int ignored, final int number) {

            return new Gate1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate(final Integer ignored, final int number) {

            return new Gate1();
        }
    }

    public static class GateGeneratorError6 {

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate(final int ignored, final Integer number) {

            return new Gate1();
        }

        @SuppressWarnings("UnusedDeclaration")
        public Gate generate(final Integer ignored, final Integer number) {

            return new Gate1();
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

    public class GateX extends Gate2 {

        @SuppressWarnings("UnusedDeclaration")
        public GateX(List<?> list, String text, int number) {

            super(11 + number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public GateX(LinkedList<?> list, String text, int number) {

            super(22 + number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public GateX(List<?> list, String text, Integer number) {

            super(33 + ((number != null) ? number : 0));
        }

        @SuppressWarnings("UnusedDeclaration")
        public GateX(ArrayList<?> list, String text, Integer number) {

            super(44 + ((number != null) ? number : 0));
        }
    }

    public class GateX1 extends Gate2 {

        @SuppressWarnings("UnusedDeclaration")
        public GateX1(List<?> list, String text, int number) {

            super(77 + number);
        }

        @SuppressWarnings("UnusedDeclaration")
        public GateX1(int number) {

            super(99 + number);
        }
    }

    public class GateX2 extends Gate2 {

        @Generator
        public GateX2(List<?> list, String text, int number) {

            super(777 + number);
        }
    }
}