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
package com.bmd.wtf.xtr.dam;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.bdr.FloatingException;
import com.bmd.wtf.bdr.Stream;
import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.bsn.Basin;
import com.bmd.wtf.xtr.dam.DamBuilder.DischargeHandler;
import com.bmd.wtf.xtr.dam.DamBuilder.FlushHandler;
import com.bmd.wtf.xtr.dam.DamBuilder.PullHandler;
import com.bmd.wtf.xtr.dam.DamBuilder.PushHandler;
import com.bmd.wtf.xtr.ppl.Pipeline;

import junit.framework.TestCase;

import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link com.bmd.wtf.xtr.dam} package classes.
 * <p/>
 * Created by davide on 4/12/14.
 */
public class DamBuilderTest extends TestCase {

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
                              }).thenFlowingThrough(DamBuilders.openDam())
                );
        final Basin<Object, Object> basin2 =
                Basin.collect(basin1.thenFlow().thenFlowingThrough(DamBuilders.closedDam()));

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

        final Basin<Object, Object> basin3 =
                Basin.collect(Waterfall.fallingFrom(DamBuilders.openDam()));
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

    public void testDamBuilder() {

        final Dam<String, Integer> dam1 =
                DamBuilder.basedOn(new DischargeHandler<String, Integer>() {

                                       @Override
                                       public Integer onDischarge(final String drop) {

                                           return Integer.parseInt(drop);
                                       }

                                   }
                ).build();
        final ArrayList<Object> debris = new ArrayList<Object>();
        assertThat(Basin.collect(Waterfall.fallingFrom(dam1)).thenFeedWith("1", "test", "2")
                        .collectPushedDebrisInto(debris).collectOutput()).containsExactly(1, 2);
        assertThat(debris.get(0)).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(debris).hasSize(1);

        final Dam<String, Integer> dam2 =
                DamBuilder.basedOn(new DischargeHandler<String, Integer>() {

                                       @Override
                                       public Integer onDischarge(final String drop) {

                                           return Integer.parseInt(drop);
                                       }

                                   }
                ).avoidFlush().build();
        debris.clear();
        assertThat(Basin.collect(
                           Waterfall.fallingFrom(dam2).thenFlowingThrough(new OpenDam<Integer>() {

                                                                              @Override
                                                                              public Object onFlush(
                                                                                      final Floodgate<Integer, Integer> gate) {

                                                                                  throw new IllegalStateException();
                                                                              }
                                                                          }
                           )
                   ).thenFeedWith("1", "test", "2").collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly(1, 2);
        assertThat(debris.get(0)).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(debris).hasSize(1);

        final Dam<String, String> dam3 = DamBuilder.basedOn(new DischargeHandler<String, String>() {

                                                                @Override
                                                                public String onDischarge(
                                                                        final String drop) {

                                                                    return drop;
                                                                }

                                                            }
        ).avoidNull().build();
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(dam3)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly("1", "test", "2");
        assertThat(debris).isEmpty();

        final Dam<String, String> dam4 = DamBuilder.basedOn(new DischargeHandler<String, String>() {

                                                                @Override
                                                                public String onDischarge(
                                                                        final String drop) {

                                                                    return drop;
                                                                }

                                                            }
        ).avoidPull().build();
        final Basin<String, String> basin1 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<String>()));
        debris.clear();
        assertThat(Basin.collect(basin1.thenFlow().thenFlowingThrough(dam4)
                                       .thenFlowingThrough(new OpenDam<String>() {

                                                               @Override
                                                               public Object onDischarge(
                                                                       final Floodgate<String, String> gate,
                                                                       final String drop) {

                                                                   if (drop == null) {

                                                                       throw new NullPointerException();
                                                                   }

                                                                   return super
                                                                           .onDischarge(gate, drop);
                                                               }
                                                           }
                                       )
                   ).thenFeedWith("1", "test", null, "2").collectPushedDebrisInto(debris)
                        .collectOutput()
        ).containsExactly("1", "test", "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(NullPointerException.class);
        assertThat(debris).hasSize(1);
        assertThat(basin1.collectPulledDebris()).isEmpty();

        final Dam<String, String> dam5 = DamBuilder.basedOn(new DischargeHandler<String, String>() {

                                                                @Override
                                                                public String onDischarge(
                                                                        final String drop) {

                                                                    return drop;
                                                                }

                                                            }
        ).avoidPush().build();
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(new OpenDam<String>() {

            @Override
            public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

                if (drop == null) {

                    throw new NullPointerException();
                }

                return super.onDischarge(gate, drop);
            }
        }).thenFlowingThrough(dam5)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput())
                .containsExactly("1", "test", "2");
        assertThat(debris).isEmpty();

        final Dam<String, String> dam6 = DamBuilder.basedOn(new DischargeHandler<String, String>() {

                                                                @Override
                                                                public String onDischarge(
                                                                        final String drop) {

                                                                    return drop;
                                                                }

                                                            }
        ).onPullDebris(new PullHandler() {

            @Override
            public Object onPullDebris(final Object debris) {

                throw new FloatingException(debris);
            }
        }).downstreamDebris().build();
        final Basin<String, String> basin2 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<String>()));
        debris.clear();
        assertThat(Basin.collect(basin2.thenFlow().thenFlowingThrough(dam6)
                                       .thenFlowingThrough(new OpenDam<String>() {

                                                               @Override
                                                               public Object onDischarge(
                                                                       final Floodgate<String, String> gate,
                                                                       final String drop) {

                                                                   if (drop == null) {

                                                                       throw new NullPointerException();
                                                                   }

                                                                   return super
                                                                           .onDischarge(gate, drop);
                                                               }
                                                           }
                                       )
                   ).thenFeedWith("1", "test", null, "2").collectPushedDebrisInto(debris)
                        .collectOutput()
        ).containsExactly("1", "test", "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(NullPointerException.class);
        assertThat(debris).hasSize(1);
        assertThat(basin2.collectPulledDebris()).isEmpty();

        final Dam<String, String> dam7 = DamBuilder.basedOn(new DischargeHandler<String, String>() {

                                                                @Override
                                                                public String onDischarge(
                                                                        final String drop) {

                                                                    return drop;
                                                                }

                                                            }
        ).onPushDebris(new PushHandler() {

            @Override
            public Object onPushDebris(final Object debris) {

                throw new FloatingException(debris);
            }
        }).onFlush(new FlushHandler() {

            @Override
            public boolean onFlush() {

                return true;
            }
        }).upstreamDebris().build();
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(new OpenDam<String>() {

            @Override
            public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

                if (drop == null) {

                    throw new NullPointerException();
                }

                return super.onDischarge(gate, drop);
            }
        }).thenFlowingThrough(dam7)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput())
                .containsExactly("1", "test", "2");
        assertThat(debris).isEmpty();

        final Dam<String, String> dam8 = DamBuilder.basedOn(new DischargeHandler<String, String>() {

                                                                @Override
                                                                public String onDischarge(
                                                                        final String drop) {

                                                                    return drop;
                                                                }

                                                            }
        ).onPushDebris(new PushHandler() {

            @Override
            public Object onPushDebris(final Object debris) {

                throw new FloatingException(debris);
            }
        }).onPullDebris(new PullHandler() {

            @Override
            public Object onPullDebris(final Object debris) {

                throw new FloatingException(debris);
            }
        }).onFlush(new FlushHandler() {

            @Override
            public boolean onFlush() {

                return false;
            }
        }).noDebris().build();
        final Basin<String, String> basin3 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<String>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<String, String> gate,
                                          final String drop) {

                                      if ("pull".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        debris.clear();
        assertThat(Basin.collect(basin3.thenFlow().thenFlowingThrough(dam8)
                                       .thenFlowingThrough(new OpenDam<String>() {

                                                               @Override
                                                               public Object onDischarge(
                                                                       final Floodgate<String, String> gate,
                                                                       final String drop) {

                                                                   if ("push".equals(drop)) {

                                                                       throw new IllegalStateException();
                                                                   }

                                                                   return super
                                                                           .onDischarge(gate, drop);
                                                               }
                                                           }
                                       )
                   ).thenFeedWith("1", "pull", "push", "2").collectPushedDebrisInto(debris)
                        .collectOutput()
        ).containsExactly("1", "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(debris).hasSize(1);
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        final Dam<String, String> dam9 =
                DamBuilder.basedOn(new DischargeHandler<String, Integer>() {

                                       @Override
                                       public Integer onDischarge(final String drop) {

                                           throw new NumberFormatException();
                                       }

                                   }
                ).onFlush(new FlushHandler() {

                    @Override
                    public boolean onFlush() {

                        throw new IllegalStateException();
                    }
                }).onDischarge(new DischargeHandler<String, String>() {

                    @Override
                    public String onDischarge(final String drop) {

                        return drop;
                    }
                }).build();
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(dam9)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly("1", "test", null, "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(debris).hasSize(1);

        final Dam<String, String> dam10 =
                DamBuilder.basedOn(new DischargeHandler<String, String>() {

                                       @Override
                                       public String onDischarge(final String drop) {

                                           return drop;
                                       }

                                   }
                ).onFlush(new FlushHandler() {

                    boolean mFlush;

                    @Override
                    public boolean onFlush() {

                        mFlush = !mFlush;

                        return mFlush;
                    }
                }).noDebris().avoidNull().build();
        final Basin<String, String> basin4 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<String>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<String, String> gate,
                                          final String drop) {

                                      if ("pull".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        debris.clear();
        assertThat(Basin.collect(basin4.thenFlow().thenFlowingThrough(dam10)
                                       .thenFlowingThrough(new OpenDam<String>() {

                                                               @Override
                                                               public Object onDischarge(
                                                                       final Floodgate<String, String> gate,
                                                                       final String drop) {

                                                                   if ("push".equals(drop)) {

                                                                       throw new IllegalStateException();
                                                                   }

                                                                   return super
                                                                           .onDischarge(gate, drop);
                                                               }
                                                           }
                                       )
                   ).thenFeedWith("1", "pull", "push").thenFeedWith(null, "2")
                        .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly("1", "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(debris).hasSize(1);
        assertThat(basin4.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin4.collectFirstPushedDebris()).isNull();
        assertThat(basin4.collectPulledDebris()).isEmpty();
    }

    public void testDamBuilderError() {

        try {

            DamBuilder.basedOn(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            DamBuilder.basedOn(new DischargeHandler<Object, Object>() {

                @Override
                public Object onDischarge(final Object drop) {

                    return null;
                }
            }).onFlush(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            DamBuilder.basedOn(new DischargeHandler<Object, Object>() {

                @Override
                public Object onDischarge(final Object drop) {

                    return null;
                }
            }).onPullDebris(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            DamBuilder.basedOn(new DischargeHandler<Object, Object>() {

                @Override
                public Object onDischarge(final Object drop) {

                    return null;
                }
            }).onPushDebris(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            DamBuilder.basedOn(new DischargeHandler<Object, Object>() {

                @Override
                public Object onDischarge(final Object drop) {

                    return null;
                }
            }).onDischarge(null);

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
                                                                   DamBuilders.downstreamDebris(
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
                                                                   DamBuilders.downstreamDebris(
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

    public void testError() {

        try {

            Pipeline.binding(null);

            fail();

        } catch (final Exception ignored) {

        }
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
        final Basin<Object, Object> basin2 = Basin.collect(basin1.thenFlow().thenFlowingThrough(
                                                                   DamBuilders.noDebris(
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
                                                                   DamBuilders.noDebris(
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

    public void testSimpleDam() {

        final Dam<Object, Object> dam1 =
                DamBuilders.simpleDamBasedOn(new DischargeHandler<Object, Object>() {

                                                 @Override
                                                 public Object onDischarge(final Object drop) {

                                                     return drop;
                                                 }
                                             }
                );
        final ArrayList<Object> debris = new ArrayList<Object>();
        assertThat(Basin.collect(Waterfall.fallingFrom(dam1)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly("1", "test", null, "2");
        assertThat(debris).isEmpty();

        final Dam<Object, Object> dam2 =
                DamBuilders.simpleDamAvoidingNullBasedOn(new DischargeHandler<Object, Object>() {

                                                             @Override
                                                             public Object onDischarge(
                                                                     final Object drop) {

                                                                 return drop;
                                                             }
                                                         }
                );
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(dam2)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly("1", "test", "2");
        assertThat(debris).isEmpty();

        try {

            DamBuilders.simpleDamBasedOn(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            DamBuilders.simpleDamAvoidingNullBasedOn(null);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testSimpleDownstreamDam() {

        final Dam<Object, Object> dam1 =
                DamBuilders.downstreamDebrisBasedOn(new DischargeHandler<Object, Object>() {

                    @Override
                    public Object onDischarge(final Object drop) {

                        if ("test".equals(drop)) {

                            throw new NullPointerException();
                        }

                        return drop;
                    }
                });
        final Basin<Object, Object> basin1 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      if ("push".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        final Basin<Object, Object> basin2 = Basin.collect(
                basin1.thenFlow().thenFlowingThrough(dam1)
                      .thenFlowingThrough(new OpenDam<Object>() {

                                              @Override
                                              public Object onDischarge(
                                                      final Floodgate<Object, Object> gate,
                                                      final Object drop) {

                                                  if ("pull".equals(drop)) {

                                                      throw new IllegalStateException();
                                                  }

                                                  return super.onDischarge(gate, drop);
                                              }
                                          }
                      )
        );
        basin2.thenFeedWith("1", "push", "test", null, "pull", "2");
        assertThat(basin1.collectOutput()).containsExactly("1", "test", null, "pull", "2");
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).containsExactly("1", null, "2");
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        final Dam<Object, Object> dam2 = DamBuilders
                .downstreamDebrisAvoidingNullBasedOn(new DischargeHandler<Object, Object>() {

                                                         @Override
                                                         public Object onDischarge(
                                                                 final Object drop) {

                                                             if ("test".equals(drop)) {

                                                                 throw new NullPointerException();
                                                             }

                                                             return drop;
                                                         }
                                                     }
                );
        final Basin<Object, Object> basin3 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      if ("push".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        final Basin<Object, Object> basin4 = Basin.collect(
                basin3.thenFlow().thenFlowingThrough(dam2)
                      .thenFlowingThrough(new OpenDam<Object>() {

                                              @Override
                                              public Object onDischarge(
                                                      final Floodgate<Object, Object> gate,
                                                      final Object drop) {

                                                  if ("pull".equals(drop)) {

                                                      throw new IllegalStateException();
                                                  }

                                                  return super.onDischarge(gate, drop);
                                              }
                                          }
                      )
        );
        basin4.thenFeedWith("1", "push", "test", null, "pull", "2");
        assertThat(basin3.collectOutput()).containsExactly("1", "test", null, "pull", "2");
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectFirstPulledDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin3.collectFirstPulledDebris()).isNull();
        assertThat(basin4.collectOutput()).containsExactly("1", "2");
        assertThat(basin4.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin4.collectFirstPushedDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin4.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin4.collectFirstPushedDebris()).isNull();
        assertThat(basin4.collectPulledDebris()).isEmpty();

        try {

            DamBuilders.downstreamDebrisBasedOn(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            DamBuilders.downstreamDebrisAvoidingNullBasedOn(null);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testSimpleNoErrorDam() {

        final Dam<Object, Object> dam1 =
                DamBuilders.noDebrisBasedOn(new DischargeHandler<Object, Object>() {

                    @Override
                    public Object onDischarge(final Object drop) {

                        if ("test".equals(drop)) {

                            throw new NullPointerException();
                        }

                        return drop;
                    }
                });
        final Basin<Object, Object> basin1 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      if ("push".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        final Basin<Object, Object> basin2 = Basin.collect(
                basin1.thenFlow().thenFlowingThrough(dam1)
                      .thenFlowingThrough(new OpenDam<Object>() {

                                              @Override
                                              public Object onDischarge(
                                                      final Floodgate<Object, Object> gate,
                                                      final Object drop) {

                                                  if ("pull".equals(drop)) {

                                                      throw new IllegalStateException();
                                                  }

                                                  return super.onDischarge(gate, drop);
                                              }
                                          }
                      )
        );
        basin2.thenFeedWith("1", "push", "test", null, "pull", "2");
        assertThat(basin1.collectOutput()).containsExactly("1", "test", null, "pull", "2");
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectPulledDebris()).isEmpty();
        assertThat(basin2.collectOutput()).containsExactly("1", null, "2");
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        final Dam<Object, Object> dam2 =
                DamBuilders.noDebrisAvoidingNullBasedOn(new DischargeHandler<Object, Object>() {

                    @Override
                    public Object onDischarge(final Object drop) {

                        if ("test".equals(drop)) {

                            throw new NullPointerException();
                        }

                        return drop;
                    }
                });
        final Basin<Object, Object> basin3 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      if ("push".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        final Basin<Object, Object> basin4 = Basin.collect(
                basin3.thenFlow().thenFlowingThrough(dam2)
                      .thenFlowingThrough(new OpenDam<Object>() {

                                              @Override
                                              public Object onDischarge(
                                                      final Floodgate<Object, Object> gate,
                                                      final Object drop) {

                                                  if ("pull".equals(drop)) {

                                                      throw new IllegalStateException();
                                                  }

                                                  return super.onDischarge(gate, drop);
                                              }
                                          }
                      )
        );
        basin4.thenFeedWith("1", "push", "test", null, "pull", "2");
        assertThat(basin3.collectOutput()).containsExactly("1", "test", null, "pull", "2");
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();
        assertThat(basin4.collectOutput()).containsExactly("1", "2");
        assertThat(basin4.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin4.collectFirstPushedDebris()).isNull();
        assertThat(basin4.collectPulledDebris()).isEmpty();

        try {

            DamBuilders.noDebrisBasedOn(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            DamBuilders.noDebrisAvoidingNullBasedOn(null);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testSimpleUpstreamDam() {

        final Dam<Object, Object> dam1 =
                DamBuilders.upstreamDebrisBasedOn(new DischargeHandler<Object, Object>() {

                    @Override
                    public Object onDischarge(final Object drop) {

                        if ("test".equals(drop)) {

                            throw new NullPointerException();
                        }

                        return drop;
                    }
                });
        final Basin<Object, Object> basin1 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      if ("push".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        final Basin<Object, Object> basin2 = Basin.collect(
                basin1.thenFlow().thenFlowingThrough(dam1)
                      .thenFlowingThrough(new OpenDam<Object>() {

                                              @Override
                                              public Object onDischarge(
                                                      final Floodgate<Object, Object> gate,
                                                      final Object drop) {

                                                  if ("pull".equals(drop)) {

                                                      throw new IllegalStateException();
                                                  }

                                                  return super.onDischarge(gate, drop);
                                              }
                                          }
                      )
        );
        basin2.thenFeedWith("1", "push", "test", null, "pull", "2");
        assertThat(basin1.collectOutput()).containsExactly("1", "test", null, "pull", "2");
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        assertThat(basin2.collectOutput()).containsExactly("1", null, "2");
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin2.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin2.collectFirstPushedDebris()).isNull();
        assertThat(basin2.collectPulledDebris()).isEmpty();

        final Dam<Object, Object> dam2 = DamBuilders
                .upstreamDebrisAvoidingNullBasedOn(new DischargeHandler<Object, Object>() {

                                                       @Override
                                                       public Object onDischarge(
                                                               final Object drop) {

                                                           if ("test".equals(drop)) {

                                                               throw new NullPointerException();
                                                           }

                                                           return drop;
                                                       }
                                                   }
                );
        final Basin<Object, Object> basin3 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<Object, Object> gate,
                                          final Object drop) {

                                      if ("push".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        final Basin<Object, Object> basin4 = Basin.collect(
                basin3.thenFlow().thenFlowingThrough(dam2)
                      .thenFlowingThrough(new OpenDam<Object>() {

                                              @Override
                                              public Object onDischarge(
                                                      final Floodgate<Object, Object> gate,
                                                      final Object drop) {

                                                  if ("pull".equals(drop)) {

                                                      throw new IllegalStateException();
                                                  }

                                                  return super.onDischarge(gate, drop);
                                              }
                                          }
                      )
        );
        basin4.thenFeedWith("1", "push", "test", null, "pull", "2");
        assertThat(basin3.collectOutput()).containsExactly("1", "test", null, "pull", "2");
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectFirstPulledDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin3.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin3.collectFirstPulledDebris()).isNull();
        assertThat(basin4.collectOutput()).containsExactly("1", "2");
        assertThat(basin4.collectFirstPushedDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin4.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin4.collectFirstPushedDebris()).isNull();
        assertThat(basin4.collectPulledDebris()).isEmpty();

        try {

            DamBuilders.upstreamDebrisBasedOn(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            DamBuilders.upstreamDebrisAvoidingNullBasedOn(null);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testStreamDamBuilder() {

        final ArrayList<Object> debris = new ArrayList<Object>();
        assertThat(Basin.collect(
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, Integer>() {

                                                 @Override
                                                 public Integer onDischarge(final String drop) {

                                                     return Integer.parseInt(drop);
                                                 }

                                             }
                                ).fallFrom()
        ).thenFeedWith("1", "test", "2").collectPushedDebrisInto(debris).collectOutput())
                .containsExactly(1, 2);
        assertThat(debris.get(0)).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(debris).hasSize(1);

        final Dam<String, Integer> dam2 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, Integer>() {

                                                 @Override
                                                 public Integer onDischarge(final String drop) {

                                                     return Integer.parseInt(drop);
                                                 }

                                             }
                                ).avoidFlush().build();
        debris.clear();
        assertThat(Basin.collect(
                           Waterfall.fallingFrom(dam2).thenFlowingThrough(new OpenDam<Integer>() {

                                                                              @Override
                                                                              public Object onFlush(
                                                                                      final Floodgate<Integer, Integer> gate) {

                                                                                  throw new IllegalStateException();
                                                                              }
                                                                          }
                           )
                   ).thenFeedWith("1", "test", "2").collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly(1, 2);
        assertThat(debris.get(0)).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(debris).hasSize(1);

        final Dam<String, String> dam3 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, String>() {

                                    @Override
                                    public String onDischarge(final String drop) {

                                        return drop;
                                    }

                                }).avoidNull().build();
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(dam3)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly("1", "test", "2");
        assertThat(debris).isEmpty();

        final Dam<String, String> dam4 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, String>() {

                                    @Override
                                    public String onDischarge(final String drop) {

                                        return drop;
                                    }

                                }).avoidPull().build();
        final Basin<String, String> basin1 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<String>()));
        debris.clear();
        assertThat(Basin.collect(basin1.thenFlow().thenFlowingThrough(dam4)
                                       .thenFlowingThrough(new OpenDam<String>() {

                                                               @Override
                                                               public Object onDischarge(
                                                                       final Floodgate<String, String> gate,
                                                                       final String drop) {

                                                                   if (drop == null) {

                                                                       throw new NullPointerException();
                                                                   }

                                                                   return super
                                                                           .onDischarge(gate, drop);
                                                               }
                                                           }
                                       )
                   ).thenFeedWith("1", "test", null, "2").collectPushedDebrisInto(debris)
                        .collectOutput()
        ).containsExactly("1", "test", "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(NullPointerException.class);
        assertThat(debris).hasSize(1);
        assertThat(basin1.collectPulledDebris()).isEmpty();

        final Dam<String, String> dam5 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, String>() {

                                                 @Override
                                                 public String onDischarge(final String drop) {

                                                     return drop;
                                                 }

                                             }
                                ).avoidPush().build();
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(new OpenDam<String>() {

            @Override
            public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

                if (drop == null) {

                    throw new NullPointerException();
                }

                return super.onDischarge(gate, drop);
            }
        }).thenFlowingThrough(dam5)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput())
                .containsExactly("1", "test", "2");
        assertThat(debris).isEmpty();

        final Dam<String, String> dam6 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, String>() {

                                                 @Override
                                                 public String onDischarge(final String drop) {

                                                     return drop;
                                                 }

                                             }
                                ).onPullDebris(new PullHandler() {

                    @Override
                    public Object onPullDebris(final Object debris) {

                        throw new FloatingException(debris);
                    }
                }).downstreamDebris().build();
        final Basin<String, String> basin2 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<String>()));
        debris.clear();
        assertThat(Basin.collect(basin2.thenFlow().thenFlowingThrough(dam6)
                                       .thenFlowingThrough(new OpenDam<String>() {

                                                               @Override
                                                               public Object onDischarge(
                                                                       final Floodgate<String, String> gate,
                                                                       final String drop) {

                                                                   if (drop == null) {

                                                                       throw new NullPointerException();
                                                                   }

                                                                   return super
                                                                           .onDischarge(gate, drop);
                                                               }
                                                           }
                                       )
                   ).thenFeedWith("1", "test", null, "2").collectPushedDebrisInto(debris)
                        .collectOutput()
        ).containsExactly("1", "test", "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(NullPointerException.class);
        assertThat(debris).hasSize(1);
        assertThat(basin2.collectPulledDebris()).isEmpty();

        final Dam<String, String> dam7 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, String>() {

                                                 @Override
                                                 public String onDischarge(final String drop) {

                                                     return drop;
                                                 }

                                             }
                                ).onPushDebris(new PushHandler() {

                    @Override
                    public Object onPushDebris(final Object debris) {

                        throw new FloatingException(debris);
                    }
                }).onFlush(new FlushHandler() {

                    @Override
                    public boolean onFlush() {

                        return true;
                    }
                }).upstreamDebris().build();
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(new OpenDam<String>() {

            @Override
            public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

                if (drop == null) {

                    throw new NullPointerException();
                }

                return super.onDischarge(gate, drop);
            }
        }).thenFlowingThrough(dam7)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput())
                .containsExactly("1", "test", "2");
        assertThat(debris).isEmpty();

        final Dam<String, String> dam8 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, String>() {

                                                 @Override
                                                 public String onDischarge(final String drop) {

                                                     return drop;
                                                 }

                                             }
                                ).onPushDebris(new PushHandler() {

                    @Override
                    public Object onPushDebris(final Object debris) {

                        throw new FloatingException(debris);
                    }
                }).onPullDebris(new PullHandler() {

                    @Override
                    public Object onPullDebris(final Object debris) {

                        throw new FloatingException(debris);
                    }
                }).onFlush(new FlushHandler() {

                    @Override
                    public boolean onFlush() {

                        return false;
                    }
                }).noDebris().build();
        final Basin<String, String> basin3 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<String>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<String, String> gate,
                                          final String drop) {

                                      if ("pull".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        debris.clear();
        assertThat(Basin.collect(basin3.thenFlow().thenFlowingThrough(dam8)
                                       .thenFlowingThrough(new OpenDam<String>() {

                                                               @Override
                                                               public Object onDischarge(
                                                                       final Floodgate<String, String> gate,
                                                                       final String drop) {

                                                                   if ("push".equals(drop)) {

                                                                       throw new IllegalStateException();
                                                                   }

                                                                   return super
                                                                           .onDischarge(gate, drop);
                                                               }
                                                           }
                                       )
                   ).thenFeedWith("1", "pull", "push", "2").collectPushedDebrisInto(debris)
                        .collectOutput()
        ).containsExactly("1", "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(debris).hasSize(1);
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
        assertThat(basin3.collectPulledDebris()).isEmpty();

        final Dam<String, String> dam9 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, Integer>() {

                                                 @Override
                                                 public Integer onDischarge(final String drop) {

                                                     throw new NumberFormatException();
                                                 }

                                             }
                                ).onFlush(new FlushHandler() {

                    @Override
                    public boolean onFlush() {

                        throw new IllegalStateException();
                    }
                }).onDischarge(new DischargeHandler<String, String>() {

                    @Override
                    public String onDischarge(final String drop) {

                        return drop;
                    }
                }).build();
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(dam9)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly("1", "test", null, "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(debris).hasSize(1);

        final Dam<String, String> dam10 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>()))
                                .onDischarge(new DischargeHandler<String, String>() {

                                                 @Override
                                                 public String onDischarge(final String drop) {

                                                     return drop;
                                                 }

                                             }
                                ).onFlush(new FlushHandler() {

                    boolean mFlush;

                    @Override
                    public boolean onFlush() {

                        mFlush = !mFlush;

                        return mFlush;
                    }
                }).noDebris().avoidNull().build();
        final Basin<String, String> basin4 =
                Basin.collect(Waterfall.fallingFrom(new OpenDam<String>() {

                                  @Override
                                  public Object onDischarge(final Floodgate<String, String> gate,
                                          final String drop) {

                                      if ("pull".equals(drop)) {

                                          throw new IllegalArgumentException();
                                      }

                                      return super.onDischarge(gate, drop);
                                  }
                              })
                );
        debris.clear();
        assertThat(Basin.collect(basin4.thenFlow().thenFlowingThrough(dam10)
                                       .thenFlowingThrough(new OpenDam<String>() {

                                                               @Override
                                                               public Object onDischarge(
                                                                       final Floodgate<String, String> gate,
                                                                       final String drop) {

                                                                   if ("push".equals(drop)) {

                                                                       throw new IllegalStateException();
                                                                   }

                                                                   return super
                                                                           .onDischarge(gate, drop);
                                                               }
                                                           }
                                       )
                   ).thenFeedWith("1", "pull", "push").thenFeedWith(null, "2")
                        .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly("1", "2");
        assertThat(debris.get(0)).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(debris).hasSize(1);
        assertThat(basin4.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin4.collectFirstPushedDebris()).isNull();
        assertThat(basin4.collectPulledDebris()).isEmpty();

        final Dam<String, ?> dam11 =
                StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<String>())).build();
        debris.clear();
        assertThat(Basin.collect(Waterfall.fallingFrom(dam11)).thenFeedWith("1", "test", null, "2")
                        .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly("1", "test", null, "2");
        assertThat(debris).isEmpty();
    }

    public void testStreamDamBuilderError() {

        try {

            StreamDamBuilder.basedOn(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            StreamDamBuilder.blocking((Stream<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<Object>()))
                            .onDischarge(new DischargeHandler<Object, Object>() {

                                             @Override
                                             public Object onDischarge(final Object drop) {

                                                 return null;
                                             }
                                         }
                            ).onFlush(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<Object>()))
                            .onDischarge(new DischargeHandler<Object, Object>() {

                                             @Override
                                             public Object onDischarge(final Object drop) {

                                                 return null;
                                             }
                                         }
                            ).onPullDebris(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<Object>()))
                            .onDischarge(new DischargeHandler<Object, Object>() {

                                             @Override
                                             public Object onDischarge(final Object drop) {

                                                 return null;
                                             }
                                         }
                            ).onPushDebris(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            StreamDamBuilder.blocking(Waterfall.fallingFrom(new OpenDam<Object>()))
                            .onDischarge(new DischargeHandler<Object, Object>() {

                                             @Override
                                             public Object onDischarge(final Object drop) {

                                                 return null;
                                             }
                                         }
                            ).onDischarge(null);

            fail();

        } catch (final Exception ignored) {

        }
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
        final Basin<Object, Object> basin2 = Basin.collect(basin1.thenFlow().thenFlowingThrough(
                                                                   DamBuilders.upstreamDebris(
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
                                                                   DamBuilders.upstreamDebris(
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

        Dam<Object, Object> openDam = DamBuilders.openDam();
        final Dam<Object, Object> weak1 = DamBuilders.weak(openDam);
        final Dam<Object, Object> weak2 = DamBuilders.weak(openDam);

        assertThat(weak1).isEqualTo(weak1);
        assertThat(weak1).isEqualTo(weak2);
        assertThat(weak1).isNotEqualTo(openDam);
        assertThat(weak1).isNotEqualTo(null);
        assertThat(weak1.hashCode()).isEqualTo(weak2.hashCode());

        final Dam<Object, Object> weak3 = DamBuilders.weak(openDam, true);

        assertThat(weak1).isNotEqualTo(weak3);
        assertThat(weak2).isNotEqualTo(weak3);

        //noinspection UnusedAssignment
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
        final Basin<Object, Object> basin1 =
                Basin.collect(Waterfall.fallingFrom(DamBuilders.weak(dam1)));
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
                Basin.collect(basin1.thenFlow().thenFlowingThrough(DamBuilders.weak(dam2, true)));

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

        //noinspection UnusedAssignment
        dam1 = null;
        //noinspection UnusedAssignment
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
}