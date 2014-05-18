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
package com.bmd.wtf.xtr.arr;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.bdr.Stream;
import com.bmd.wtf.crr.Current;
import com.bmd.wtf.crr.Currents;
import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.Dams;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.bsn.Basin;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link com.bmd.wtf.xtr.arr} package classes.
 * <p/>
 * Created by davide on 4/8/14.
 */
public class ArrayTest extends TestCase {

    public void testBarrage() {

        final ArrayList<String> output = new ArrayList<String>();

        final Basin<String, String> basin = Basin.collect(
                WaterfallArray.formingFrom(Waterfall.fallingFrom(new OpenDam<String>() {

                                                                     @Override
                                                                     public Object onDischarge(
                                                                             final Floodgate<String, String> gate,
                                                                             final String drop) {

                                                                         if ("test".equals(drop)) {

                                                                             return new IllegalArgumentException();
                                                                         }

                                                                         return super
                                                                                 .onDischarge(gate,
                                                                                              drop);
                                                                     }

                                                                 }
                )).thenSplittingIn(2).thenFlowingThrough(new AbstractBarrage<String, String>() {

                                                             @Override
                                                             public Object onDischarge(
                                                                     final int streamNumber,
                                                                     final Floodgate<String, String> gate,
                                                                     final String drop) {

                                                                 if ((drop.length() == 0) ||
                                                                         drop.toLowerCase()
                                                                             .charAt(0) < 'm') {

                                                                     if (streamNumber == 0) {

                                                                         gate.discharge(drop);
                                                                     }

                                                                 } else if (streamNumber == 1) {

                                                                     gate.discharge(drop);
                                                                 }

                                                                 return null;
                                                             }
                                                         }
                ).thenFlowingThrough(new DamFactory<String, List<String>>() {

                    @Override
                    public Dam<String, List<String>> createForStream(final int streamNumber) {

                        if (streamNumber == 0) {

                            return new AbstractDam<String, List<String>>() {

                                private final ArrayList<String> mWords = new ArrayList<String>();

                                @Override
                                public Object onDischarge(
                                        final Floodgate<String, List<String>> gate,
                                        final String drop) {

                                    if ("atest".equals(drop)) {

                                        throw new IllegalStateException();
                                    }

                                    mWords.add(drop);

                                    return null;
                                }

                                @Override
                                public Object onFlush(final Floodgate<String, List<String>> gate) {

                                    Collections.sort(mWords);
                                    gate.discharge(mWords);

                                    return null;
                                }
                            };
                        }

                        return new AbstractDam<String, List<String>>() {

                            private final ArrayList<String> mWords = new ArrayList<String>();

                            @Override
                            public Object onDischarge(final Floodgate<String, List<String>> gate,
                                    final String drop) {

                                mWords.add(drop);

                                return null;
                            }

                            @Override
                            public Object onFlush(final Floodgate<String, List<String>> gate) {

                                Collections.sort(mWords, Collections.reverseOrder());
                                gate.discharge(mWords);

                                return null;
                            }
                        };
                    }

                }).thenMergingThrough(new AbstractDam<List<String>, String>() {

                    private int mCount;

                    private ArrayList<String> mList = new ArrayList<String>();

                    @Override
                    public Object onDischarge(final Floodgate<List<String>, String> gate,
                            final List<String> drop) {

                        if (mList.isEmpty() || drop.isEmpty()) {

                            mList.addAll(drop);

                        } else {

                            final String first = drop.get(0);

                            if ((first.length() == 0) || first.toLowerCase().charAt(0) < 'm') {

                                mList.addAll(0, drop);

                            } else {

                                mList.addAll(drop);
                            }
                        }

                        if (++mCount == 2) {

                            gate.discharge(mList).flush();
                        }

                        return null;
                    }

                })
        ).thenFeedWith("Ciao", "This", "zOO", null, "is", "a", "3", "test", "1111", "CAPITAL",
                       "atest").collectOutputInto(output);

        assertThat(output)
                .containsExactly("1111", "3", "CAPITAL", "Ciao", "a", "is", "zOO", "This");
        assertThat(basin.collectFirstPushedDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin.collectFirstPushedDebris())
                .isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin.collectFirstPulledDebris()).isNull();

