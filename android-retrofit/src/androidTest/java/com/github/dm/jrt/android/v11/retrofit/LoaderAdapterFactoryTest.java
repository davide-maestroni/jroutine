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

package com.github.dm.jrt.android.v11.retrofit;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.retrofit.GitHubService;
import com.github.dm.jrt.android.retrofit.Repo;
import com.github.dm.jrt.android.retrofit.ServiceAdapterFactory;
import com.github.dm.jrt.android.retrofit.service.TestService;
import com.github.dm.jrt.android.retrofit.test.R;
import com.github.dm.jrt.android.v11.core.LoaderSource;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.operator.JRoutineOperators;
import com.github.dm.jrt.stream.routine.StreamRoutine;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.github.dm.jrt.android.core.ServiceSource.serviceOf;
import static com.github.dm.jrt.android.v11.core.LoaderSource.loaderOf;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.JRoutineFunction.onOutput;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine adapter factory unit tests.
 * <p>
 * Created by davide-maestroni on 05/19/2016.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class LoaderAdapterFactoryTest extends ActivityInstrumentationTestCase2<TestActivity> {

  private static final String BODY = "[{\"id\":\"1\", \"name\":\"Repo1\"}, {\"id\":\"2\","
      + " \"name\":\"Repo2\"}, {\"id\":\"3\", \"name\":\"Repo3\", \"isPrivate\":true}]";

  public LoaderAdapterFactoryTest() {
    super(TestActivity.class);
  }

  private static void testBodyDelegate(@NotNull final LoaderSource loaderSource) throws
      IOException {
    final MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(BODY));
    server.enqueue(new MockResponse().setBody(BODY));
    server.start();
    try {
      final BodyAdapterFactory factory = new BodyAdapterFactory();
      final LoaderAdapterFactory adapterFactory = //
          LoaderAdapterFactory.factoryOn(loaderSource)
                              .delegateFactory(factory)
                              .withInvocation()
                              .withOutputTimeout(seconds(10))
                              .configuration()
                              .withLoader()
                              .withCacheStrategy(CacheStrategyType.CLEAR)
                              .configuration()
                              .buildFactory();
      final GsonConverterFactory converterFactory = GsonConverterFactory.create();
      final Retrofit retrofit = new Builder().baseUrl("http://localhost:" + server.getPort())
                                             .addCallAdapterFactory(adapterFactory)
                                             .addConverterFactory(converterFactory)
                                             .build();
      {
        final GitHubService service = retrofit.create(GitHubService.class);
        final List<Repo> repos = service.listRepos("octocat").invoke().close().get();
        assertThat(repos).hasSize(3);
        assertThat(repos.get(0).getId()).isEqualTo("1");
        assertThat(repos.get(0).getName()).isEqualTo("Repo1");
        assertThat(repos.get(0).isPrivate()).isFalse();
        assertThat(repos.get(1).getId()).isEqualTo("2");
        assertThat(repos.get(1).getName()).isEqualTo("Repo2");
        assertThat(repos.get(1).isPrivate()).isFalse();
        assertThat(repos.get(2).getId()).isEqualTo("3");
        assertThat(repos.get(2).getName()).isEqualTo("Repo3");
        assertThat(repos.get(2).isPrivate()).isTrue();
      }

      {
        final GitHubService service = retrofit.create(GitHubService.class);
        assertThat(service.streamRepos("octocat")
                          .map(JRoutineOperators.<Repo>unfold())
                          .invoke()
                          .consume(onOutput(new Consumer<Repo>() {

                            public void accept(final Repo repo) throws Exception {

                              final int id = Integer.parseInt(repo.getId());
                              assertThat(id).isBetween(1, 3);
                              assertThat(repo.getName()).isEqualTo("Repo" + id);
                              assertThat(repo.isPrivate()).isEqualTo(id == 3);
                            }
                          }))
                          .close()
                          .in(seconds(10))
                          .getError()).isNull();
      }

    } finally {
      server.shutdown();
    }
  }

  private static void testOutputChannelAdapter(@NotNull final LoaderSource loaderSource) throws
      IOException {
    final MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(BODY));
    server.start();
    try {
      final LoaderAdapterFactory adapterFactory = LoaderAdapterFactory.factoryOn(loaderSource)
                                                                      .withInvocation()
                                                                      .withOutputTimeout(
                                                                          seconds(10))
                                                                      .configuration()
                                                                      .buildFactory();
      final GsonConverterFactory converterFactory = GsonConverterFactory.create();
      final Retrofit retrofit = new Builder().baseUrl("http://localhost:" + server.getPort())
                                             .addCallAdapterFactory(adapterFactory)
                                             .addConverterFactory(converterFactory)
                                             .build();
      final GitHubService service = retrofit.create(GitHubService.class);
      final List<Repo> repos = service.listRepos("octocat").invoke().close().get();
      assertThat(repos).hasSize(3);
      assertThat(repos.get(0).getId()).isEqualTo("1");
      assertThat(repos.get(0).getName()).isEqualTo("Repo1");
      assertThat(repos.get(0).isPrivate()).isFalse();
      assertThat(repos.get(1).getId()).isEqualTo("2");
      assertThat(repos.get(1).getName()).isEqualTo("Repo2");
      assertThat(repos.get(1).isPrivate()).isFalse();
      assertThat(repos.get(2).getId()).isEqualTo("3");
      assertThat(repos.get(2).getName()).isEqualTo("Repo3");
      assertThat(repos.get(2).isPrivate()).isTrue();

    } finally {
      server.shutdown();
    }
  }

  private static void testServiceDelegate(@NotNull final LoaderSource loaderSource) throws
      IOException {
    final MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(BODY));
    server.enqueue(new MockResponse().setBody(BODY));
    server.start();
    try {
      final ServiceAdapterFactory factory = ServiceAdapterFactory.factoryOn(
          serviceOf(ConstantConditions.notNull(loaderSource.getLoaderContext()), TestService.class))
                                                                 .buildFactory();
      final LoaderAdapterFactory adapterFactory = //
          LoaderAdapterFactory.factoryOn(loaderSource)
                              .delegateFactory(factory)
                              .withInvocation()
                              .withOutputTimeout(seconds(10))
                              .configuration()
                              .withLoader()
                              .withCacheStrategy(CacheStrategyType.CLEAR)
                              .configuration()
                              .buildFactory();
      final GsonConverterFactory converterFactory = GsonConverterFactory.create();
      final Retrofit retrofit = new Builder().baseUrl("http://localhost:" + server.getPort())
                                             .addCallAdapterFactory(adapterFactory)
                                             .addConverterFactory(converterFactory)
                                             .build();
      {
        final GitHubService service = retrofit.create(GitHubService.class);
        final List<Repo> repos = service.listRepos("octocat").invoke().close().get();
        assertThat(repos).hasSize(3);
        assertThat(repos.get(0).getId()).isEqualTo("1");
        assertThat(repos.get(0).getName()).isEqualTo("Repo1");
        assertThat(repos.get(0).isPrivate()).isFalse();
        assertThat(repos.get(1).getId()).isEqualTo("2");
        assertThat(repos.get(1).getName()).isEqualTo("Repo2");
        assertThat(repos.get(1).isPrivate()).isFalse();
        assertThat(repos.get(2).getId()).isEqualTo("3");
        assertThat(repos.get(2).getName()).isEqualTo("Repo3");
        assertThat(repos.get(2).isPrivate()).isTrue();
      }

      {
        final GitHubService service = retrofit.create(GitHubService.class);
        assertThat(service.streamRepos("octocat")
                          .map(JRoutineOperators.<Repo>unfold())
                          .invoke()
                          .consume(onOutput(new Consumer<Repo>() {

                            public void accept(final Repo repo) throws Exception {

                              final int id = Integer.parseInt(repo.getId());
                              assertThat(id).isBetween(1, 3);
                              assertThat(repo.getName()).isEqualTo("Repo" + id);
                              assertThat(repo.isPrivate()).isEqualTo(id == 3);
                            }
                          }))
                          .close()
                          .in(seconds(10))
                          .getError()).isNull();
      }

    } finally {
      server.shutdown();
    }
  }

  private static void testStreamBuilderAdapter(@NotNull final LoaderSource loaderSource) throws
      IOException {
    final MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(BODY));
    server.start();
    try {
      final LoaderAdapterFactory adapterFactory =
          LoaderAdapterFactory.factoryOn(loaderSource).buildFactory();
      final GsonConverterFactory converterFactory = GsonConverterFactory.create();
      final Retrofit retrofit = new Builder().baseUrl("http://localhost:" + server.getPort())
                                             .addCallAdapterFactory(adapterFactory)
                                             .addConverterFactory(converterFactory)
                                             .build();
      final GitHubService service = retrofit.create(GitHubService.class);
      assertThat(service.streamRepos("octocat")
                        .map(JRoutineOperators.<Repo>unfold())
                        .invoke()
                        .consume(onOutput(new Consumer<Repo>() {

                          public void accept(final Repo repo) throws Exception {

                            final int id = Integer.parseInt(repo.getId());
                            assertThat(id).isBetween(1, 3);
                            assertThat(repo.getName()).isEqualTo("Repo" + id);
                            assertThat(repo.isPrivate()).isEqualTo(id == 3);
                          }
                        }))
                        .close()
                        .in(seconds(10))
                        .getError()).isNull();

    } finally {
      server.shutdown();
    }
  }

  public void testBodyDelegate() throws IOException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testBodyDelegate(loaderOf(getActivity()));
  }

  public void testBodyDelegateFragment() throws IOException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final TestFragment fragment =
        (TestFragment) getActivity().getFragmentManager().findFragmentById(R.id.test_fragment);
    testBodyDelegate(loaderOf(fragment));
  }

  public void testOutputChannelAdapter() throws IOException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testOutputChannelAdapter(loaderOf(getActivity()));
  }

  public void testOutputChannelAdapterFragment() throws IOException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final TestFragment fragment =
        (TestFragment) getActivity().getFragmentManager().findFragmentById(R.id.test_fragment);
    testOutputChannelAdapter(loaderOf(fragment));
  }

  public void testServiceDelegate() throws IOException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testServiceDelegate(loaderOf(getActivity()));
  }

  public void testServiceDelegateFragment() throws IOException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final TestFragment fragment =
        (TestFragment) getActivity().getFragmentManager().findFragmentById(R.id.test_fragment);
    testServiceDelegate(loaderOf(fragment));
  }

  public void testStreamBuilderAdapter() throws IOException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testStreamBuilderAdapter(loaderOf(getActivity()));
  }

  public void testStreamBuilderAdapterFragment() throws IOException {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final TestFragment fragment =
        (TestFragment) getActivity().getFragmentManager().findFragmentById(R.id.test_fragment);
    testStreamBuilderAdapter(loaderOf(fragment));
  }

  private static class BodyAdapterFactory extends CallAdapter.Factory {

    @Override
    public CallAdapter<?, ?> get(@NotNull final Type returnType,
        @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {
      if (returnType instanceof ParameterizedType) {
        final Type rawType = ((ParameterizedType) returnType).getRawType();
        if ((rawType == Routine.class) || (rawType == StreamRoutine.class)) {
          return null;
        }

      } else if ((returnType == Routine.class) || (returnType == StreamRoutine.class)) {
        return null;
      }

      return new CallAdapter<Object, Object>() {

        public Type responseType() {
          return returnType;
        }

        public Object adapt(@NotNull final Call<Object> call) {
          try {
            return call.execute().body();

          } catch (final IOException e) {
            throw new InvocationException(e);
          }
        }
      };
    }
  }
}
