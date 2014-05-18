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
import com.bmd.wtf.dam.ClosedDam;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;
import com.bmd.wtf.xtr.arr.CurrentFactories;
import com.bmd.wtf.xtr.arr.CurrentFactory;
import com.bmd.wtf.xtr.arr.DamFactory;
import com.bmd.wtf.xtr.arr.RotatingArrayBalancer;
import com.bmd.wtf.xtr.arr.WaterfallArray;
import com.bmd.wtf.xtr.bsn.BlockingBasin;

import junit.framework.TestCase;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for exception classes.
 * <p/>
 * Created by davide on 4/8/14.
 */
public class ExceptionTest extends TestCase {

    public void testExample() {

        final float[] input = new float[]{12, -2354, 636, -77, 93, 39};

        double sum = 0;

        for (final float i : input) {

            sum += (i * i);
        }

        final double rms = Math.sqrt(sum / input.length);

        assertThat(ParallelMath.rootMeanSquare(input)).isEqualTo(rms);
        assertThat(ParallelRootMeanSquare.open().push(input).close()).isEqualTo(rms);
    }

    @SuppressWarnings("UnusedAssignment")
    public void testExceptions() {

        ClosedLoopException cex = new ClosedLoopException();
        cex = new ClosedLoopException("test");
        cex = new ClosedLoopException("test", cex);
        cex = new ClosedLoopException(cex);

        DelayInterruptedException dex = new DelayInterruptedException();
        dex = new DelayInterruptedException("test");
        dex = new DelayInterruptedException("test", dex);
        dex = new DelayInterruptedException("test", dex, true, true);
        dex = new DelayInterruptedException(dex);

        DryStreamException yex = new DryStreamException();
        yex = new DryStreamException("test");
        yex = new DryStreamException("test", yex);
        yex = new DryStreamException(yex);

        DuplicateDamException uex = new DuplicateDamException();
        uex = new DuplicateDamException("test");
        uex = new DuplicateDamException("test", uex);
        uex = new DuplicateDamException(uex);

        FloatingException fex = new FloatingException();
        fex = new FloatingException("test");
        fex = new FloatingException("test", fex);
        fex = new FloatingException("test", fex, true, true);
        fex = new FloatingException(new Object());
        fex = new FloatingException(fex);

        UnauthorizedDischargeException aex = new UnauthorizedDischargeException();
        aex = new UnauthorizedDischargeException("test");
        aex = new UnauthorizedDischargeException("test", aex);
        aex = new UnauthorizedDischargeException(aex);
    }

    public static class BackgroundCapitalizedPrinter {

        private static final OpenDam<String> sCapitalizer = new OpenDam<String>() {

            @Override
            public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

                final StringBuilder builder = new StringBuilder();

                for (final String s : drop.split(" ")) {

                    if (builder.length() > 0) {

                        builder.append(" ");
                    }

                    final int length = s.length();

                    if (length == 1) {

                        builder.append(s.toUpperCase());

                    } else if (length > 1) {

                        builder.append(s.substring(0, 1).toUpperCase());
                        builder.append(s.substring(1).toLowerCase());
                    }
                }

                return super.onDischarge(gate, builder.toString());
            }
        };

        private static final Spring<String> sSpring =
                Waterfall.flowingInto(Currents.threadPoolCurrent(4))
                         .thenFlowingThrough(sCapitalizer)
                         .thenFlowingThrough(new OpenDam<String>() {

                             @Override
                             public Object onDischarge(final Floodgate<String, String> gate,
                                     final String drop) {

                                 System.out.println(drop);

                                 return super.onDischarge(gate, drop);
                             }
                         }).backToSource();