        basin.thenFeedWith("test");
        assertThat(basin.collectFirstOutput()).isNull();
        assertThat(basin.collectFirstPushedDebris())
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin.collectFirstPulledDebris()).isNull();

        final Dam<Integer, Integer> dam1 = Dams.openDam();
        final Dam<Integer, Integer> dam2 = Dams.openDam();
        final Dam<Integer, Integer> dam3 = Dams.openDam();
        final Stream<Integer, Integer, Integer> stream1 = Waterfall.fallingFrom(dam1);
        final Stream<Integer, Integer, Integer> stream2 = Waterfall.fallingFrom(dam2);
        final Stream<Integer, Integer, Integer> stream3 = Waterfall.fallingFrom(dam3);

        final Basin<Integer, Integer> basin1 = Basin.collect(
                WaterfallArray.formingFrom(stream1, stream2, stream3, stream2)
                              .thenMergingInto(Currents.straightCurrent())
        );
        Basin.collect(stream1, stream2, stream3).thenFeedWith(1);
        assertThat(basin1.collectOutput()).containsExactly(1, 1, 1);

        final Basin<Integer, Integer> basin2 = Basin.collect(
                WaterfallArray.formingFrom(Arrays.asList(stream1, stream2, stream3, stream1))
                              .thenMergingInto(Currents.straightCurrent())
        );
        Basin.collect(stream1, stream2, stream3).thenFeedWith(1);
        assertThat(basin2.collectOutput()).containsExactly(1, 1, 1);
    }

    public void testError() {

        try {

            WaterfallArray.formingFrom((Stream<Object, Object, Object>) null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom((Stream<Object, Object, Object>[]) null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom();

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom(Waterfall.fallingFrom(Dams.openDam()), null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom((Iterable<Stream<Object, Object, Object>>) null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom(new ArrayList<Stream<Object, Object, Object>>());

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            final ArrayList<Stream<Object, Object, Object>> streams =
                    new ArrayList<Stream<Object, Object, Object>>();
            streams.add(null);
            streams.add(Waterfall.fallingFrom(Dams.openDam()));

            WaterfallArray.formingFrom();

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom(Waterfall.fallingFrom(Dams.openDam())).thenSplittingIn(0);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom(Waterfall.fallingFrom(Dams.openDam())).thenSplittingIn(-1);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom(Waterfall.fallingFrom(Dams.openDam())).thenSplittingIn(1)
                          .thenMergingInto(null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom(Waterfall.fallingFrom(Dams.openDam())).thenSplittingIn(1)
                          .thenFlowingInto(null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom(Waterfall.fallingFrom(Dams.openDam())).thenSplittingIn(1)
                          .thenMergingThrough(null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom(Waterfall.fallingFrom(Dams.openDam())).thenSplittingIn(1)
                          .thenFlowingThrough((DamFactory<Object, Object>) null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            WaterfallArray.formingFrom(Waterfall.fallingFrom(Dams.openDam())).thenSplittingIn(1)
                          .thenFlowingThrough((Barrage<Object, Object>) null);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }

        try {

            final Barrage<Object, Object> barrage = new Barrage<Object, Object>() {

                @Override
                public Object onDischarge(final int streamNumber,
                        final Floodgate<Object, Object> gate, final Object drop) {

                    return null;
                }

                @Override
                public Object onFlush(final int streamNumber,
                        final Floodgate<Object, Object> gate) {

                    return null;
                }

                @Override
                public Object onPullDebris(final int streamNumber,
                        final Floodgate<Object, Object> gate, final Object debris) {

                    return null;
                }

                @Override
                public Object onPushDebris(final int streamNumber,
                        final Floodgate<Object, Object> gate, final Object debris) {

                    return null;
                }
            };

            WaterfallArray.formingFrom(Waterfall.fallingFrom(Dams.openDam())).thenSplittingIn(1)
                          .thenFlowingThrough(barrage).thenFlowingThrough(barrage);

            fail();

        } catch (final Exception e) {

            // Ignore it
        }
    }

    public void testOrder() {

        final Current current = Currents.straightCurrent();

        assertThat(Basin.collect(
                WaterfallArray.formingFrom(Waterfall.fallingFrom(new OpenDam<Integer>()))
                              .thenSplittingIn(2)
                              .thenFlowingInto(CurrentFactories.singletonCurrentFactory(current))
                              .thenBalancedBy(new AbstractArrayBalancer<Integer>() {

                                  @Override
                                  public int chooseDataStream(final Integer drop,
                                          final int streamCount) {

                                      return (drop % 2);
                                  }
                              }).thenMerging()
        ).thenFeedWith(1, 2, 3).collectOutput()).contains(1, 2, 3);
        assertThat(Basin.collect(
                WaterfallArray.formingFrom(Waterfall.fallingFrom(new OpenDam<Integer>()))
                              .thenSplittingIn(2)
                              .thenBalancedBy(new AbstractArrayBalancer<Integer>() {

                                  @Override
                                  public int chooseDataStream(final Integer drop,
                                          final int streamCount) {

                                      return (drop % 2);
                                  }
                              }).thenFlowingInto(CurrentFactories.singletonCurrentFactory(current))
                              .thenMerging()
        ).thenFeedWith(1, 2, 3).collectOutput()).contains(1, 2, 3);
    }
}