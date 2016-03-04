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

package com.github.dm.jrt.android.v11.core.channel;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

/**
 * Test fragment.
 * <p/>
 * Created by davide-maestroni on 12/16/2014.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class TestFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NotNull final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {

        return new View(getActivity());
    }
}
