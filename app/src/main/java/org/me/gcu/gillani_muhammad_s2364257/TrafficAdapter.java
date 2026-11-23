package org.me.gcu.gillani_muhammad_s2364257;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.time.Duration;
import java.util.Locale;

final class TrafficAdapter extends ArrayAdapter<TrafficSituation> {
    TrafficAdapter(Context context) { super(context, R.layout.traffic_row); }

    @Override public View getView(int position, View recycled, ViewGroup parent) {
        View row = recycled;
        if (row == null) row = LayoutInflater.from(getContext()).inflate(R.layout.traffic_row, parent, false);
        TrafficSituation report = getItem(position);
        TextView road = row.findViewById(R.id.rowRoad);
        TextView type = row.findViewById(R.id.rowType);
        TextView description = row.findViewById(R.id.rowComment);
        TextView duration = row.findViewById(R.id.rowDuration);
        ImageView icon = row.findViewById(R.id.situationIcon);

        road.setText(report.roadLabel());
        String name = TrafficSituation.readable(report.type);
        type.setText(name.isEmpty() || name.equalsIgnoreCase(report.category)
                ? report.category : report.category + " · " + name);
        description.setText(report.description);
        description.setVisibility(report.description.isEmpty() ? View.GONE : View.VISIBLE);
        icon.setImageResource(iconFor(report));
        icon.setColorFilter(getContext().getColor(R.color.traffic_accent));
        Duration works = TrafficViewModel.ROADWORKS.equals(report.category) ? report.worksDuration() : null;
        duration.setVisibility(works == null ? View.GONE : View.VISIBLE);
        if (works != null) {
            boolean longer = report.isLongWork(works);
            duration.setText(longer ? R.string.long_works : R.string.short_works);
            duration.setTextColor(getContext().getColor(longer ? R.color.traffic_extended : R.color.traffic_brief));
        }
        return row;
    }

    private static int iconFor(TrafficSituation report) {
        if (TrafficViewModel.ROADWORKS.equals(report.category)) return R.drawable.traffic_cone;
        String type = TrafficSituation.readable(report.type).toLowerCase(Locale.ROOT);
        if (type.contains("vehicle") || type.contains("accident")) return R.drawable.traffic_vehicle;
        if (type.contains("abnormal traffic") || type.contains("congestion")) return R.drawable.traffic_queue;
        if (type.contains("lane management") || type.contains("closure")) return R.drawable.traffic_closure;
        if (type.contains("weather") || type.contains("environmental")) return R.drawable.traffic_weather;
        return R.drawable.traffic_alert;
    }
}
