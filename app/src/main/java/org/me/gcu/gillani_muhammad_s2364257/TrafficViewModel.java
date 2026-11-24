package org.me.gcu.gillani_muhammad_s2364257;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficViewModel extends ViewModel {
    static final String INCIDENT = "Incident";
    static final String ROADWORKS = "Roadworks";

    static final class Feed {
        final String category;
        final String publication;
        List<TrafficSituation> records = Collections.emptyList();
        boolean loading;
        boolean failed;
        long lastSuccess;

        Feed(String category, String publication) {
            this.category = category;
            this.publication = publication;
        }
    }

    final Feed incidents = new Feed(INCIDENT, "UnplannedEvents");
    final Feed works = new Feed(ROADWORKS, "CurrentRoadworks");
    String road = "";
    private final MutableLiveData<Integer> revision = new MutableLiveData<>(0);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private boolean started;
    private boolean closed;

    LiveData<Integer> changes() { return revision; }

    void setRoad(String value) {
        road = value.trim();
        changed();
    }

    List<TrafficSituation> results(String scope) {
        List<TrafficSituation> matches = new ArrayList<>();
        for (Feed feed : new Feed[]{incidents, works}) {
            for (TrafficSituation situation : feed.records) {
                if (situation.matches(road, scope)) matches.add(situation);
            }
        }
        return matches;
    }

    void loadOnStart() {
        if (!started) refresh();
    }

    void refresh() {
        started = true;
        for (Feed feed : new Feed[]{incidents, works}) {
            if (feed.loading) continue;
            feed.loading = true;
            workers.execute(() -> download(feed));
        }
        changed();
    }

    private void download(Feed feed) {
        HttpURLConnection connection = null;
        try {
            if (BuildConfig.TRAFFIC_CLIENT_ID.isEmpty() || BuildConfig.TRAFFIC_CLIENT_KEY.isEmpty()) {
                throw new IOException("Missing feed configuration");
            }
            URL url = new URL("https://datex2.trafficscotland.org/rest/2.3/publications/"
                    + feed.publication + "/Content.xml");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            String credentials = BuildConfig.TRAFFIC_CLIENT_ID + ":" + BuildConfig.TRAFFIC_CLIENT_KEY;
            connection.setRequestProperty("Authorization", Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Feed request failed");
            }

            List<TrafficSituation> records;

            String xml;

            try (InputStream input = connection.getInputStream();
                 java.io.ByteArrayOutputStream output =
                         new java.io.ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int count;

                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }

                xml = output.toString("UTF-8");
            }

            records = new TrafficXmlParser().parse(xml, feed.category);

            main.post(() -> finish(feed, records));
        } catch (IOException | org.xmlpull.v1.XmlPullParserException e) {
            main.post(() -> finish(feed, null));
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    void finish(Feed feed, List<TrafficSituation> records) {
        if (closed) return;
        feed.loading = false;
        feed.failed = records == null;
        if (records != null) {
            feed.records = records;
            feed.lastSuccess = System.currentTimeMillis();
        }
        changed();
    }

    private void changed() {
        Integer previous = revision.getValue();
        revision.setValue(previous == null ? 0 : previous + 1);
    }

    @Override protected void onCleared() {
        closed = true;
        workers.shutdownNow();
        main.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
