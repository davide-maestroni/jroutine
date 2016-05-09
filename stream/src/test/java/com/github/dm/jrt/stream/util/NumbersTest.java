/*
 * Copyright (c) 2016. Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.stream.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Numbers unit tests.
 * <p>
 * Created by davide-maestroni on 05/05/2016.
 */
public class NumbersTest {

    @Test
    public void testAdd() {

        assertThat(Numbers.add(1, 2)).isEqualTo(3);
        assertThat(Numbers.add(1, (byte) 2)).isEqualTo(3);
        assertThat(Numbers.add((short) -1, 2.5f)).isEqualTo(1.5f);
        assertThat(Numbers.add(-1L, 2.5)).isEqualTo(1.5);
        assertThat(Numbers.add(BigDecimal.ZERO, 2.5)).isEqualTo(new BigDecimal(2.5));
        assertThat(Numbers.add(BigInteger.ONE, 2.5)).isEqualTo(new BigDecimal(3.5));
        assertThat(Numbers.add(BigInteger.ONE, -1)).isEqualTo(BigInteger.ZERO);
        assertThat(Numbers.add(BigInteger.ONE, new Number() {

            @Override
            public int intValue() {

                return 0;
            }

            @Override
            public long longValue() {

                return 0;
            }

            @Override
            public float floatValue() {

                return 0;
            }

            @Override
            public double doubleValue() {

                return 0;
            }
        })).isNull();
        assertThat(Numbers.add(new Number() {

            @Override
            public int intValue() {

                return 0;
            }

            @Override
            public long longValue() {

                return 0;
            }

            @Override
            public float floatValue() {

                return 0;
            }

            @Override
            public double doubleValue() {

                return 0;
            }
        }, BigInteger.ONE)).isNull();
    }

    @Test
    public void testAddOptimistic() {

        assertThat(Numbers.addOptimistic(1, 2)).isEqualTo(3);
        assertThat(Numbers.addOptimistic(1, (byte) 2)).isEqualTo(3);
        assertThat(Numbers.addOptimistic((short) -1, 2.5f)).isEqualTo(1.5f);
        assertThat(Numbers.addOptimistic(-1L, 2.5)).isEqualTo(1.5);
        assertThat(Numbers.addOptimistic(BigDecimal.ZERO, 2.5)).isEqualTo(new BigDecimal(2.5));
        assertThat(Numbers.addOptimistic(BigInteger.ONE, 2.5)).isEqualTo(new BigDecimal(3.5));
        assertThat(Numbers.addOptimistic(BigInteger.ONE, -1)).isEqualTo(BigInteger.ZERO);
        assertThat(Numbers.addOptimistic(BigInteger.ONE, new Number() {

            @Override
            public int intValue() {

                return 0;
            }

            @Override
            public long longValue() {

                return 0;
            }

            @Override
            public float floatValue() {

                return 0;
            }

            @Override
            public double doubleValue() {

                return 0;
            }
        })).isEqualTo(BigInteger.ONE);
        assertThat(Numbers.addOptimistic(new Number() {

            @Override
            public int intValue() {

                return 0;
            }

            @Override
            public long longValue() {

                return 0;
            }

            @Override
            public float floatValue() {

                return 0;
            }

            @Override
            public double doubleValue() {

                return 0;
            }
        }, BigInteger.ONE)).isEqualTo(BigInteger.ONE);
    }

