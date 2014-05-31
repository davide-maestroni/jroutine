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
package com.bmd.wtf.xtr.qdc;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.crr.Currents;
import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.dam.ClosedDam;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.Dams;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;
import com.bmd.wtf.xtr.arr.DamFactory;
import com.bmd.wtf.xtr.arr.WaterfallArray;
import com.bmd.wtf.xtr.bsn.BlockingBasin;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link com.bmd.wtf.xtr.qdc} package classes.
 * <p/>
 * Created by davide on 4/8/14.
 */
public class AqueductTest extends TestCase {

    public void testAqueduct() {

        final ArrayList<Integer> data = new ArrayList<Integer>(1);
        final ArrayList<Object> debris = new ArrayList<Object>(2);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        BlockingBasin.collect(Aqueduct.fedBy(Waterfall.fallingFrom(new OpenDam<String>()))
                                      .thenCrossingThrough(new AbstractArchway<String, String>() {

                                          @Override
                                          public void onDischarge(final Floodgate<String, String> gate,
                                                  final List<Spring<String>> springs, final String drop) {

                                              springs.get(0).discharge(drop.toLowerCase());
                                          }
                                      }).thenFlowingThrough(new Archway<String, Integer>() {

                    @Override
                    public void onDischarge(final Floodgate<String, Integer> gate, final List<Spring<Integer>> springs,
                            final String drop) {

                        executorService.execute(new Runnable() {

                            @Override
                            public void run() {

                                try {

                                    Thread.sleep(100);

                                    springs.get(0).discharge(Integer.parseInt(drop));

                                } catch (final InterruptedException e) {

                                    Thread.currentThread().interrupt();

                                } catch (final Throwable t) {

                                    springs.get(0).drop(t);
                                }
                            }
                        });
                    }

                    @Override
                    public void onDrop(final Floodgate<String, Integer> gate, final List<Spring<Integer>> springs,
                            final Object debris) {

                        executorService.execute(new Runnable() {

                            @Override
                            public void run() {

                                springs.get(0).drop(debris);
                            }
                        });
                    }

                    @Override
                    public void onFlush(final Floodgate<String, Integer> gate, final List<Spring<Integer>> springs) {

                        executorService.execute(new Runnable() {

                            @Override
                            public void run() {

                                springs.get(0).flush();
                            }
                        });
                    }
                }).get(0).thenFallingThrough(new AbstractDam<Integer, Integer>() {

                                                 private int mSum = 0;

                                                 @Override
                                                 public void onDischarge(final Floodgate<Integer, Integer> gate,
                                                         final Integer drop) {

                                                     mSum += drop;
                                                 }

                                                 @Override
                                                 public void onFlush(final Floodgate<Integer, Integer> gate) {

                                                     gate.discharge(mSum).flush();
                                                 }
                                             }
                )).thenFeedWith("test", "1", "ciao", "2", "5", null).afterMax(1, TimeUnit.MINUTES)
                     .collectOutputInto(data).collectDebrisInto(debris);
        assertThat(data).containsExactly(8);
        assertThat(debris).hasSize(3);
        assertThat(debris.get(0)).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(debris.get(1)).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(debris.get(2)).isExactlyInstanceOf(NullPointerException.class);
    }

