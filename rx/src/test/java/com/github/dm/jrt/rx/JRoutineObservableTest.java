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

package com.github.dm.jrt.rx;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.MappingInvocation;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JRoutine Reactive Extension unit tests.
 * <p>
 * Created by davide-maestroni on 12/09/2016.
 */
public class JRoutineObservableTest {

  @Test
  public void testBuilder0() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineObservable.create(JRoutineCore.with(new CommandInvocation<String>(null) {

      public void onComplete(@NotNull final Channel<String, ?> result) {
        result.pass("test");
      }
    })).map(new Func1<String, String>() {

      public String call(final String s) {
        return s.toUpperCase();
      }
    }).subscribe(new Action1<String>() {

      public void call(final String s) {
        reference.set(s);
      }
    });
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  public void testBuilder1() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineObservable.create(JRoutineCore.with(new MappingInvocation<String, String>(null) {

      public void onInput(final String input, @NotNull final Channel<String, ?> result) {
        if (input != null) {
          result.pass(input);
        }
      }
    }), "test").map(new Func1<String, String>() {

      public String call(final String s) {
        return s.toUpperCase();
      }
    }).subscribe(new Action1<String>() {

      public void call(final String s) {
        reference.set(s);
      }
    });
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  public void testBuilder2() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineObservable.create(JRoutineCore.with(new MappingInvocation<String, String>(null) {

      public void onInput(final String input, @NotNull final Channel<String, ?> result) {
        if (input != null) {
          result.pass(input);
        }
      }
    }), null, "test").map(new Func1<String, String>() {

      public String call(final String s) {
        return s.toUpperCase();
      }
    }).subscribe(new Action1<String>() {

      public void call(final String s) {
        reference.set(s);
      }
    });
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  public void testBuilder3() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineObservable.create(JRoutineCore.with(new MappingInvocation<String, String>(null) {

      public void onInput(final String input, @NotNull final Channel<String, ?> result) {
        if (input != null) {
          result.pass(input);
        }
      }
    }), Arrays.asList(null, "test")).map(new Func1<String, String>() {

      public String call(final String s) {
        return s.toUpperCase();
      }
    }).subscribe(new Action1<String>() {

      public void call(final String s) {
        reference.set(s);
      }
    });
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  public void testChannel() {
    final Channel<?, String> channel = JRoutineObservable.with(Observable.just("test1", "test2"))
                                                         .channelConfiguration()
                                                         .withMaxSize(2)
                                                         .apply()
                                                         .buildChannel();
    assertThat(channel.all()).containsExactly("test1", "test2");
  }

  @Test
  public void testChannelAbort() {
    final Channel<?, String> channel =
        JRoutineObservable.with(Observable.just("test1", "test2").delay(1, TimeUnit.SECONDS))
                          .channelConfiguration()
                          .withMaxSize(2)
                          .apply()
                          .buildChannel();
    assertThat(channel.abort()).isTrue();
  }

  @Test
  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
  public void testChannelError() {
    final Channel<?, String> channel =
        JRoutineObservable.with(Observable.just("test").map(new Func1<String, String>() {

          public String call(final String s) {
            throw new IllegalStateException(s);
          }
        })).buildChannel();
    assertThat(channel.getError()).isExactlyInstanceOf(InvocationException.class);
    assertThat(channel.getError().getCause()).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(channel.getError().getCause().getMessage()).isEqualTo("test");
  }

  @Test
  public void testObservable() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    JRoutineObservable.create(JRoutineCore.of("test").buildChannel())
                      .map(new Func1<String, String>() {

                        public String call(final String s) {
                          return s.toUpperCase();
                        }
                      })
                      .subscribe(new Action1<String>() {

                        public void call(final String s) {
                          reference.set(s);
                        }
                      });
    assertThat(reference.get()).isEqualTo("TEST");
  }

  @Test
  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void testObservableError() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    final AtomicReference<Throwable> errorReference = new AtomicReference<Throwable>();
    JRoutineObservable.create(JRoutineCore.of("test").buildChannel())
                      .map(new Func1<String, String>() {

                        public String call(final String s) {
                          throw new IllegalStateException(s);
                        }
                      })
                      .subscribe(new Observer<String>() {

                        public void onCompleted() {
                        }

                        public void onError(final Throwable e) {
                          errorReference.set(e);
                        }

                        public void onNext(final String s) {
                          reference.set(s);
                        }
                      });
    assertThat(reference.get()).isNull();
    assertThat(errorReference.get()).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(errorReference.get().getMessage()).isEqualTo("test");
  }

  @Test
  public void testObservableError2() {
    final AtomicReference<String> reference = new AtomicReference<String>();
    final AtomicReference<Throwable> errorReference = new AtomicReference<Throwable>();
    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    channel.abort();
    JRoutineObservable.create(channel).map(new Func1<String, String>() {

      public String call(final String s) {
        return s.toUpperCase();
      }
    }).subscribe(new Observer<String>() {

      public void onCompleted() {
      }

      public void onError(final Throwable e) {
        errorReference.set(e);
      }

      public void onNext(final String s) {
        reference.set(s);
      }
    });
    assertThat(reference.get()).isNull();
    assertThat(errorReference.get()).isExactlyInstanceOf(AbortException.class);
  }

  @Test
  public void testObservableUnsubscribe() throws InterruptedException {
    final AtomicReference<String> reference = new AtomicReference<String>();
    final AtomicReference<Throwable> errorReference = new AtomicReference<Throwable>();
    final Channel<String, String> channel = JRoutineCore.<String>ofInputs().buildChannel();
    JRoutineObservable.create(channel.after(seconds(.5)).pass("test"))
                      .map(new Func1<String, String>() {

                        public String call(final String s) {
                          return s.toUpperCase();
                        }
                      })
                      .subscribeOn(Schedulers.computation())
                      .subscribe(new Observer<String>() {

                        public void onCompleted() {
                        }

                        public void onError(final Throwable e) {
                          errorReference.set(e);
                        }

                        public void onNext(final String s) {
                          reference.set(s);
                        }
                      })
                      .unsubscribe();
    assertThat(channel.in(seconds(1)).getError()).isExactlyInstanceOf(AbortException.class);
    assertThat(reference.get()).isNull();
    assertThat(errorReference.get()).isNull();
  }
}
