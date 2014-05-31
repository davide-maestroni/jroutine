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

        final Basin<String, Integer> basin = Basin.collect(Waterfall.fallingFrom(new AbstractDam<String, Integer>() {

            @Override
            public void onDischarge(final Floodgate<String, Integer> gate, final String drop) {

                if ("test".equals(drop)) {

                    throw new IllegalArgumentException();
                }

                gate.discharge(Integer.parseInt(drop));

            }

        }).thenFallingThrough(new AbstractDam<Integer, Integer>() {

            private int mSum = 0;

            @Override
            public void onDischarge(final Floodgate<Integer, Integer> gate, final Integer drop) {

                mSum += drop;
            }

            @Override
            public void onFlush(final Floodgate<Integer, Integer> gate) {

                gate.discharge(mSum).flush();

                mSum = 0;
            }

        }));
        assertThat(basin.thenFeedWith("1", "ciao", "2", "5").collectFirstOutput()).isEqualTo(8);
        assertThat(basin.collectFirstDebris()).isExactlyInstanceOf(NumberFormatException.class);

        final ArrayList<Integer> output = new ArrayList<Integer>(1);
        basin.thenFeedWith(Arrays.asList("1", "0")).collectOutputInto(output);
        assertThat(output).containsExactly(1);

        final LinkedHashSet<Object> debris = new LinkedHashSet<Object>(1);
        basin.thenFeedWith("test", "test").collectDebrisInto(debris);
        final Iterator<Object> iterator = debris.iterator();
        assertThat(iterator.next()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(iterator.next()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(iterator.hasNext()).isFalse();

        final Dam<String, String> dam1 = Dams.openDam();
        final Dam<String, String> dam2 = Dams.openDam();
        final Dam<String, String> dam3 = Dams.openDam();
        final Stream<String, String, String> stream1 = Waterfall.fallingFrom(dam1);
        final Stream<String, String, String> stream2 = Waterfall.fallingFrom(dam2);
        final Stream<String, String, String> stream3 = Waterfall.fallingFrom(dam3);

        assertThat(Basin.collect(stream1, stream2, stream3, stream1).thenFeedWith("test").collectOutput())
                .containsExactly("test", "test", "test");
        //noinspection unchecked
        assertThat(
                Basin.collect(Arrays.asList(stream1, stream2, stream3, stream2)).thenFeedWith("test").collectOutput())
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
                WaterfallArray.formingFrom(Waterfall.fallingFrom(new OpenDam<String>())).thenSplittingIn(2)
                              .thenFlowingThrough(new AbstractBarrage<String, String>() {

                                  @Override
                                  public void onDischarge(final int streamNumber, final Floodgate<String, String> gate,
                                          final String drop) {

                                      if ((drop.length() == 0) || drop.toLowerCase().charAt(0) < 'm') {

                                          if (streamNumber == 0) {

                                              gate.discharge(drop);
                                          }

                                      } else if (streamNumber == 1) {

                                          gate.discharge(drop);
                                      }
                                  }

                              })
                              .thenFlowingInto(CurrentFactories.singletonCurrentFactory(Currents.threadPoolCurrent(2)))
                              .thenFallingThrough(new DamFactory<String, List<String>>() {

                                  @Override
                                  public Dam<String, List<String>> createForStream(final int streamNumber) {

                                      if (streamNumber == 0) {

                                          return new AbstractDam<String, List<String>>() {

                                              private final ArrayList<String> mWords = new ArrayList<String>();

                                              @Override
                                              public void onDischarge(final Floodgate<String, List<String>> gate,
                                                      final String drop) {

                                                  if (drop.length() == 0) {

                                                      throw new NullPointerException();
                                                  }

                                                  mWords.add(drop);
                                              }

                                              @Override
                                              public void onFlush(final Floodgate<String, List<String>> gate) {

                                                  Collections.sort(mWords);
                                                  gate.discharge(mWords);
                                              }
                                          };
                                      }

                                      return new AbstractDam<String, List<String>>() {

                                          private final ArrayList<String> mWords = new ArrayList<String>();

                                          @Override
                                          public void onDischarge(final Floodgate<String, List<String>> gate,
                                                  final String drop) {

                                              mWords.add(drop);
                                          }

                                          @Override
                                          public void onFlush(final Floodgate<String, List<String>> gate) {

                                              Collections.sort(mWords, Collections.reverseOrder());
                                              gate.discharge(mWords);
                                          }
                                      };

                                  }

                              }).thenMergingThrough(new AbstractDam<List<String>, String>() {

                    private int mCount;

                    private ArrayList<String> mList = new ArrayList<String>();

                    @Override
                    public void onDischarge(final Floodgate<List<String>, String> gate, final List<String> drop) {

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
                    }

                })
        ).thenFeedWith("Ciao", "This", "zOO", "is", "", "a", "3", "test", "1111", "CAPITAL")
                     .afterMax(2000, TimeUnit.MILLISECONDS).collectOutputInto(output);

        assertThat(output).containsExactly("1111", "3", "CAPITAL", "Ciao", "a", "is", "zOO", "test", "This");

        final BlockingBasin<String, Integer> basin = BlockingBasin.collect(
                Waterfall.flowingInto(Currents.threadPoolCurrent(1))
                         .thenFlowingThrough(new AbstractDam<String, Integer>() {

                             @Override
                             public void onDischarge(final Floodgate<String, Integer> gate, final String drop) {

                                 gate.discharge(Integer.parseInt(drop));
                             }

                         }).thenFallingThrough(new AbstractDam<Integer, Integer>() {

                                                   private int mSum = 0;

                                                   @Override
                                                   public void onDischarge(final Floodgate<Integer, Integer> gate,
                                                           final Integer drop) {

                                                       if (drop == null) {

                                                           throw new IllegalArgumentException();
                                                       }

                                                       mSum += drop;
                                                   }

                                                   @Override
                                                   public void onFlush(final Floodgate<Integer, Integer> gate) {

                                                       gate.discharge(mSum).flush();

                                                       mSum = 0;
                                                   }
                                               }
                )
        ).afterMax(2, TimeUnit.SECONDS).throwIfTimeout(new UnsupportedOperationException());
        assertThat(basin.thenFeedWith("1", "ciao", "2", "5").collectFirstOutput()).isEqualTo(8);
        assertThat(basin.collectFirstDebris()).isExactlyInstanceOf(NumberFormatException.class);

        final ArrayList<Integer> outInts = new ArrayList<Integer>(1);
        basin.thenFeedWith(Arrays.asList("1", "0")).collectOutputInto(outInts);
        assertThat(outInts).containsExactly(1);
        basin.thenFlow().backToSource().discharge(Arrays.asList("1", "0"));
        basin.thenFlush();
        assertThat(basin.collectFirstOutput()).isEqualTo(1);

        final LinkedHashSet<Object> debris = new LinkedHashSet<Object>(1);
        basin.thenFeedWith(null, null);
        do {

            basin.collectDebrisInto(debris);

        } while (debris.size() < 2);
        final Iterator<Object> iterator = debris.iterator();
        assertThat(iterator.next()).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(iterator.next()).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(iterator.hasNext()).isFalse();

        final Dam<String, String> dam1 = Dams.openDam();
        final Dam<String, String> dam2 = Dams.openDam();
        final Dam<String, String> dam3 = Dams.openDam();
        final Stream<String, String, String> stream1 = Waterfall.fallingFrom(dam1);
        final Stream<String, String, String> stream2 = Waterfall.fallingFrom(dam2);
        final Stream<String, String, String> stream3 = Waterfall.fallingFrom(dam3);

        assertThat(BlockingBasin.collect(stream1, stream2, stream3, stream1).thenFeedWith("test").collectOutput())
                .containsExactly("test", "test", "test");
        //noinspection unchecked
        assertThat(BlockingBasin.collect(Arrays.asList(stream1, stream2, stream3, stream2)).thenFeedWith("test")
                                .collectOutput()).containsExactly("test", "test", "test");

        final BlockingBasin<String, String> basin1 = BlockingBasin
                .collect(Waterfall.flowingInto(Currents.threadPoolCurrent(1)).thenFlowingThrough(new OpenDam<String>() {

                             public String mDrop;

                             @Override
                             public void onDischarge(final Floodgate<String, String> gate, final String drop) {

                                 mDrop = drop;
                             }

                             @Override
                             public void onFlush(final Floodgate<String, String> gate) {

                                 try {

                                     Thread.sleep(1000);

                                 } catch (final InterruptedException ignored) {

                                 }

                                 gate.discharge(mDrop).flush();
                             }
                         })
                );
        assertThat(basin1.thenFeedWith("test").immediately().collectOutput()).isEmpty();
        assertThat(basin1.whenAvailable().collectOutput()).containsExactly("test");
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
                    public void onDischarge(final Floodgate<String, Integer> gate, final String drop) {

                        gate.discharge(Integer.parseInt(drop));
                    }

                }).thenFlowingInto(Currents.threadPoolCurrent(1)).thenFallingThrough(closed))
                             .afterMax(1, TimeUnit.MILLISECONDS);

        final ArrayList<Integer> output = new ArrayList<Integer>(1);
        final LinkedHashSet<Object> debris = new LinkedHashSet<Object>(1);
        assertThat(basin1.collectFirstOutput()).isNull();
        basin1.collectOutputInto(output);
        assertThat(output).isEmpty();
        assertThat(basin1.collectFirstDebris()).isNull();
        basin1.collectDebrisInto(debris);
        assertThat(debris).isEmpty();
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

            basin1.collectFirstDebris();

            fail();

        } catch (final Exception ignored) {

        }

        try {

            basin1.collectDebrisInto(debris);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testPartialFeed() {

        final Basin<Object, Object> basin = Basin.collect(Waterfall.fallingFrom(new OpenDam<Object>() {

            @Override
            public void onFlush(final Floodgate<Object, Object> gate) {

                gate.discharge(new IllegalStateException());
            }
        }));

        assertThat(basin.thenPartiallyFeedWith("test").collectOutput()).containsExactly("test");
        assertThat(basin.thenPartiallyFeedWith("1", "ciao", "2", "5").collectOutput())
                .containsExactly("1", "ciao", "2", "5");
        assertThat(basin.thenPartiallyFeedWith(Arrays.asList("1", "ciao", "2", "5")).collectOutput())
                .containsExactly("1", "ciao", "2", "5");
        final List<Object> objects = basin.thenFeedWith("test").collectOutput();
        assertThat(objects).hasSize(2);
        assertThat(objects.get(0)).isEqualTo("test");
        assertThat(objects.get(1)).isExactlyInstanceOf(IllegalStateException.class);

        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Byte>())), (byte[]) null)
                        .collectOutput()).isEmpty();
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Byte>())), new byte[]{1, 2, 3})
                        .collectOutput()).containsExactly((byte) 1, (byte) 2, (byte) 3);
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Character>())), (char[]) null)
                        .collectOutput()
        ).isEmpty();
        assertThat(
                Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Character>())), new char[]{1, 2, 3})
                     .collectOutput()
        ).containsExactly((char) 1, (char) 2, (char) 3);
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Boolean>())), (boolean[]) null)
                        .collectOutput()).isEmpty();
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Boolean>())), true, true, false)
                        .collectOutput()).containsExactly(true, true, false);
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Short>())), (short[]) null)
                        .collectOutput()).isEmpty();
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Short>())), new short[]{1, 2, 3})
                        .collectOutput()
        ).containsExactly((short) 1, (short) 2, (short) 3);
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Integer>())), (int[]) null)
                        .collectOutput()).isEmpty();
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Integer>())), 1, 2, 3)
                        .collectOutput()
        ).containsExactly(1, 2, 3);
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Long>())), (long[]) null)
                        .collectOutput()).isEmpty();
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Long>())), 1L, 2L, 3L)
                        .collectOutput()
        ).containsExactly((long) 1, (long) 2, (long) 3);
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Float>())), (float[]) null)
                        .collectOutput()).isEmpty();
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Float>())), 1f, 2f, 3f)
                        .collectOutput()
        ).containsExactly((float) 1, (float) 2, (float) 3);
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Double>())), (double[]) null)
                        .collectOutput()).isEmpty();
        assertThat(Basin.partiallyFeed(Basin.collect(Waterfall.fallingFrom(new OpenDam<Double>())), 1D, 2D, 3D)
                        .collectOutput()
        ).containsExactly((double) 1, (double) 2, (double) 3);
    }
}