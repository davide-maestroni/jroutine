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
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.Builder;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.method.annotation.Input;
import com.github.dm.jrt.method.annotation.Output;
import com.github.dm.jrt.object.InvocationTarget;
import com.github.dm.jrt.object.JRoutineObject;
import com.github.dm.jrt.object.config.ObjectConfigurable;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
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
import static com.github.dm.jrt.core.util.Reflection.boxingDefault;
import static com.github.dm.jrt.core.util.Reflection.cloneArgs;
import static com.github.dm.jrt.core.util.Reflection.findBestMatchingMethod;

/**
 * This class provides an easy way to implement a routine which can be combined in complex ways
 * with other ones.
 * <h2>How to implement a routine</h2>
 * A routine is implemented by extending the class and defining a method taking input and output
 * channels as parameters. The number of input and output channels can be arbitrarily chosen,
 * moreover, any other type of parameters can be passed in any order.
 * <br>
 * The method will be called each time a new input is passed to one of the input channels.
 * Additionally, the method is called once when the invocation is aborted and when it completes.
 * In the former case every input channel will behave as an aborted one (see {@link Channel}),
 * while, in the latter, no data will be available, so, it is always advisable to verify that an
 * input is available before reading it.
 * <br>
 * The method parameters used as input channels are identified by the
 * {@link com.github.dm.jrt.method.annotation.Input} annotation, while the output ones by the
 * {@link com.github.dm.jrt.method.annotation.Output} annotation. Channel parameters without any
 * specific annotation will be passed as is to the target method.
 * <p>
 * For example, a routine computing the square of integers can be implemented as follows:
 * <pre>
 *     <code>
 *
 *         final Channel&lt;Integer, Integer&gt; inputChannel =
 *                 JRoutineCore.&lt;Integer&gt;ofInputs().buildChannel();
 *         final Channel&lt;Integer, Integer&gt; outputChannel =
 *                 JRoutineCore.&lt;Integer&gt;ofInputs().buildChannel();
 *         new RoutineMethod() {
 *
 *             public void square(&#64;Input final Channel&lt;?, Integer&gt; input,
 *                     &#64;Output final Channel&lt;Integer, ?&gt; output) {
 *                 if (input.hasNext()) {
 *                     final int i = input.next();
 *                     output.pass(i * i);
 *                 }
 *             }
 *         }.call(inputChannel, outputChannel);
 *         inputChannel.pass(1, 2, 3).close();
 *         outputChannel.inMax(seconds(1)).all(); // expected values: 1, 4, 9
 *     </code>
 * </pre>
 * The {@code call()} method returns an output channel producing the outputs returned by the target
 * method. In the above case no output is returned (in fact, the return type is {@code void}), still
 * the channel will be notified of the invocation abortion and completion.
 * <p>
 * Several methods can be defined, though, be aware that the number and type of parameters are
 * employed to identify the method to call. Any clash in the method signatures will raise an
 * exception.
 * <h2>Channels vs static parameters</h2>
 * When parameters other than {@code Channel}s are passed to the {@code call()} method, the very
 * same values are employed each time a new input is available.
 * <p>
 * For example, a routine transforming the case of a string can be implemented as follows:
 * <pre>
 *     <code>
 *
 *         final Channel&lt;String, String&gt; inputChannel =
 *                 JRoutineCore.&lt;String&gt;ofInputs().buildChannel();
 *         final Channel&lt;?, String&gt; outputChannel = new RoutineMethod() {
 *
 *             public String switchCase(&#64;Input final Channel&lt;?, String&gt; input,
 *                     final boolean isUpper) {
 *                 if (input.hasNext()) {
 *                     final String str = input.next();
 *                     return (isUpper) ? str.toUpperCase() : str.toLowerCase();
 *                 }
 *                 return ignoreReturnValue();
 *             }
 *         }.call(inputChannel, true);
 *         inputChannel.pass("Hello", "JRoutine", "!").close();
 *         outputChannel.inMax(seconds(1)).all(); // expected values: "HELLO", "JROUTINE", "!"
 *     </code>
 * </pre>
 * Note that outputs will be collected through the channel returned by the {@code call()} method.
 * <br>
 * Note also that, when the invocation completes, that is, no input is available, the method
 * {@code ignoreReturnValue()} is called to avoid pushing any output to the result channel. The same
 * method can be called any time no output is produced.
 * <p>
 * In case the very same input or output channel instance has to be passed as parameter, it is
 * sufficient to avoid in/out annotations, like shown below:
 * <pre>
 *     <code>
 *
 *         final Channel&lt;String, String&gt; outputChannel =
 *                 JRoutineCore.&lt;String&gt;ofInputs().buildChannel();
 *         new MyRoutine() {
 *
 *             void run(final Channel&lt;String, ?&gt; output) {
 *                 output.pass("...");
 *             }
 *
 *         }.call(outputChannel);
 *     </code>
 * </pre>
 * <h2>Parallel and multiple invocations</h2>
 * In order to enable parallel invocation of the routine, it is necessary to provide the proper
 * parameters to the routine method non-default constructor. In fact, parallel invocations will
 * employ several instances of the implementing class.
 * <br>
 * Note that, for anonymous and inner classes, synthetic constructors will be created by the
 * compiler, hence the enclosing class, along with any variable captured inside the method, must be
 * explicitly passed to the constructor.
 * <br>
 * Like, for example:
 * <pre>
 *     <code>
 *
 *         final Locale locale = Locale.getDefault();
 *         final Channel&lt;String, String&gt; inputChannel =
 *                 JRoutineCore.&lt;String&gt;ofInputs().buildChannel();
 *         final Channel&lt;?, String&gt; outputChannel = new RoutineMethod(this, locale) {
 *
 *             public String switchCase(&#64;Input final Channel&lt;?, String&gt; input,
 *                     final boolean isUpper) {
 *                 if (input.hasNext()) {
 *                     final String str = input.next();
 *                     return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
 *                 }
 *                 return ignoreReturnValue();
 *             }
 *         }.callParallel(inputChannel, true);
 *         inputChannel.pass("Hello", "JRoutine", "!").close();
 *         outputChannel.inMax(seconds(1)).all(); // expected values: "HELLO", "JROUTINE", "!"
 *     </code>
 * </pre>
 * Or, for an inner class:
 * <pre>
 *     <code>
 *
 *         class MyMethod extends RoutineMethod {
 *
 *             private final Locale mLocale;
 *
 *             MyMethod(final Locale locale) {
 *                 super(OuterClass.this, locale);
 *                 mLocale = locale;
 *             }
 *
 *             String switchCase(&#64;Input final Channel&lt;?, String&gt; input,
 *                    final boolean isUpper) {
 *                 if (input.hasNext()) {
 *                     final String str = input.next();
 *                     return (isUpper) ? str.toUpperCase(mLocale) : str.toLowerCase(mLocale);
 *                 }
 *                 return ignoreReturnValue();
 *             }
 *         }
 *     </code>
 * </pre>
 * The same holds true for static classes, with the only difference that just the declared
 * parameters must be passed:
 * <pre>
 *     <code>
 *
 *         static class MyMethod extends RoutineMethod {
 *
 *             private final Locale mLocale;
 *
 *             MyMethod(final Locale locale) {
 *                 super(locale);
 *                 mLocale = locale;
 *             }
 *
 *             String switchCase(&#64;Input final Channel&lt;?, String&gt; input,
 *                    final boolean isUpper) {
 *                 if (input.hasNext()) {
 *                     final String str = input.next();
 *                     return (isUpper) ? str.toUpperCase(mLocale) : str.toLowerCase(mLocale);
 *                 }
 *                 return ignoreReturnValue();
 *             }
 *         }
 *     </code>
 * </pre>
 * The above conditions must be met also to be able to invoke the same routine several times through
 * the {@code call()} method.
 * <h2>Multiple inputs</h2>
 * The routine method implementation allows for multiple input channels to deliver data to the
 * method invocation. In such case it is possible to easily identify the channel for which an input
 * is ready by calling the {@code switchInput()} protected method. The method will return one of the
 * input channels passed as parameters (since the very same instance is returned == comparison is
 * allowed) or null, if the invocation takes no input channel.
 * <p>
 * For example, a routine printing the inputs of different types can be implemented as follows:
 * <pre>
 *     <code>
 *
 *         final Channel&lt;Integer, Integer&gt; inputInts =
 *                 JRoutineCore.&lt;Integer&gt;ofInputs().buildChannel();
 *         final Channel&lt;String, String&gt; inputStrings =
 *                 JRoutineCore.&lt;String&gt;ofInputs().buildChannel();
 *         final Channel&lt;String, String&gt; outputChannel =
 *                 JRoutineCore.&lt;String&gt;ofInputs().buildChannel();
 *         new RoutineMethod() {
 *
 *             void run(&#64;Input final Channel&lt;?, Integer&gt; inputInts,
 *                     &#64;Input final Channel&lt;?, String&gt; inputStrings,
 *                     &#64;Output final Channel&lt;String, ?&gt; output) {
 *                 final Channel&lt;?, ?&gt; inputChannel = switchInput();
 *                 if (inputChannel.hasNext()) {
 *                     if (inputChannel == inputInts) {
 *                         output.pass("Number: " + inputChannel.next());
 *                     } else if (inputChannel == inputStrings) {
 *                         output.pass("String: " + inputChannel.next());
 *                     }
 *                 }
 *             }
 *         }.call(inputInts, inputStrings, outputChannel);
 *         inputStrings.pass("Hello", "JRoutine", "!");
 *         inputInts.pass(1, 2, 3);
 *         outputChannel.bind(new TemplateChannelConsumer&lt;String&gt;() {
 *
 *             &#64;Override
 *             public void onOutput(final String out) {
 *                 System.out.println(out);
 *             }
 *         });
 *     </code>
 * </pre>
 * <h2>Routine concatenation</h2>
 * The output channels passed as parameters, or the one returned by the method, can be successfully
 * bound to other channels in order to concatenate several routine invocations.
 * <br>
 * The same is true for other method routines, when output channels can be passed as inputs as shown
 * in the following example:
 * <pre>
 *     <code>
 *
 *         final Channel&lt;Integer, Integer&gt; inputChannel =
 *                 JRoutineCore.&lt;Integer&gt;ofInputs().buildChannel();
 *         final Channel&lt;Integer, Integer&gt; outputChannel =
 *                 JRoutineCore.&lt;Integer&gt;ofInputs().buildChannel();
 *         new RoutineMethod() {
 *
 *             public void square(&#64;Input final Channel&lt;?, Integer&gt; input,
 *                     &#64;Output final Channel&lt;Integer, ?&gt; output) {
 *                 if (input.hasNext()) {
 *                     final int i = input.next();
 *                     output.pass(i * i);
 *                 }
 *             }
 *         }.call(inputChannel, outputChannel);
 *         final Channel&lt;Integer, Integer&gt; resultChannel =
 *                 JRoutineCore.&lt;Integer&gt;ofInputs().buildChannel();
 *         new RoutineMethod() {
 *
 *             private int mSum;
 *
 *             public void sum(&#64;Input final Channel&lt;?, Integer&gt; input,
 *                     &#64;Output final Channel&lt;Integer, ?&gt; output) {
 *                 if (input.hasNext()) {
 *                     mSum += input.next();
 *                 } else {
 *                     output.pass(mSum);
 *                 }
 *             }
 *         }.call(outputChannel, resultChannel);
 *         inputChannel.pass(1, 2, 3, 4).close();
 *         resultChannel.inMax(seconds(1)).next(); // expected value: 30
 *     </code>
 * </pre>
 * Note how the input channel is closed before reading the output, since the sum routine relies
 * on the completion notification before producing any result.
 * <br>
 * It is also possible to feed the routine method with outputs coming from another routine. In case,
 * for instance, a routine parsing strings into integers has already been implemented, it can be
 * concatenated to the sum routine method as shown below:
 * <pre>
 *     <code>
 *
 *         final Channel&lt;String, Integer&gt; channel = parseRoutine.call();
 *         final Channel&lt;Integer, Integer&gt; outputChannel =
 *                 JRoutineCore.&lt;Integer&gt;ofInputs().buildChannel();
 *         new RoutineMethod() {
 *
 *             private int mSum;
 *
 *             public void sum(&#64;Input final Channel&lt;?, Integer&gt; input,
 *                     &#64;Output final Channel&lt;Integer, ?&gt; output) {
 *                 if (input.hasNext()) {
 *                     mSum += input.next();
 *                 } else {
 *                     output.pass(mSum);
 *                 }
 *             }
 *         }.call(channel, outputChannel);
 *         channel.pass("1", "2", "3", "4").close();
 *         outputChannel.inMax(seconds(1)).next(); // expected value: 10
 *     </code>
 * </pre>
 * <h2>Handling of abortion exception</h2>
 * The routine method is notified also when one of the input or output channels is aborted.
 * <br>
 * When that happens, channel methods used to read or write data will throw a
 * {@code RoutineException} with the abortion reason as cause. Hence, it is possible to properly
 * react to such event by catching the exception and inspecting it.
 * <br>
 * Note however that, as soon as the invocation is aborted, no input can be read and no output can
 * be passed to any of the channels.
 * <p>
 * The following example shows how to implement a routine cleaning up external resources when the
 * invocation completes or is aborted:
 * <pre>
 *     <code>
 *
 *         final ExternalStorage storage = ExternalStorage.create();
 *         final Channel&lt;String, String&gt; inputChannel =
 *                 JRoutineCore.&lt;String&gt;ofInputs().buildChannel();
 *         new RoutineMethod(this, storage) {
 *
 *             private final StorageConnection mConnection = storage.openConnection();
 *
 *             void store(&#64;Input final Channel&lt;?, String&gt; input) {
 *                 try {
 *                     if (input.hasNext()) {
 *                         mConnection.put(input.next());
 *                     } else {
 *                         mConnection.close();
 *                     }
 *                 } catch(final RoutineException e) {
 *                     final Throwable t = e.getCause();
 *                     if (t instanceof FlushConnectionException) {
 *                         mConnection.close();
 *                     } else {
 *                         mConnection.rollback();
 *                     }
 *                 }
 *             }
 *         }.call(inputChannel);
 *     </code>
 * </pre>
 * <h2>Wrapping existing method</h2>
 * An additional way to create a routine method is by wrapping a method of an existing object or
 * class. The routine is created by calling one of the provided {@code from()} methods.
 * <br>
 * No strong reference to the target object will be retained, so, it's up to the caller to ensure
 * that it does not get garbage collected.
 * <p>
 * Input channels can be passed to the {@code call()} method in place of the actual parameter
 * values, though, the order and total number of inputs must match the target method signature.
 * Moreover, all the input channels must be closed before the wrapped method is actually invoked.
 * <br>
 * Results can be collected through the returned output channel.
 * <p>
 * For example, a routine wrapping the {@code String.format()} method can be built as shown below:
 * <pre>
 *     <code>
 *
 *         final Channel&lt;Object[], Object[]&gt; inputChannel =
 *                 JRoutineCore.&lt;Object[]&gt;ofInputs().buildChannel();
 *         final Channel&lt;?, String&gt; outputChannel =
 *                 RoutineMethod.from(
 *                     String.class.getMethod("format", String.class, Object[].class))
 *                              .call("%s %s!", inputChannel);
 *         inputChannel.pass(new Object[]{"Hello", "JRoutine"}).close();
 *         outputChannel.inMax(seconds(1)).next(); // expected value: "Hello JRoutine!"
 *     </code>
 * </pre>
 * In case the very same input or output channel instance has to be passed as parameter, it has to
 * be wrapped in another input channel, like shown below:
 * <pre>
 *     <code>
 *
 *         final Channel&lt;String, String&gt; channel =
 *                 JRoutineCore.&lt;String&gt;ofInputs().buildChannel();
 *         RoutineMethod.from(MyClass.class.getMethod("run", Channel.class))
 *                      .call(JRoutineCore.&lt;Channel&lt;String, String&gt;&gt;of(channel)
 *                                        .buildChannel());
 *     </code>
 * </pre>
 *
 * <p>
 * Created by davide-maestroni on 08/10/2016.
 */
