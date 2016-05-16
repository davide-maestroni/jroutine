/*
 * Copyright (c) 2016. Davide Maestroni
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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.stream.Streams;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;

import java.util.List;

import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Routine adapter factory unit tests.
 * <p>
 * Created by davide-maestroni on 05/16/2016.
 */
public class RoutineAdapterFactoryTest {

    private static final String BODY = "[{\"id\":\"1\", \"name\":\"Repo1\"}, {\"id\":\"2\","
            + " \"name\":\"Repo2\"}, {\"id\":\"3\", \"name\":\"Repo3\", \"isPrivate\":true}]";

    private static ClientAndServer sServer;

    @BeforeClass
    public static void setUp() {

        sServer = startClientAndServer(10013);
    }

    @AfterClass
    public static void tearDown() {

        sServer.stop();
    }

    @Test
    public void testOutputChannelAdapter() {

        sServer.when(request("/users/octocat/repos")).respond(response(BODY));
        final RoutineAdapterFactory factory = RoutineAdapterFactory.builder()
                                                                   .invocationConfiguration()
                                                                   .withReadTimeout(seconds(3))
                                                                   .apply()
                                                                   .buildFactory();
        final Retrofit retrofit = new Builder().baseUrl("http://localhost:" + sServer.getPort())
                                               .addCallAdapterFactory(factory)
                                               .addConverterFactory(GsonConverterFactory.create())
                                               .build();
        final GitHubService service = retrofit.create(GitHubService.class);
        final List<Repo> repos = service.listRepos("octocat").next();
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

    @Test
    public void testStreamChannelAdapter() {

        sServer.when(request("/users/octocat/repos")).respond(response(BODY));
        final Retrofit retrofit = new Builder().baseUrl("http://localhost:" + sServer.getPort())
                                               .addCallAdapterFactory(
                                                       RoutineAdapterFactory.defaultFactory())
                                               .addConverterFactory(GsonConverterFactory.create())
                                               .build();
        final GitHubService service = retrofit.create(GitHubService.class);
        assertThat(service.streamRepos("octocat")
                          .mapMore(Streams.<Repo>unfold())
                          .onOutput(new Consumer<Repo>() {

                              public void accept(final Repo repo) throws Exception {

                                  final int id = Integer.parseInt(repo.getId());
                                  assertThat(id).isBetween(1, 3);
                                  assertThat(repo.getName()).isEqualTo("Repo" + id);
                                  assertThat(repo.isPrivate()).isEqualTo(id == 3);
                              }
                          })
                          .afterMax(seconds(3))
                          .getError()).isNull();
    }
}
