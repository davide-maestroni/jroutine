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

package com.github.dm.jrt.android.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.github.dm.jrt.core.channel.TemplateOutputConsumer;
import com.github.dm.jrt.core.common.RoutineException;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Main activity.
 */
public class MainActivity extends AppCompatActivity {

    private ArrayAdapter<Repo> mRepoAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRepoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        final ListView repoList = (ListView) findViewById(R.id.repo_list);
        if (repoList != null) {
            repoList.setAdapter(mRepoAdapter);
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        final OutputChannelCallAdapter callAdapter = new OutputChannelCallAdapter(this);
        final Retrofit retrofit = new Builder().baseUrl("https://api.github.com")
                                               .addCallAdapterFactory(callAdapter)
                                               .addConverterFactory(GsonConverterFactory.create())
                                               .build();
        final GitHubService service = retrofit.create(GitHubService.class);
        service.listRepos("octocat").bind(new TemplateOutputConsumer<List<Repo>>() {

            @Override
            public void onError(@NotNull final RoutineException error) throws Exception {

                final Throwable cause = error.getCause();
                Toast.makeText(MainActivity.this,
                        (cause != null) ? cause.getMessage() : "Cannot load repository list",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onOutput(final List<Repo> output) throws Exception {

                final ArrayAdapter<Repo> adapter = mRepoAdapter;
                for (final Repo repo : output) {
                    adapter.setNotifyOnChange(false);
                    adapter.add(repo);
                }

                adapter.setNotifyOnChange(true);
                adapter.notifyDataSetChanged();
            }
        });
    }
}