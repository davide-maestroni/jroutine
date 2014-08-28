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
import com.bmd.wtf.flw.Bridge.Action;
import com.bmd.wtf.flw.Bridge.ConditionEvaluator;
import com.bmd.wtf.gts.OpenGate;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.GateCondition;

import junit.framework.TestCase;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bmd.wtf.fll.Waterfall.fall;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for rapid bridge objects.
 * <p/>
 * Created by davide on 7/10/14.
 */
public class RapidBridgeTest extends TestCase {

    public void testBridge() {

        final BridgeGate2 bridgeGate = new BridgeGate2(1);

        final Waterfall<Object, Object, Object> fall =
                fall().bridge(BridgeGate2.class).chain(bridgeGate);

        assertThat(Rapid.bridge(fall.on(BridgeGate.class))
                        .immediately()
                        .performAs(BridgeId.class)
                        .getId()).isEqualTo(1);

        assertThat(Rapid.bridge(fall.on(Classification.ofType(BridgeGate2.class)))
                        .immediately()
                        .performAs(Classification.ofType(BridgeId.class))
                        .getId()).isEqualTo(1);

        assertThat(Rapid.bridge(fall.on(bridgeGate))
                        .immediately()
                        .performAs(BridgeId.class)
                        .getId()).isEqualTo(1);

        assertThat(Rapid.bridge(fall.on(bridgeGate))
                        .immediately()
                        .performAs(Classification.ofType(BridgeId.class))
                        .getId()).isEqualTo(1);

        assertThat(Rapid.bridge(fall.on(bridgeGate))
                        .eventuallyThrow(new IllegalStateException())
                        .afterMax(1, TimeUnit.SECONDS)
                        .performAs(BridgeId.class)
                        .getId()).isEqualTo(1);

        assertThat(Rapid.bridge(
                fall().inBackground().bridge().start(new BridgeGate2(33)).on(BridgeGate2.class))
                        .eventually()
                        .when(new ConditionEvaluator<BridgeId>() {

                            @Override
                            public boolean isSatisfied(final BridgeId gate) {

                                return (gate.getId() == 33);
                            }
                        })
                        .performAs(BridgeId.class)
                        .getId()).isEqualTo(33);

        assertThat(Rapid.bridge(
                fall().inBackground().bridge().start(new BridgeGate3()).on(BridgeGate2.class))
                        .eventually()
                        .whenSatisfies(44)
                        .performAs(BridgeId.class)
                        .getId()).isEqualTo(17);

        assertThat(Rapid.bridge(
                fall().inBackground().bridge().start(new BridgeGate4()).on(BridgeGate2.class))
                        .eventually()
                        .whenSatisfies(44)
                        .performAs(BridgeId.class)
                        .getId()).isEqualTo(71);

        assertThat(Rapid.bridge(
                fall().inBackground().bridge().start(new BridgeGate4()).on(BridgeGate2.class))
                        .eventually()
                        .whenSatisfies(44)
                        .perform(new Action<Integer, BridgeGate2>() {

                            @Override
                            public Integer doOn(final BridgeGate2 gate, final Object... args) {

                                return gate.getId();
                            }
                        })).isEqualTo(71);
    }

    public void testError() {

        try {

            new DefaultRapidBridge<Object>(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DefaultRapidBridge<Object>(null, null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DefaultRapidBridge<Object>(null, Object.class);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.bridge(fall().bridge().start(new OpenGate<Object>()).on(OpenGate.class))
                 .perform();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.bridge(fall().bridge().start(new OpenGate<Object>()).on(OpenGate.class))
                 .performAs(OpenGate.class);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.bridge(fall().bridge().start(new OpenGate<Object>()).on(OpenGate.class))
                 .performAs(Classification.ofType(OpenGate.class));

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.bridge(fall().bridge().start(new BridgeGateError1()).on(OpenGate.class))
                 .whenSatisfies(31)
                 .eventually()
                 .performAs(BridgeId.class)
                 .getId();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Rapid.bridge(fall().bridge().start(new BridgeGateError2()).on(OpenGate.class))
                 .whenSatisfies(31)
                 .eventually()
                 .performAs(BridgeId.class)
                 .getId();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new DefaultRapidBridge<OpenGate>(
                    fall().bridge().start(new OpenGate<Object>()).on(OpenGate.class),
                    OpenGate.class).performAs(List.class);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public interface BridgeId {

        public int getId();
    }

    public static class BridgeGate extends OpenGate<Object> implements BridgeId {

        @Override
        public int getId() {

            return 0;
        }
    }

    public static class BridgeGate2 extends BridgeGate {

        private int mId;

        public BridgeGate2(final int id) {

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

    public static class BridgeGate3 extends BridgeGate2 {

        public BridgeGate3() {

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

    public static class BridgeGate4 extends BridgeGate2 {

        public BridgeGate4() {

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

    public static class BridgeGateError1 extends BridgeGate2 {

        public BridgeGateError1() {

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

    public static class BridgeGateError2 extends BridgeGate2 {

        public BridgeGateError2() {

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