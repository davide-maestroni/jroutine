/*
 * Copyright 2016 Davide Maestroni
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

package com.github.dm.jrt.method;

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.InvocationConfigurable;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.object.InvocationTarget;
import com.github.dm.jrt.object.JRoutineObject;
import com.github.dm.jrt.object.builder.ObjectConfigurable;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.boxingClass;
import static com.github.dm.jrt.core.util.Reflection.cloneArgs;
import static com.github.dm.jrt.core.util.Reflection.findBestMatchingMethod;

/**
 * TODO
 * <p>
 * Created by davide-maestroni on 08/10/2016.
 */
public class RoutineMethod implements InvocationConfigurable<RoutineMethod> {

    private final Object[] mArgs;

    private final Constructor<? extends RoutineMethod> mConstructor;

    private final AtomicBoolean mIsFirstCall = new AtomicBoolean(true);

    private final ThreadLocal<InputChannel<?>> mLocalChannel = new ThreadLocal<InputChannel<?>>();

    private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

    /**
     * Constructor.
     */
    public RoutineMethod() {
        this((Object[]) null);
    }

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    public RoutineMethod(@Nullable final Object... args) {
        final Class<? extends RoutineMethod> type = getClass();
        if (type.isAnonymousClass() && !Reflection.hasStaticScope(type)) {
            final Object[] safeArgs = Reflection.asArgs(args);
            final Object[] syntheticArgs = (mArgs = new Object[safeArgs.length + 1]);
            System.arraycopy(safeArgs, 0, syntheticArgs, 0, safeArgs.length);
            syntheticArgs[safeArgs.length] = Reflection.NO_ARGS;

        } else {
            mArgs = Reflection.cloneArgs(args);
        }

        Constructor<? extends RoutineMethod> constructor = null;
        try {
            constructor = Reflection.findBestMatchingConstructor(type, mArgs);

        } catch (final IllegalArgumentException ignored) {
        }

        mConstructor = constructor;
    }

    /**
     * Builds an object routine method by wrapping one static method of the specified class.
     *
     * @param type           the class type.
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @return the routine method instance.
     * @throws java.lang.NoSuchMethodException if no method with the specified signature is found.
     */
    @NotNull
    public static ObjectRoutineMethod from(@NotNull final Class<?> type, @NotNull final String name,
            @Nullable final Class<?>... parameterTypes) throws NoSuchMethodException {
        return from(type.getMethod(name, parameterTypes));
    }

    /**
     * Builds an object routine method by wrapping the specified static method.
     *
     * @param method the method.
     * @return the routine method instance.
     */
    @NotNull
    public static ObjectRoutineMethod from(@NotNull final Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("the method is not static: " + method);
        }

