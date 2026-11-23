package org.me.gcu.gillani_muhammad_s2364257;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.Button;

public class TrafficDetailsFragment extends Fragment {

    public TrafficDetailsFragment() {
        // Required empty constructor
    }

    public static TrafficDetailsFragment newInstance(
            String road,
            String details) {

        TrafficDetailsFragment fragment =
                new TrafficDetailsFragment();

        Bundle args = new Bundle();
        args.putString("road", road);
        args.putString("details", details);

        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_traffic_details,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        Button backButton =
                view.findViewById(R.id.detailsBackButton);

        backButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );


        TextView detailsTitle =
                view.findViewById(R.id.detailsTitle);

        TextView detailsContent =
                view.findViewById(R.id.detailsContent);

        Bundle args = getArguments();

        if (args != null) {

            String road =
                    args.getString("road", "Unknown Road");

            String details =
                    args.getString("details", "No details available");

            detailsTitle.setText(road);
            detailsContent.setText(details);
        }
    }
}
