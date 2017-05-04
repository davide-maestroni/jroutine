package com.github.dm.jrt.rx;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.builder.FlowableBuilder;
import com.github.dm.jrt.rx.config.FlowableConfiguration;
import com.github.dm.jrt.rx.config.FlowableConfiguration.Builder;

import org.jetbrains.annotations.NotNull;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Cancellable;

import static com.github.dm.jrt.core.util.DurationMeasure.noTime;

/**
 * Default implementation of a builder of Flowables.
 * <p>
 * Created by davide-maestroni on 05/04/2017.
 */
class DefaultFlowableBuilder implements FlowableBuilder {

  private FlowableConfiguration mConfiguration = FlowableConfiguration.defaultConfiguration();

  /**
   * Constructor.
   */
  DefaultFlowableBuilder() {
  }

  @NotNull
  public Builder<FlowableBuilder> flowableConfiguration() {
    return new Builder<FlowableBuilder>(this, mConfiguration);
  }

  @NotNull
  public <OUT> Flowable<OUT> of(@NotNull final Channel<?, OUT> channel) {
    return Flowable.create(new ChannelOnSubscribe<OUT>(channel),
        mConfiguration.getBackpressureOrElse(BackpressureStrategy.BUFFER));
  }

  @NotNull
  public FlowableBuilder withConfiguration(@NotNull final FlowableConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("Flowable configuration", configuration);
    return this;
  }

  /**
   * Function binding a channel to an Emitter.
   *
   * @param <OUT> the output data type.
   */
  private static class ChannelOnSubscribe<OUT> implements FlowableOnSubscribe<OUT>, Cancellable {

    private final Channel<?, OUT> mChannel;

    /**
     * Constructor.
     *
     * @param channel the channel instance.
     */
    ChannelOnSubscribe(@NotNull final Channel<?, OUT> channel) {
      mChannel = ConstantConditions.notNull("channel instance", channel);
    }

    public void cancel() {
      mChannel.after(noTime()).abort();
    }

    public void subscribe(final FlowableEmitter<OUT> emitter) {
      emitter.setCancellable(this);
      mChannel.consume(new EmitterConsumer<OUT>(emitter));
    }
  }

  /**
   * Channel consumer feeding an Emitter.
   *
   * @param <OUT> the output data type.
   */
  private static class EmitterConsumer<OUT> implements ChannelConsumer<OUT> {

    private final Emitter<OUT> mEmitter;

    /**
     * Constructor.
     *
     * @param emitter the Emitter instance.
     */
    EmitterConsumer(@NotNull final FlowableEmitter<OUT> emitter) {
      mEmitter = ConstantConditions.notNull("emitter instance", emitter);
    }

    public void onComplete() {
      mEmitter.onComplete();
    }

    public void onError(@NotNull final RoutineException error) {
      mEmitter.onError(error);
    }

    public void onOutput(final OUT output) {
      mEmitter.onNext(output);
    }
  }
}
