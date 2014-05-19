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
package com.bmd.wtf.bdr;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.crr.Current;
import com.bmd.wtf.crr.Currents;
import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.Dams;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.bsn.Basin;

import junit.framework.TestCase;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link com.bmd.wtf.bdr.Stream} class.
 * <p/>
 * Created by davide on 4/8/14.
 */
public class StreamTest extends TestCase {

    public void testDrain() {

        final Stream<Integer, Integer, Integer> stream1 =
                Waterfall.fallingFrom(new OpenDam<Integer>());
        final Stream<Integer, Integer, String> stream2 =
                stream1.thenFlowingThrough(new AbstractDam<Integer, Integer>() {

                    @Override
                    public Object onDischarge(final Floodgate<Integer, Integer> gate,
                            final Integer drop) {

                        gate.discharge(drop - 1);

                        if (drop == 0) {

                            gate.drain();
                        }

                        return null;
                    }
                }).thenFlowingThrough(new AbstractDam<Integer, String>() {

                    @Override
                    public Object onDischarge(final Floodgate<Integer, String> gate,
                            final Integer drop) {

                        gate.discharge(drop.toString());

                        return null;
                    }
                });
        final Basin<Integer, String> basin = Basin.collect(stream2);

        assertThat(basin.thenFeedWith(1).collectFirstOutput()).isEqualTo("0");
        assertThat(basin.thenFeedWith(0).collectFirstOutput()).isNull();
        assertThat(basin.thenFeedWith(1).collectFirstOutput()).isNull();

        stream1.thenFlowingThrough(new AbstractDam<Integer, Integer>() {

            @Override
            public Object onDischarge(final Floodgate<Integer, Integer> gate, final Integer drop) {

                gate.discharge(drop + 1);

                if (drop == -1) {

                    gate.exhaust();
                }

                return null;
            }
        }).thenFeeding(stream2);

        assertThat(basin.thenFeedWith(0).collectFirstOutput()).isEqualTo("1");
        assertThat(basin.thenFeedWith(-1).collectFirstOutput()).isNull();
        assertThat(basin.thenFeedWith(0).collectFirstOutput()).isNull();

        stream1.thenFlowingThrough(new AbstractDam<Integer, Integer>() {

            @Override
            public Object onDischarge(final Floodgate<Integer, Integer> gate, final Integer drop) {

                gate.discharge(drop);

                return null;
            }
        }).thenFeeding(stream2);

        assertThat(basin.thenFeedWith(0).collectFirstOutput()).isNull();
        assertThat(basin.thenFeedWith(1).collectFirstOutput()).isNull();
    }

