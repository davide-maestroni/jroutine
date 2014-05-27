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
package com.bmd.wtf.xtr.fld;

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
            public void onDischarge(final Floodgate<Integer, String> gate, final Integer drop) {

                gate.discharge(drop.toString());
            }

            @Override
            public void onFlush(final Floodgate<Integer, String> gate) {

                gate.flush();
            }

            @Override
            public void onDrop(final Floodgate<Integer, String> gate, final Object debris) {

                gate.drop(debris);
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
            public void onDischarge(final Floodgate<Integer, String> gate, final Integer drop) {

                gate.discharge(drop.toString());
            }

            @Override
            public void onFlush(final Floodgate<Integer, String> gate) {

                gate.flush();
            }

            @Override
            public void onDrop(final Floodgate<Integer, String> gate, final Object debris) {

                gate.drop(debris);
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

        } catch (final Exception ignored) {

        }

        try {

            control.controlling(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            control.leveeControlledBy(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new FloodControl<Integer, String, TestObserver>(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new FloodControl<String, String, TestObserverImpl>(TestObserverImpl.class);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testLevee() {

        final FloodControl<String, String, ObjectObserver> control =
                new FloodControl<String, String, ObjectObserver>(ObjectObserver.class);

        final Spring<String> spring = Waterfall.fallingFrom(new OpenDam<String>() {

            @Override
            public void onDischarge(final Floodgate<String, String> gate, final String drop) {

                if ("push".equals(drop)) {

                    gate.drop(drop);

                    return;
                }

                super.onDischarge(gate, drop);
            }

        }).thenFallingThrough(control.leveeControlledBy(new ObjectObserverImpl()))
                                               .thenFallingThrough(new OpenDam<String>() {

                                                   @Override
                                                   public void onDischarge(
                                                           final Floodgate<String, String> gate,
                                                           final String drop) {

                                                       if ("pull".equals(drop)) {

                                                           gate.drop(drop);

                                                           return;
                                                       }

                                                       super.onDischarge(gate, drop);
                                                   }

                                               }).backToSource();

        final ObjectObserver controller = control.controller();

        assertThat(controller.getLastDrop()).isNull();
        assertThat(controller.getLastDebris()).isNull();

        spring.discharge("test");

        assertThat(controller.getLastDrop()).isEqualTo("test");
        assertThat(controller.getLastDebris()).isNull();

        spring.discharge("test1");

        assertThat(controller.getLastDrop()).isEqualTo("test1");
        assertThat(controller.getLastDebris()).isNull();

        spring.discharge("push");

        assertThat(controller.getLastDrop()).isEqualTo("test1");
        assertThat(controller.getLastDebris()).isEqualTo("push");

        spring.discharge("test2");

        assertThat(controller.getLastDrop()).isEqualTo("test2");
        assertThat(controller.getLastDebris()).isEqualTo("push");

        spring.discharge("pull");

        assertThat(controller.getLastDrop()).isEqualTo("pull");
        assertThat(controller.getLastDebris()).isEqualTo("push");

        spring.flush();

        assertThat(controller.getLastDrop()).isEqualTo("pull");
        assertThat(controller.getLastDebris()).isEqualTo("push");
    }

    @Override
    protected void setUp() throws Exception {

        mControl = new FloodControl<Integer, String, TestObserver>(TestObserver.class);
    }

    private interface ObjectObserver extends FloodObserver<String, String> {

        public Object getLastDebris();

        public String getLastDrop();
    }

    private interface TestObserver extends FloodObserver<Integer, String> {

    }

    private static class ObjectObserverImpl extends OpenDam<String> implements ObjectObserver {

        private Object mLastDebris;

        private String mLastDrop;

        @Override
        public Object getLastDebris() {

            return mLastDebris;
        }

        @Override
        public String getLastDrop() {

            return mLastDrop;
        }

        @Override
        public void onDischarge(final Floodgate<String, String> gate, final String drop) {

            mLastDrop = drop;

            super.onDischarge(gate, drop);
        }

        @Override
        public void onDrop(final Floodgate<String, String> gate, final Object debris) {

            mLastDebris = debris;

            super.onDrop(gate, debris);
        }
    }

    private static class TestObserverImpl extends OpenDam<String>
            implements FloodObserver<String, String> {

    }
}