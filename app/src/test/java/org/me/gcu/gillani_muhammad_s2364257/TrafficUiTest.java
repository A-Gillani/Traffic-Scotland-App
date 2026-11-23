package org.me.gcu.gillani_muhammad_s2364257;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rGB-w393dp-h850dp-port")
public class TrafficUiTest {
    // Runs the production activity and layout with fixed feeds. No credentials or network requests.
    public static class SampleActivity extends MainActivity {
        @Override protected void onCreate(Bundle state) {
            setTheme(R.style.Theme_Gillani_Muhammad_S2364257);
            super.onCreate(state);
        }

        @NonNull @Override public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
            return new ViewModelProvider.Factory() {
                @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
                    TrafficViewModel model = new TrafficViewModel() {
                        @Override void loadOnStart() {}
                        @Override void refresh() {}
                    };
                    model.incidents.records = java.util.Arrays.asList(
                            record("M8", "Incident", "VehicleObstruction", "Vehicle stopped near junction 18."),
                            record("A9", "Incident", "AbnormalTraffic", "Slow traffic on the approach."));
                    model.works.records = java.util.Collections.singletonList(
                            record("M8", "Roadworks", "MaintenanceWorks", "Lane repairs at junction 7."));
                    model.incidents.lastSuccess = model.works.lastSuccess = 1_785_000_000_000L;
                    return type.cast(model);
                }
            };
        }
    }

    private static TrafficSituation record(String road, String category, String type, String comment) {
        TrafficSituation record = new TrafficSituation();
        record.roadName = road;
        record.category = category;
        record.type = type;
        record.description = comment;
        return record;
    }

    @Test public void searchesBothFeedsAndNavigatesToDetailsAndBack() {
        try (ActivityController<SampleActivity> controller = Robolectric.buildActivity(SampleActivity.class).setup()) {
            SampleActivity activity = controller.get();
            ListView list = activity.findViewById(R.id.trafficListView);
            ((EditText) activity.findViewById(R.id.roadSearchInput)).setText("m8");
            assertEquals(2, list.getCount());
            com.google.android.material.bottomnavigation.BottomNavigationView nav = activity.findViewById(R.id.bottomNavigation);
            nav.setSelectedItemId(R.id.nav_roadworks);
            assertEquals(1, list.getCount());
            list.performItemClick(list.getAdapter().getView(0, null, list), 0, 0);
            activity.getSupportFragmentManager().executePendingTransactions();
            assertTrue(((TextView) activity.findViewById(R.id.detailsContent)).getText().toString().contains("Lane repairs"));
            activity.findViewById(R.id.detailsBackButton).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.mainContent).getVisibility());
            assertEquals(1, list.getCount());
            nav.setSelectedItemId(R.id.nav_all);
            ((EditText) activity.findViewById(R.id.roadSearchInput)).setText("M90");
            assertTrue(((TextView) activity.findViewById(R.id.resultsStatus)).getText().toString().contains("No reports for M90"));
        }
    }

    @Test public void restoredRoadAndCategorySurviveActivityRecreation() {
        try (ActivityController<SampleActivity> controller = Robolectric.buildActivity(SampleActivity.class).setup()) {
            SampleActivity activity = controller.get();
            ((EditText) activity.findViewById(R.id.roadSearchInput)).setText("M8");
            ((com.google.android.material.bottomnavigation.BottomNavigationView) activity.findViewById(R.id.bottomNavigation))
                    .setSelectedItemId(R.id.nav_incidents);
            controller.recreate();
            SampleActivity restored = controller.get();
            assertEquals("M8", ((EditText) restored.findViewById(R.id.roadSearchInput)).getText().toString());
            assertEquals(1, ((ListView) restored.findViewById(R.id.trafficListView)).getCount());
        }
    }

    @Test @Config(qualifiers = "en-rGB-w850dp-h393dp-land")
    public void landscapePlacesTheListBesideTheControls() {
        try (ActivityController<SampleActivity> controller = Robolectric.buildActivity(SampleActivity.class).setup()) {
            SampleActivity activity = controller.get();
            android.widget.LinearLayout content = activity.findViewById(R.id.mainContent);
            assertEquals(android.widget.LinearLayout.HORIZONTAL, content.getOrientation());
            assertEquals(3, ((ListView) activity.findViewById(R.id.trafficListView)).getCount());
        }
    }

    @Test public void failedRefreshKeepsReportsAndExplainsTheirAge() {
        try (ActivityController<SampleActivity> controller = Robolectric.buildActivity(SampleActivity.class).setup()) {
            SampleActivity activity = controller.get();
            TrafficViewModel model = new ViewModelProvider(activity).get(TrafficViewModel.class);
            model.finish(model.incidents, null);
            model.setRoad("M8");
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(2, ((ListView) activity.findViewById(R.id.trafficListView)).getCount());
            String status = ((TextView) activity.findViewById(R.id.resultsStatus)).getText().toString();
            assertTrue(status.contains("Showing earlier"));
        }
    }

    @Test public void successfulEmptyFeedClearsOnlyItsOwnOldRecords() {
        try (ActivityController<SampleActivity> controller = Robolectric.buildActivity(SampleActivity.class).setup()) {
            SampleActivity activity = controller.get();
            TrafficViewModel model = new ViewModelProvider(activity).get(TrafficViewModel.class);
            model.finish(model.works, java.util.Collections.emptyList());
            assertEquals(2, model.results("").size());
            assertTrue(model.results(TrafficViewModel.ROADWORKS).isEmpty());
            assertFalse(model.works.failed);
        }
    }

    @Test public void mapOpensWithTheListRoadAndCategory() {
        try (ActivityController<SampleActivity> controller = Robolectric.buildActivity(SampleActivity.class).setup()) {
            SampleActivity activity = controller.get();
            ((EditText) activity.findViewById(R.id.roadSearchInput)).setText("M8");
            com.google.android.material.bottomnavigation.BottomNavigationView nav = activity.findViewById(R.id.bottomNavigation);
            nav.setSelectedItemId(R.id.nav_roadworks);
            nav.setSelectedItemId(R.id.nav_map);
            activity.getSupportFragmentManager().executePendingTransactions();
            android.widget.Spinner categories = activity.findViewById(R.id.mapFilterSpinner);
            assertEquals("Roadworks Only", categories.getSelectedItem());
            assertTrue(((TextView) activity.findViewById(R.id.mapRoad)).getText().toString().contains("M8"));
        }
    }

    @Test public void firstDownloadFailureShowsRecoveryAndDoesNotClaimAnEmptyFeed() {
        try (ActivityController<SampleActivity> controller = Robolectric.buildActivity(SampleActivity.class).setup()) {
            SampleActivity activity = controller.get();
            TrafficViewModel model = new ViewModelProvider(activity).get(TrafficViewModel.class);
            model.incidents.records = java.util.Collections.emptyList();
            model.incidents.lastSuccess = 0;
            model.finish(model.incidents, null);
            ((com.google.android.material.bottomnavigation.BottomNavigationView) activity.findViewById(R.id.bottomNavigation))
                    .setSelectedItemId(R.id.nav_incidents);
            String status = ((TextView) activity.findViewById(R.id.resultsStatus)).getText().toString();
            assertTrue(status.contains("tap refresh"));
            assertFalse(status.contains("No reports"));
        }
    }
}
