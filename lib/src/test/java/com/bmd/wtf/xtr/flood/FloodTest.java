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
package com.bmd.wtf.xtr.flood;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;

import junit.framework.TestCase;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for flood control classes.
 * <p/>
 * Created by davide on 5/5/14.
 */
public class FloodTest extends TestCase {

    private FloodControl<Integer, String, TestObserver> mControl;

    public void testEquals() {

        final FloodControl<Integer, String, TestObserver> control = mControl;

        final TestObserver observer1 = new TestObserver() {

            @Override
            public Object onDischarge(final Floodgate<Integer, String> gate, final Integer drop) {

                gate.discharge(drop.toString());

                return null;
            }

            @Override
            public Object onFlush(final Floodgate<Integer, String> gate) {

                gate.flush();

                return null;
            }

            @Override
            public Object onPullDebris(final Floodgate<Integer, String> gate, final Object debris) {

                return debris;
            }

            @Override
            public Object onPushDebris(final Floodgate<Integer, String> gate, final Object debris) {

                return debris;
            }
        };

        assertThat(control.controlling(observer1)).isNull();
        assertThat(control.controllers()).isEmpty();

        final Levee<Integer, String, TestObserver> levee1 = control.leveeControlledBy(observer1);

        assertThat(observer1.equals(observer1)).isTrue();
        assertThat(levee1.equals(levee1)).isTrue();
        assertThat(levee1.equals(observer1)).isFalse();
        assertThat(observer1.equals(levee1)).isFalse();
        assertThat(levee1.controller().equals(levee1)).isFalse();
        assertThat(levee1.equals(levee1.controller())).isFalse();
        assertThat(levee1.controller() == levee1).isFalse();
        assertThat(levee1.controller().equals(levee1.controller())).isTrue();
        assertThat(control.controller().equals(levee1.controller())).isTrue();
        assertThat(control.controlling(observer1).equals(levee1.controller())).isTrue();
        assertThat(control.controllers()).hasSize(1);

        final Levee<Integer, String, TestObserver> levee2 = control.leveeControlledBy(observer1);

        assertThat(levee1.controller().equals(levee2.controller())).isTrue();
        assertThat(levee2.controller().equals(levee1.controller())).isTrue();
        assertThat(control.controller().equals(levee1.controller())).isTrue();
        assertThat(control.controlling(observer1).equals(levee1.controller())).isTrue();
        assertThat(control.controllers()).hasSize(1);

        final TestObserver observer2 = new TestObserver() {

            @Override
            public Object onDischarge(final Floodgate<Integer, String> gate, final Integer drop) {

                gate.discharge(drop.toString());

                return null;
            }

            @Override
            public Object onFlush(final Floodgate<Integer, String> gate) {

                gate.flush();

                return null;
            }

            @Override
            public Object onPullDebris(final Floodgate<Integer, String> gate, final Object debris) {

                return debris;
            }

            @Override
            public Object onPushDebris(final Floodgate<Integer, String> gate, final Object debris) {

                return debris;
            }
        };

        final Levee<Integer, String, TestObserver> levee3 = control.leveeControlledBy(observer2);

        assertThat(levee1.controller().equals(levee3.controller())).isFalse();
        assertThat(levee3.controller().equals(levee1.controller())).isFalse();
        assertThat(levee3.controller().equals(levee2.controller())).isFalse();
        assertThat(levee2.controller().equals(levee3.controller())).isFalse();
        assertThat(control.controlling(observer1).equals(levee1.controller())).isTrue();
        assertThat(control.controlling(observer2).equals(levee3.controller())).isTrue();
        assertThat(control.controllers()).hasSize(2);
    }

