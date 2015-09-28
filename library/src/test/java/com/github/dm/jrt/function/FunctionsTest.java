/*
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
package com.github.dm.jrt.function;

import com.github.dm.jrt.function.Functions.BiConsumerObject;
import com.github.dm.jrt.function.Functions.ConsumerObject;
import com.github.dm.jrt.function.Functions.FunctionObject;
import com.github.dm.jrt.function.Functions.SupplierObject;

import org.junit.Test;

import static com.github.dm.jrt.function.Functions.biSink;
import static com.github.dm.jrt.function.Functions.constant;
import static com.github.dm.jrt.function.Functions.identity;
import static com.github.dm.jrt.function.Functions.newBiConsumer;
import static com.github.dm.jrt.function.Functions.newConsumer;
import static com.github.dm.jrt.function.Functions.newFunction;
import static com.github.dm.jrt.function.Functions.newSupplier;
import static com.github.dm.jrt.function.Functions.sink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Functions unit tests.
 * <p/>
 * Created by davide-maestroni on 09/24/2015.
 */
public class FunctionsTest {

    @Test
    public void testBiConsumer() {

        final TestBiConsumer consumer1 = new TestBiConsumer();
        final BiConsumerObject<Object, Object> consumer2 = newBiConsumer(consumer1);
        assertThat(newBiConsumer(consumer2)).isSameAs(consumer2);
        consumer2.accept("test", "test");
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        final TestBiConsumer consumer3 = new TestBiConsumer();
        final BiConsumerObject<Object, Object> consumer4 = consumer2.andThen(consumer3);
        consumer4.accept("test", "test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
    }

    @Test
    public void testBiConsumerContext() {

        final BiConsumerObject<Object, Object> consumer1 =
                newBiConsumer(new TestBiConsumer()).andThen(new TestBiConsumer());
        assertThat(consumer1.hasStaticContext()).isTrue();
        assertThat(consumer1.andThen(new BiConsumer<Object, Object>() {

            public void accept(final Object o, final Object o2) {

            }
        }).hasStaticContext()).isFalse();
        assertThat(consumer1.andThen(consumer1).hasStaticContext()).isTrue();
    }

    @Test
    public void testBiConsumerEquals() {

        final TestBiConsumer consumer1 = new TestBiConsumer();
        assertThat(newBiConsumer(consumer1)).isEqualTo(newBiConsumer(consumer1));
        final BiConsumerObject<Object, Object> consumer2 = newBiConsumer(consumer1);
        assertThat(consumer2).isNotEqualTo(null);
        assertThat(consumer2).isNotEqualTo("test");
        assertThat(newBiConsumer(consumer1).andThen(consumer2).hashCode()).isEqualTo(
                consumer2.andThen(consumer2).hashCode());
        assertThat(newBiConsumer(consumer1).andThen(consumer2)).isEqualTo(
                consumer2.andThen(consumer2));
        assertThat(consumer2.andThen(consumer2)).isEqualTo(
                newBiConsumer(consumer1).andThen(consumer2));
        assertThat(newBiConsumer(consumer1).andThen(consumer2).hashCode()).isEqualTo(
                consumer2.andThen(consumer1).hashCode());
        assertThat(newBiConsumer(consumer1).andThen(consumer2)).isEqualTo(
                consumer2.andThen(consumer1));
        assertThat(consumer2.andThen(consumer1)).isEqualTo(
                newBiConsumer(consumer1).andThen(consumer2));
        assertThat(newBiConsumer(consumer1).andThen(consumer2).hashCode()).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
        assertThat(newBiConsumer(consumer1).andThen(consumer2)).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)));
        assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
                newBiConsumer(consumer1).andThen(consumer2));
        assertThat(newBiConsumer(consumer1).andThen(consumer1).hashCode()).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
        assertThat(newBiConsumer(consumer1).andThen(consumer1)).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)));
        assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
                newBiConsumer(consumer1).andThen(consumer1));
        assertThat(consumer2.andThen(consumer1).hashCode()).isNotEqualTo(
                consumer2.andThen(biSink()).hashCode());
        assertThat(consumer2.andThen(consumer1)).isNotEqualTo(consumer2.andThen(biSink()));
        assertThat(consumer2.andThen(biSink())).isNotEqualTo(consumer2.andThen(consumer1));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBiConsumerError() {

        try {

            newBiConsumer(new TestBiConsumer()).andThen(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBiSink() {

        final TestBiConsumer consumer1 = new TestBiConsumer();
        final BiConsumerObject<Object, Object> consumer2 = biSink().andThen(consumer1);
        consumer2.accept("test", "test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.hasStaticContext()).isTrue();
        assertThat(biSink()).isSameAs(biSink());
    }

    @Test
    public void testConstant() {

        final TestFunction function = new TestFunction();
        final SupplierObject<Object> supplier = constant("test").andThen(function);
        assertThat(supplier.get()).isEqualTo("test");
        assertThat(function.isCalled()).isTrue();
        assertThat(supplier.hasStaticContext()).isTrue();
    }

    @Test
    public void testConsumer() {

        final TestConsumer consumer1 = new TestConsumer();
        final ConsumerObject<Object> consumer2 = newConsumer(consumer1);
        assertThat(newConsumer(consumer2)).isSameAs(consumer2);
        consumer2.accept("test");
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        final TestConsumer consumer3 = new TestConsumer();
        final ConsumerObject<Object> consumer4 = consumer2.andThen(consumer3);
        consumer4.accept("test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
    }

    @Test
    public void testConsumerContext() {

        final ConsumerObject<Object> consumer1 =
                newConsumer(new TestConsumer()).andThen(new TestConsumer());
        assertThat(consumer1.hasStaticContext()).isTrue();
        assertThat(consumer1.andThen(new Consumer<Object>() {

            public void accept(final Object o) {

            }
        }).hasStaticContext()).isFalse();
        assertThat(consumer1.andThen(consumer1).hasStaticContext()).isTrue();
    }

    @Test
    public void testConsumerEquals() {

        final TestConsumer consumer1 = new TestConsumer();
        assertThat(newConsumer(consumer1)).isEqualTo(newConsumer(consumer1));
        final ConsumerObject<Object> consumer2 = newConsumer(consumer1);
        assertThat(consumer2).isNotEqualTo(null);
        assertThat(consumer2).isNotEqualTo("test");
        assertThat(newConsumer(consumer1).andThen(consumer2).hashCode()).isEqualTo(
                consumer2.andThen(consumer2).hashCode());
        assertThat(newConsumer(consumer1).andThen(consumer2)).isEqualTo(
                consumer2.andThen(consumer2));
        assertThat(consumer2.andThen(consumer2)).isEqualTo(
                newConsumer(consumer1).andThen(consumer2));
        assertThat(newConsumer(consumer1).andThen(consumer2).hashCode()).isEqualTo(
                consumer2.andThen(consumer1).hashCode());
        assertThat(newConsumer(consumer1).andThen(consumer2)).isEqualTo(
                consumer2.andThen(consumer1));
        assertThat(consumer2.andThen(consumer1)).isEqualTo(
                newConsumer(consumer1).andThen(consumer2));
        assertThat(newConsumer(consumer1).andThen(consumer2).hashCode()).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
        assertThat(newConsumer(consumer1).andThen(consumer2)).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)));
        assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
                newConsumer(consumer1).andThen(consumer2));
        assertThat(newConsumer(consumer1).andThen(consumer1).hashCode()).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)).hashCode());
        assertThat(newConsumer(consumer1).andThen(consumer1)).isNotEqualTo(
                consumer2.andThen(consumer2.andThen(consumer1)));
        assertThat(consumer2.andThen(consumer2.andThen(consumer1))).isNotEqualTo(
                newConsumer(consumer1).andThen(consumer1));
        assertThat(consumer2.andThen(consumer1).hashCode()).isNotEqualTo(
                consumer2.andThen(sink()).hashCode());
        assertThat(consumer2.andThen(consumer1)).isNotEqualTo(consumer2.andThen(sink()));
        assertThat(consumer2.andThen(sink())).isNotEqualTo(consumer2.andThen(consumer1));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConsumerError() {

        try {

            newConsumer(new TestConsumer()).andThen(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFunction() {

        final TestFunction function1 = new TestFunction();
        final FunctionObject<Object, Object> function2 = newFunction(function1);
        assertThat(newFunction(function2)).isSameAs(function2);
        assertThat(function2.apply("test")).isEqualTo("test");
        assertThat(function1.isCalled()).isTrue();
        function1.reset();
        final TestFunction function3 = new TestFunction();
        final FunctionObject<Object, Object> function4 = function2.andThen(function3);
        assertThat(function4.apply("test")).isEqualTo("test");
        assertThat(function1.isCalled()).isTrue();
        assertThat(function3.isCalled()).isTrue();
        final FunctionObject<String, Integer> function5 =
                newFunction(new Function<String, Integer>() {

                    public Integer apply(final String s) {

                        return s.length();
                    }
                }).andThen(new Function<Integer, Integer>() {

                    public Integer apply(final Integer integer) {

                        return integer * 3;
                    }
                });
        assertThat(function5.apply("test")).isEqualTo(12);
        assertThat(function5.compose(new Function<String, String>() {

            public String apply(final String s) {

                return s + s;
            }
        }).apply("test")).isEqualTo(24);
    }

    @Test
    public void testFunctionContext() {

        final FunctionObject<Object, Object> function1 =
                newFunction(new TestFunction()).andThen(new TestFunction());
        assertThat(function1.hasStaticContext()).isTrue();
        assertThat(function1.andThen(new Function<Object, Object>() {

            public Object apply(final Object o) {

                return null;
            }
        }).hasStaticContext()).isFalse();
        assertThat(function1.andThen(function1).hasStaticContext()).isTrue();
        assertThat(function1.compose(new Function<Object, Object>() {

            public Object apply(final Object o) {

                return null;
            }
        }).hasStaticContext()).isFalse();
        assertThat(function1.compose(function1).hasStaticContext()).isTrue();
    }

    @Test
    public void testFunctionEquals() {

        final TestFunction function1 = new TestFunction();
        assertThat(newFunction(function1)).isEqualTo(newFunction(function1));
        final FunctionObject<Object, Object> function2 = newFunction(function1);
        assertThat(function2).isNotEqualTo(null);
        assertThat(function2).isNotEqualTo("test");
        assertThat(newFunction(function1).andThen(function2).hashCode()).isEqualTo(
                function2.andThen(function2).hashCode());
        assertThat(newFunction(function1).andThen(function2)).isEqualTo(
                function2.andThen(function2));
        assertThat(function2.andThen(function2)).isEqualTo(
                newFunction(function1).andThen(function2));
        assertThat(newFunction(function1).andThen(function2).hashCode()).isEqualTo(
                function2.andThen(function1).hashCode());
        assertThat(newFunction(function1).andThen(function2)).isEqualTo(
                function2.andThen(function1));
        assertThat(function2.andThen(function1)).isEqualTo(
                newFunction(function1).andThen(function2));
        assertThat(newFunction(function1).andThen(function2).hashCode()).isNotEqualTo(
                function2.andThen(function2.andThen(function1)).hashCode());
        assertThat(newFunction(function1).andThen(function2)).isNotEqualTo(
                function2.andThen(function2.andThen(function1)));
        assertThat(function2.andThen(function2.andThen(function1))).isNotEqualTo(
                newFunction(function1).andThen(function2));
        assertThat(newFunction(function1).andThen(function1).hashCode()).isNotEqualTo(
                function2.andThen(function2.andThen(function1)).hashCode());
        assertThat(newFunction(function1).andThen(function1)).isNotEqualTo(
                function2.andThen(function2.andThen(function1)));
        assertThat(function2.andThen(function2.andThen(function1))).isNotEqualTo(
                newFunction(function1).andThen(function1));
        assertThat(function2.andThen(function1).hashCode()).isNotEqualTo(
                function2.andThen(identity()).hashCode());
        assertThat(function2.andThen(function1)).isNotEqualTo(function2.andThen(identity()));
        assertThat(function2.andThen(identity())).isNotEqualTo(function2.andThen(function1));
        assertThat(newFunction(function1).compose(function2).hashCode()).isEqualTo(
                function2.compose(function2).hashCode());
        assertThat(newFunction(function1).compose(function2)).isEqualTo(
                function2.compose(function2));
        assertThat(function2.compose(function2)).isEqualTo(
                newFunction(function1).compose(function2));
        assertThat(newFunction(function1).compose(function2).hashCode()).isEqualTo(
                function2.compose(function1).hashCode());
        assertThat(newFunction(function1).compose(function2)).isEqualTo(
                function2.compose(function1));
        assertThat(function2.compose(function1)).isEqualTo(
                newFunction(function1).compose(function2));
        assertThat(newFunction(function1).compose(function2).hashCode()).isNotEqualTo(
                function2.compose(function2.compose(function1)).hashCode());
        assertThat(newFunction(function1).compose(function2)).isNotEqualTo(
                function2.compose(function2.compose(function1)));
        assertThat(function2.compose(function2.compose(function1))).isNotEqualTo(
                newFunction(function1).compose(function2));
        assertThat(newFunction(function1).compose(function1).hashCode()).isNotEqualTo(
                function2.compose(function2.compose(function1)).hashCode());
        assertThat(newFunction(function1).compose(function1)).isNotEqualTo(
                function2.compose(function2.compose(function1)));
        assertThat(function2.compose(function2.compose(function1))).isNotEqualTo(
                newFunction(function1).compose(function1));
        assertThat(function2.compose(function1).hashCode()).isNotEqualTo(
                function2.compose(identity()).hashCode());
        assertThat(function2.compose(function1)).isNotEqualTo(function2.compose(identity()));
        assertThat(function2.compose(identity())).isNotEqualTo(function2.compose(function1));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFunctionError() {

        try {

            newFunction(new TestFunction()).andThen(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            identity().compose(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testIdentity() {

        final TestFunction function1 = new TestFunction();
        final FunctionObject<Object, Object> function2 = identity().andThen(function1);
        assertThat(function2.apply("test")).isEqualTo("test");
        assertThat(function1.isCalled()).isTrue();
        assertThat(function2.hasStaticContext()).isTrue();
        assertThat(identity()).isSameAs(identity());
    }

    @Test
    public void testSink() {

        final TestConsumer consumer1 = new TestConsumer();
        final ConsumerObject<Object> consumer2 = sink().andThen(consumer1);
        consumer2.accept("test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.hasStaticContext()).isTrue();
        assertThat(sink()).isSameAs(sink());
    }

    @Test
    public void testSupplier() {

        final TestSupplier supplier1 = new TestSupplier();
        final SupplierObject<Object> supplier2 = newSupplier(supplier1);
        assertThat(newSupplier(supplier2)).isSameAs(supplier2);
        assertThat(supplier2.get()).isSameAs(supplier1);
        assertThat(supplier1.isCalled()).isTrue();
        supplier1.reset();
        final TestFunction function = new TestFunction();
        final SupplierObject<Object> supplier3 = supplier2.andThen(function);
        assertThat(supplier3.get()).isSameAs(supplier1);
        assertThat(supplier1.isCalled()).isTrue();
        assertThat(function.isCalled()).isTrue();
        assertThat(constant("test").andThen(new Function<String, Integer>() {

            public Integer apply(final String s) {

                return s.length();
            }
        }).andThen(new Function<Integer, Integer>() {

            public Integer apply(final Integer integer) {

                return integer * 3;
            }
        }).get()).isEqualTo(12);
    }

    @Test
    public void testSupplierContext() {

        final SupplierObject<Object> supplier1 =
                newSupplier(new TestSupplier()).andThen(new TestFunction());
        assertThat(supplier1.hasStaticContext()).isTrue();
        assertThat(supplier1.andThen(new Function<Object, Object>() {

            public Object apply(final Object o) {

                return null;
            }
        }).hasStaticContext()).isFalse();
        assertThat(supplier1.andThen(identity()).hasStaticContext()).isTrue();
    }

    @Test
    public void testSupplierEquals() {

        final TestSupplier supplier1 = new TestSupplier();
        assertThat(newSupplier(supplier1)).isEqualTo(newSupplier(supplier1));
        final SupplierObject<Object> supplier2 = newSupplier(supplier1);
        assertThat(supplier2).isNotEqualTo(null);
        assertThat(supplier2).isNotEqualTo("test");
        final TestFunction function = new TestFunction();
        assertThat(newSupplier(supplier1).andThen(function).hashCode()).isEqualTo(
                supplier2.andThen(function).hashCode());
        assertThat(newSupplier(supplier1).andThen(function)).isEqualTo(supplier2.andThen(function));
        assertThat(supplier2.andThen(function)).isEqualTo(newSupplier(supplier1).andThen(function));
        assertThat(newSupplier(supplier1).andThen(newFunction(function)).hashCode()).isEqualTo(
                supplier2.andThen(function).hashCode());
        assertThat(newSupplier(supplier1).andThen(newFunction(function))).isEqualTo(
                supplier2.andThen(function));
        assertThat(supplier2.andThen(function)).isEqualTo(
                newSupplier(supplier1).andThen(newFunction(function)));
        assertThat(newSupplier(supplier1).andThen(newFunction(function)).hashCode()).isNotEqualTo(
                supplier2.andThen(newFunction(function).andThen(function)).hashCode());
        assertThat(newSupplier(supplier1).andThen(newFunction(function))).isNotEqualTo(
                supplier2.andThen(newFunction(function).andThen(function)));
        assertThat(supplier2.andThen(newFunction(function).andThen(function))).isNotEqualTo(
                newSupplier(supplier1).andThen(newFunction(function)));
        assertThat(newSupplier(supplier1).andThen(function).hashCode()).isNotEqualTo(
                supplier2.andThen(newFunction(function).andThen(function)).hashCode());
        assertThat(newSupplier(supplier1).andThen(function)).isNotEqualTo(
                supplier2.andThen(newFunction(function).andThen(function)));
        assertThat(supplier2.andThen(newFunction(function).andThen(function))).isNotEqualTo(
                newSupplier(supplier1).andThen(function));
        assertThat(supplier2.andThen(function).hashCode()).isNotEqualTo(
                supplier2.andThen(identity()).hashCode());
        assertThat(supplier2.andThen(function)).isNotEqualTo(supplier2.andThen(identity()));
        assertThat(supplier2.andThen(identity())).isNotEqualTo(supplier2.andThen(function));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSupplierError() {

        try {

            newSupplier(new TestSupplier()).andThen(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class TestBiConsumer implements BiConsumer<Object, Object> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public void accept(final Object in1, final Object in2) {

            mIsCalled = true;
        }
    }

    private static class TestConsumer implements Consumer<Object> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public void accept(final Object out) {

            mIsCalled = true;
        }
    }

    private static class TestFunction implements Function<Object, Object> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public Object apply(final Object in) {

            mIsCalled = true;
            return in;
        }
    }

    private static class TestSupplier implements Supplier<Object> {

        private boolean mIsCalled;

        public Object get() {

            mIsCalled = true;
            return this;
        }

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }
    }
}