    public void testError() {

        try {

            Aqueduct.fedBy(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Aqueduct.fedBy(Waterfall.fallingFrom(Dams.openDam())).thenSeparatingIn(0);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Aqueduct.fedBy(Waterfall.fallingFrom(Dams.openDam())).thenSeparatingIn(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Aqueduct.fedBy(Waterfall.fallingFrom(Dams.openDam())).thenCrossingThrough(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Aqueduct.fedBy(Waterfall.fallingFrom(Dams.openDam())).thenFlowingThrough((Archway<Object, Object>) null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final AbstractArchway<Object, Object> archway = new AbstractArchway<Object, Object>() {

                @Override
                public void onDischarge(final Floodgate<Object, Object> gate, final List<Spring<Object>> springs,
                        final Object drop) {

                }
            };

            Aqueduct.fedBy(Waterfall.fallingFrom(Dams.openDam())).thenCrossingThrough(archway)
                    .thenFlowingThrough(archway);

            fail();

        } catch (final Exception ignored) {

        }

        final BlockingBasin<Object, Object> basin = BlockingBasin.collect(
                Aqueduct.fedBy(Waterfall.fallingFrom(Dams.openDam())).thenFlowingThrough(new Archway<Object, Object>() {

                    @Override
                    public void onDischarge(final Floodgate<Object, Object> gate, final List<Spring<Object>> springs,
                            final Object drop) {

                        throw new NullPointerException();
                    }

                    @Override
                    public void onDrop(final Floodgate<Object, Object> gate, final List<Spring<Object>> springs,
                            final Object debris) {

                        throw new IllegalArgumentException();
                    }

                    @Override
                    public void onFlush(final Floodgate<Object, Object> gate, final List<Spring<Object>> springs) {

                        throw new IllegalStateException();
                    }
                }).get(0)
        );
        basin.thenFeedWith("test");
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstDebris()).isExactlyInstanceOf(NullPointerException.class);
        assertThat(basin.collectFirstDebris()).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin.collectFirstDebris()).isNull();
        basin.thenDrop("test");
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstDebris()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(basin.collectFirstDebris()).isNull();
        basin.thenFlush();
        assertThat(basin.collectOutput()).isEmpty();
        assertThat(basin.collectFirstDebris()).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(basin.collectFirstDebris()).isNull();
    }

    public void testQueueArchway() {

        final QueueArchway<Float> archway = new QueueArchway<Float>();

        final double result = BlockingBasin.collect(WaterfallArray.formingFrom(
                Aqueduct.fedBy(Waterfall.fallingFrom(new OpenDam<Float>())).thenSeparatingIn(3)
                        .thenFlowingThrough(archway)
        ).thenFallingThrough(new DamFactory<Float, Double>() {

            @Override
            public Dam<Float, Double> createForStream(final int streamNumber) {

                return new AbstractDam<Float, Double>() {

                    @Override
                    public void onDischarge(final Floodgate<Float, Double> gate, final Float drop) {

                        final double sample = drop;
                        gate.discharge(sample * sample);

                        assertThat(archway.isWaiting(drop)).isFalse();
                        assertThat(archway.removeIfWaiting(drop)).isFalse();
                        assertThat(archway.levelOf(drop)).isEqualTo(streamNumber);

                        new Thread() {

                            @Override
                            public void run() {

                                try {

                                    Thread.sleep(100);

                                } catch (final InterruptedException ignored) {

                                }

                                if (streamNumber == 0) {

                                    assertThat(archway.consume(drop)).isTrue();

                                } else {

                                    assertThat(archway.consume(3333f)).isFalse();
                                    assertThat(archway.releaseLevel(streamNumber)).isTrue();
                                }
                            }
                        }.start();
                    }
                };
            }
        }).thenMergingThrough(new ClosedDam<Double, Double>() {

            private int mCount;

            private int mFlushes;

            private double mSum;

            @Override
            public void onDischarge(final Floodgate<Double, Double> gate, final Double drop) {

                mSum += drop;
                ++mCount;
            }

            @Override
            public void onFlush(final Floodgate<Double, Double> gate) {

                if (++mFlushes >= 3) {

                    gate.discharge(Math.sqrt(mSum / mCount)).flush();
                }
            }
        })).thenDrop(null).thenFeedWith(12f, -2354f, 636f, -77f, 93f, 39f).collectFirstOutput();

        final float[] input = new float[]{12, -2354, 636, -77, 93, 39};

        double sum = 0;

        for (final float i : input) {

            sum += (i * i);
        }

        final double rms = Math.sqrt(sum / input.length);

        assertThat(result).isEqualTo(rms);
    }

    public void testQueueArchwayAsync() {

        final QueueArchway<Float> archway = new QueueArchway<Float>();

        final double result = BlockingBasin.collect(WaterfallArray.formingFrom(Aqueduct.fedBy(
                Waterfall.fallingFrom(new OpenDam<Float>()).thenFlowingInto(Currents.threadPoolCurrent(1)))
                                                                                       .thenSeparatingIn(3)
                                                                                       .thenFlowingThrough(archway))
                                                                  .thenFallingThrough(new DamFactory<Float, Double>() {

                                                                                          @Override
                                                                                          public Dam<Float, Double> createForStream(
                                                                                                  final int streamNumber) {

                                                                                              return new AbstractDam<Float, Double>() {

                                                                                                  @Override
                                                                                                  public void onDischarge(
                                                                                                          final Floodgate<Float, Double> gate,
                                                                                                          final Float drop) {

                                                                                                      final double
                                                                                                              sample =
                                                                                                              drop;
                                                                                                      gate.discharge(
                                                                                                              sample
                                                                                                                      * sample);

                                                                                                      assertThat(
                                                                                                              archway.isWaiting(
                                                                                                                      drop)
                                                                                                      ).isFalse();
                                                                                                      assertThat(
                                                                                                              archway.removeIfWaiting(
                                                                                                                      drop)
                                                                                                      ).isFalse();
                                                                                                      assertThat(
                                                                                                              archway.levelOf(
                                                                                                                      drop)
                                                                                                      ).isEqualTo(
                                                                                                              streamNumber);

                                                                                                      new Thread() {

                                                                                                          @Override
                                                                                                          public void run() {

                                                                                                              try {

                                                                                                                  Thread.sleep(
                                                                                                                          100);

                                                                                                              } catch (final InterruptedException ignored) {

                                                                                                              }

                                                                                                              if (streamNumber
                                                                                                                      == 0) {

                                                                                                                  assertThat(
                                                                                                                          archway.consume(
                                                                                                                                  drop)
                                                                                                                  ).isTrue();

                                                                                                              } else {

                                                                                                                  assertThat(
                                                                                                                          archway.consume(
                                                                                                                                  3333f)
                                                                                                                  ).isFalse();
                                                                                                                  assertThat(
                                                                                                                          archway.releaseLevel(
                                                                                                                                  streamNumber)
                                                                                                                  ).isTrue();
                                                                                                              }
                                                                                                          }
                                                                                                      }.start();
                                                                                                  }
                                                                                              };
                                                                                          }
                                                                                      }
                                                                  ).thenMergingThrough(new ClosedDam<Double, Double>() {

                                                                                           private int mCount;

                                                                                           private int mFlushes;

                                                                                           private double mSum;

                                                                                           @Override
                                                                                           public void onDischarge(
                                                                                                   final Floodgate<Double, Double> gate,
                                                                                                   final Double drop) {

                                                                                               mSum += drop;
                                                                                               ++mCount;
                                                                                           }

                                                                                           @Override
                                                                                           public void onFlush(
                                                                                                   final Floodgate<Double, Double> gate) {

                                                                                               if (++mFlushes >= 3) {

                                                                                                   gate.discharge(
                                                                                                           Math.sqrt(
                                                                                                                   mSum
                                                                                                                           / mCount))
                                                                                                       .flush();
                                                                                               }
                                                                                           }
                                                                                       }
                )).thenDrop(null).thenFeedWith(12f, -2354f, 636f, -77f, 93f, 39f).whenAvailable().collectFirstOutput();

        final float[] input = new float[]{12, -2354, 636, -77, 93, 39};

        double sum = 0;

        for (final float i : input) {

            sum += (i * i);
        }

        final double rms = Math.sqrt(sum / input.length);

        assertThat(result).isEqualTo(rms);
    }

    public void testRotatingArchway() {

        final double result = BlockingBasin.collect(WaterfallArray.formingFrom(
                Aqueduct.fedBy(Waterfall.fallingFrom(new OpenDam<Float>())).thenSeparatingIn(3)
                        .thenFlowingThrough(new RotatingArchway<Float>())
        ).thenFallingThrough(new DamFactory<Float, Double>() {

            @Override
            public Dam<Float, Double> createForStream(final int streamNumber) {

                return new AbstractDam<Float, Double>() {

                    @Override
                    public void onDischarge(final Floodgate<Float, Double> gate, final Float drop) {

                        final double sample = drop;
                        gate.discharge(sample * sample);
                    }
                };
            }
        }).thenMergingThrough(new ClosedDam<Double, Double>() {

            private int mCount;

            private int mFlushes;

            private double mSum;

            @Override
            public void onDischarge(final Floodgate<Double, Double> gate, final Double drop) {

                mSum += drop;
                ++mCount;
            }

            @Override
            public void onFlush(final Floodgate<Double, Double> gate) {

                if (++mFlushes >= 3) {

                    gate.discharge(Math.sqrt(mSum / mCount)).flush();
                }
            }
        })).thenFeedWith(12f, -2354f, 636f, -77f, 93f, 39f).collectFirstOutput();

        final float[] input = new float[]{12, -2354, 636, -77, 93, 39};

        double sum = 0;

        for (final float i : input) {

            sum += (i * i);
        }

        final double rms = Math.sqrt(sum / input.length);

        assertThat(result).isEqualTo(rms);
    }
}