    public void testErrors() {

        final FloodControl<Integer, String, TestObserver> control = mControl;

        try {

            control.controller();

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            control.controlling(null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            control.leveeControlledBy(null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            new FloodControl<Integer, String, TestObserver>(null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            new FloodControl<String, String, TestObserverImpl>(TestObserverImpl.class);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }
    }

    public void testLevee() {

        final FloodControl<String, String, ObjectObserver> control =
                new FloodControl<String, String, ObjectObserver>(ObjectObserver.class);

        final Spring<String> spring = Waterfall.fallingFrom(new OpenDam<String>() {

            @Override
            public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

                if ("push".equals(drop)) {

                    return drop;
                }

                return super.onDischarge(gate, drop);
            }

        }).thenFlowingThrough(control.leveeControlledBy(new ObjectObserverImpl()))
                                               .thenFlowingThrough(new OpenDam<String>() {

                                                   @Override
                                                   public Object onDischarge(
                                                           final Floodgate<String, String> gate,
                                                           final String drop) {

                                                       if ("pull".equals(drop)) {

                                                           return drop;
                                                       }

                                                       return super.onDischarge(gate, drop);
                                                   }

                                               }).backToSource();

        final ObjectObserver controller = control.controller();

        assertThat(controller.getLastDrop()).isNull();
        assertThat(controller.getLastPulledDebris()).isNull();
        assertThat(controller.getLastPushedDebris()).isNull();

        spring.discharge("test");

        assertThat(controller.getLastDrop()).isEqualTo("test");
        assertThat(controller.getLastPulledDebris()).isNull();
        assertThat(controller.getLastPushedDebris()).isNull();

        spring.discharge("test1");

        assertThat(controller.getLastDrop()).isEqualTo("test1");
        assertThat(controller.getLastPulledDebris()).isNull();
        assertThat(controller.getLastPushedDebris()).isNull();

        spring.discharge("push");

        assertThat(controller.getLastDrop()).isEqualTo("test1");
        assertThat(controller.getLastPulledDebris()).isNull();
        assertThat(controller.getLastPushedDebris()).isEqualTo("push");

        spring.discharge("test2");

        assertThat(controller.getLastDrop()).isEqualTo("test2");
        assertThat(controller.getLastPulledDebris()).isNull();
        assertThat(controller.getLastPushedDebris()).isEqualTo("push");

        spring.discharge("pull");

        assertThat(controller.getLastDrop()).isEqualTo("pull");
        assertThat(controller.getLastPulledDebris()).isEqualTo("pull");
        assertThat(controller.getLastPushedDebris()).isEqualTo("push");

        spring.flush();

        assertThat(controller.getLastDrop()).isEqualTo("pull");
        assertThat(controller.getLastPulledDebris()).isEqualTo("pull");
        assertThat(controller.getLastPushedDebris()).isEqualTo("push");
    }

    @Override
    protected void setUp() throws Exception {

        mControl = new FloodControl<Integer, String, TestObserver>(TestObserver.class);
    }

    private interface ObjectObserver extends FloodObserver<String, String> {

        public String getLastDrop();

        public Object getLastPulledDebris();

        public Object getLastPushedDebris();
    }

    private interface TestObserver extends FloodObserver<Integer, String> {

    }

    private static class ObjectObserverImpl extends OpenDam<String> implements ObjectObserver {

        private String mLastDrop;

        private Object mLastPulledDebris;

        private Object mLastPushedDebris;

        @Override
        public String getLastDrop() {

            return mLastDrop;
        }

        @Override
        public Object getLastPulledDebris() {

            return mLastPulledDebris;
        }

        @Override
        public Object getLastPushedDebris() {

            return mLastPushedDebris;
        }

        @Override
        public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

            mLastDrop = drop;

            return super.onDischarge(gate, drop);
        }

        @Override
        public Object onPullDebris(final Floodgate<String, String> gate, final Object debris) {

            mLastPulledDebris = debris;

            return super.onPullDebris(gate, debris);
        }

        @Override
        public Object onPushDebris(final Floodgate<String, String> gate, final Object debris) {

            mLastPushedDebris = debris;

            return super.onPushDebris(gate, debris);
        }
    }

    private static class TestObserverImpl extends OpenDam<String>
            implements FloodObserver<String, String> {

    }
}