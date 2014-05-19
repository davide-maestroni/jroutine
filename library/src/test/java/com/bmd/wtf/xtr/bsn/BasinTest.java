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
package com.bmd.wtf.xtr.bsn;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.bdr.Stream;
import com.bmd.wtf.crr.Currents;
import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.Dams;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.arr.AbstractBarrage;
import com.bmd.wtf.xtr.arr.CurrentFactories;
import com.bmd.wtf.xtr.arr.DamFactory;
import com.bmd.wtf.xtr.arr.WaterfallArray;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link com.bmd.wtf.xtr.bsn} package classes.
 * <p/>
 * Created by davide on 4/8/14.
 */
public class BasinTest extends TestCase {

    public void testBasin() {

        final Basin<String, Integer> basin =
                Basin.collect(Waterfall.fallingFrom(new AbstractDam<String, Integer>() {

                    @Override
                    public Object onDischarge(final Floodgate<String, Integer> gate,
                            final String drop) {

                        if ("test".equals(drop)) {

                            return new IllegalArgumentException();
                        }

                        gate.discharge(Integer.parseInt(drop));

                        return null;
                    }

                }).thenFlowingThrough(new AbstractDam<Integer, Integer>() {

                    private int mSum = 0;

                    @Override
                    public Object onDischarge(final Floodgate<Integer, Integer> gate,
                            final Integer drop) {

                        mSum += drop;

                        return null;
                    }

                    @Override
                    public Object onFlush(final Floodgate<Integer, Integer> gate) {

                        gate.discharge(mSum).flush();

                        mSum = 0;

                        return null;
                    }

                }));
        assertThat(basin.thenFeedWith("1", "ciao", "2", "5").collectFirstOutput()).isEqualTo(8);
        assertThat(basin.collectFirstPushedDebris())
                .isExactlyInstanceOf(NumberFormatException.class);

        final ArrayList<Integer> output = new ArrayList<Integer>(1);
        basin.thenFeedWith(Arrays.asList("1", "0")).collectOutputInto(output);
        assertThat(output).containsExactly(1);

        final LinkedHashSet<Object> debris = new LinkedHashSet<Object>(1);
        basin.thenFeedWith("test", "test").collectPushedDebrisInto(debris);
        final Iterator<Object> iterator = debris.iterator();
        assertThat(iterator.next()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(iterator.next()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(iterator.hasNext()).isFalse();

        Basin.collect(basin.thenFlow().thenFlowingThrough(new AbstractDam<Integer, Object>() {

            @Override
            public Object onDischarge(final Floodgate<Integer, Object> gate, final Integer drop) {

                return new IllegalStateException();
            }

        })).thenFeedWith("test");
        debris.clear();
        basin.collectPulledDebrisInto(debris);
        assertThat(debris.iterator().next()).isExactlyInstanceOf(IllegalStateException.class);

        final Dam<String, String> dam1 = Dams.openDam();
        final Dam<String, String> dam2 = Dams.openDam();
        final Dam<String, String> dam3 = Dams.openDam();
        final Stream<String, String, String> stream1 = Waterfall.fallingFrom(dam1);
        final Stream<String, String, String> stream2 = Waterfall.fallingFrom(dam2);
        final Stream<String, String, String> stream3 = Waterfall.fallingFrom(dam3);

        assertThat(Basin.collect(stream1, stream2, stream3, stream1).thenFeedWith("test")
                        .collectOutput()).containsExactly("test", "test", "test");
        //noinspection unchecked
        assertThat(Basin.collect(Arrays.asList(stream1, stream2, stream3, stream2))
                        .thenFeedWith("test").collectOutput())
                .containsExactly("test", "test", "test");
    }

    public void testBasinError() {

        try {

            Basin.collect((Stream<Object, ?, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Basin.collect((Stream<Object, ?, Object>[]) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            //noinspection unchecked
            Basin.collect(new Stream[0]);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Basin.collect((Iterable<Stream<Object, ?, Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testBlocking() throws InterruptedException {

        final ArrayList<String> output = new ArrayList<String>();

        BlockingBasin.collect(
                WaterfallArray.formingFrom(Waterfall.fallingFrom(new OpenDam<String>()))
                              .thenSplittingIn(2)
                              .thenFlowingThrough(new AbstractBarrage<String, String>() {

                                  @Override
                                  public Object onDischarge(final int streamNumber,
                                          final Floodgate<String, String> gate, final String drop) {

                                      if ((drop.length() == 0)
                                              || drop.toLowerCase().charAt(0) < 'm') {

                                          if (streamNumber == 0) {

                                              gate.discharge(drop);
                                          }

                                      } else if (streamNumber == 1) {

                                          gate.discharge(drop);
                                      }

                                      return null;
                                  }

                              }).thenFlowingInto(
                        CurrentFactories.singletonCurrentFactory(Currents.threadPoolCurrent(2)))
                              .thenFlowingThrough(new DamFactory<String, List<String>>() {

                                  @Override
                                  public Dam<String, List<String>> createForStream(
                                          final int streamNumber) {

                                      if (streamNumber == 0) {

                                          return new AbstractDam<String, List<String>>() {

                                              private final ArrayList<String> mWords =
                                                      new ArrayList<String>();

                                              @Override
                                              public Object onDischarge(
                                                      final Floodgate<String, List<String>> gate,
                                                      final String drop) {

                                                  if (drop.length() == 0) {

                                                      throw new NullPointerException();
                                                  }

                                                  mWords.add(drop);

                                                  return null;
                                              }

                                              @Override
                                              public Object onFlush(
                                                      final Floodgate<String, List<String>> gate) {

                                                  Collections.sort(mWords);
                                                  gate.discharge(mWords);

                                                  return null;
                                              }
                                          };
                                      }

                                      return new AbstractDam<String, List<String>>() {

                                          private final ArrayList<String> mWords =
                                                  new ArrayList<String>();

                                          @Override
                                          public Object onDischarge(
                                                  final Floodgate<String, List<String>> gate,
                                                  final String drop) {

                                              mWords.add(drop);

                                              return null;
                                          }

                                          @Override
                                          public Object onFlush(
                                                  final Floodgate<String, List<String>> gate) {

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
        ).thenFeedWith("Ciao", "This", "zOO", "is", "", "a", "3", "test", "1111", "CAPITAL")
                     .afterMax(2000, TimeUnit.MILLISECONDS).collectOutputInto(output);

        assertThat(output)
                .containsExactly("1111", "3", "CAPITAL", "Ciao", "a", "is", "zOO", "test", "This");

        final BlockingBasin<String, Integer> basin1 = BlockingBasin.collect(
                Waterfall.flowingInto(Currents.threadPoolCurrent(1))
                         .thenFlowingThrough(new AbstractDam<String, Integer>() {

                             @Override
                             public Object onDischarge(final Floodgate<String, Integer> gate,
                                     final String drop) {

                                 gate.discharge(Integer.parseInt(drop));

                                 return null;
                             }

                         }).thenFlowingThrough(new AbstractDam<Integer, Integer>() {

                                                   private int mSum = 0;

                                                   @Override
                                                   public Object onDischarge(
                                                           final Floodgate<Integer, Integer> gate,
                                                           final Integer drop) {

                                                       if (drop == null) {

                                                           throw new IllegalArgumentException();
                                                       }

                                                       mSum += drop;

                                                       return null;
                                                   }

                                                   @Override
                                                   public Object onFlush(
                                                           final Floodgate<Integer, Integer> gate) {

                                                       gate.discharge(mSum).flush();

                                                       mSum = 0;

                                                       return null;
                                                   }

                                               }
                )
        ).afterMax(2, TimeUnit.SECONDS).throwIfTimeout(new UnsupportedOperationException());
        assertThat(basin1.thenFeedWith("1", "ciao", "2", "5").collectFirstOutput()).isEqualTo(8);
        assertThat(basin1.collectFirstPushedDebris())
                .isExactlyInstanceOf(NumberFormatException.class);

        final ArrayList<Integer> outInts = new ArrayList<Integer>(1);
        basin1.thenFeedWith(Arrays.asList("1", "0")).collectOutputInto(outInts);
        assertThat(outInts).containsExactly(1);
        basin1.thenFlow().backToSource().discharge(Arrays.asList("1", "0"));
        basin1.flush();
        assertThat(basin1.collectFirstOutput()).isEqualTo(1);

        final LinkedHashSet<Object> debris = new LinkedHashSet<Object>(1);
        basin1.thenFeedWith(null, null);
        do {

            basin1.collectPushedDebrisInto(debris);

        } while (debris.size() < 2);
        final Iterator<Object> iterator = debris.iterator();
        assertThat(iterator.next()).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(iterator.next()).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(iterator.hasNext()).isFalse();

        final Basin<String, Object> basin2 = Basin.collect(
                basin1.thenFlow().thenFlowingThrough(new AbstractDam<Integer, Object>() {

                                                         @Override
                                                         public Object onDischarge(
                                                                 final Floodgate<Integer, Object> gate,
                                                                 final Integer drop) {

                                                             throw new IllegalStateException();
                                                         }

                                                     }
                )
        );
        basin2.thenFeedWith("test");
        debris.clear();
        basin1.collectPulledDebrisInto(debris);
        assertThat(debris.iterator().next()).isExactlyInstanceOf(IllegalStateException.class);
        basin2.thenFeedWith("test");
        debris.clear();
        assertThat(basin1.collectFirstPulledDebris())
                .isExactlyInstanceOf(IllegalStateException.class);

        final Dam<String, String> dam1 = Dams.openDam();
        final Dam<String, String> dam2 = Dams.openDam();
        final Dam<String, String> dam3 = Dams.openDam();
        final Stream<String, String, String> stream1 = Waterfall.fallingFrom(dam1);
        final Stream<String, String, String> stream2 = Waterfall.fallingFrom(dam2);
        final Stream<String, String, String> stream3 = Waterfall.fallingFrom(dam3);

        assertThat(BlockingBasin.collect(stream1, stream2, stream3, stream1).thenFeedWith("test")
                                .collectOutput()
        ).containsExactly("test", "test", "test");
        //noinspection unchecked
        assertThat(BlockingBasin.collect(Arrays.asList(stream1, stream2, stream3, stream2))
                                .thenFeedWith("test").collectOutput())
                .containsExactly("test", "test", "test");
    }

    public void testBlockingError() {

        try {

            BlockingBasin.collect((Stream<Object, ?, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            BlockingBasin.collect((Stream<Object, ?, Object>[]) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            //noinspection unchecked
            BlockingBasin.collect(new Stream[0]);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            BlockingBasin.collect((Iterable<Stream<Object, ?, Object>>) null);

            fail();

        } catch (final Exception ignored) {

        }

        final Dam<Integer, Integer> closed = Dams.closedDam();
        final BlockingBasin<String, Integer> basin1 =
                BlockingBasin.collect(Waterfall.fallingFrom(new AbstractDam<String, Integer>() {

                    @Override
                    public Object onDischarge(final Floodgate<String, Integer> gate,
                            final String drop) {

                        gate.discharge(Integer.parseInt(drop));

                        return null;
                    }

                }).thenFlowingInto(Currents.threadPoolCurrent(1)).thenFlowingThrough(closed))
                             .afterMax(1, TimeUnit.MILLISECONDS);

        final ArrayList<Integer> output = new ArrayList<Integer>(1);
        final LinkedHashSet<Object> debris = new LinkedHashSet<Object>(1);
        assertThat(basin1.collectFirstOutput()).isNull();
        basin1.collectOutputInto(output);
        assertThat(output).isEmpty();
        assertThat(basin1.collectFirstPushedDebris()).isNull();
        basin1.collectPushedDebrisInto(debris);
        assertThat(debris).isEmpty();
        assertThat(basin1.collectFirstPulledDebris()).isNull();
        basin1.collectPulledDebrisInto(debris);
        assertThat(debris).isEmpty();

        basin1.throwIfTimeout(new UnsupportedOperationException());

        try {

            basin1.collectFirstOutput();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            basin1.collectOutputInto(output);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            basin1.collectFirstPushedDebris();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            basin1.collectPushedDebrisInto(debris);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            basin1.collectFirstPulledDebris();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            basin1.collectPulledDebrisInto(debris);

            fail();

        } catch (final Exception ignored) {

        }
    }
}