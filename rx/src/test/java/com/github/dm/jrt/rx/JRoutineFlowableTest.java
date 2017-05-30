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
import com.github.dm.jrt.core.invocation.InvocationException;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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
  public void testBuilder() throws InterruptedException {
    final Semaphore semaphore = new Semaphore(0);
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineFlowable.flowable()
                    .of(JRoutineCore.channel().of("test"))
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
    final Channel<?, ?> channel = JRoutineFlowable.channel()
                                                  .withChannel()
                                                  .withMaxSize(2)
                                                  .configuration()
                                                  .of(Completable.complete()
                                                                 .delay(1, TimeUnit.SECONDS));
    assertThat(channel.abort()).isTrue();
  }

  @Test
  public void testChannelAbortFlowable() {
    final Channel<?, String> channel = JRoutineFlowable.channel()
                                                       .withChannel()
                                                       .withMaxSize(2)
                                                       .configuration()
                                                       .of(Flowable.just("test1", "test2")
                                                                   .delay(1, TimeUnit.SECONDS));
    assertThat(channel.abort()).isTrue();
  }

  @Test
  public void testChannelAbortObservable() {
    final Channel<?, String> channel = JRoutineFlowable.channel()
                                                       .withChannel()
                                                       .withMaxSize(2)
                                                       .configuration()
                                                       .of(Observable.just("test1", "test2")
                                                                     .delay(1, TimeUnit.SECONDS));
    assertThat(channel.abort()).isTrue();
  }

  @Test
  public void testChannelAbortSingle() {
    final Channel<?, String> channel = JRoutineFlowable.channel()
                                                       .withChannel()
                                                       .withMaxSize(2)
                                                       .configuration()
                                                       .of(Single.just("test1")
                                                                 .delay(1, TimeUnit.SECONDS));
    assertThat(channel.abort()).isTrue();
  }

  @Test
  public void testChannelCompletable() {
    final Channel<?, ?> channel = JRoutineFlowable.channel()
                                                  .withChannel()
                                                  .withMaxSize(2)
                                                  .configuration()
                                                  .of(Completable.complete());
    assertThat(channel.all()).isEmpty();
  }

  @Test
  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
  public void testChannelErrorFlowable() {
    final Channel<?, String> channel =
        JRoutineFlowable.channel().of(Flowable.just("test").map(new Function<String, String>() {

          public String apply(final String s) {
            throw new IllegalStateException(s);
          }
        }));
    assertThat(channel.getError()).isExactlyInstanceOf(InvocationException.class);
    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(channel.getError().getCause().getMessage()).isEqualTo("test");
  }

  @Test
  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
  public void testChannelErrorObservable() {
    final Channel<?, String> channel =
        JRoutineFlowable.channel().of(Observable.just("test").map(new Function<String, String>() {

          public String apply(final String s) {
            throw new IllegalStateException(s);
          }
        }));
    assertThat(channel.getError()).isExactlyInstanceOf(InvocationException.class);
    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(channel.getError().getCause().getMessage()).isEqualTo("test");
  }

  @Test
  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
  public void testChannelErrorSingle() {
    final Channel<?, String> channel =
        JRoutineFlowable.channel().of(Single.just("test").map(new Function<String, String>() {

          public String apply(final String s) {
            throw new IllegalStateException(s);
          }
        }));
    assertThat(channel.getError()).isExactlyInstanceOf(InvocationException.class);
    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(channel.getError().getCause().getMessage()).isEqualTo("test");
  }

  @Test
  public void testChannelFlowable() {
    final Channel<?, String> channel = JRoutineFlowable.channel()
                                                       .withChannel()
                                                       .withMaxSize(2)
                                                       .configuration()
                                                       .of(Flowable.just("test1", "test2"));
    assertThat(channel.all()).containsExactly("test1", "test2");
  }

  @Test
  public void testChannelObservable() {
    final Channel<?, String> channel = JRoutineFlowable.channel()
                                                       .withChannel()
                                                       .withMaxSize(2)
                                                       .configuration()
                                                       .of(Observable.just("test1", "test2"));
    assertThat(channel.all()).containsExactly("test1", "test2");
  }

  @Test
  public void testChannelSingle() {
    final Channel<?, String> channel = JRoutineFlowable.channel()
                                                       .withChannel()
                                                       .withMaxSize(2)
                                                       .configuration()
                                                       .of(Single.just("test1"));
    assertThat(channel.all()).containsExactly("test1");
  }

  @Test
  public void testConstructor() {
    boolean failed = false;
    try {
      new JRoutineFlowable();
      failed = true;

    } catch (final Throwable ignored) {
    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testFlowable() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineFlowable.flowable()
                    .of(JRoutineCore.channel().of("test"))
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
    JRoutineFlowable.flowable()
                    .of(JRoutineCore.channel().of("test"))
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
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    channel.abort();
    JRoutineFlowable.flowable().of(channel).map(new Function<String, String>() {

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
    final Channel<String, String> channel = JRoutineCore.channel().ofType();
    final Disposable disposable = JRoutineFlowable.flowable()
                                                  .of(channel.after(seconds(2)).pass("test"))
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
