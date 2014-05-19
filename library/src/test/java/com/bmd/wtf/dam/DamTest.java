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
package com.bmd.wtf.dam;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.bsn.Basin;

import junit.framework.TestCase;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link com.bmd.wtf.dam.Dam} classes.
 * <p/>
 * Created by davide on 4/8/14.
 */
public class DamTest extends TestCase {

    public void testClosed() {

        final Basin<Object, Object> basin1 =
                Basin.collect(Waterfall.fallingFrom(new AbstractDam<Object, Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      if (drop == null) {

                                          throw new IllegalArgumentException();
                                      }

                                      gate.discharge(drop);

                                      return null;
                                  }
                              }).thenFlowingThrough(Dams.openDam())
                );
        final Basin<Object, Object> basin2 =
                Basin.collect(basin1.thenFlow().thenFlowingThrough(Dams.closedDam()));

        basin1.thenFeedWith("test");
        assertThat(basin1.collectFirstOutput()).isEqualTo("test");
        assertThat(basin2.collectFirstOutput()).isNull();

        basin2.thenFeedWith("test");
        assertThat(basin1.collectFirstOutput()).isEqualTo("test");
        assertThat(basin2.collectFirstOutput()).isNull();

        basin1.thenFlow().backToSource().discharge((Object) null);
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();

        basin2.thenFlow().backToSource().discharge((Object) null);
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();

        final Basin<Object, Object> basin3 = Basin.collect(Waterfall.fallingFrom(Dams.openDam()));
        basin3.thenFlow().thenFlowingThrough(new AbstractDam<Object, Object>() {

            @Override
            public Object onDischarge(final Floodgate<Object, Object> gate, final Object drop) {

                throw new NullPointerException();
            }
        });

        basin2.thenFlow().thenFeeding(basin3.thenFlow());

        basin3.thenFeedWith("test");
        assertThat(basin3.collectFirstOutput()).isEqualTo("test");
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectFirstPulledDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin2.collectFirstOutput()).isNull();
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin1.collectFirstOutput()).isNull();
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
    }

    public void testDecorator() {

        final Dam<Object, Object> dam1 = Dams.openDam();
        final Dam<Object, Object> dam2 = Dams.openDam();

        final MyDecorator decorator1 = new MyDecorator(dam1);
        final MyDecorator decorator2 = new MyDecorator(dam1);
        final MyDecorator decorator3 = new MyDecorator(dam2);

        assertThat(decorator1.getWrapped()).isEqualTo(dam1);
        assertThat(decorator1).isEqualTo(decorator1);
        assertThat(decorator1).isEqualTo(decorator2);
        assertThat(decorator1.hashCode()).isEqualTo(decorator2.hashCode());
        assertThat(decorator1).isNotEqualTo(new MyDecorator(decorator1));
        assertThat(decorator1).isNotEqualTo(null);
        assertThat(decorator1).isNotEqualTo(decorator3);

        try {

            new MyDecorator(null);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testDownstreamError() {

        final Basin<Object, Object> basin1 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      final String string = drop.toString();

                                      if (string.startsWith("push")) {

                                          throw new IllegalStateException(string);
                                      }

                                      return super.onDischarge(gate, drop);
                                  }

                              })
                );
        final Basin<Object, Object> basin2 = Basin.collect(basin1.thenFlow().thenFlowingThrough(
                                                                   Dams.downstreamDebris(
                                                                           new Dam<Object, Object>() {

                                                                               private Object mLast;

                                                                               @Override
                                                                               public Object onDischarge(
                                                                                       final Floodgate<Object, Object> gate,
                                                                                       final Object drop) {

                                                                                   mLast = drop;

                                                                                   if ("discharge1"
                                                                                           .equals(drop)) {

                                                                                       throw new IllegalArgumentException(
                                                                                               "discharge1");
                                                                                   }

                                                                                   gate.discharge(
                                                                                           drop);

                                                                                   return null;
                                                                               }

                                                                               @Override
                                                                               public Object onFlush(
                                                                                       final Floodgate<Object, Object> gate) {

                                                                                   if ("flush1"
                                                                                           .equals(mLast)) {

                                                                                       throw new IllegalArgumentException(
                                                                                               "flush1");
                                                                                   }

                                                                                   gate.flush();

                                                                                   return null;
                                                                               }

                                                                               @Override
                                                                               public Object onPullDebris(
                                                                                       final Floodgate<Object, Object> gate,
                                                                                       final Object debris) {

                                                                                   if ((debris instanceof Throwable)
                                                                                           && "pull1"
                                                                                           .equals(((Throwable) debris)
                                                                                                           .getMessage())) {

                                                                                       throw new IllegalArgumentException(
                                                                                               "pull1");
                                                                                   }

                                                                                   return debris;
                                                                               }

                                                                               @Override
                                                                               public Object onPushDebris(
                                                                                       final Floodgate<Object, Object> gate,
                                                                                       final Object debris) {

                                                                                   if ((debris instanceof Throwable)
                                                                                           && "push1"
                                                                                           .equals(((Throwable) debris)
                                                                                                           .getMessage())) {

                                                                                       throw new IllegalArgumentException(
                                                                                               "push1");
                                                                                   }

                                                                                   return debris;
                                                                               }
                                                                           }
                                                                   )
                                                           )
        );
        final Basin<Object, Object> basin3 = Basin.collect(basin2.thenFlow().thenFlowingThrough(
                                                                   Dams.downstreamDebris(
                                                                           new Dam<Object, Object>() {

                                                                               private Object mLast;

                                                                               @Override
                                                                               public Object onDischarge(
                                                                                       final Floodgate<Object, Object> gate,
                                                                                       final Object drop) {

                                                                                   mLast = drop;

                                                                                   if ("discharge2"
                                                                                           .equals(drop)) {

                                                                                       throw new IllegalArgumentException(
                                                                                               "discharge2");
                                                                                   }

                                                                                   final String
                                                                                           string =
                                                                                           drop.toString();

                                                                                   if (string
                                                                                           .startsWith(
                                                                                                   "pull")
                                                                                           || string
                                                                                           .startsWith(
                                                                                                   "push")) {

                                                                                       throw new IllegalStateException(
                                                                                               string);

                                                                                   } else {

                                                                                       gate.discharge(
                                                                                               drop);
                                                                                   }

                                                                                   return null;
                                                                               }

                                                                               @Override
                                                                               public Object onFlush(
                                                                                       final Floodgate<Object, Object> gate) {

                                                                                   if ("flush2"
                                                                                           .equals(mLast)) {

                                                                                       throw new IllegalArgumentException(
                                                                                               "flush2");
                                                                                   }

                                                                                   gate.flush();

                                                                                   return null;
                                                                               }

                                                                               @Override
                                                                               public Object onPullDebris(
                                                                                       final Floodgate<Object, Object> gate,
                                                                                       final Object debris) {

                                                                                   return debris;
                                                                               }

                                                                               @Override
                                                                               public Object onPushDebris(
                                                                                       final Floodgate<Object, Object> gate,
                                                                                       final Object debris) {

                                                                                   if ((debris instanceof Throwable)
                                                                                           && "push2"
                                                                                           .equals(((Throwable) debris)
                                                                                                           .getMessage())) {

                                                                                       throw new IllegalArgumentException(
                                                                                               "push2");
                                                                                   }

                                                                                   return debris;
                                                                               }
                                                                           }
                                                                   )
                                                           )
        );

        basin2.thenFeedWith("discharge");
        assertThat(basin1.collectOutput()).containsExactly("discharge");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("discharge");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).containsExactly("discharge");
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge1");
        assertThat(basin1.collectOutput()).containsExactly("discharge1");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge2");
        assertThat(basin1.collectOutput()).containsExactly("discharge2");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("discharge2");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPulledDebris()).isNull();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush1");
        assertThat(basin1.collectOutput()).containsExactly("flush1");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).containsExactly("flush1");
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).containsExactly("flush1");
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush2");
        assertThat(basin1.collectOutput()).containsExactly("flush2");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("flush2");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPulledDebris()).isNull();
        assertThat(basin3.collectOutput()).containsExactly("flush2");
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push1");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push2");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPulledDebris()).isNull();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull");
        assertThat(basin1.collectOutput()).containsExactly("pull");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("pull");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPulledDebris()).isNull();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull1");
        assertThat(basin1.collectOutput()).containsExactly("pull1");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("pull1");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPulledDebris()).isNull();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();
    }

    public void testNoError() {

        final Basin<Object, Object> basin1 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      final String string = drop.toString();

                                      if (string.startsWith("push")) {

                                          throw new IllegalStateException(string);
                                      }

                                      return super.onDischarge(gate, drop);
                                  }

                              })
                );
        final Basin<Object, Object> basin2 = Basin.collect(
                basin1.thenFlow().thenFlowingThrough(Dams.noDebris(new Dam<Object, Object>() {

                                                                       private Object mLast;

                                                                       @Override
                                                                       public Object onDischarge(
                                                                               final Floodgate<Object, Object> gate,
                                                                               final Object drop) {

                                                                           mLast = drop;

                                                                           if ("discharge1"
                                                                                   .equals(drop)) {

                                                                               throw new IllegalArgumentException(
                                                                                       "discharge1");
                                                                           }

                                                                           gate.discharge(drop);

                                                                           return null;
                                                                       }

                                                                       @Override
                                                                       public Object onFlush(
                                                                               final Floodgate<Object, Object> gate) {

                                                                           if ("flush1"
                                                                                   .equals(mLast)) {

                                                                               throw new IllegalArgumentException(
                                                                                       "flush1");
                                                                           }

                                                                           gate.flush();

                                                                           return null;
                                                                       }

                                                                       @Override
                                                                       public Object onPullDebris(
                                                                               final Floodgate<Object, Object> gate,
                                                                               final Object debris) {

                                                                           if ((debris instanceof Throwable)
                                                                                   && "pull1"
                                                                                   .equals(((Throwable) debris)
                                                                                                   .getMessage()
                                                                                   )) {

                                                                               throw new IllegalArgumentException(
                                                                                       "pull1");
                                                                           }

                                                                           return debris;
                                                                       }

                                                                       @Override
                                                                       public Object onPushDebris(
                                                                               final Floodgate<Object, Object> gate,
                                                                               final Object debris) {

                                                                           if ((debris instanceof Throwable)
                                                                                   && "push1"
                                                                                   .equals(((Throwable) debris)
                                                                                                   .getMessage()
                                                                                   )) {

                                                                               throw new IllegalArgumentException(
                                                                                       "push1");
                                                                           }

                                                                           return debris;
                                                                       }
                                                                   }
                                                     )
                )
        );
        final Basin<Object, Object> basin3 = Basin.collect(
                basin2.thenFlow().thenFlowingThrough(Dams.noDebris(new Dam<Object, Object>() {

                                                                       private Object mLast;

                                                                       @Override
                                                                       public Object onDischarge(
                                                                               final Floodgate<Object, Object> gate,
                                                                               final Object drop) {

                                                                           mLast = drop;

                                                                           if ("discharge2"
                                                                                   .equals(drop)) {

                                                                               throw new IllegalArgumentException(
                                                                                       "discharge2");
                                                                           }

                                                                           final String string =
                                                                                   drop.toString();

                                                                           if (string.startsWith(
                                                                                   "pull") || string
                                                                                   .startsWith(
                                                                                           "push")) {

                                                                               throw new IllegalStateException(
                                                                                       string);

                                                                           } else {

                                                                               gate.discharge(drop);
                                                                           }

                                                                           return null;
                                                                       }

                                                                       @Override
                                                                       public Object onFlush(
                                                                               final Floodgate<Object, Object> gate) {

                                                                           if ("flush2"
                                                                                   .equals(mLast)) {

                                                                               throw new IllegalArgumentException(
                                                                                       "flush2");
                                                                           }

                                                                           gate.flush();

                                                                           return null;
                                                                       }

                                                                       @Override
                                                                       public Object onPullDebris(
                                                                               final Floodgate<Object, Object> gate,
                                                                               final Object debris) {

                                                                           return debris;
                                                                       }

                                                                       @Override
                                                                       public Object onPushDebris(
                                                                               final Floodgate<Object, Object> gate,
                                                                               final Object debris) {

                                                                           if ((debris instanceof Throwable)
                                                                                   && "push2"
                                                                                   .equals(((Throwable) debris)
                                                                                                   .getMessage()
                                                                                   )) {

                                                                               throw new IllegalArgumentException(
                                                                                       "push2");
                                                                           }

                                                                           return debris;
                                                                       }
                                                                   }
                                                     )
                )
        );

        basin2.thenFeedWith("discharge");
        assertThat(basin1.collectOutput()).containsExactly("discharge");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("discharge");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).containsExactly("discharge");
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge1");
        assertThat(basin1.collectOutput()).containsExactly("discharge1");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge2");
        assertThat(basin1.collectOutput()).containsExactly("discharge2");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("discharge2");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush1");
        assertThat(basin1.collectOutput()).containsExactly("flush1");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("flush1");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).containsExactly("flush1");
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush2");
        assertThat(basin1.collectOutput()).containsExactly("flush2");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("flush2");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).containsExactly("flush2");
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push1");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push2");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull");
        assertThat(basin1.collectOutput()).containsExactly("pull");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("pull");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull1");
        assertThat(basin1.collectOutput()).containsExactly("pull1");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("pull1");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();
    }

    public void testUpstreamError() {

        final Basin<Object, Object> basin1 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      final String string = drop.toString();

                                      if (string.startsWith("push")) {

                                          throw new IllegalStateException(string);
                                      }

                                      return super.onDischarge(gate, drop);
                                  }

                              })
                );
        final Basin<Object, Object> basin2 = Basin.collect(
                basin1.thenFlow().thenFlowingThrough(Dams.upstreamDebris(new Dam<Object, Object>() {

                                                                             private Object mLast;

                                                                             @Override
                                                                             public Object onDischarge(
                                                                                     final Floodgate<Object, Object> gate,
                                                                                     final Object drop) {

                                                                                 mLast = drop;

                                                                                 if ("discharge1"
                                                                                         .equals(drop)) {

                                                                                     throw new IllegalArgumentException(
                                                                                             "discharge1");
                                                                                 }

                                                                                 gate.discharge(
                                                                                         drop);

                                                                                 return null;
                                                                             }

                                                                             @Override
                                                                             public Object onFlush(
                                                                                     final Floodgate<Object, Object> gate) {

                                                                                 if ("flush1"
                                                                                         .equals(mLast)) {

                                                                                     throw new IllegalArgumentException(
                                                                                             "flush1");
                                                                                 }

                                                                                 gate.flush();

                                                                                 return null;
                                                                             }

                                                                             @Override
                                                                             public Object onPullDebris(
                                                                                     final Floodgate<Object, Object> gate,
                                                                                     final Object debris) {

                                                                                 if ((debris instanceof Throwable)
                                                                                         && "pull1"
                                                                                         .equals(((Throwable) debris)
                                                                                                         .getMessage()
                                                                                         )) {

                                                                                     throw new IllegalArgumentException(
                                                                                             "pull1");
                                                                                 }

                                                                                 return debris;
                                                                             }

                                                                             @Override
                                                                             public Object onPushDebris(
                                                                                     final Floodgate<Object, Object> gate,
                                                                                     final Object debris) {

                                                                                 if ((debris instanceof Throwable)
                                                                                         && "push1"
                                                                                         .equals(((Throwable) debris)
                                                                                                         .getMessage()
                                                                                         )) {

                                                                                     throw new IllegalArgumentException(
                                                                                             "push1");
                                                                                 }

                                                                                 return debris;
                                                                             }
                                                                         }
                                                     )
                )
        );
        final Basin<Object, Object> basin3 = Basin.collect(
                basin2.thenFlow().thenFlowingThrough(Dams.upstreamDebris(new Dam<Object, Object>() {

                                                                             private Object mLast;

                                                                             @Override
                                                                             public Object onDischarge(
                                                                                     final Floodgate<Object, Object> gate,
                                                                                     final Object drop) {

                                                                                 mLast = drop;

                                                                                 if ("discharge2"
                                                                                         .equals(drop)) {

                                                                                     throw new IllegalArgumentException(
                                                                                             "discharge2");
                                                                                 }

                                                                                 final String
                                                                                         string =
                                                                                         drop.toString();

                                                                                 if (string
                                                                                         .startsWith(
                                                                                                 "pull")
                                                                                         || string
                                                                                         .startsWith(
                                                                                                 "push")) {

                                                                                     throw new IllegalStateException(
                                                                                             string);

                                                                                 } else {

                                                                                     gate.discharge(
                                                                                             drop);
                                                                                 }

                                                                                 return null;
                                                                             }

                                                                             @Override
                                                                             public Object onFlush(
                                                                                     final Floodgate<Object, Object> gate) {

                                                                                 if ("flush2"
                                                                                         .equals(mLast)) {

                                                                                     throw new IllegalArgumentException(
                                                                                             "flush2");
                                                                                 }

                                                                                 gate.flush();

                                                                                 return null;
                                                                             }

                                                                             @Override
                                                                             public Object onPullDebris(
                                                                                     final Floodgate<Object, Object> gate,
                                                                                     final Object debris) {

                                                                                 return debris;
                                                                             }

                                                                             @Override
                                                                             public Object onPushDebris(
                                                                                     final Floodgate<Object, Object> gate,
                                                                                     final Object debris) {

                                                                                 if ((debris instanceof Throwable)
                                                                                         && "push2"
                                                                                         .equals(((Throwable) debris)
                                                                                                         .getMessage()
                                                                                         )) {

                                                                                     throw new IllegalArgumentException(
                                                                                             "push2");
                                                                                 }

                                                                                 return debris;
                                                                             }
                                                                         }
                                                     )
                )
        );

        basin2.thenFeedWith("discharge");
        assertThat(basin1.collectOutput()).containsExactly("discharge");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("discharge");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).containsExactly("discharge");
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge1");
        assertThat(basin1.collectOutput()).containsExactly("discharge1");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge2");
        assertThat(basin1.collectOutput()).containsExactly("discharge2");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).containsExactly("discharge2");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPulledDebris()).isNull();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush1");
        assertThat(basin1.collectOutput()).containsExactly("flush1");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).containsExactly("flush1");
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).containsExactly("flush1");
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush2");
        assertThat(basin1.collectOutput()).containsExactly("flush2");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).containsExactly("flush2");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPulledDebris()).isNull();
        assertThat(basin3.collectOutput()).containsExactly("flush2");
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push1");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push2");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectPushedDebris()).isEmpty();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull");
        assertThat(basin1.collectOutput()).containsExactly("pull");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).containsExactly("pull");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPulledDebris()).isNull();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull1");
        assertThat(basin1.collectOutput()).containsExactly("pull1");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).containsExactly("pull1");
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPulledDebris()).isNull();
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();
    }

    public void testWeak() {

        Dam<Object, Object> openDam = Dams.openDam();
        final Dam<Object, Object> weak1 = Dams.weak(openDam);
        final Dam<Object, Object> weak2 = Dams.weak(openDam);

        assertThat(weak1).isEqualTo(weak1);
        assertThat(weak1).isEqualTo(weak2);
        assertThat(weak1).isNotEqualTo(openDam);
        assertThat(weak1).isNotEqualTo(null);
        assertThat(weak1.hashCode()).isEqualTo(weak2.hashCode());

        final Dam<Object, Object> weak3 = Dams.weak(openDam, true);

        assertThat(weak1).isNotEqualTo(weak3);
        assertThat(weak2).isNotEqualTo(weak3);

        openDam = null;

        System.gc();
        System.gc();

        assertThat(weak1).isEqualTo(weak2);
        assertThat(weak1.hashCode()).isEqualTo(weak2.hashCode());
        assertThat(weak1).isNotEqualTo(weak3);
        assertThat(weak2).isNotEqualTo(weak3);

        Dam<Object, Object> dam1 = new Dam<Object, Object>() {

            private Object mLast;

            @Override
            public Object onDischarge(final Floodgate<Object, Object> gate, final Object drop) {

                mLast = drop;

                if ("discharge1".equals(drop)) {

                    throw new IllegalArgumentException("discharge1");
                }

                final String string = drop.toString();

                if (string.startsWith("push")) {

                    throw new IllegalStateException(string);
                }

                gate.discharge(drop);

                return null;
            }

            @Override
            public Object onFlush(final Floodgate<Object, Object> gate) {

                if ("flush1".equals(mLast)) {

                    throw new IllegalArgumentException("flush1");
                }

                gate.flush();

                return null;
            }

            @Override
            public Object onPullDebris(final Floodgate<Object, Object> gate, final Object debris) {

                if ((debris instanceof Throwable) && "pull1"
                        .equals(((Throwable) debris).getMessage())) {

                    throw new IllegalArgumentException("pull1");
                }

                return debris;
            }

            @Override
            public Object onPushDebris(final Floodgate<Object, Object> gate, final Object debris) {

                if ((debris instanceof Throwable) && "push1"
                        .equals(((Throwable) debris).getMessage())) {

                    throw new IllegalArgumentException("push1");
                }

                return debris;
            }
        };
        final Basin<Object, Object> basin1 = Basin.collect(Waterfall.fallingFrom(Dams.weak(dam1)));
        Dam<Object, Object> dam2 = new Dam<Object, Object>() {

            private Object mLast;

            @Override
            public Object onDischarge(final Floodgate<Object, Object> gate, final Object drop) {

                mLast = drop;

                final String string = drop.toString();

                if ("discharge2".equals(string)) {

                    throw new IllegalArgumentException("discharge2");
                }

                if (string.startsWith("pull")) {

                    throw new IllegalStateException(string);

                } else {

                    gate.discharge(drop);
                }

                return null;
            }

            @Override
            public Object onFlush(final Floodgate<Object, Object> gate) {

                if ("flush2".equals(mLast)) {

                    throw new IllegalArgumentException("flush2");
                }

                gate.flush();

                return null;
            }

            @Override
            public Object onPullDebris(final Floodgate<Object, Object> gate, final Object debris) {

                return debris;
            }

            @Override
            public Object onPushDebris(final Floodgate<Object, Object> gate, final Object debris) {

                if ((debris instanceof Throwable) && "push2"
                        .equals(((Throwable) debris).getMessage())) {

                    throw new IllegalArgumentException("push2");
                }

                return debris;
            }
        };
        final Basin<Object, Object> basin2 =
                Basin.collect(basin1.thenFlow().thenFlowingThrough(Dams.weak(dam2, true)));

        basin2.thenFeedWith("discharge");
        assertThat(basin1.collectOutput()).containsExactly("discharge");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("discharge");
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge1");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge2");
        assertThat(basin1.collectOutput()).containsExactly("discharge2");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush1");
        assertThat(basin1.collectOutput()).containsExactly("flush1");
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("flush1");
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush2");
        assertThat(basin1.collectOutput()).containsExactly("flush2");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).containsExactly("flush2");
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push1");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push2");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull");
        assertThat(basin1.collectOutput()).containsExactly("pull");
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull1");
        assertThat(basin1.collectOutput()).containsExactly("pull1");
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        dam1 = null;
        dam2 = null;

        System.gc();
        System.gc();

        basin2.thenFeedWith("discharge");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge1");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("discharge2");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush1");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("flush2");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push1");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFlow().backToSource().discharge("push2");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        basin2.thenFeedWith("pull1");
        assertThat(basin1.collectOutput()).isEmpty();
        assertThat(basin1.collectPushedDebris()).isEmpty();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).isEmpty();
        assertThat(basin2.collectPushedDebris()).isEmpty();
        assertThat(basin2.collectPulledDebris()).isEmpty();
    }

    private static class MyDecorator extends DamDecorator<Object, Object> {

        public MyDecorator(final Dam<Object, Object> wrapped) {

            super(wrapped);
        }

        public Dam<Object, Object> getWrapped() {

            return wrapped();
        }
    }
}