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

package com.github.dm.jrt.swagger.generator;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.function.lambda.Consumer;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.retrofit.RoutineAdapterFactory;
import com.github.dm.jrt.swagger.client.UsersApiClient;
import com.github.dm.jrt.swagger.client.api.UsersApi;
import com.github.dm.jrt.swagger.client.model.Repo;
import com.github.dm.jrt.swagger.client.model.Repos;

import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.JRoutineFunction.onOutput;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generator tests.
 * <p>
 * Created by davide-maestroni on 12/07/2016.
 */
public class JRoutineCodegenTest {

  private static final String BODY = "[{\"id\":\"1\", \"name\":\"Repo1\"}, {\"id\":\"2\","
      + " \"name\":\"Repo2\"}, {\"id\":\"3\", \"name\":\"Repo3\", \"private\":true}]";

  @Test
  public void testOutputChannelAdapter() throws IOException {
    final MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(BODY));
    server.enqueue(new MockResponse().setBody(BODY));
    server.enqueue(new MockResponse().setBody(BODY));
    server.start();
    try {
      {
        final UsersApi service =
            new UsersApiClient().withBaseUrl("http://localhost:" + server.getPort())
                                .createService(UsersApi.class);
        final Repos repos = service.getRepos("octocat", null, null, null).next();
        assertThat(repos).hasSize(3);
        assertThat(repos.get(0).getId()).isEqualTo("1");
        assertThat(repos.get(0).getName()).isEqualTo("Repo1");
        assertThat(repos.get(0).getPrivate()).isFalse();
        assertThat(repos.get(1).getId()).isEqualTo("2");
        assertThat(repos.get(1).getName()).isEqualTo("Repo2");
        assertThat(repos.get(1).getPrivate()).isFalse();
        assertThat(repos.get(2).getId()).isEqualTo("3");
        assertThat(repos.get(2).getName()).isEqualTo("Repo3");
        assertThat(repos.get(2).getPrivate()).isTrue();
      }

      {
        final RoutineAdapterFactory factory = RoutineAdapterFactory.buildFactory();
        final RoutineAdapterFactory adapterFactory = //
            RoutineAdapterFactory.builder()
                                 .delegateFactory(factory)
                                 .invocationConfiguration()
                                 .withOutputTimeout(seconds(3))
                                 .apply()
                                 .buildFactory();
        final UsersApi service =
            new UsersApiClient().withBaseUrl("http://localhost:" + server.getPort())
                                .withAdapterFactory(adapterFactory)
                                .createService(UsersApi.class);
        final Repos repos = service.getRepos("octocat", null, null, null).next();
        assertThat(repos).hasSize(3);
        assertThat(repos.get(0).getId()).isEqualTo("1");
        assertThat(repos.get(0).getName()).isEqualTo("Repo1");
        assertThat(repos.get(0).getPrivate()).isFalse();
        assertThat(repos.get(1).getId()).isEqualTo("2");
        assertThat(repos.get(1).getName()).isEqualTo("Repo2");
        assertThat(repos.get(1).getPrivate()).isFalse();
        assertThat(repos.get(2).getId()).isEqualTo("3");
        assertThat(repos.get(2).getName()).isEqualTo("Repo3");
        assertThat(repos.get(2).getPrivate()).isTrue();
      }

      {
        final BodyAdapterFactory factory = new BodyAdapterFactory();
        final RoutineAdapterFactory adapterFactory = //
            RoutineAdapterFactory.builder()
                                 .delegateFactory(factory)
                                 .invocationConfiguration()
                                 .withOutputTimeout(seconds(3))
                                 .apply()
                                 .buildFactory();
        final UsersApi service =
            new UsersApiClient().withBaseUrl("http://localhost:" + server.getPort())
                                .withAdapterFactory(adapterFactory)
                                .createService(UsersApi.class);
        final Repos repos = service.getRepos("octocat", null, null, null).next();
        assertThat(repos).hasSize(3);
        assertThat(repos.get(0).getId()).isEqualTo("1");
        assertThat(repos.get(0).getName()).isEqualTo("Repo1");
        assertThat(repos.get(0).getPrivate()).isFalse();
        assertThat(repos.get(1).getId()).isEqualTo("2");
        assertThat(repos.get(1).getName()).isEqualTo("Repo2");
        assertThat(repos.get(1).getPrivate()).isFalse();
        assertThat(repos.get(2).getId()).isEqualTo("3");
        assertThat(repos.get(2).getName()).isEqualTo("Repo3");
        assertThat(repos.get(2).getPrivate()).isTrue();
      }

    } finally {
      server.shutdown();
    }
  }

  @Test
  public void testStreamBuilderAdapter() throws IOException {
    final MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(BODY));
    server.enqueue(new MockResponse().setBody(BODY));
    server.enqueue(new MockResponse().setBody(BODY));
    server.start();
    try {
      {
        final UsersApi service =
            new UsersApiClient().withBaseUrl("http://localhost:" + server.getPort())
                                .createService(UsersApi.class);
        assertThat(service.getReposStream("octocat", null, null, null)
                          .map(Operators.<Repo>unfold())
                          .invoke()
                          .consume(onOutput(new Consumer<Repo>() {

                            public void accept(final Repo repo) throws Exception {
                              final int id = repo.getId().intValue();
                              assertThat(id).isBetween(1, 3);
                              assertThat(repo.getName()).isEqualTo("Repo" + id);
                              assertThat(repo.getPrivate()).isEqualTo(id == 3);
                            }
                          }))
                          .close()
                          .in(seconds(3))
                          .getError()).isNull();
      }

      {
        final RoutineAdapterFactory factory = RoutineAdapterFactory.buildFactory();
        final RoutineAdapterFactory adapterFactory =
            RoutineAdapterFactory.builder().delegateFactory(factory).buildFactory();
        final UsersApi service =
            new UsersApiClient().withBaseUrl("http://localhost:" + server.getPort())
                                .withAdapterFactory(adapterFactory)
                                .createService(UsersApi.class);
        assertThat(service.getReposStream("octocat", null, null, null)
                          .map(Operators.<Repo>unfold())
                          .invoke()
                          .consume(onOutput(new Consumer<Repo>() {

                            public void accept(final Repo repo) throws Exception {
                              final int id = repo.getId().intValue();
                              assertThat(id).isBetween(1, 3);
                              assertThat(repo.getName()).isEqualTo("Repo" + id);
                              assertThat(repo.getPrivate()).isEqualTo(id == 3);
                            }
                          }))
                          .close()
                          .in(seconds(3))
                          .getError()).isNull();
      }

      {
        final BodyAdapterFactory factory = new BodyAdapterFactory();
        final RoutineAdapterFactory adapterFactory =
            RoutineAdapterFactory.builder().delegateFactory(factory).buildFactory();
        final UsersApi service =
            new UsersApiClient().withBaseUrl("http://localhost:" + server.getPort())
                                .withAdapterFactory(adapterFactory)
                                .createService(UsersApi.class);
        assertThat(service.getReposStream("octocat", null, null, null)
                          .map(Operators.<Repo>unfold())
                          .invoke()
                          .consume(onOutput(new Consumer<Repo>() {

                            public void accept(final Repo repo) throws Exception {
                              final int id = repo.getId().intValue();
                              assertThat(id).isBetween(1, 3);
                              assertThat(repo.getName()).isEqualTo("Repo" + id);
                              assertThat(repo.getPrivate()).isEqualTo(id == 3);
                            }
                          }))
                          .close()
                          .in(seconds(3))
                          .getError()).isNull();
      }

    } finally {
      server.shutdown();
    }
  }

  private static class BodyAdapterFactory extends CallAdapter.Factory {

    @Override
    public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
        final Retrofit retrofit) {
      if (returnType instanceof ParameterizedType) {
        if (((ParameterizedType) returnType).getRawType() == Channel.class) {
          return null;
        }

      } else if (returnType == Channel.class) {
        return null;
      }

      return new CallAdapter<Object>() {

        public Type responseType() {
          return returnType;
        }

        public <R> Object adapt(final Call<R> call) {
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
