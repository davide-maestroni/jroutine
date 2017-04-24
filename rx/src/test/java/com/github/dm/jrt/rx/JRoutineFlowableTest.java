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

package com.github.dm.jrt.rx;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.MappingInvocation;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JRoutine Flowable unit tests.
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 */
public class JRoutineFlowableTest {

  @Test
  public void testBuilder0() throws InterruptedException {
    final Semaphore semaphore = new Semaphore(0);
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineFlowable.from(JRoutineCore.with(new CommandInvocation<String>(null) {

      public void onComplete(@NotNull final Channel<String, ?> result) {
        result.pass("test");
      }
    })).buildFlowable().map(new Function<String, String>() {

      public String apply(final String s) {
        return s.toUpperCase();
      }

    }).subscribe(new Consumer<String>() {

      public void accept(final String s) {
        reference.set(s);
        semaphore.release();
      }
    });
    semaphore.tryAcquire(1, TimeUnit.SECONDS);
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  public void testBuilder1() throws InterruptedException {
    final Semaphore semaphore = new Semaphore(0);
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineFlowable.from(JRoutineCore.with(new MappingInvocation<String, String>(null) {

      public void onInput(final String input, @NotNull final Channel<String, ?> result) {
        if (input != null) {
          result.pass(input);
        }
      }
    }))
                    .flowableConfiguration()
                    .withInput("test")
                    .apply()
                    .buildFlowable()
                    .map(new Function<String, String>() {

                      public String apply(final String s) {
                        return s.toUpperCase();
                      }
                    })
                    .subscribe(new Consumer<String>() {

                      public void accept(final String s) {
                        reference.set(s);
                        semaphore.release();
                      }
                    });
    semaphore.tryAcquire(1, TimeUnit.SECONDS);
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  public void testBuilder2() throws InterruptedException {
    final Semaphore semaphore = new Semaphore(0);
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineFlowable.from(JRoutineCore.with(new MappingInvocation<String, String>(null) {

      public void onInput(final String input, @NotNull final Channel<String, ?> result) {
        if (input != null) {
          result.pass(input);
        }
      }
    }))
                    .flowableConfiguration()
                    .withInputs(null, "test")
                    .apply()
                    .buildFlowable()
                    .map(new Function<String, String>() {

                      public String apply(final String s) {
                        return s.toUpperCase();
                      }
                    })
                    .subscribe(new Consumer<String>() {

                      public void accept(final String s) {
                        reference.set(s);
                        semaphore.release();
                      }
                    });
    semaphore.tryAcquire(1, TimeUnit.SECONDS);
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  public void testBuilder3() throws InterruptedException {
    final Semaphore semaphore = new Semaphore(0);
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineFlowable.from(JRoutineCore.with(new MappingInvocation<String, String>(null) {

      public void onInput(final String input, @NotNull final Channel<String, ?> result) {
        if (input != null) {
          result.pass(input);
        }
      }
    }))
                    .flowableConfiguration()
                    .withInputs(Arrays.asList(null, "test"))
                    .apply()
                    .buildFlowable()
                    .map(new Function<String, String>() {

                      public String apply(final String s) {
                        return s.toUpperCase();
                      }
                    })
                    .subscribe(new Consumer<String>() {

                      public void accept(final String s) {
                        reference.set(s);
                        semaphore.release();
                      }
                    });
    semaphore.tryAcquire(1, TimeUnit.SECONDS);
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  public void testChannelAbortCompletable() {
    final Channel<?, ?> channel =
        JRoutineFlowable.with(Completable.complete().delay(1, TimeUnit.SECONDS))
                        .withChannel()
                        .withMaxSize(2)
                        .configured()
                        .buildChannel();
    assertThat(channel.abort()).isTrue();
  }

  @Test
  public void testChannelAbortFlowable() {
    final Channel<?, String> channel =
        JRoutineFlowable.with(Flowable.just("test1", "test2").delay(1, TimeUnit.SECONDS))
                        .withChannel()
                        .withMaxSize(2)
                        .configured()
                        .buildChannel();
    assertThat(channel.abort()).isTrue();
  }

  @Test
  public void testChannelAbortObservable() {
    final Channel<?, String> channel =
        JRoutineFlowable.with(Observable.just("test1", "test2").delay(1, TimeUnit.SECONDS))
                        .withChannel()
                        .withMaxSize(2)
                        .configured()
                        .buildChannel();
    assertThat(channel.abort()).isTrue();
  }

  @Test
  public void testChannelAbortSingle() {
    final Channel<?, String> channel =
        JRoutineFlowable.with(Single.just("test1").delay(1, TimeUnit.SECONDS))
                        .withChannel()
                        .withMaxSize(2)
                        .configured()
                        .buildChannel();
    assertThat(channel.abort()).isTrue();
  }

  @Test
  public void testChannelCompletable() {
    final Channel<?, ?> channel = JRoutineFlowable.with(Completable.complete())
                                                  .withChannel()
                                                  .withMaxSize(2)
                                                  .configured()
                                                  .buildChannel();
    assertThat(channel.all()).isEmpty();
  }

  @Test
  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
  public void testChannelErrorFlowable() {
    final Channel<?, String> channel =
        JRoutineFlowable.with(Flowable.just("test").map(new Function<String, String>() {

          public String apply(final String s) {
            throw new IllegalStateException(s);
          }
        })).buildChannel();
    assertThat(channel.getError()).isExactlyInstanceOf(InvocationException.class);
    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(channel.getError().getCause().getMessage()).isEqualTo("test");
  }

  @Test
  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
  public void testChannelErrorObservable() {
    final Channel<?, String> channel =
        JRoutineFlowable.with(Observable.just("test").map(new Function<String, String>() {

          public String apply(final String s) {
            throw new IllegalStateException(s);
          }
        })).buildChannel();
    assertThat(channel.getError()).isExactlyInstanceOf(InvocationException.class);
    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(channel.getError().getCause().getMessage()).isEqualTo("test");
  }

  @Test
  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
  public void testChannelErrorSingle() {
    final Channel<?, String> channel =
        JRoutineFlowable.with(Single.just("test").map(new Function<String, String>() {

          public String apply(final String s) {
            throw new IllegalStateException(s);
          }
        })).buildChannel();
    assertThat(channel.getError()).isExactlyInstanceOf(InvocationException.class);
    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(channel.getError().getCause().getMessage()).isEqualTo("test");
  }

  @Test
  public void testChannelFlowable() {
    final Channel<?, String> channel = JRoutineFlowable.with(Flowable.just("test1", "test2"))
                                                       .withChannel()
                                                       .withMaxSize(2)
                                                       .configured()
                                                       .buildChannel();
    assertThat(channel.all()).containsExactly("test1", "test2");
  }

  @Test
  public void testChannelObservable() {
    final Channel<?, String> channel = JRoutineFlowable.with(Observable.just("test1", "test2"))
                                                       .withChannel()
                                                       .withMaxSize(2)
                                                       .configured()
                                                       .buildChannel();
    assertThat(channel.all()).containsExactly("test1", "test2");
  }

  @Test
  public void testChannelSingle() {
    final Channel<?, String> channel = JRoutineFlowable.with(Single.just("test1"))
                                                       .withChannel()
                                                       .withMaxSize(2)
                                                       .configured()
                                                       .buildChannel();
    assertThat(channel.all()).containsExactly("test1");
  }

  @Test
  public void testFlowable() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineFlowable.from(JRoutineCore.of("test").buildChannel())
                    .buildFlowable()
                    .map(new Function<String, String>() {

                      public String apply(final String s) {
                        return s.toUpperCase();
                      }
                    })
                    .subscribe(new Consumer<String>() {

                      public void accept(final String s) {
                        reference.set(s);
                      }
                    });
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void testFlowableError() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    final AtomicReference<Throwable> errorReference = new AtomicReference<Throwable>();
    JRoutineFlowable.from(JRoutineCore.of("test").buildChannel())
                    .buildFlowable()
                    .map(new Function<String, String>() {

                      public String apply(final String s) {
                        throw new IllegalStateException(s);
                      }
                    })
                    .subscribe(new Subscriber<String>() {

                      public void onSubscribe(final Subscription s) {
                        s.request(Long.MAX_VALUE);
                      }

                      public void onNext(final String s) {
                        reference.set(s);
                      }

                      public void onError(final Throwable e) {
                        errorReference.set(e);
                      }

                      public void onComplete() {
                      }
                    });
    assertThat(reference.get()).isNull();
    assertThat(errorReference.get()).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(errorReference.get().getMessage()).isEqualTo("test");
  }

  @Test
  public void testFlowableError2() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    final AtomicReference<Throwable> errorReference = new AtomicReference<Throwable>();
    final Channel<String, String> channel = JRoutineCore.<String>ofData().buildChannel();
    channel.abort();
    JRoutineFlowable.from(channel).buildFlowable().map(new Function<String, String>() {

      public String apply(final String s) {
        return s.toUpperCase();
      }
    }).subscribe(new Subscriber<String>() {

      public void onSubscribe(final Subscription s) {
        s.request(Long.MAX_VALUE);
      }

      public void onNext(final String s) {
        reference.set(s);
      }

      public void onError(final Throwable e) {
        errorReference.set(e);
      }

      public void onComplete() {
      }
    });
    assertThat(reference.get()).isNull();
    assertThat(errorReference.get()).isExactlyInstanceOf(AbortException.class);
  }

  @Test
  public void testFlowableUnsubscribe() throws InterruptedException {
    final AtomicReference<String> reference = new AtomicReference<String>();
    final AtomicReference<Throwable> errorReference = new AtomicReference<Throwable>();
    final Channel<String, String> channel = JRoutineCore.<String>ofData().buildChannel();
    final Disposable disposable = JRoutineFlowable.from(channel.after(seconds(2)).pass("test"))
                                                  .buildFlowable()
                                                  .map(new Function<String, String>() {

                                                    public String apply(final String s) {
                                                      return s.toUpperCase();
                                                    }
                                                  })
                                                  .subscribeOn(Schedulers.computation())
                                                  .subscribe(new Consumer<String>() {

                                                    public void accept(final String s) {
                                                      reference.set(s);
                                                    }
                                                  }, new Consumer<Throwable>() {

                                                    public void accept(final Throwable t) {
                                                      errorReference.set(t);
                                                    }
                                                  });
    seconds(.5).sleepAtLeast();
    disposable.dispose();
    assertThat(channel.in(seconds(1)).getError()).isExactlyInstanceOf(AbortException.class);
    assertThat(reference.get()).isNull();
    assertThat(errorReference.get()).isNull();
  }
}