        return new ObjectRoutineMethod(InvocationTarget.classOfType(method.getDeclaringClass()),
                method);
    }

    /**
     * Builds an object routine method by wrapping one method of the specified object.
     *
     * @param target the target object.
     * @param method the method.
     * @return the routine method instance.
     */
    @NotNull
    public static ObjectRoutineMethod from(@NotNull final Object target,
            @NotNull final Method method) {
        if (!method.getDeclaringClass().isInstance(target)) {
            throw new IllegalArgumentException(
                    "the method is not applicable to the specified object: " + method);
        }

        return new ObjectRoutineMethod(InvocationTarget.instance(target), method);
    }

    /**
     * Builds an object routine method by wrapping one method of the specified object.
     *
     * @param target         the target object.
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @return the routine method instance.
     * @throws java.lang.NoSuchMethodException if no method with the specified signature is found.
     */
    @NotNull
    public static ObjectRoutineMethod from(@NotNull final Object target, @NotNull final String name,
            @Nullable final Class<?>... parameterTypes) throws NoSuchMethodException {
        return from(target, target.getClass().getMethod(name, parameterTypes));
    }

    /**
     * Builds a new input channel.
     *
     * @param <IN> the input data type.
     * @return the input channel instance.
     */
    @NotNull
    public static <IN> InputChannel<IN> inputChannel() {
        return inputFrom(JRoutineCore.io().<IN>buildChannel());
    }

    /**
     * Builds a new input channel wrapping the specified one.
     *
     * @param channel the channel to wrap.
     * @param <IN>    the input data type.
     * @return the input channel instance.
     */
    @NotNull
    public static <IN> InputChannel<IN> inputFrom(@NotNull final Channel<IN, IN> channel) {
        return new InputChannel<IN>(channel);
    }

    /**
     * Builds a new input channel producing no data.
     * <p>
     * Note that the channel will be already closed.
     *
     * @param <IN> the input data type.
     * @return the input channel instance.
     */
    @NotNull
    public static <IN> InputChannel<IN> inputOf() {
        return inputFrom(JRoutineCore.io().<IN>of());
    }

    /**
     * Builds a new input channel producing the specified input.
     * <p>
     * Note that the channel will be already closed.
     *
     * @param input the input.
     * @param <IN>  the input data type.
     * @return the input channel instance.
     */
    @NotNull
    public static <IN> InputChannel<IN> inputOf(@Nullable final IN input) {
        return inputFrom(JRoutineCore.io().of(input));
    }

    /**
     * Builds a new input channel producing the specified inputs.
     * <p>
     * Note that the channel will be already closed.
     *
     * @param inputs the inputs.
     * @param <IN>   the input data type.
     * @return the input channel instance.
     */
    @NotNull
    public static <IN> InputChannel<IN> inputOf(@Nullable final IN... inputs) {
        return inputFrom(JRoutineCore.io().of(inputs));
    }

    /**
     * Builds a new input channel producing the inputs returned by the specified iterable.
     * <p>
     * Note that the channel will be already closed.
     *
     * @param inputs the iterable returning the input data.
     * @param <IN>   the input data type.
     * @return the input channel instance.
     */
    @NotNull
    public static <IN> InputChannel<IN> inputOf(@Nullable final Iterable<IN> inputs) {
        return inputFrom(JRoutineCore.io().of(inputs));
    }

    /**
     * Builds a new output channel.
     *
     * @param <OUT> the output data type.
     * @return the output channel instance.
     */
    @NotNull
    public static <OUT> OutputChannel<OUT> outputChannel() {
        return outputFrom(JRoutineCore.io().<OUT>buildChannel());
    }

    /**
     * Builds a new output channel wrapping the specified one.
     *
     * @param channel the channel to wrap.
     * @param <OUT>   the output data type.
     * @return the output channel instance.
     */
    @NotNull
    public static <OUT> OutputChannel<OUT> outputFrom(@NotNull final Channel<OUT, OUT> channel) {
        return new OutputChannel<OUT>(channel);
    }

    @NotNull
    public RoutineMethod apply(@NotNull final InvocationConfiguration configuration) {
        mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
        return this;
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends RoutineMethod> applyInvocationConfiguration() {
        return new Builder<RoutineMethod>(this, mConfiguration);
    }

    /**
     * Calls the routine.
     * <br>
     * The output channel will produced the data returned by the method. In case the method does not
     * return any output, the channel will be anyway notified of invocation abortion and completion.
     * <p>
     * Note that the specific method will be selected based on the specified parameters. If no
     * matching method is found, the call will fail with an exception.
     * <br>
     * Note also that, in case no proper arguments are passed to the constructor, it will be
     * possible to invoke this method only once.
     *
     * @param params the parameters.
     * @param <OUT>  the output data type.
     * @return the output channel instance.
     */
    @NotNull
    public <OUT> OutputChannel<OUT> call(@Nullable final Object... params) {
        final Object[] safeParams = asArgs(params);
        final Method method = findBestMatchingMethod(getClass(), safeParams);
        final InvocationFactory<Selectable<Object>, Selectable<Object>> factory;
        final Constructor<? extends RoutineMethod> constructor = mConstructor;
        if (constructor != null) {
            factory = new MultiInvocationFactory(constructor, method, safeParams);

        } else {
            if (!mIsFirstCall.getAndSet(false)) {
                throw new IllegalStateException(
                        "cannot invoke the routine in more than once: please provide proper "
                                + "constructor arguments");
            }

            factory = new SingleInvocationFactory(method, safeParams);
        }

        return call(factory, InvocationMode.ASYNC, safeParams);
    }

    /**
     * Calls the routine in parallel mode.
     * <br>
     * The output channel will produced the data returned by the method. In case the method does not
     * return any output, the channel will be anyway notified of invocation abortion and completion.
     * <p>
     * Note that the specific method will be selected based on the specified parameters. If no
     * matching method is found, the call will fail with an exception.
     * <br>
     * Note also that, in case no proper arguments are passed to the constructor, it will be
     * possible to invoke this method only once.
     *
     * @param params the parameters.
     * @param <OUT>  the output data type.
     * @return the output channel instance.
     * @see com.github.dm.jrt.core.routine.Routine Routine
     */
    @NotNull
    public <OUT> OutputChannel<OUT> callParallel(@Nullable final Object... params) {
        final Constructor<? extends RoutineMethod> constructor = mConstructor;
        if (constructor == null) {
            throw new IllegalStateException(
                    "cannot invoke the routine in parallel mode: please provide proper "
                            + "constructor arguments");
        }

        final Object[] safeParams = asArgs(params);
        final Method method = findBestMatchingMethod(getClass(), safeParams);
        return call(new MultiInvocationFactory(constructor, method, safeParams),
                InvocationMode.PARALLEL, safeParams);
    }

    /**
     * Returns the invocation configuration.
     *
     * @return the invocation configuration.
     */
    @NotNull
    protected InvocationConfiguration getConfiguration() {
        return mConfiguration;
    }

    /**
     * Returns the input channel which is ready to produce data. If the method has no input channel
     * as parameter, null will be returned.
     *
     * @param <IN> the input data type.
     * @return the input channel producing data or null.
     */
    @SuppressWarnings("unchecked")
    protected <IN> InputChannel<IN> switchInput() {
        return (InputChannel<IN>) mLocalChannel.get();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <OUT> OutputChannel<OUT> call(
            @NotNull final InvocationFactory<Selectable<Object>, Selectable<Object>> factory,
            @NotNull final InvocationMode mode, @NotNull final Object[] params) {
        final ArrayList<InputChannel<?>> inputChannels = new ArrayList<InputChannel<?>>();
        final ArrayList<OutputChannel<?>> outputChannels = new ArrayList<OutputChannel<?>>();
        for (final Object param : params) {
            if (param instanceof InputChannel) {
                inputChannels.add((InputChannel<?>) param);

            } else if (param instanceof OutputChannel) {
                outputChannels.add((OutputChannel<?>) param);
            }
        }

        final OutputChannel<OUT> resultChannel = outputChannel();
        outputChannels.add(resultChannel);
        final Channel<?, ? extends Selectable<Object>> inputChannel =
                (!inputChannels.isEmpty()) ? Channels.merge(inputChannels).buildChannels()
                        : JRoutineCore.io().<Selectable<Object>>of();
        final Channel<Selectable<Object>, Selectable<Object>> outputChannel =
                mode.invoke(JRoutineCore.with(factory).apply(getConfiguration()))
                    .pass(inputChannel)
                    .close();
        final Map<Integer, Channel<?, Object>> channelMap =
                Channels.selectOutput(0, outputChannels.size(), outputChannel).buildChannels();
        for (final Entry<Integer, Channel<?, Object>> entry : channelMap.entrySet()) {
            entry.getValue()
                 .bind((OutputChannel<Object>) outputChannels.get(entry.getKey()))
                 .close();
        }

        return resultChannel;
    }

    /**
     * Implementation of routine method wrapping an object method.
     */
    public static class ObjectRoutineMethod extends RoutineMethod
            implements ObjectConfigurable<ObjectRoutineMethod> {

        private final Method mMethod;

        private final InvocationTarget<?> mTarget;

        private ObjectConfiguration mConfiguration = ObjectConfiguration.defaultConfiguration();

        /**
         * Constructor.
         *
         * @param target the invocation target.
         * @param method the method instance.
         */
        private ObjectRoutineMethod(@NotNull final InvocationTarget<?> target,
                @NotNull final Method method) {
            mTarget = target;
            mMethod = ConstantConditions.notNull("target method", method);
        }

        @NotNull
        public ObjectRoutineMethod apply(@NotNull final ObjectConfiguration configuration) {
            mConfiguration = ConstantConditions.notNull("object configuration", configuration);
            return this;
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        public Builder<? extends ObjectRoutineMethod> applyInvocationConfiguration() {
            return (Builder<? extends ObjectRoutineMethod>) super.applyInvocationConfiguration();
        }

        @NotNull
        @Override
        public <OUT> OutputChannel<OUT> call(@Nullable final Object... params) {
            return call(InvocationMode.ASYNC, params);
        }

        @NotNull
        @Override
        public <OUT> OutputChannel<OUT> callParallel(@Nullable final Object... params) {
            return call(InvocationMode.PARALLEL, params);
        }

        @NotNull
        public ObjectConfiguration.Builder<? extends ObjectRoutineMethod>
        applyObjectConfiguration() {
            return new ObjectConfiguration.Builder<ObjectRoutineMethod>(this, mConfiguration);
        }

        @NotNull
        @SuppressWarnings("unchecked")
        private <OUT> OutputChannel<OUT> call(@NotNull final InvocationMode mode,
                @Nullable final Object... params) {
            final Object[] safeParams = asArgs(params);
            final Method method = mMethod;
            if (method.getParameterTypes().length != safeParams.length) {
                throw new IllegalArgumentException("wrong number of parameters: expected <" +
                        method.getParameterTypes().length + "> but was <" + safeParams.length
                        + ">");
            }

            final Routine<Object, Object> routine = JRoutineObject.with(mTarget)
                                                                  .apply(getConfiguration())
                                                                  .apply(mConfiguration)
                                                                  .method(method);
            final Channel<Object, Object> channel = mode.invoke(routine).sortedByCall();
            for (final Object param : safeParams) {
                if (param instanceof InputChannel) {
                    channel.pass((InputChannel<?>) param);

                } else if (!(param instanceof OutputChannel)) {
                    channel.pass(param);
                }
            }

            return (OutputChannel<OUT>) outputFrom(channel.close());
        }
    }

    /**
     * Base invocation implementation.
     */
    private abstract class AbstractInvocation
            extends TemplateInvocation<Selectable<Object>, Selectable<Object>> {

        private final boolean mReturnResults;

        private boolean mIsAborted;

        private boolean mIsBound;

        private boolean mIsComplete;

        /**
         * Constructor.
         *
         * @param method the method instance.
         */
        private AbstractInvocation(@NotNull final Method method) {
            mReturnResults = (boxingClass(method.getReturnType()) != Void.class);
        }

        @Override
        public void onAbort(@NotNull final RoutineException reason) throws Exception {
            mIsAborted = true;
            final List<InputChannel<?>> inputChannels = getInputChannels();
            for (final InputChannel<?> inputChannel : inputChannels) {
                inputChannel.abort(reason);
            }

            final ThreadLocal<InputChannel<?>> localChannel = mLocalChannel;
            localChannel.set((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
            try {
                if (!mIsComplete) {
                    internalInvoke();
                }

            } finally {
                localChannel.set(null);
                for (final OutputChannel<?> outputChannel : getOutputChannels()) {
                    outputChannel.abort(reason);
                }
            }
        }

        @Override
        public void onComplete(@NotNull final Channel<Selectable<Object>, ?> result) throws
                Exception {
            bind(result);
            mIsComplete = true;
            if (!mIsAborted) {
                final List<InputChannel<?>> inputChannels = getInputChannels();
                for (final InputChannel<?> inputChannel : inputChannels) {
                    inputChannel.close();
                }

                final ThreadLocal<InputChannel<?>> localChannel = mLocalChannel;
                localChannel.set((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
                final List<OutputChannel<?>> outputChannels = getOutputChannels();
                try {
                    final Object methodResult = internalInvoke();
                    if (mReturnResults) {
                        result.pass(new Selectable<Object>(methodResult, outputChannels.size()));
                    }

                } finally {
                    localChannel.set(null);
                }

                for (final OutputChannel<?> outputChannel : outputChannels) {
                    outputChannel.close();
                }
            }
        }

        @Override
        public void onInput(final Selectable<Object> input,
                @NotNull final Channel<Selectable<Object>, ?> result) throws Exception {
            bind(result);
            @SuppressWarnings("unchecked") final InputChannel<Object> inputChannel =
                    (InputChannel<Object>) getInputChannels().get(input.index);
            inputChannel.pass(input.data);
            final ThreadLocal<InputChannel<?>> localChannel = mLocalChannel;
            localChannel.set(inputChannel);
            try {
                final Object methodResult = internalInvoke();
                if (mReturnResults) {
                    result.pass(new Selectable<Object>(methodResult, getOutputChannels().size()));
                }

            } finally {
                localChannel.set(null);
            }
        }

        /**
         * Returns the list of input channels representing the input of the method.
         *
         * @return the list of input channels.
         */
        @NotNull
        protected abstract List<InputChannel<?>> getInputChannels();

        /**
         * Returns the list of output channels representing the output of the method.
         *
         * @return the list of output channels.
         */
        @NotNull
        protected abstract List<OutputChannel<?>> getOutputChannels();

        /**
         * Invokes the method.
         *
         * @return the method result.
         * @throws java.lang.Exception if an error occurred during the invocation.
         */
        protected abstract Object invokeMethod() throws Exception;

        private void bind(@NotNull final Channel<Selectable<Object>, ?> result) {
            if (!mIsBound) {
                mIsBound = true;
                final List<OutputChannel<?>> outputChannels = getOutputChannels();
                if (!outputChannels.isEmpty()) {
                    result.pass(Channels.merge(outputChannels).buildChannels());
                }
            }
        }

        private Object internalInvoke() throws Exception {
            try {
                return invokeMethod();

            } catch (final InvocationTargetException e) {
                throw InvocationException.wrapIfNeeded(e.getTargetException());
            }
        }

        @Override
        public void onRestart() throws Exception {
            mIsBound = false;
            mIsAborted = false;
            mIsComplete = false;
        }
    }

    /**
     * Invocation implementation supporting multiple invocation of the routine method.
     */
    private class MultiInvocation extends AbstractInvocation {

        private final Constructor<?> mConstructor;

        private final Object[] mConstructorArgs;

        private final ArrayList<InputChannel<?>> mInputChannels = new ArrayList<InputChannel<?>>();

        private final Method mMethod;

        private final Object[] mOrigParams;

        private final ArrayList<OutputChannel<?>> mOutputChannels =
                new ArrayList<OutputChannel<?>>();

        private Object mInstance;

        private Object[] mParams;

        /**
         * Constructor.
         *
         * @param constructor the routine method constructor.
         * @param args        the constructor arguments.
         * @param method      the method instance.
         * @param params      the method parameters.
         */
        public MultiInvocation(@NotNull final Constructor<?> constructor,
                @NotNull final Object[] args, @NotNull final Method method,
                @NotNull final Object... params) {
            super(method);
            mConstructor = constructor;
            mConstructorArgs = args;
            mMethod = method;
            mOrigParams = params;
        }

        @NotNull
        @Override
        protected List<InputChannel<?>> getInputChannels() {
            return mInputChannels;
        }

        @Override
        public void onRestart() throws Exception {
            super.onRestart();
            mInstance = mConstructor.newInstance(mConstructorArgs);
            final ArrayList<Object> parameters = new ArrayList<Object>();
            final ArrayList<InputChannel<?>> inputChannels = mInputChannels;
            final ArrayList<OutputChannel<?>> outputChannels = mOutputChannels;
            for (final Object param : mOrigParams) {
                if (param instanceof InputChannel) {
                    final InputChannel<Object> inputChannel =
                            inputFrom(JRoutineCore.io().buildChannel());
                    inputChannels.add(inputChannel);
                    parameters.add(inputChannel);

                } else if (param instanceof OutputChannel) {
                    final OutputChannel<Object> outputChannel =
                            outputFrom(JRoutineCore.io().buildChannel());
                    outputChannels.add(outputChannel);
                    parameters.add(outputChannel);

                } else {
                    parameters.add(param);
                }
            }

            mParams = parameters.toArray();
        }

        @Override
        public void onRecycle(final boolean isReused) throws Exception {
            super.onRecycle(isReused);
            mInputChannels.clear();
            mOutputChannels.clear();
        }

        @NotNull
        @Override
        protected List<OutputChannel<?>> getOutputChannels() {
            return mOutputChannels;
        }

        @Override
        protected Object invokeMethod() throws InvocationTargetException, IllegalAccessException {
            return mMethod.invoke(mInstance, mParams);
        }
    }

    /**
     * Invocation factory supporting multiple invocation of the routine method.
     */
    private class MultiInvocationFactory
            extends InvocationFactory<Selectable<Object>, Selectable<Object>> {

        private final Constructor<? extends RoutineMethod> mConstructor;

        private final Method mMethod;

        private final Object[] mParams;

        /**
         * Constructor.
         *
         * @param constructor the routine method constructor.
         * @param method      the method instance.
         * @param params      the method parameters.
         */
        private MultiInvocationFactory(
                @NotNull final Constructor<? extends RoutineMethod> constructor,
                @NotNull final Method method, @NotNull final Object... params) {
            super(asArgs(RoutineMethod.this.getClass(), mArgs, method, cloneArgs(params)));
            mConstructor = constructor;
            mMethod = method;
            mParams = cloneArgs(params);
        }

        @NotNull
        @Override
        public Invocation<Selectable<Object>, Selectable<Object>> newInvocation() throws
                IllegalAccessException, InvocationTargetException, InstantiationException {
            return new MultiInvocation(mConstructor, mArgs, mMethod, mParams);
        }
    }

    /**
     * Invocation implementation supporting single invocation of the routine method.
     */
    private class SingleInvocation extends AbstractInvocation {

        private final ArrayList<InputChannel<?>> mInputChannels;

        private final Object mInstance;

        private final Method mMethod;

        private final ArrayList<OutputChannel<?>> mOutputChannels;

        private final Object[] mParams;

        /**
         * Constructor.
         *
         * @param inputChannels  the list of input channels.
         * @param outputChannels the list of output channels.
         * @param instance       the target instance.
         * @param method         the method instance.
         * @param params         the method parameters.
         */
        private SingleInvocation(@NotNull final ArrayList<InputChannel<?>> inputChannels,
                @NotNull final ArrayList<OutputChannel<?>> outputChannels,
                @NotNull final Object instance, @NotNull final Method method,
                @NotNull final Object... params) {
            super(method);
            mInputChannels = inputChannels;
            mOutputChannels = outputChannels;
            mInstance = instance;
            mMethod = method;
            mParams = params;
        }

        @NotNull
        @Override
        protected List<InputChannel<?>> getInputChannels() {
            return mInputChannels;
        }

        @NotNull
        @Override
        protected List<OutputChannel<?>> getOutputChannels() {
            return mOutputChannels;
        }

        @Override
        protected Object invokeMethod() throws InvocationTargetException, IllegalAccessException {
            return mMethod.invoke(mInstance, mParams);
        }
    }

    /**
     * Invocation factory supporting single invocation of the routine method.
     */
    private class SingleInvocationFactory
            extends InvocationFactory<Selectable<Object>, Selectable<Object>> {

        private final ArrayList<InputChannel<?>> mInputChannels;

        private final Method mMethod;

        private final ArrayList<OutputChannel<?>> mOutputChannels;

        private final Object[] mParams;

        /**
         * Constructor.
         *
         * @param method the method instance.
         * @param params the method parameters.
         */
        private SingleInvocationFactory(@NotNull final Method method,
                @NotNull final Object... params) {
            super(asArgs(RoutineMethod.this.getClass(), method, cloneArgs(params)));
            mMethod = method;
            final ArrayList<Object> parameters = new ArrayList<Object>();
            final ArrayList<InputChannel<?>> inputChannels = new ArrayList<InputChannel<?>>();
            final ArrayList<OutputChannel<?>> outputChannels = new ArrayList<OutputChannel<?>>();
            for (final Object param : params) {
                if (param instanceof InputChannel) {
                    final InputChannel<Object> inputChannel =
                            inputFrom(JRoutineCore.io().buildChannel());
                    inputChannels.add(inputChannel);
                    parameters.add(inputChannel);

                } else if (param instanceof OutputChannel) {
                    final OutputChannel<Object> outputChannel =
                            outputFrom(JRoutineCore.io().buildChannel());
                    outputChannels.add(outputChannel);
                    parameters.add(outputChannel);

                } else {
                    parameters.add(param);
                }
            }

            mParams = parameters.toArray();
            mInputChannels = inputChannels;
            mOutputChannels = outputChannels;
        }

        @NotNull
        @Override
        public Invocation<Selectable<Object>, Selectable<Object>> newInvocation() {
            return new SingleInvocation(mInputChannels, mOutputChannels, RoutineMethod.this,
                    mMethod, mParams);
        }
    }
}
