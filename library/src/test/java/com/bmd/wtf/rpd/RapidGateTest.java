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

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Gate.Action;
import com.bmd.wtf.flw.Gate.ConditionEvaluator;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.rpd.RapidAnnotations.GateCondition;

import junit.framework.TestCase;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bmd.wtf.fll.Waterfall.fall;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for rapid gate objects.
 * <p/>
 * Created by davide on 7/10/14.
 */
public class RapidGateTest extends TestCase {

    public void testError() {

        try {

            new DefaultRapidGate<Object>(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DefaultRapidGate<Object>(null, null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DefaultRapidGate<Object>(null, Object.class);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gate(fall().asGate().start(new FreeLeap<Object>()).on(FreeLeap.class)).perform();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gate(fall().asGate().start(new FreeLeap<Object>()).on(FreeLeap.class))
                 .performAs(FreeLeap.class);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gate(fall().asGate().start(new FreeLeap<Object>()).on(FreeLeap.class))
                 .performAs(Classification.ofType(FreeLeap.class));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gate(fall().asGate().start(new GateLeapError1()).on(FreeLeap.class))
                 .whenSatisfies(31)
                 .eventually()
                 .performAs(GateId.class)
                 .getId();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.gate(fall().asGate().start(new GateLeapError2()).on(FreeLeap.class))
                 .whenSatisfies(31)
                 .eventually()
                 .performAs(GateId.class)
                 .getId();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DefaultRapidGate<FreeLeap>(
                    fall().asGate().start(new FreeLeap<Object>()).on(FreeLeap.class),
                    FreeLeap.class).performAs(List.class);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testGate() {

        final GateLeap2 gateLeap = new GateLeap2(1);

        final Waterfall<Object, Object, Object> fall = fall().as(GateLeap2.class).chain(gateLeap);

        assertThat(Rapid.gate(fall.on(GateLeap.class))
                        .immediately()
                        .performAs(GateId.class)
                        .getId()).isEqualTo(1);

        assertThat(Rapid.gate(fall.on(Classification.ofType(GateLeap2.class)))
                        .immediately()
                        .performAs(Classification.ofType(GateId.class))
                        .getId()).isEqualTo(1);

        assertThat(Rapid.gate(fall.on(gateLeap))
                        .immediately()
                        .performAs(GateId.class)
                        .getId()).isEqualTo(1);

        assertThat(Rapid.gate(fall.on(gateLeap))
                        .immediately()
                        .performAs(Classification.ofType(GateId.class))
                        .getId()).isEqualTo(1);

        assertThat(Rapid.gate(fall.on(gateLeap))
                        .eventuallyThrow(new IllegalStateException())
                        .afterMax(1, TimeUnit.SECONDS)
                        .performAs(GateId.class)
                        .getId()).isEqualTo(1);

        assertThat(Rapid.gate(
                fall().inBackground().asGate().start(new GateLeap2(33)).on(GateLeap2.class))
                        .eventually()
                        .when(new ConditionEvaluator<GateId>() {

                            @Override
                            public boolean isSatisfied(final GateId leap) {

                                return (leap.getId() == 33);
                            }
                        })
                        .performAs(GateId.class)
                        .getId()).isEqualTo(33);

        assertThat(Rapid.gate(
                fall().inBackground().asGate().start(new GateLeap3()).on(GateLeap2.class))
                        .eventually()
                        .whenSatisfies(44)
                        .performAs(GateId.class)
                        .getId()).isEqualTo(17);

        assertThat(Rapid.gate(
                fall().inBackground().asGate().start(new GateLeap4()).on(GateLeap2.class))
                        .eventually()
                        .whenSatisfies(44)
                        .performAs(GateId.class)
                        .getId()).isEqualTo(71);

        assertThat(Rapid.gate(
                fall().inBackground().asGate().start(new GateLeap4()).on(GateLeap2.class))
                        .eventually()
                        .whenSatisfies(44)
                        .perform(new Action<Integer, GateLeap2>() {

                            @Override
                            public Integer doOn(final GateLeap2 leap, final Object... args) {

                                return leap.getId();
                            }
                        })).isEqualTo(71);
    }

    public interface GateId {

        public int getId();
    }

    public static class GateLeap extends FreeLeap<Object> implements GateId {

        @Override
        public int getId() {

            return 0;
        }
    }

    public static class GateLeap2 extends GateLeap {

        private int mId;

        public GateLeap2(final int id) {

            mId = id;
        }

        @Override
        public int getId() {

            return mId;
        }

        @SuppressWarnings("UnusedDeclaration")
        public boolean hasId(final int id) {

            return mId == id;
        }
    }

    public static class GateLeap3 extends GateLeap2 {

        public GateLeap3() {

            super(17);
        }

        @SuppressWarnings({"UnusedDeclaration", "BooleanParameter"})
        public boolean condition(final int id, final boolean isEqual) {

            return (isEqual) ? getId() == id : notId(id);
        }

        @SuppressWarnings({"UnusedDeclaration", "BooleanParameter"})
        public boolean condition(final String id, final boolean isEqual) {

            return (isEqual) ? Integer.toString(getId()).equals(id) : notId(id);
        }

        @GateCondition
        public boolean notId(final int id) {

            return getId() != id;
        }

        @GateCondition
        public boolean notId(final String id) {

            return !Integer.toString(getId()).equals(id);
        }
    }

    public static class GateLeap4 extends GateLeap2 {

        public GateLeap4() {

            super(71);
        }

        @SuppressWarnings("UnusedDeclaration")
        public boolean conditionFalse(final int id) {

            return false;
        }

        @SuppressWarnings("UnusedDeclaration")
        public boolean conditionTrue(final int id) {

            return true;
        }

        @GateCondition
        public boolean notId(final int id) {

            return getId() != id;
        }
    }

    public static class GateLeapError1 extends GateLeap2 {

        public GateLeapError1() {

            super(19);
        }

        @SuppressWarnings("UnusedDeclaration")
        public boolean isId(final int id) {

            return getId() == id;
        }

        @SuppressWarnings("UnusedDeclaration")
        public boolean notId(final int id) {

            return getId() != id;
        }
    }

    public static class GateLeapError2 extends GateLeap2 {

        public GateLeapError2() {

            super(23);
        }

        @GateCondition
        public boolean isId(final int id) {

            return getId() == id;
        }

        @GateCondition
        public boolean notId(final int id) {

            return getId() != id;
        }
    }
}