    public void testError() {

        try {

            Waterfall.flowingInto(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Waterfall.fallingFrom(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenFlowingThrough(dam);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenFlowingThrough(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenFlowingInto((Current) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenJoining(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenJoiningInto(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            final Stream<Object, Object, Object> stream1 = Waterfall.fallingFrom(dam);
            final Stream<Object, Object, Object> stream2 =
                    stream1.thenJoiningInto(stream1).thenFlowingThrough(Dams.closedDam());
            stream2.thenJoining(stream1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            final Stream<Object, Object, Object> stream1 = Waterfall.fallingFrom(dam);
            final Stream<Object, Object, Object> stream2 =
                    stream1.thenJoiningInto(stream1).thenFlowingThrough(Dams.closedDam());
            stream2.thenJoiningInto(stream1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenFeeding(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            final Stream<Object, Object, Object> stream1 = Waterfall.fallingFrom(dam);
            final Stream<Object, Object, Object> stream2 =
                    stream1.thenFeeding(stream1).thenFlowingThrough(Dams.closedDam());
            stream2.thenFeeding(stream1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenFlowingInto((Stream<?, Object, ?>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            final Stream<Object, Object, Object> stream1 = Waterfall.fallingFrom(dam);
            final Stream<Object, Object, Object> stream2 =
                    stream1.thenFlowingInto(stream1).thenFlowingThrough(Dams.closedDam());
            stream2.thenFlowingInto(stream1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenMerging((Stream<Object, Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenMerging((Stream<Object, Object, Object>[]) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final Dam<Object, Object> dam = Dams.openDam();

            Waterfall.fallingFrom(dam).thenMerging((Iterable<Stream<Object, Object, Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }

        final boolean[] fail = new boolean[1];

        Basin.collect(Waterfall.flowingInto(Currents.straightCurrent())
                               .thenFlowingThrough(new OpenDam<Object>() {

                                                       public boolean mFlushed;

                                                       @Override
                                                       public Object onDischarge(
                                                               final Floodgate<Object, Object> gate,
                                                               final Object drop) {

                                                           if (fail[0] || !"test".equals(drop)) {

                                                               return null;
                                                           }

                                                           fail[0] = true;

                                                           gate.discharge((Object) null)
                                                               .discharge((Object[]) null)
                                                               .discharge((Iterable<Object>) null)
                                                               .discharge(new Object[0])
                                                               .discharge(Arrays.asList())
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               (Object) null)
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               (Object[]) null)
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               (Iterable<Object>) null)
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               new Object[0])
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               Arrays.asList())
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              (Object) null)
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              (Object[]) null)
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              (Iterable<Object>) null)
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              new Object[0])
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              Arrays.asList());

                                                           fail[0] = false;

                                                           return null;
                                                       }

                                                       @Override
                                                       public Object onFlush(
                                                               final Floodgate<Object, Object> gate) {

                                                           if (fail[0] || mFlushed) {

                                                               return null;
                                                           }

                                                           mFlushed = true;

                                                           fail[0] = true;

                                                           gate.discharge((Object) null)
                                                               .discharge((Object[]) null)
                                                               .discharge((Iterable<Object>) null)
                                                               .discharge(new Object[0])
                                                               .discharge(Arrays.asList())
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               (Object) null)
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               (Object[]) null)
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               (Iterable<Object>) null)
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               new Object[0])
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               Arrays.asList())
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              (Object) null)
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              (Object[]) null)
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              (Iterable<Object>) null)
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              new Object[0])
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              Arrays.asList());

                                                           fail[0] = false;

                                                           return null;
                                                       }

                                                       @Override
                                                       public Object onPushDebris(
                                                               final Floodgate<Object, Object> gate,
                                                               final Object debris) {

                                                           if (fail[0] || !"test".equals(debris)) {

                                                               return debris;
                                                           }

                                                           fail[0] = true;

                                                           gate.discharge((Object) null)
                                                               .discharge((Object[]) null)
                                                               .discharge((Iterable<Object>) null)
                                                               .discharge(new Object[0])
                                                               .discharge(Arrays.asList())
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               (Object) null)
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               (Object[]) null)
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               (Iterable<Object>) null)
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               new Object[0])
                                                               .dischargeAfter(0,
                                                                               TimeUnit.MILLISECONDS,
                                                                               Arrays.asList())
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              (Object) null)
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              (Object[]) null)
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              (Iterable<Object>) null)
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              new Object[0])
                                                               .rechargeAfter(0,
                                                                              TimeUnit.MILLISECONDS,
                                                                              Arrays.asList());

                                                           fail[0] = false;

                                                           return debris;
                                                       }

                                                   }
                               )).thenFeedWith("test").thenFlow().backToSource().discharge("push");

        if (fail[0]) {

            fail();
        }
    }

    public void testFall() {

        final Basin<Object, Integer> basin =
                Basin.collect(Waterfall.fallingFrom(new AbstractDam<Object, Integer>() {

                    @Override
                    public Object onDischarge(final Floodgate<Object, Integer> gate,
                            final Object drop) {

                        if (drop instanceof Iterable) {

                            final Iterable<?> iterable = (Iterable<?>) drop;

                            if (iterable.iterator().next() instanceof Integer) {

                                //noinspection unchecked
                                gate.dischargeAfter(100, TimeUnit.MILLISECONDS,
                                                    (Iterable<Integer>) drop);

                            } else {

                                final ArrayList<Object> list = new ArrayList<Object>();

                                for (final Object obj : iterable) {

                                    list.add(Integer.valueOf(obj.toString()));
                                }

                                gate.rechargeAfter(100, TimeUnit.MILLISECONDS, list);
                            }

                        } else if (drop instanceof Integer) {

                            final Integer integer = (Integer) drop;

                            gate.dischargeAfter(integer, TimeUnit.MILLISECONDS, integer);

                        } else if (drop.getClass().isArray()) {

                            if (drop.getClass().getComponentType().equals(String.class)) {

                                final int length = Array.getLength(drop);

                                final Object[] integers = new Object[length];

                                for (int i = 0; i < length; i++) {

                                    integers[i] = Integer.valueOf(Array.get(drop, i).toString());
                                }

                                gate.rechargeAfter(100, TimeUnit.MILLISECONDS, integers);

                            } else {

                                gate.dischargeAfter(100, TimeUnit.MILLISECONDS, (Integer[]) drop);
                            }

                        } else if (drop instanceof String) {

                            gate.rechargeAfter(100, TimeUnit.MILLISECONDS,
                                               Integer.valueOf(drop.toString()));
                        }

                        if (Integer.valueOf(-1).equals(drop)) {

                            gate.drain();
                        }

                        return null;
                    }
                }));

        basin.thenFeedWith((Object) null);
        assertThat(basin.collectFirstOutput()).isNull();
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstPushedDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith(1);
        assertThat(basin.collectFirstOutput()).isEqualTo(1);
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith("1");
        assertThat(basin.collectOutput()).containsExactly(1);
        assertThat(basin.collectFirstOutput()).isNull();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith((Object) new Integer[]{1});
        assertThat(basin.collectFirstOutput()).isEqualTo(1);
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith((Object) new String[]{"1"});
        assertThat(basin.collectOutput()).containsExactly(1);
        assertThat(basin.collectFirstOutput()).isNull();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith(1, 2);
        assertThat(basin.collectFirstOutput()).isEqualTo(1);
        assertThat(basin.collectFirstOutput()).isEqualTo(2);
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith("1", "2");
        assertThat(basin.collectOutput()).containsExactly(1, 2);
        assertThat(basin.collectFirstOutput()).isNull();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith((Object) new Integer[]{1, 2});
        assertThat(basin.collectFirstOutput()).isEqualTo(1);
        assertThat(basin.collectFirstOutput()).isEqualTo(2);
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith((Object) new String[]{"1", "2"});
        assertThat(basin.collectOutput()).containsExactly(1, 2);
        assertThat(basin.collectFirstOutput()).isNull();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith(Arrays.asList("2", "1"));
        assertThat(basin.collectFirstOutput()).isEqualTo(2);
        assertThat(basin.collectFirstOutput()).isEqualTo(1);
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith(Arrays.asList(2, 1));
        assertThat(basin.collectOutput()).containsExactly(2, 1);
        assertThat(basin.collectFirstOutput()).isNull();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith(-1);
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstOutput()).isNull();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();

        basin.thenFeedWith(1);
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstOutput()).isNull();
        assertThat(basin.collectFirstPushedDebris()).isNull();
        assertThat(basin.collectPushedDebris()).isEmpty();
    }

    public void testJoin() {

        final Stream<Character, Character, Integer> stream1 =
                Waterfall.fallingFrom(new AbstractDam<Character, Integer>() {

                    private final StringBuffer mBuffer = new StringBuffer();

                    @Override
                    public Object onDischarge(final Floodgate<Character, Integer> gate,
                            final Character drop) {

                        mBuffer.append(drop);

                        return null;
                    }

                    @Override
                    public Object onFlush(final Floodgate<Character, Integer> gate) {

                        gate.discharge(Integer.valueOf(mBuffer.toString())).flush();

                        mBuffer.setLength(0);

                        return null;
                    }
                });

        final Basin<Character, Integer> basin1 =
                Basin.collect(stream1.thenFlowingThrough(new OpenDam<Integer>() {

                    private int mSum;

                    @Override
                    public Object onDischarge(final Floodgate<Integer, Integer> gate,
                            final Integer drop) {

                        mSum += drop;

                        return null;
                    }

                    @Override
                    public Object onFlush(final Floodgate<Integer, Integer> gate) {

                        gate.discharge(new Integer[]{mSum}).flush();

                        mSum = 0;

                        return null;
                    }
                }));

        final Stream<Integer, Integer, Integer> stream2 =
                Waterfall.fallingFrom(new OpenDam<Integer>());

        stream2.thenJoiningInto(stream1).backToSource().discharge('0', '1', '2', '3');
        stream2.backToSource().discharge(0, 1, 2, 3);

        final ArrayList<Integer> output = new ArrayList<Integer>(1);
        basin1.thenFlow().backToSource().flush();
        basin1.collectOutputInto(output);

        assertThat(output).containsExactly(129);

        stream2.backToSource().exhaust();

        final Stream<Integer, Integer, Integer> stream3 =
                Waterfall.fallingFrom(new OpenDam<Integer>());

        stream1.backToSource().discharge('0', '1', '2', '3');
        stream3.thenJoining(stream1).backToSource().discharge(Arrays.asList(4, 5, -4));
        stream2.backToSource().discharge(77);

        output.clear();
        basin1.thenFlow().backToSource().flush();
        basin1.collectOutputInto(output);

        assertThat(output).containsExactly(128);

        final Basin<Character, Integer> basin2 =
                Basin.collect(stream1.thenFlowingThrough(new OpenDam<Integer>() {

                                  private int mAbsSum;

                                  @Override
                                  public Object onDischarge(final Floodgate<Integer, Integer> gate,
                                          final Integer drop) {

                                      mAbsSum += Math.abs(drop);

                                      return null;
                                  }

                                  @Override
                                  public Object onFlush(final Floodgate<Integer, Integer> gate) {

                                      gate.discharge(mAbsSum).flush();

                                      mAbsSum = 0;

                                      return null;
                                  }
                              })
                );

        stream1.backToSource().discharge('0', '1', '2', '3');
        stream3.thenJoining(stream1).backToSource().discharge(Arrays.asList(4, 5, -4));

        output.clear();
        basin1.flush().collectOutputInto(output);
        basin2.collectOutputInto(output);

        assertThat(output).containsExactly(128, 136);
    }

    public void testMerge() {

        final Dam<Integer, Integer> openDam = Dams.openDam();

        final Stream<Integer, Integer, Integer> stream1 = Waterfall.fallingFrom(openDam);
        final Stream<Integer, Integer, Integer> stream2 =
                Waterfall.fallingFrom(new AbstractDam<Integer, Integer>() {

                    @Override
                    public Object onDischarge(final Floodgate<Integer, Integer> gate,
                            final Integer drop) {

                        gate.discharge(drop, drop);

                        return null;
                    }
                });
        final Stream<Integer, Integer, Integer> stream3 =
                Waterfall.fallingFrom(new AbstractDam<Integer, Integer>() {

                    @Override
                    public Object onDischarge(final Floodgate<Integer, Integer> gate,
                            final Integer drop) {

                        gate.discharge(Arrays.asList(drop + 1, drop + 1));

                        return null;
                    }
                });

        assertThat(Basin.collect(stream1.thenMerging(stream1)).thenFeedWith(5).collectOutput())
                .containsExactly(5);

        final Basin<Integer, Integer> basin1 = Basin.collect(stream1.thenMerging(stream2));

        stream1.backToSource().discharge(5);
        stream2.backToSource().discharge(5);
        assertThat(basin1.collectOutput()).containsExactly(5, 5, 5);

        final Basin<Integer, Integer> basin2 = Basin.collect(stream1.thenMerging(stream2, stream1));

        stream1.backToSource().discharge(5);
        stream2.backToSource().discharge(5);
        assertThat(basin2.collectOutput()).containsExactly(5, 5, 5);

        @SuppressWarnings("unchecked") final Basin<Integer, Integer> basin3 =
                Basin.collect(stream1.thenMerging(Arrays.asList(stream1, stream2, stream3)));

        stream1.backToSource().discharge(5);
        stream2.backToSource().discharge(5);
        stream3.backToSource().discharge(4);
        assertThat(basin3.collectOutput()).containsExactly(5, 5, 5, 5, 5);

        stream1.backToSource().dischargeAfter(100, TimeUnit.MILLISECONDS, 4);
        assertThat(basin3.collectOutput()).containsExactly(4);

        stream1.backToSource().dischargeAfter(100, TimeUnit.MILLISECONDS, 4, 4);
        assertThat(basin3.collectOutput()).containsExactly(4, 4);

        stream1.backToSource().dischargeAfter(100, TimeUnit.MILLISECONDS, Arrays.asList(4, 4, 4));
        assertThat(basin3.collectOutput()).containsExactly(4, 4, 4);

        stream1.backToSource().discharge((Integer) null);
        assertThat(basin3.collectOutput()).containsExactly((Integer) null);
        assertThat(basin3.collectPushedDebris()).isEmpty();

        stream2.backToSource().discharge((Integer) null);
        assertThat(basin3.collectOutput()).containsExactly(null, null);
        assertThat(basin3.collectPushedDebris()).isEmpty();

        stream3.backToSource().discharge((Integer) null);
        assertThat(basin3.collectOutput()).isEmpty();
        assertThat(basin3.collectFirstPushedDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin3.collectFirstPushedDebris()).isNull();
    }
}