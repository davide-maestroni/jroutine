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

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Dam.Action;
import com.bmd.wtf.flw.Dam.ConditionEvaluator;
import com.bmd.wtf.lps.OpenGate;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.GateCondition;

import junit.framework.TestCase;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bmd.wtf.fll.Waterfall.fall;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for rapid dam objects.
 * <p/>
 * Created by davide on 7/10/14.
 */
public class RapidDamTest extends TestCase {

    public void testDam() {

        final DamGate2 damGate = new DamGate2(1);

        final Waterfall<Object, Object, Object> fall = fall().dam(DamGate2.class).chain(damGate);

        assertThat(Rapid.dam(fall.on(DamGate.class))
                        .immediately()
                        .performAs(DamId.class)
                        .getId()).isEqualTo(1);

        assertThat(Rapid.dam(fall.on(Classification.ofType(DamGate2.class)))
                        .immediately()
                        .performAs(Classification.ofType(DamId.class))
                        .getId()).isEqualTo(1);

        assertThat(
                Rapid.dam(fall.on(damGate)).immediately().performAs(DamId.class).getId()).isEqualTo(
                1);

        assertThat(Rapid.dam(fall.on(damGate))
                        .immediately()
                        .performAs(Classification.ofType(DamId.class))
                        .getId()).isEqualTo(1);

        assertThat(Rapid.dam(fall.on(damGate))
                        .eventuallyThrow(new IllegalStateException())
                        .afterMax(1, TimeUnit.SECONDS)
                        .performAs(DamId.class)
                        .getId()).isEqualTo(1);

        assertThat(Rapid.dam(fall().inBackground().dam().start(new DamGate2(33)).on(DamGate2.class))
                        .eventually()
                        .when(new ConditionEvaluator<DamId>() {

                            @Override
                            public boolean isSatisfied(final DamId gate) {

                                return (gate.getId() == 33);
                            }
                        })
                        .performAs(DamId.class)
                        .getId()).isEqualTo(33);

        assertThat(Rapid.dam(fall().inBackground().dam().start(new DamGate3()).on(DamGate2.class))
                        .eventually()
                        .whenSatisfies(44)
                        .performAs(DamId.class)
                        .getId()).isEqualTo(17);

        assertThat(Rapid.dam(fall().inBackground().dam().start(new DamGate4()).on(DamGate2.class))
                        .eventually()
                        .whenSatisfies(44)
                        .performAs(DamId.class)
                        .getId()).isEqualTo(71);

        assertThat(Rapid.dam(fall().inBackground().dam().start(new DamGate4()).on(DamGate2.class))
                        .eventually()
                        .whenSatisfies(44)
                        .perform(new Action<Integer, DamGate2>() {

                            @Override
                            public Integer doOn(final DamGate2 gate, final Object... args) {

                                return gate.getId();
                            }
                        })).isEqualTo(71);
    }

    public void testError() {

        try {

            new DefaultRapidDam<Object>(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DefaultRapidDam<Object>(null, null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DefaultRapidDam<Object>(null, Object.class);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.dam(fall().dam().start(new OpenGate<Object>()).on(OpenGate.class)).perform();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.dam(fall().dam().start(new OpenGate<Object>()).on(OpenGate.class))
                 .performAs(OpenGate.class);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.dam(fall().dam().start(new OpenGate<Object>()).on(OpenGate.class))
                 .performAs(Classification.ofType(OpenGate.class));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.dam(fall().dam().start(new DamGateError1()).on(OpenGate.class))
                 .whenSatisfies(31)
                 .eventually()
                 .performAs(DamId.class)
                 .getId();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.dam(fall().dam().start(new DamGateError2()).on(OpenGate.class))
                 .whenSatisfies(31)
                 .eventually()
                 .performAs(DamId.class)
                 .getId();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DefaultRapidDam<OpenGate>(
                    fall().dam().start(new OpenGate<Object>()).on(OpenGate.class),
                    OpenGate.class).performAs(List.class);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public interface DamId {

        public int getId();
    }

    public static class DamGate extends OpenGate<Object> implements DamId {

        @Override
        public int getId() {

            return 0;
        }
    }

    public static class DamGate2 extends DamGate {

        private int mId;

        public DamGate2(final int id) {

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

    public static class DamGate3 extends DamGate2 {

        public DamGate3() {

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

    public static class DamGate4 extends DamGate2 {

        public DamGate4() {

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

    public static class DamGateError1 extends DamGate2 {

        public DamGateError1() {

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

    public static class DamGateError2 extends DamGate2 {

        public DamGateError2() {

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