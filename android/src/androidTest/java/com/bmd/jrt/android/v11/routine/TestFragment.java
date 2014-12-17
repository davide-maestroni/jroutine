package com.bmd.jrt.android.v11.routine;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by davide on 12/16/14.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class TestFragment extends Fragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        JRoutine.enable(this);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {

        return new View(getActivity());
    }
}
