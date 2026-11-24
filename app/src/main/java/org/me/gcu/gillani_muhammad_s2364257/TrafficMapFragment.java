package org.me.gcu.gillani_muhammad_s2364257;


import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class TrafficMapFragment extends Fragment
        implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private TrafficViewModel viewModel;

    private int selectedCategory = 0;
    private int selectedDistance = 0;

    // Fixed search centre: Glasgow
    private static final double CENTRE_LAT = 55.8642;
    private static final double CENTRE_LON = -4.2518;

    private final String[] categories = {
            "All Traffic",
            "Incidents Only",
            "Roadworks Only"
    };

    private final String[] distances = {
            "Any distance",
            "5 km",
            "10 km",
            "25 km",
            "50 km"
    };

    private final double[] radiusKm = {
            Double.MAX_VALUE, 5, 10, 25, 50
    };

    public TrafficMapFragment() {
    }

    static TrafficMapFragment forScope(String scope) {
        TrafficMapFragment fragment = new TrafficMapFragment();
        Bundle args = new Bundle();
        args.putInt("initial_category", TrafficViewModel.INCIDENT.equals(scope) ? 1 :
                TrafficViewModel.ROADWORKS.equals(scope) ? 2 : 0);
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
                R.layout.fragment_traffic_map,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity())
                .get(TrafficViewModel.class);

        selectedCategory = getArguments() == null ? 0 : getArguments().getInt("initial_category", 0);
        if (savedInstanceState != null) {
            selectedCategory =
                    savedInstanceState.getInt("map_category", 0);
            selectedDistance =
                    savedInstanceState.getInt("map_distance", 0);
        }

        Spinner categorySpinner =
                view.findViewById(R.id.mapFilterSpinner);

        Spinner distanceSpinner =
                view.findViewById(R.id.distanceSpinner);

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        categories
                );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setSelection(selectedCategory);

        ArrayAdapter<String> distanceAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        distances
                );

        distanceAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        distanceSpinner.setAdapter(distanceAdapter);
        distanceSpinner.setSelection(selectedDistance);

        categorySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id) {

                        selectedCategory = position;
                        displayMarkers();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        distanceSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id) {

                        selectedDistance = position;
                        displayMarkers();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager()
                        .findFragmentById(
                                R.id.googleMapContainer
                        );

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.googleMapContainer,
                            mapFragment
                    )
                    .commitNow();
        }

        android.widget.TextView roadLabel = view.findViewById(R.id.mapRoad);
        viewModel.changes().observe(getViewLifecycleOwner(), ignored -> {
            roadLabel.setText(viewModel.road.isEmpty() ? getString(R.string.map_radius)
                    : getString(R.string.map_road, viewModel.road) + " · " + getString(R.string.map_radius));
            roadLabel.setVisibility(View.VISIBLE);
            displayMarkers();
        });
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        if (getView() == null) return;
        googleMap = map;

        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        new LatLng(56.4907, -4.2026),
                        7
                )
        );

        displayMarkers();
    }

    private void displayMarkers() {

        if (googleMap == null || viewModel == null) {
            return;
        }

        googleMap.clear();
        android.util.Log.d("TrafficMap",
                "Category=" + selectedCategory
                        + " Radius=" + radiusKm[selectedDistance]);

        String scope = selectedCategory == 1 ? TrafficViewModel.INCIDENT :
                selectedCategory == 2 ? TrafficViewModel.ROADWORKS : "";
        List<TrafficSituation> situations = viewModel.results(scope);

        for (TrafficSituation situation : situations) {

            if (selectedCategory == 1 &&
                    !TrafficViewModel.INCIDENT.equalsIgnoreCase(situation.category)) {
                continue;
            }

            if (selectedCategory == 2 &&
                    !TrafficViewModel.ROADWORKS.equalsIgnoreCase(situation.category)) {
                continue;
            }


            boolean incident =
                    "Incident".equalsIgnoreCase(
                            situation.category
                    );

            Double latitude = situation.fromLatitude;
            Double longitude = situation.fromLongitude;

            if (latitude == null || longitude == null) {
                latitude = situation.toLatitude;
                longitude = situation.toLongitude;
            }

            if (latitude == null || longitude == null) {
                latitude = situation.pointLatitude;
                longitude = situation.pointLongitude;
            }

            if (latitude == null || longitude == null) {
                continue;
            }

            if (!Double.isFinite(latitude)
                    || !Double.isFinite(longitude)
                    || latitude < -90 || latitude > 90
                    || longitude < -180 || longitude > 180) {
                continue;
            }

            float[] results = new float[1];

            Location.distanceBetween(
                    CENTRE_LAT,
                    CENTRE_LON,
                    latitude,
                    longitude,
                    results
            );

            double distanceKm = results[0] / 1000.0;

            if (distanceKm > radiusKm[selectedDistance]) {
                continue;
            }

            float colour = incident
                    ? BitmapDescriptorFactory.HUE_RED
                    : BitmapDescriptorFactory.HUE_ORANGE;

            android.util.Log.d("TrafficMap",
                    "Category=" + selectedCategory
                            + " Radius=" + radiusKm[selectedDistance]);

            googleMap.addMarker(
                    new MarkerOptions()
                            .position(
                                    new LatLng(latitude, longitude)
                            )
                            .title(situation.roadLabel())
                            .snippet(
                                    situation.description
                                            + " | "
                                            + String.format(
                                            java.util.Locale.UK,
                                            "%.1f km from Glasgow",
                                            distanceKm
                                    )
                            )
                            .icon(
                                    BitmapDescriptorFactory
                                            .defaultMarker(colour)
                            )
            );
        }
    }

    @Override
    public void onDestroyView() {
        googleMap = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        displayMarkers();
    }

    @Override
    public void onSaveInstanceState(
            @NonNull Bundle outState) {

        outState.putInt("map_category", selectedCategory);
        outState.putInt("map_distance", selectedDistance);

        super.onSaveInstanceState(outState);
    }
}
