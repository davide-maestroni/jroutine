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
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.InvocationException;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JRoutine Reactive Extension unit tests.
 * <p>
 * Created by davide-maestroni on 12/09/2016.
 */
public class JRoutineObservableTest {

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
}
