package org.me.gcu.gillani_muhammad_s2364257;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final long REFRESH_INTERVAL = 5 * 60 * 1000L;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private final Runnable scheduledRefresh = new Runnable() {
        @Override public void run() {
            model.refresh();
            timer.postDelayed(this, REFRESH_INTERVAL);
        }
    };
    private TrafficViewModel model;
    private TrafficAdapter adapter;
    private BottomNavigationView navigation;
    private int tab = R.id.nav_all;
    private int listTab = R.id.nav_all;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        model = new ViewModelProvider(this).get(TrafficViewModel.class);
        if (saved != null) {
            tab = saved.getInt("selected_tab", R.id.nav_all);
            listTab = saved.getInt("list_tab", R.id.nav_all);
            model.setRoad(saved.getString("road", ""));
        }
        EditText road = findViewById(R.id.roadSearchInput);
        road.setText(model.road);
        road.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                model.setRoad(s.toString());
            }
            @Override public void afterTextChanged(Editable text) {}
        });
        findViewById(R.id.refreshButton).setOnClickListener(v -> model.refresh());
        ListView list = findViewById(R.id.trafficListView);
        adapter = new TrafficAdapter(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, row, position, id) -> {
            TrafficSituation item = adapter.getItem(position);
            if (item != null) getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detailsContainer, TrafficDetailsFragment.newInstance(item.roadLabel(), item.details()))
                    .addToBackStack(null).commit();
        });

        navigation = findViewById(R.id.bottomNavigation);
        navigation.setSelectedItemId(tab);
        navigation.setOnItemSelectedListener(item -> {
            tab = item.getItemId();
            if (tab != R.id.nav_map) listTab = tab;
            showScreen();
            renderList();
            return true;
        });
        getSupportFragmentManager().addOnBackStackChangedListener(this::showScreen);
        showScreen();
        model.changes().observe(this, ignored -> renderList());
    }

    private String scope() {
        if (listTab == R.id.nav_incidents) return TrafficViewModel.INCIDENT;
        if (listTab == R.id.nav_roadworks) return TrafficViewModel.ROADWORKS;
        return "";
    }

    private void showScreen() {
        boolean details = getSupportFragmentManager().getBackStackEntryCount() > 0;
        boolean map = tab == R.id.nav_map && !details;
        findViewById(R.id.mainContent).setVisibility(details || map ? View.GONE : View.VISIBLE);
        findViewById(R.id.detailsContainer).setVisibility(details ? View.VISIBLE : View.GONE);
        findViewById(R.id.mapContainer).setVisibility(map ? View.VISIBLE : View.GONE);
        navigation.setVisibility(details ? View.GONE : View.VISIBLE);
        androidx.fragment.app.Fragment existing = getSupportFragmentManager().findFragmentById(R.id.mapContainer);
        if (map && existing == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, TrafficMapFragment.forScope(scope())).commit();
        } else if (!map && existing != null) {
            getSupportFragmentManager().beginTransaction().remove(existing).commit();
        }
    }

    private void renderList() {
        List<TrafficSituation> reports = model.results(scope());
        adapter.setNotifyOnChange(false);
        adapter.clear();
        adapter.addAll(reports);
        adapter.notifyDataSetChanged();

        boolean loading = false;
        long oldest = Long.MAX_VALUE;
        List<String> notes = new ArrayList<>();
        for (TrafficViewModel.Feed feed : new TrafficViewModel.Feed[]{model.incidents, model.works}) {
            if (!scope().isEmpty() && !scope().equals(feed.category)) continue;
            loading |= feed.loading;
            if (feed.lastSuccess > 0) oldest = Math.min(oldest, feed.lastSuccess);
            if (feed.failed && !feed.loading) {
                String label = getString(feed == model.works ? R.string.roadworks : R.string.incidents);
                notes.add(getString(feed.lastSuccess == 0 ? R.string.feed_failed : R.string.feed_stale, label));
            }
        }
        String count = getResources().getQuantityString(R.plurals.report_count, reports.size(), reports.size());
        if (loading) notes.add(getString(R.string.loading));
        if (oldest != Long.MAX_VALUE) {
            String time = android.text.format.DateFormat.getTimeFormat(this).format(new Date(oldest));
            notes.add(0, count + " · " + getString(R.string.update_time, time));
        }
        if (reports.isEmpty() && notes.size() == 1 && oldest != Long.MAX_VALUE && !loading) {
            notes.add(model.road.isEmpty() ? getString(R.string.no_records) : getString(R.string.no_road, model.road));
        }
        TextView status = findViewById(R.id.resultsStatus);
        status.setText(android.text.TextUtils.join("\n", notes));
    }

    @Override protected void onStart() {
        super.onStart();
        model.loadOnStart();
        timer.postDelayed(scheduledRefresh, REFRESH_INTERVAL);
    }

    @Override protected void onStop() {
        timer.removeCallbacks(scheduledRefresh);
        super.onStop();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("selected_tab", tab);
        state.putInt("list_tab", listTab);
        state.putString("road", model.road);
    }
}
