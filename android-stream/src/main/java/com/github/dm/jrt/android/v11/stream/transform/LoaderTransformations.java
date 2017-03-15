/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.android.v11.stream.transform;

import com.github.dm.jrt.android.core.ChannelContextInvocation;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.stream.builder.AbstractLoaderTransformationBuilder;
import com.github.dm.jrt.android.stream.builder.LoaderTransformationBuilder;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.FunctionDecorator;
import com.github.dm.jrt.stream.config.StreamConfiguration;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.FunctionDecorator.decorate;

/**
 * Utility class providing transformation functions based on Loader instances.
 * <p>
 * See {@link com.github.dm.jrt.android.v4.stream.transform.LoaderTransformationsCompat
 * LoaderTransformationsCompat} for support of API levels lower than
 * {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p>
 * Created by davide-maestroni on 01/29/2017.
 */
public class LoaderTransformations {

  /**
   * Avoid explicit instantiation.
   */
  private LoaderTransformations() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of functions making the stream routine run on the specified Loader.
   * <p>
   * The example below shows how it's possible to make the computation happen in a dedicated Loader:
   * <pre><code>
   * JRoutineStream.withStreamOf(data)
   *               .map(getMappingFunction())
   *               .lift(LoaderTransformations.runOn(loaderFrom(activity))
   *                                          .loaderConfiguration()
   *                                          .withInvocationId(INVOCATION_ID)
   *                                          .apply()
   *                                          .buildFunction())
   *               .invoke()
   *               .consume(getConsumer())
   *               .close();
   * </code></pre>
   * Note that the Loader ID, by default, will only depend on the inputs, so that, in order to avoid
   * clashing, it is advisable to explicitly set the invocation ID like shown in the example.
   *
   * @param context the Loader context.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the lifting function builder.
   */
  @NotNull
  public static <IN, OUT> LoaderTransformationBuilder<IN, OUT, IN, OUT> runOn(
      @NotNull final LoaderContext context) {
    ConstantConditions.notNull("Loader context", context);
    return new AbstractLoaderTransformationBuilder<IN, OUT, IN, OUT>() {

      @NotNull
      @Override
      protected Function<Channel<?, IN>, Channel<?, OUT>> liftFunction(
          @NotNull final LoaderConfiguration loaderConfiguration,
          @NotNull final StreamConfiguration streamConfiguration,
          @NotNull final Function<Channel<?, IN>, Channel<?, OUT>> function) {
        return new Function<Channel<?, IN>, Channel<?, OUT>>() {

          @Override
          public Channel<?, OUT> apply(final Channel<?, IN> channel) {
            return JRoutineLoader.on(context)
                                 .with(new LoaderContextInvocationFactory<IN, OUT>(
                                     decorate(function)))
                                 .apply(streamConfiguration.toInvocationConfiguration())
                                 .apply(loaderConfiguration)
                                 .invoke()
                                 .pass(channel)
                                 .close();
          }
        };
      }
    };
  }

  /**
   * Context invocation backing a binding function.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class LoaderContextInvocation<IN, OUT> extends ChannelContextInvocation<IN, OUT> {

    private final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> mFunction;

    /**
     * Constructor.
     *
     * @param function the binding function.
     */
    private LoaderContextInvocation(
        @NotNull final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> function) {
      mFunction = function;
    }

    @NotNull
    @Override
    protected Channel<?, OUT> onChannel(@NotNull final Channel<?, IN> channel) throws Exception {
      return mFunction.apply(channel);
    }
  }

  /**
   * Factory of context invocation backing a binding function.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class LoaderContextInvocationFactory<IN, OUT>
      extends ContextInvocationFactory<IN, OUT> {

    private final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> mFunction;

    /**
     * Constructor.
     *
     * @param function the binding function.
     */
    private LoaderContextInvocationFactory(
        @NotNull final FunctionDecorator<Channel<?, IN>, Channel<?, OUT>> function) {
      super(asArgs(function));
      mFunction = ConstantConditions.notNull("binding function", function);
    }

    @NotNull
    @Override
    public ContextInvocation<IN, OUT> newInvocation() {
      return new LoaderContextInvocation<IN, OUT>(mFunction);
    }
  }
}