        public static void println(final String text) {

            sSpring.discharge(text);
        }
    }

    public static class BackgroundCapitalizer {

        private static final Current S_CURRENT = Currents.threadPoolCurrent(4);

        public static String println(final String text) {

            final OpenDam<String> capitalizer = new OpenDam<String>() {

                @Override
                public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

                    final StringBuilder builder = new StringBuilder();

                    for (final String s : drop.split(" ")) {

                        if (builder.length() > 0) {

                            builder.append(" ");
                        }

                        final int length = s.length();

                        if (length == 1) {

                            builder.append(s.toUpperCase());

                        } else if (length > 1) {

                            builder.append(s.substring(0, 1).toUpperCase());
                            builder.append(s.substring(1).toLowerCase());
                        }
                    }

                    return super.onDischarge(gate, builder.toString());
                }
            };

            final OpenDam<String> printer = new OpenDam<String>() {

                @Override
                public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

                    System.out.println(drop);

                    return super.onDischarge(gate, drop);
                }
            };

            return BlockingBasin.collect(
                    Waterfall.flowingInto(S_CURRENT).thenFlowingThrough(capitalizer)
                             .thenFlowingThrough(printer)
            ).thenFeedWith(text).whenAvailable().collectFirstOutput();
        }
    }

    public static class BackgroundPrinter {

        private static final Spring<String> sSpring =
                Waterfall.flowingInto(Currents.threadPoolCurrent(4))
                         .thenFlowingThrough(new OpenDam<String>() {

                                                 @Override
                                                 public Object onDischarge(
                                                         final Floodgate<String, String> gate,
                                                         final String drop) {

                                                     System.out.println(drop);

                                                     return super.onDischarge(gate, drop);
                                                 }
                                             }
                         ).backToSource();

        public static void println(final String text) {

            sSpring.discharge(text);
        }
    }

    public static class ParallelMath {

        private static final Current S_SQUARE_CURRENT = Currents.threadPoolCurrent(3);

        private static final CurrentFactory S_SUM_CURRENT_FACTORY =
                CurrentFactories.singletonCurrentFactory(Currents.threadPoolCurrent(1));

        private static final DamFactory<float[], Double> sSquareDamFactory =
                new DamFactory<float[], Double>() {

                    @Override
                    public Dam<float[], Double> createForStream(final int streamNumber) {

                        return new ClosedDam<float[], Double>() {

                            @Override
                            public Object onDischarge(final Floodgate<float[], Double> gate,
                                    final float[] drop) {

                                final double sample = (double) drop[streamNumber];
                                gate.discharge(sample * sample);

                                return null;
                            }
                        };
                    }
                };

        public static double rootMeanSquare(final float... data) {

            final int length = data.length;

            final ClosedDam<Double, Double> meanDam = new ClosedDam<Double, Double>() {

                private int mCount;

                private double mSum;

                @Override
                public Object onDischarge(final Floodgate<Double, Double> gate, final Double drop) {

                    mSum += drop;

                    if (++mCount == length) {

                        gate.discharge(Math.sqrt(mSum / mCount)).flush();
                    }

                    return null;
                }
            };

            return BlockingBasin.collect(WaterfallArray.formingFrom(
                    Waterfall.flowingInto(S_SQUARE_CURRENT)
                             .thenFlowingThrough(new OpenDam<float[]>())).thenSplittingIn(length)
                                                       .thenFlowingThrough(sSquareDamFactory)
                                                       .thenFlowingInto(S_SUM_CURRENT_FACTORY)
                                                       .thenMergingThrough(meanDam))
                                .thenFeedWith(data).whenAvailable().collectFirstOutput();
        }
    }

    public static class ParallelRootMeanSquare {

        private static final Current S_SQUARE_CURRENT = Currents.threadPoolCurrent(3);

        private static final Current S_SUM_CURRENT = Currents.threadPoolCurrent(1);

        private static final DamFactory<Float, Double> sSquareDamFactory =
                new DamFactory<Float, Double>() {

                    @Override
                    public Dam<Float, Double> createForStream(final int streamNumber) {

                        return new AbstractDam<Float, Double>() {

                            @Override
                            public Object onDischarge(final Floodgate<Float, Double> gate,
                                    final Float drop) {

                                final double sample = drop;
                                gate.discharge(sample * sample);

                                return null;
                            }
                        };
                    }
                };

        private final BlockingBasin<Float, Double> mCollectingBasin;

        private ParallelRootMeanSquare() {

            final OpenDam<Float> countingDam = new OpenDam<Float>() {

                private int mCount;

                @Override
                public Object onFlush(final Floodgate<Float, Float> gate) {

                    super.onFlush(gate);

                    return mCount;
                }

                @Override
                public Object onDischarge(final Floodgate<Float, Float> gate, final Float drop) {

                    ++mCount;

                    return super.onDischarge(gate, drop);
                }


            };

            final ClosedDam<Double, Double> meanDam = new ClosedDam<Double, Double>() {

                private int mCount;

                private int mSize = Integer.MAX_VALUE;

                private double mSum;

                @Override
                public Object onDischarge(final Floodgate<Double, Double> gate, final Double drop) {

                    mSum += drop;
                    ++mCount;

                    onFlush(gate);

                    return null;
                }

                @Override
                public Object onFlush(final Floodgate<Double, Double> gate) {

                    if (mCount >= mSize) {

                        gate.discharge(Math.sqrt(mSum / mCount)).flush();
                    }

                    return null;
                }

                @Override
                public Object onPushDebris(final Floodgate<Double, Double> gate,
                        final Object debris) {

                    mSize = (Integer) debris;

                    onFlush(gate);

                    return null;
                }
            };

            mCollectingBasin = BlockingBasin.collect(WaterfallArray.formingFrom(
                    Waterfall.fallingFrom(countingDam).thenFlowingInto(S_SQUARE_CURRENT))
                                                                   .thenSplittingIn(3)
                                                                   .thenBalancedBy(
                                                                           new RotatingArrayBalancer<Float>())
                                                                   .thenFlowingThrough(
                                                                           sSquareDamFactory)
                                                                   .thenMergingInto(S_SUM_CURRENT)
                                                                   .thenFlowingThrough(meanDam));
        }

        public static ParallelRootMeanSquare open() {

            return new ParallelRootMeanSquare();
        }

        public double close() {

            return mCollectingBasin.thenFeedWith().whenAvailable().collectFirstOutput();
        }

        public ParallelRootMeanSquare push(final float... data) {

            final Spring<Float> spring = mCollectingBasin.thenFlow().backToSource();

            for (final float v : data) {

                spring.discharge(v);
            }

            return this;
        }
    }
}