package org.me.gcu.gillani_muhammad_s2364257;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class TrafficSituation {
    public String id = "";
    public String category = "";
    public String type = "";
    public String description = "";
    public String roadName = "";
    public String direction = "";
    public String startTime = "";
    public String endTime = "";
    public String delayType = "";
    public String restrictedLanes = "";
    public String operationalLanes = "";
    public Double fromLatitude, fromLongitude, toLatitude, toLongitude;
    public Double pointLatitude, pointLongitude;

    boolean matches(String road, String scope) {
        return (scope.isEmpty() || scope.equalsIgnoreCase(category))
                && (road.isEmpty() || road.trim().equalsIgnoreCase(roadName.trim()));
    }

    boolean hasStart() { return fromLatitude != null && fromLongitude != null; }
    boolean hasEnd() { return toLatitude != null && toLongitude != null; }
    boolean hasPoint() { return pointLatitude != null && pointLongitude != null; }

    Duration worksDuration() {
        try {
            Duration duration = Duration.between(OffsetDateTime.parse(startTime), OffsetDateTime.parse(endTime));
            return duration.isNegative() ? null : duration;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    boolean isLongWork(Duration duration) {
        return duration.compareTo(Duration.ofHours(24)) > 0;
    }

    static String readable(String value) {
        if (value == null || value.isEmpty()) return "";
        String words = value.substring(value.lastIndexOf(':') + 1)
                .replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ").replace('_', ' ').trim();
        if (words.isEmpty()) return "";
        return words.substring(0, 1).toUpperCase(Locale.UK) + words.substring(1).toLowerCase(Locale.UK);
    }

    String roadLabel() { return roadName.isEmpty() ? "Road unspecified" : roadName; }

    String details() {
        StringBuilder lines = new StringBuilder();
        append(lines, "", description);
        String typeName = readable(type);
        append(lines, "Situation", typeName.isEmpty() || typeName.equalsIgnoreCase(category)
                ? category : category + " · " + typeName);
        append(lines, "Direction", readable(direction));
        append(lines, "Starts", prettyTime(startTime));
        append(lines, "Ends", prettyTime(endTime));
        append(lines, "Reported delay", readable(delayType));
        append(lines, "Lanes restricted", restrictedLanes);
        append(lines, "Operational lanes", operationalLanes);
        if (hasStart()) append(lines, "From", coordinates(fromLatitude, fromLongitude));
        if (hasEnd()) append(lines, "To", coordinates(toLatitude, toLongitude));
        if (hasPoint()) append(lines, "Location", coordinates(pointLatitude, pointLongitude));
        return lines.length() == 0 ? "No further details reported." : lines.toString();
    }

    private static String prettyTime(String text) {
        if (text.isEmpty()) return "";
        try {
            java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter
                    .ofPattern("d MMM yyyy, HH:mm O", Locale.UK);
            return OffsetDateTime.parse(text).format(format);
        } catch (DateTimeParseException e) {
            return text;
        }
    }

    private static String coordinates(double lat, double lon) {
        return String.format(Locale.UK, "%.4f, %.4f", lat, lon);
    }

    private static void append(StringBuilder text, String label, String value) {
        if (value == null || value.isEmpty()) return;
        if (text.length() > 0) text.append("\n\n");
        if (!label.isEmpty()) text.append(label).append("\n");
        text.append(value);
    }
}
