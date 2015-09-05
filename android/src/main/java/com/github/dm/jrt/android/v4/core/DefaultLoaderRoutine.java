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
package com.github.dm.jrt.android.v4.core;

import android.content.Context;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.invocation.ContextInvocation;
import com.github.dm.jrt.android.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.routine.LoaderRoutine;
import com.github.dm.jrt.android.runner.Runners;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.DeadlockException;
import com.github.dm.jrt.channel.InputChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.OutputConsumer;
import com.github.dm.jrt.core.AbstractRoutine;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.log.Logger;
import com.github.dm.jrt.runner.TemplateExecution;
import com.github.dm.jrt.util.TimeDuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Routine implementation delegating to Android loaders the asynchronous processing.
 * <p/>
 * Created by davide-maestroni on 01/10/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderRoutine<IN, OUT> extends AbstractRoutine<IN, OUT>
        implements LoaderRoutine<IN, OUT> {

    private final LoaderConfiguration mConfiguration;

    private final LoaderContext mContext;

    private final ContextInvocationFactory<IN, OUT> mFactory;

    private final int mLoaderId;

    private final OrderType mOrderType;

    /**
     * Constructor.
     *
     * @param context                 the routine context.
     * @param factory                 the invocation factory.
     * @param invocationConfiguration the invocation configuration.
     * @param loaderConfiguration     the loader configuration.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderRoutine(@Nonnull final LoaderContext context,
            @Nonnull final ContextInvocationFactory<IN, OUT> factory,
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nonnull final LoaderConfiguration loaderConfiguration) {

        super(invocationConfiguration);

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        if (factory == null) {

            throw new NullPointerException("the context invocation factory must not be null");
        }

        mContext = context;
        mFactory = factory;
        mConfiguration = loaderConfiguration;
        mLoaderId = loaderConfiguration.getLoaderIdOr(LoaderConfiguration.AUTO);
        mOrderType = invocationConfiguration.getOutputOrderTypeOr(null);
        getLogger().dbg("building context routine with configuration: %s", loaderConfiguration);
    }

    @Nonnull
    @Override
    public InvocationChannel<IN, OUT> asyncInvoke() {

        return new InvocationChannelDecorator<IN, OUT>(super.asyncInvoke());
    }

    @Nonnull
    @Override
    public InvocationChannel<IN, OUT> parallelInvoke() {

        return new InvocationChannelDecorator<IN, OUT>(super.parallelInvoke());
    }

    @Override
    public void purge() {

        super.purge();
        final LoaderContext context = mContext;

        if (context.getComponent() != null) {

            Runners.mainRunner()
                   .run(new PurgeExecution(context, mFactory, mLoaderId), 0, TimeUnit.MILLISECONDS);
        }
    }

    @Nonnull
    @Override
    protected Invocation<IN, OUT> convertInvocation(@Nonnull final Invocation<IN, OUT> invocation,
            @Nonnull final InvocationType type) {

        try {

            invocation.onDestroy();

        } catch (final Throwable t) {

            InvocationInterruptedException.ignoreIfPossible(t);
            getLogger().wrn(t, "ignoring exception while destroying invocation instance");
        }

        return newInvocation(type);
    }

    @Nonnull
    @Override
    protected Invocation<IN, OUT> newInvocation(@Nonnull final InvocationType type) {

        final Logger logger = getLogger();

        if (type == InvocationType.ASYNC) {

            return new LoaderInvocation<IN, OUT>(mContext, mFactory, mConfiguration, mOrderType,
                                                 logger);
        }

        final Context loaderContext = mContext.getLoaderContext();

        if (loaderContext == null) {

            throw new IllegalStateException("the routine context has been destroyed");
        }

        try {

            final ContextInvocationFactory<IN, OUT> factory = mFactory;
            logger.dbg("creating a new invocation instance");
            final ContextInvocation<IN, OUT> invocation = factory.newInvocation();
            invocation.onContext(loaderContext.getApplicationContext());
            return invocation;

        } catch (final Throwable t) {

            logger.err(t, "error creating the invocation instance");
            throw InvocationException.wrapIfNeeded(t);
        }
    }

    public void purge(@Nullable final IN input) {

        final LoaderContext context = mContext;

        if (context.getComponent() != null) {

            final List<IN> inputList = Collections.singletonList(input);
            final PurgeInputsExecution<IN> execution =
                    new PurgeInputsExecution<IN>(context, mFactory, mLoaderId, inputList);
            Runners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final IN... inputs) {

        final LoaderContext context = mContext;

        if (context.getComponent() != null) {

            final List<IN> inputList;

            if (inputs == null) {

                inputList = Collections.emptyList();

            } else {

                inputList = new ArrayList<IN>(inputs.length);
                Collections.addAll(inputList, inputs);
            }

            final PurgeInputsExecution<IN> execution =
                    new PurgeInputsExecution<IN>(context, mFactory, mLoaderId, inputList);
            Runners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    public void purge(@Nullable final Iterable<? extends IN> inputs) {

        final LoaderContext context = mContext;

        if (context.getComponent() != null) {

            final List<IN> inputList;

            if (inputs == null) {

                inputList = Collections.emptyList();

            } else {

                inputList = new ArrayList<IN>();

                for (final IN input : inputs) {

                    inputList.add(input);
                }
            }

            final PurgeInputsExecution<IN> execution =
                    new PurgeInputsExecution<IN>(context, mFactory, mLoaderId, inputList);
            Runners.mainRunner().run(execution, 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Decorator of invocation channels used to detect deadlocks.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class InvocationChannelDecorator<IN, OUT> implements InvocationChannel<IN, OUT> {

        private final InvocationChannel<IN, OUT> mChannel;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped channel.
         */
        private InvocationChannelDecorator(@Nonnull final InvocationChannel<IN, OUT> wrapped) {

            mChannel = wrapped;
        }

        public boolean abort() {

            return mChannel.abort();
        }

        @Nonnull
        public InvocationChannel<IN, OUT> after(@Nonnull final TimeDuration delay) {

            mChannel.after(delay);
            return this;
        }

        @Nonnull
        public InvocationChannel<IN, OUT> after(final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mChannel.after(delay, timeUnit);
            return this;
        }

        @Nonnull
        public InvocationChannel<IN, OUT> now() {

            mChannel.now();
            return this;
        }

        @Nonnull
        public InvocationChannel<IN, OUT> orderByCall() {

            mChannel.orderByCall();
            return this;
        }

        @Nonnull
        public InvocationChannel<IN, OUT> orderByChance() {

            mChannel.orderByChance();
            return this;
        }

        @Nonnull
        public InvocationChannel<IN, OUT> orderByDelay() {

            mChannel.orderByDelay();
            return this;
        }

        @Nonnull
        public InvocationChannel<IN, OUT> pass(
                @Nullable final OutputChannel<? extends IN> channel) {

            mChannel.pass(channel);
            return this;
        }

        @Nonnull
        public InvocationChannel<IN, OUT> pass(@Nullable final Iterable<? extends IN> inputs) {

            mChannel.pass(inputs);
            return this;
        }

        @Nonnull
        public InvocationChannel<IN, OUT> pass(@Nullable final IN input) {

            mChannel.pass(input);
            return this;
        }

        @Nonnull
        public InvocationChannel<IN, OUT> pass(@Nullable final IN... inputs) {

            mChannel.pass(inputs);
            return this;
        }

        @Nonnull
        public OutputChannel<OUT> result() {

            return new OutputChannelDecorator<OUT>(mChannel);
        }

        public boolean hasPendingInputs() {

            return mChannel.hasPendingInputs();
        }

        public boolean abort(@Nullable final Throwable reason) {

            return mChannel.abort(reason);
        }

        public boolean isOpen() {

            return mChannel.isOpen();
        }
    }

    /**
     * Decorator of invocation channels used to detect deadlocks.
     *
     * @param <OUT> the output data type.
     */
    private static class OutputChannelDecorator<OUT> implements OutputChannel<OUT> {

        private final InvocationChannel<?, OUT> mInvocationChannel;

        private final OutputChannel<OUT> mResultChannel;

        /**
         * Constructor.
         *
         * @param channel the invocation channel.
         */
        private OutputChannelDecorator(@Nonnull final InvocationChannel<?, OUT> channel) {

            mInvocationChannel = channel;
            mResultChannel = channel.result();
        }

        @Nonnull
        public OutputChannel<OUT> afterMax(@Nonnull final TimeDuration timeout) {

            if (!timeout.isZero() && mInvocationChannel.hasPendingInputs()) {

                throw new DeadlockException(
                        "cannot wait for outputs when inputs are still pending");
            }

            mResultChannel.afterMax(timeout);
            return this;
        }

        @Nonnull
        public OutputChannel<OUT> afterMax(final long timeout, @Nonnull final TimeUnit timeUnit) {

            if ((timeout > 0) && mInvocationChannel.hasPendingInputs()) {

                throw new DeadlockException(
                        "cannot wait for outputs when inputs are still pending");
            }

            mResultChannel.afterMax(timeout, timeUnit);
            return this;
        }

        @Nonnull
        public List<OUT> all() {

            return mResultChannel.all();
        }

        @Nonnull
        public OutputChannel<OUT> allInto(@Nonnull final Collection<? super OUT> results) {

            mResultChannel.allInto(results);
            return this;
        }

        public boolean checkComplete() {

            return mResultChannel.checkComplete();
        }

        @Nonnull
        public OutputChannel<OUT> eventually() {

            if (mInvocationChannel.hasPendingInputs()) {

                throw new DeadlockException(
                        "cannot wait for outputs when inputs are still pending");
            }

            mResultChannel.eventually();
            return this;
        }

        @Nonnull
        public OutputChannel<OUT> eventuallyAbort() {

            mResultChannel.eventuallyAbort();
            return this;
        }

        @Nonnull
        public OutputChannel<OUT> eventuallyExit() {

            mResultChannel.eventuallyExit();
            return this;
        }

        @Nonnull
        public OutputChannel<OUT> eventuallyThrow() {

            mResultChannel.eventuallyThrow();
            return this;
        }

        public boolean hasNext() {

            return mResultChannel.hasNext();
        }

        public OUT next() {

            return mResultChannel.next();
        }

        @Nonnull
        public OutputChannel<OUT> immediately() {

            mResultChannel.immediately();
            return this;
        }

        public boolean isBound() {

            return mResultChannel.isBound();
        }

        @Nonnull
        public <IN extends InputChannel<? super OUT>> IN passTo(@Nonnull final IN channel) {

            return mResultChannel.passTo(channel);
        }

        @Nonnull
        public OutputChannel<OUT> passTo(@Nonnull final OutputConsumer<? super OUT> consumer) {

            mResultChannel.passTo(consumer);
            return this;
        }

        public Iterator<OUT> iterator() {

            return mResultChannel.iterator();
        }

        public void remove() {

            mResultChannel.remove();
        }

        public boolean abort() {

            return mResultChannel.abort();
        }

        public boolean abort(@Nullable final Throwable reason) {

            return mResultChannel.abort(reason);
        }

        public boolean isOpen() {

            return mResultChannel.isOpen();
        }
    }

    /**
     * Execution implementation purging all loaders with a specific invocation factory.
     */
    private static class PurgeExecution extends TemplateExecution {

        private final LoaderContext mContext;

        private final ContextInvocationFactory<?, ?> mFactory;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param factory  the invocation factory.
         * @param loaderId the loader ID.
         */
        private PurgeExecution(@Nonnull final LoaderContext context,
                @Nonnull final ContextInvocationFactory<?, ?> factory, final int loaderId) {

            mContext = context;
            mFactory = factory;
            mLoaderId = loaderId;
        }

        public void run() {

            LoaderInvocation.purgeLoaders(mContext, mLoaderId, mFactory);
        }
    }

    /**
     * Execution implementation purging the loader with a specific invocation factory and inputs.
     *
     * @param <IN> the input data type.
     */
    private static class PurgeInputsExecution<IN> extends TemplateExecution {

        private final LoaderContext mContext;

        private final ContextInvocationFactory<?, ?> mFactory;

        private final List<IN> mInputs;

        private final int mLoaderId;

        /**
         * Constructor.
         *
         * @param context  the context instance.
         * @param factory  the invocation factory.
         * @param loaderId the loader ID.
         * @param inputs   the list of inputs.
         */
        private PurgeInputsExecution(@Nonnull final LoaderContext context,
                @Nonnull final ContextInvocationFactory<?, ?> factory, final int loaderId,
                @Nonnull final List<IN> inputs) {

            mContext = context;
            mFactory = factory;
            mLoaderId = loaderId;
            mInputs = inputs;
        }

        public void run() {

            LoaderInvocation.purgeLoader(mContext, mLoaderId, mFactory, mInputs);
        }
    }
}