@SuppressWarnings("WeakerAccess")
public class RoutineMethod implements InvocationConfigurable<RoutineMethod> {

  private final Object[] mArgs;

  private final Constructor<? extends RoutineMethod> mConstructor;

  private final AtomicBoolean mIsFirstCall = new AtomicBoolean(true);

  private final ThreadLocal<Channel<?, ?>> mLocalChannel = new ThreadLocal<Channel<?, ?>>();

  private final ThreadLocal<Boolean> mLocalIgnore = new ThreadLocal<Boolean>();

  private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

  private Class<?> mReturnType;

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
    final Object[] constructorArgs;
    if (type.isAnonymousClass()) {
      final Object[] safeArgs = asArgs(args);
      if (safeArgs.length > 0) {
        constructorArgs = new Object[safeArgs.length + 1];
        System.arraycopy(safeArgs, 0, constructorArgs, 1, safeArgs.length);
        if (Reflection.hasStaticScope(type)) {
          constructorArgs[0] = safeArgs;

        } else {
          constructorArgs[0] = safeArgs[0];
          constructorArgs[1] = safeArgs;
        }

      } else {
        constructorArgs = safeArgs;
      }

    } else {
      constructorArgs = cloneArgs(args);
    }

    Constructor<? extends RoutineMethod> constructor = null;
    try {
      constructor = Reflection.findBestMatchingConstructor(type, constructorArgs);

    } catch (final IllegalArgumentException ignored) {
    }