    @Test
    public void testAddSafe() {

        assertThat(Numbers.addSafe(1, 2)).isEqualTo(3);
        assertThat(Numbers.addSafe(1, (byte) 2)).isEqualTo(3);
        assertThat(Numbers.addSafe((short) -1, 2.5f)).isEqualTo(1.5f);
        assertThat(Numbers.addSafe(-1L, 2.5)).isEqualTo(1.5);
        assertThat(Numbers.addSafe(BigDecimal.ZERO, 2.5)).isEqualTo(new BigDecimal(2.5));
        assertThat(Numbers.addSafe(BigInteger.ONE, 2.5)).isEqualTo(new BigDecimal(3.5));
        assertThat(Numbers.addSafe(BigInteger.ONE, -1)).isEqualTo(BigInteger.ZERO);

        try {
            Numbers.addSafe(BigInteger.ONE, new Number() {

                @Override
                public int intValue() {

                    return 0;
                }

                @Override
                public long longValue() {

                    return 0;
                }

                @Override
                public float floatValue() {

                    return 0;
                }

                @Override
                public double doubleValue() {

                    return 0;
                }
            });
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            Numbers.addSafe(new Number() {

                @Override
                public int intValue() {

                    return 0;
                }

                @Override
                public long longValue() {

                    return 0;
                }

                @Override
                public float floatValue() {

                    return 0;
                }

                @Override
                public double doubleValue() {

                    return 0;
                }
            }, BigInteger.ONE);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testConstructor() {

        boolean failed = false;
        try {
            new Numbers();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testToBig() {

        assertThat(Numbers.toBig(3)).isEqualTo(new BigDecimal(3));
        assertThat(Numbers.toBig((byte) 2)).isEqualTo(new BigDecimal(2));
        assertThat(Numbers.toBig((short) -1)).isEqualTo(new BigDecimal(-1));
        assertThat(Numbers.toBig(-1L)).isEqualTo(new BigDecimal(-1L));
        assertThat(Numbers.toBig(2.5f)).isEqualTo(new BigDecimal(2.5f));
        assertThat(Numbers.toBig(2.5)).isEqualTo(new BigDecimal(2.5));
        assertThat(Numbers.toBig(BigDecimal.ZERO)).isEqualTo(new BigDecimal(0));
        assertThat(Numbers.toBig(BigInteger.ONE)).isEqualTo(new BigDecimal(1));
        assertThat(Numbers.toBig(new Number() {

            @Override
            public int intValue() {

                return 0;
            }

            @Override
            public long longValue() {

                return 0;
            }

            @Override
            public float floatValue() {

                return 0;
            }

            @Override
            public double doubleValue() {

                return 0;
            }
        })).isNull();
    }

    @Test
    public void testToBigOptimistic() {

        assertThat(Numbers.toBigOptimistic(3)).isEqualTo(new BigDecimal(3));
        assertThat(Numbers.toBigOptimistic((byte) 2)).isEqualTo(new BigDecimal(2));
        assertThat(Numbers.toBigOptimistic((short) -1)).isEqualTo(new BigDecimal(-1));
        assertThat(Numbers.toBigOptimistic(-1L)).isEqualTo(new BigDecimal(-1L));
        assertThat(Numbers.toBigOptimistic(2.5f)).isEqualTo(new BigDecimal(2.5f));
        assertThat(Numbers.toBigOptimistic(2.5)).isEqualTo(new BigDecimal(2.5));
        assertThat(Numbers.toBigOptimistic(BigDecimal.ZERO)).isEqualTo(new BigDecimal(0));
        assertThat(Numbers.toBigOptimistic(BigInteger.ONE)).isEqualTo(new BigDecimal(1));
        assertThat(Numbers.toBigOptimistic(new Number() {

            @Override
            public int intValue() {

                return 0;
            }

            @Override
            public long longValue() {

                return 0;
            }

            @Override
            public float floatValue() {

                return 0;
            }

            @Override
            public double doubleValue() {

                return 0;
            }
        })).isEqualTo(new BigDecimal(0.0));
    }

    @Test
    public void testToBigSafe() {

        assertThat(Numbers.toBigSafe(3)).isEqualTo(new BigDecimal(3));
        assertThat(Numbers.toBigSafe((byte) 2)).isEqualTo(new BigDecimal(2));
        assertThat(Numbers.toBigSafe((short) -1)).isEqualTo(new BigDecimal(-1));
        assertThat(Numbers.toBigSafe(-1L)).isEqualTo(new BigDecimal(-1L));
        assertThat(Numbers.toBigSafe(2.5f)).isEqualTo(new BigDecimal(2.5f));
        assertThat(Numbers.toBigSafe(2.5)).isEqualTo(new BigDecimal(2.5));
        assertThat(Numbers.toBigSafe(BigDecimal.ZERO)).isEqualTo(new BigDecimal(0));
        assertThat(Numbers.toBigSafe(BigInteger.ONE)).isEqualTo(new BigDecimal(1));

        try {
            assertThat(Numbers.toBigSafe(new Number() {

                @Override
                public int intValue() {

                    return 0;
                }

                @Override
                public long longValue() {

                    return 0;
                }

                @Override
                public float floatValue() {

                    return 0;
                }

                @Override
                public double doubleValue() {

                    return 0;
                }
            })).isEqualTo(new BigDecimal(0.0));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }
}