    mArgs = constructorArgs;
    mConstructor = constructor;
  }

  /**
   * Builds an object routine method by wrapping the specified static method.
   *
   * @param method the method.
   * @return the routine method instance.
   * @throws java.lang.IllegalArgumentException if the specified method is not static.
   */
  @NotNull
  public static ObjectRoutineMethod from(@NotNull final Method method) {
    if (!Modifier.isStatic(method.getModifiers())) {
      throw new IllegalArgumentException("the method is not static: " + method);
    }

    return from(InvocationTarget.classOfType(method.getDeclaringClass()), method);
  }

  /**
   * Builds an object routine method by wrapping a method of the specified target.
   *
   * @param target the invocation target.
   * @param method the method.
   * @return the routine method instance.
   * @throws java.lang.IllegalArgumentException if the specified method is not implemented by the
   *                                            target instance.
   */
  @NotNull
  public static ObjectRoutineMethod from(@NotNull final InvocationTarget<?> target,
      @NotNull final Method method) {
    if (!method.getDeclaringClass().isAssignableFrom(target.getTargetClass())) {
      throw new IllegalArgumentException(
          "the method is not applicable to the specified target class: " + target.getTargetClass());
    }

    return new ObjectRoutineMethod(target, method);
  }

  /**
   * Builds an object routine method by wrapping a method of the specified target.
   *
   * @param target         the invocation target.
   * @param name           the method name.
   * @param parameterTypes the method parameter types.
   * @return the routine method instance.
   * @throws java.lang.NoSuchMethodException if no method with the specified signature is found.
   */
  @NotNull
  public static ObjectRoutineMethod from(@NotNull final InvocationTarget<?> target,
      @NotNull final String name, @Nullable final Class<?>... parameterTypes) throws
      NoSuchMethodException {
    return from(target, target.getTargetClass().getMethod(name, parameterTypes));
  }

  /**
   * Gets the in/out annotation type related to the specified parameter and, if present, validates
   * it.
   *
   * @param param       the parameter object.
   * @param annotations the parameter annotation.
   * @return the annotation class or null.
   * @throws java.lang.IllegalArgumentException if the parameter annotations are invalid.
   * @see com.github.dm.jrt.method.annotation.Input Input
   * @see com.github.dm.jrt.method.annotation.Output Output
   */
  @Nullable
  protected static Class<? extends Annotation> getAnnotationType(@NotNull final Object param,
      @NotNull final Annotation[] annotations) {
    Class<? extends Annotation> type = null;
    for (final Annotation annotation : annotations) {
      if (annotation instanceof Input) {
        if ((type != null) || !(param instanceof Channel)) {
          throw new IllegalArgumentException("Invalid annotations for parameter: " + param);
        }

        type = Input.class;

      } else if (annotation instanceof Output) {
        if ((type != null) || !(param instanceof Channel)) {
          throw new IllegalArgumentException("Invalid annotations for parameter: " + param);
        }

        type = Output.class;
      }
    }

    return type;
  }

  /**
   * Replaces all the input and output channels in the specified parameters with newly created
   * instances.
   *
   * @param method         the target method.
   * @param params         the original method parameters.
   * @param inputChannels  the list to fill with the newly created input channels.
   * @param outputChannels the list to fill with the newly created output channels.
   * @return the replaced parameters.
   */
  @NotNull
  protected static Object[] replaceChannels(@NotNull final Method method,
      @NotNull final Object[] params, @NotNull final ArrayList<Channel<?, ?>> inputChannels,
      @NotNull final ArrayList<Channel<?, ?>> outputChannels) {
    final Annotation[][] annotations = method.getParameterAnnotations();
    final int length = params.length;
    final ArrayList<Object> parameters = new ArrayList<Object>(length);
    final ChannelBuilder<Object, Object> channelBuilder = JRoutineCore.ofInputs();
    for (int i = 0; i < length; ++i) {
      final Object param = params[i];
      final Class<? extends Annotation> annotationType = getAnnotationType(param, annotations[i]);
      if (annotationType == Input.class) {
        final Channel<Object, Object> inputChannel = channelBuilder.buildChannel();
        inputChannels.add(inputChannel);
        parameters.add(inputChannel);

      } else if (annotationType == Output.class) {
        final Channel<Object, Object> outputChannel = channelBuilder.buildChannel();
        outputChannels.add(outputChannel);
        parameters.add(outputChannel);

      } else {
        parameters.add(param);
      }
    }

    return parameters.toArray();
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
   * Note also that, in case no proper arguments are passed to the constructor, it will be possible
   * to invoke this method only once.
   *
   * @param params the parameters.
   * @param <OUT>  the output data type.
   * @return the output channel instance.
   */
  @NotNull
  public <OUT> Channel<?, OUT> call(@Nullable final Object... params) {
    final Object[] safeParams = asArgs(params);
    final Class<? extends RoutineMethod> type = getClass();
    final Method method = findBestMatchingMethod(type, safeParams);
    final InvocationFactory<Selectable<Object>, Selectable<Object>> factory;
    final Constructor<? extends RoutineMethod> constructor = mConstructor;
    if (constructor != null) {
      factory = new MultiInvocationFactory(type, constructor, mArgs, method, safeParams);

    } else {
      if (!mIsFirstCall.getAndSet(false)) {
        throw new IllegalStateException(
            "cannot invoke the routine in more than once: please provide proper "
                + "constructor arguments");
      }

      setReturnType(method.getReturnType());
      factory = new SingleInvocationFactory(this, method, safeParams);
    }

    return call(factory, method, InvocationMode.ASYNC, safeParams);
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
   * Note also that, in case no proper arguments are passed to the constructor, it will be possible
   * to invoke this method only once.
   *
   * @param params the parameters.
   * @param <OUT>  the output data type.
   * @return the output channel instance.
   * @see com.github.dm.jrt.core.routine.Routine Routine
   */
  @NotNull
  public <OUT> Channel<?, OUT> callParallel(@Nullable final Object... params) {
    final Constructor<? extends RoutineMethod> constructor = mConstructor;
    if (constructor == null) {
      throw new IllegalStateException(
          "cannot invoke the routine in parallel mode: please provide proper "
              + "constructor arguments");
    }

    final Object[] safeParams = asArgs(params);
    final Class<? extends RoutineMethod> type = getClass();
    final Method method = findBestMatchingMethod(type, safeParams);
    return call(new MultiInvocationFactory(type, constructor, mArgs, method, safeParams), method,
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
   * Tells the routine to ignore the method return value, that is, it will not be passed to the
   * output channel.
   *
   * @param <OUT> the output data type.
   * @return the return value.
   */
  @SuppressWarnings("unchecked")
  protected <OUT> OUT ignoreReturnValue() {
    mLocalIgnore.set(true);
    return (OUT) boxingDefault(mReturnType);
  }

  /**
   * Returns the input channel which is ready to produce data. If the method takes no input
   * channel as parameter, null will be returned.
   * <p>
   * Note this method will return null if called outside the routine method invocation or from a
   * different thread.
   *
   * @param <IN> the input data type.
   * @return the input channel producing data or null.
   */
  @SuppressWarnings("unchecked")
  protected <IN> Channel<?, IN> switchInput() {
    return (Channel<?, IN>) mLocalChannel.get();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <OUT> Channel<?, OUT> call(
      @NotNull final InvocationFactory<Selectable<Object>, Selectable<Object>> factory,
      @NotNull final Method method, @NotNull final InvocationMode mode,
      @NotNull final Object[] params) {
    final ArrayList<Channel<?, ?>> inputChannels = new ArrayList<Channel<?, ?>>();
    final ArrayList<Channel<?, ?>> outputChannels = new ArrayList<Channel<?, ?>>();
    final Annotation[][] annotations = method.getParameterAnnotations();
    final int length = params.length;
    for (int i = 0; i < length; ++i) {
      final Object param = params[i];
      final Class<? extends Annotation> annotationType = getAnnotationType(param, annotations[i]);
      if (annotationType == Input.class) {
        inputChannels.add((Channel<?, ?>) param);

      } else if (annotationType == Output.class) {
        outputChannels.add((Channel<?, ?>) param);
      }
    }

    final Channel<OUT, OUT> resultChannel = JRoutineCore.<OUT>ofInputs().buildChannel();
    outputChannels.add(resultChannel);
    final Channel<?, ? extends Selectable<Object>> inputChannel =
        (!inputChannels.isEmpty()) ? Channels.merge(inputChannels).buildChannel()
            : JRoutineCore.<Selectable<Object>>of().buildChannel();
    final Channel<Selectable<Object>, Selectable<Object>> outputChannel =
        mode.invoke(JRoutineCore.with(factory).apply(getConfiguration()))
            .pass(inputChannel)
            .close();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        Channels.selectOutput(0, outputChannels.size(), outputChannel).buildChannelMap();
    for (final Entry<Integer, ? extends Channel<?, Object>> entry : channelMap.entrySet()) {
      entry.getValue().bind((Channel<Object, Object>) outputChannels.get(entry.getKey())).close();
    }

    return resultChannel;
  }

  private boolean isIgnoreReturnValue() {
    return (mLocalIgnore.get() != null);
  }

  private void resetIgnoreReturnValue() {
    mLocalIgnore.set(null);
  }

  private void setLocalInput(@Nullable final Channel<?, ?> inputChannel) {
    mLocalChannel.set(inputChannel);
  }

  private void setReturnType(@NotNull final Class<?> returnType) {
    mReturnType = returnType;
  }

  /**
   * Implementation of a routine method wrapping an object method.
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
      mMethod = method;
    }

    @NotNull
    public ObjectRoutineMethod apply(@NotNull final ObjectConfiguration configuration) {
      mConfiguration = ConstantConditions.notNull("object configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public ObjectRoutineMethod apply(@NotNull final InvocationConfiguration configuration) {
      return (ObjectRoutineMethod) super.apply(configuration);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Builder<? extends ObjectRoutineMethod> applyInvocationConfiguration() {
      return (Builder<? extends ObjectRoutineMethod>) super.applyInvocationConfiguration();
    }

    @NotNull
    @Override
    public <OUT> Channel<?, OUT> call(@Nullable final Object... params) {
      return call(InvocationMode.ASYNC, params);
    }

    @NotNull
    @Override
    public <OUT> Channel<?, OUT> callParallel(@Nullable final Object... params) {
      return call(InvocationMode.PARALLEL, params);
    }

    @NotNull
    public ObjectConfiguration.Builder<? extends ObjectRoutineMethod> applyObjectConfiguration() {
      return new ObjectConfiguration.Builder<ObjectRoutineMethod>(this, mConfiguration);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <OUT> Channel<?, OUT> call(@NotNull final InvocationMode mode,
        @Nullable final Object[] params) {
      final Object[] safeParams = asArgs(params);
      final Method method = mMethod;
      if (method.getParameterTypes().length != safeParams.length) {
        throw new IllegalArgumentException("wrong number of parameters: expected <" +
            method.getParameterTypes().length + "> but was <" + safeParams.length + ">");
      }

      final Routine<Object, Object> routine = JRoutineObject.with(mTarget)
                                                            .apply(getConfiguration())
                                                            .apply(mConfiguration)
                                                            .method(method);
      final Channel<Object, Object> channel = mode.invoke(routine).sorted();
      for (final Object param : safeParams) {
        if (param instanceof Channel) {
          channel.pass((Channel<?, ?>) param);

        } else {
          channel.pass(param);
        }
      }

      return (Channel<?, OUT>) channel.close();
    }
  }

  /**
   * Base invocation implementation.
   */
  private static abstract class AbstractInvocation
      implements Invocation<Selectable<Object>, Selectable<Object>> {

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

    public void onAbort(@NotNull final RoutineException reason) throws Exception {
      mIsAborted = true;
      final List<Channel<?, ?>> inputChannels = getInputChannels();
      for (final Channel<?, ?> inputChannel : inputChannels) {
        inputChannel.abort(reason);
      }

      try {
        if (!mIsComplete) {
          internalInvoke((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
        }

      } finally {
        resetIgnoreReturnValue();
        for (final Channel<?, ?> outputChannel : getOutputChannels()) {
          outputChannel.abort(reason);
        }
      }
    }

    public void onComplete(@NotNull final Channel<Selectable<Object>, ?> result) throws Exception {
      bind(result);
      mIsComplete = true;
      if (!mIsAborted) {
        final List<Channel<?, ?>> inputChannels = getInputChannels();
        for (final Channel<?, ?> inputChannel : inputChannels) {
          inputChannel.close();
        }

        final List<Channel<?, ?>> outputChannels = getOutputChannels();
        try {
          resetIgnoreReturnValue();
          final Object methodResult =
              internalInvoke((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
          if (mReturnResults && !isIgnoreReturnValue()) {
            result.pass(new Selectable<Object>(methodResult, outputChannels.size()));
          }

        } finally {
          resetIgnoreReturnValue();
        }

        for (final Channel<?, ?> outputChannel : outputChannels) {
          outputChannel.close();
        }
      }
    }

    public void onInput(final Selectable<Object> input,
        @NotNull final Channel<Selectable<Object>, ?> result) throws Exception {
      bind(result);
      @SuppressWarnings("unchecked") final Channel<Object, Object> inputChannel =
          (Channel<Object, Object>) getInputChannels().get(input.index);
      inputChannel.pass(input.data);
      try {
        resetIgnoreReturnValue();
        final Object methodResult = internalInvoke(inputChannel);
        if (mReturnResults && !isIgnoreReturnValue()) {
          result.pass(new Selectable<Object>(methodResult, getOutputChannels().size()));
        }

      } finally {
        resetIgnoreReturnValue();
      }
    }

    /**
     * Returns the list of input channels representing the input of the method.
     *
     * @return the list of input channels.
     */
    @NotNull
    protected abstract List<Channel<?, ?>> getInputChannels();

    /**
     * Returns the list of output channels representing the output of the method.
     *
     * @return the list of output channels.
     */
    @NotNull
    protected abstract List<Channel<?, ?>> getOutputChannels();

    /**
     * Invokes the method.
     *
     * @param inputChannel the ready input channel.
     * @return the method result.
     * @throws java.lang.Exception if an error occurred during the invocation.
     */
    @Nullable
    protected abstract Object invokeMethod(@Nullable Channel<?, ?> inputChannel) throws Exception;

    /**
     * Checks if the method return value must be ignored.
     *
     * @return whether the return value must be ignored.
     */
    protected abstract boolean isIgnoreReturnValue();

    /**
     * Resets the method return value ignore flag.
     */
    protected abstract void resetIgnoreReturnValue();

    private void bind(@NotNull final Channel<Selectable<Object>, ?> result) {
      if (!mIsBound) {
        mIsBound = true;
        final List<Channel<?, ?>> outputChannels = getOutputChannels();
        if (!outputChannels.isEmpty()) {
          result.pass(Channels.merge(outputChannels).buildChannel());
        }
      }
    }

    @Nullable
    private Object internalInvoke(@Nullable final Channel<?, ?> inputChannel) throws Exception {
      try {
        return invokeMethod(inputChannel);

      } catch (final InvocationTargetException e) {
        throw InvocationException.wrapIfNeeded(e.getTargetException());
      }
    }

    public void onRestart() throws Exception {
      mIsBound = false;
      mIsAborted = false;
      mIsComplete = false;
    }
  }

  /**
   * Invocation implementation supporting multiple invocation of the routine method.
   */
  private static class MultiInvocation extends AbstractInvocation {

    private final Object[] mArgs;

    private final Constructor<? extends RoutineMethod> mConstructor;

    private final ArrayList<Channel<?, ?>> mInputChannels = new ArrayList<Channel<?, ?>>();

    private final Method mMethod;

    private final Object[] mOrigParams;

    private final ArrayList<Channel<?, ?>> mOutputChannels = new ArrayList<Channel<?, ?>>();

    private RoutineMethod mInstance;

    private Object[] mParams;

    /**
     * Constructor.
     *
     * @param constructor the routine method constructor.
     * @param args        the constructor arguments.
     * @param method      the method instance.
     * @param params      the method parameters.
     */
    private MultiInvocation(@NotNull final Constructor<? extends RoutineMethod> constructor,
        @NotNull final Object[] args, @NotNull final Method method,
        @NotNull final Object[] params) {
      super(method);
      mConstructor = constructor;
      mArgs = args;
      mMethod = method;
      mOrigParams = params;
    }

    @NotNull
    @Override
    protected List<Channel<?, ?>> getInputChannels() {
      return mInputChannels;
    }

    @Override
    public void onRestart() throws Exception {
      super.onRestart();
      final RoutineMethod instance = (mInstance = mConstructor.newInstance(mArgs));
      final Method method = mMethod;
      instance.setReturnType(method.getReturnType());
      mParams = replaceChannels(method, mOrigParams, mInputChannels, mOutputChannels);
    }

    public void onRecycle(final boolean isReused) throws Exception {
      mInputChannels.clear();
      mOutputChannels.clear();
    }

    @NotNull
    @Override
    protected List<Channel<?, ?>> getOutputChannels() {
      return mOutputChannels;
    }

    @Override
    protected Object invokeMethod(@Nullable final Channel<?, ?> inputChannel) throws
        InvocationTargetException, IllegalAccessException {
      final RoutineMethod instance = mInstance;
      instance.setLocalInput(inputChannel);
      try {
        return mMethod.invoke(instance, mParams);

      } finally {
        instance.setLocalInput(null);
      }
    }

    @Override
    protected boolean isIgnoreReturnValue() {
      return mInstance.isIgnoreReturnValue();
    }

    @Override
    protected void resetIgnoreReturnValue() {
      mInstance.resetIgnoreReturnValue();
    }
  }

  /**
   * Invocation factory supporting multiple invocation of the routine method.
   */
  private static class MultiInvocationFactory
      extends InvocationFactory<Selectable<Object>, Selectable<Object>> {

    private final Object[] mArgs;

    private final Constructor<? extends RoutineMethod> mConstructor;

    private final Method mMethod;

    private final Object[] mParams;

    /**
     * Constructor.
     *
     * @param type        the routine method type.
     * @param constructor the routine method constructor.
     * @param args        the constructor arguments.
     * @param method      the method instance.
     * @param params      the method parameters.
     */
    private MultiInvocationFactory(@NotNull final Class<? extends RoutineMethod> type,
        @NotNull final Constructor<? extends RoutineMethod> constructor,
        @NotNull final Object[] args, @NotNull final Method method,
        @NotNull final Object[] params) {
      super(asArgs(type, args, method, cloneArgs(params)));
      mConstructor = constructor;
      mArgs = args;
      mMethod = method;
      mParams = cloneArgs(params);
    }

    @NotNull
    @Override
    public Invocation<Selectable<Object>, Selectable<Object>> newInvocation() {
      return new MultiInvocation(mConstructor, mArgs, mMethod, mParams);
    }
  }

  /**
   * Invocation implementation supporting single invocation of the routine method.
   */
  private static class SingleInvocation extends AbstractInvocation {

    private final ArrayList<Channel<?, ?>> mInputChannels;

    private final RoutineMethod mInstance;

    private final Method mMethod;

    private final ArrayList<Channel<?, ?>> mOutputChannels;

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
    private SingleInvocation(@NotNull final ArrayList<Channel<?, ?>> inputChannels,
        @NotNull final ArrayList<Channel<?, ?>> outputChannels,
        @NotNull final RoutineMethod instance, @NotNull final Method method,
        @NotNull final Object[] params) {
      super(method);
      mInputChannels = inputChannels;
      mOutputChannels = outputChannels;
      mInstance = instance;
      mMethod = method;
      mParams = params;
    }

    @Override
    protected Object invokeMethod(@Nullable final Channel<?, ?> inputChannel) throws
        InvocationTargetException, IllegalAccessException {
      final RoutineMethod instance = mInstance;
      instance.setLocalInput(inputChannel);
      try {
        return mMethod.invoke(instance, mParams);

      } finally {
        instance.setLocalInput(null);
      }
    }

    public void onRecycle(final boolean isReused) {
    }

    @NotNull
    @Override
    protected List<Channel<?, ?>> getInputChannels() {
      return mInputChannels;
    }

    @NotNull
    @Override
    protected List<Channel<?, ?>> getOutputChannels() {
      return mOutputChannels;
    }

    @Override
    protected boolean isIgnoreReturnValue() {
      return mInstance.isIgnoreReturnValue();
    }

    @Override
    protected void resetIgnoreReturnValue() {
      mInstance.resetIgnoreReturnValue();
    }
  }

  /**
   * Invocation factory supporting single invocation of the routine method.
   */
  private static class SingleInvocationFactory
      extends InvocationFactory<Selectable<Object>, Selectable<Object>> {

    private final ArrayList<Channel<?, ?>> mInputChannels;

    private final RoutineMethod mInstance;

    private final Method mMethod;

    private final ArrayList<Channel<?, ?>> mOutputChannels;

    private final Object[] mParams;

    /**
     * Constructor.
     *
     * @param instance the routine method instance.
     * @param method   the method instance.
     * @param params   the method parameters.
     */
    private SingleInvocationFactory(@NotNull final RoutineMethod instance,
        @NotNull final Method method, @NotNull final Object[] params) {
      super(asArgs(instance.getClass(), method, cloneArgs(params)));
      mInstance = instance;
      mMethod = method;
      final ArrayList<Channel<?, ?>> inputChannels =
          (mInputChannels = new ArrayList<Channel<?, ?>>());
      final ArrayList<Channel<?, ?>> outputChannels =
          (mOutputChannels = new ArrayList<Channel<?, ?>>());
      mParams = replaceChannels(method, params, inputChannels, outputChannels);
    }

    @NotNull
    @Override
    public Invocation<Selectable<Object>, Selectable<Object>> newInvocation() {
      return new SingleInvocation(mInputChannels, mOutputChannels, mInstance, mMethod, mParams);
    }
  }
}
