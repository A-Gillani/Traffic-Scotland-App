package org.me.gcu.gillani_muhammad_s2364257;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class TrafficXmlParser {

    public ArrayList<TrafficSituation> parse(
            String xml, String category)
            throws XmlPullParserException, IOException {

        XmlPullParserFactory factory =
                XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        return parse(new StringReader(xml), category, factory.newPullParser());
    }

    ArrayList<TrafficSituation> parse(java.io.Reader input, String category, XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(input);
        return parse(parser, category);
    }

    ArrayList<TrafficSituation> parse(XmlPullParser parser, String category)
            throws XmlPullParserException, IOException {
        ArrayList<TrafficSituation> situations = new ArrayList<>();

        TrafficSituation current = null;
        boolean insidePublicComment = false;
        boolean insideLocations = false;
        boolean insideFrom = false;
        boolean insideTo = false;
        boolean insideLocationDescriptor = false;
        String locationDescriptor = "";
        String locationDescriptorType = "";



        int event = parser.getEventType();

        while (event != XmlPullParser.END_DOCUMENT) {

            if (event == XmlPullParser.START_TAG) {

                String tag = parser.getName();
                if (parser.getDepth() == 1 && (tag.equalsIgnoreCase("html") || tag.equalsIgnoreCase("error"))) {
                    throw new XmlPullParserException("Expected a traffic publication", parser, null);
                }

                if (tag.equals("situation")) {
                    current = new TrafficSituation();
                    current.id = parser.getAttributeValue(
                            null, "id");
                    current.category = category;

                } else if (current != null) {

                    if (tag.equals("situationRecord")) {
                        current.type = parser.getAttributeValue(
                                "http://www.w3.org/2001/XMLSchema-instance",
                                "type");

                    } else if (tag.equals("generalPublicComment")) {
                        insidePublicComment = true;

                    } else if (tag.equals("value")
                            && insidePublicComment) {
                        String description = parser.nextText();

                        if (current.description.isEmpty()) {
                            current.description = description;
                        } else {
                            current.description += "\n" + description;
                        }


                    } else if (tag.equals("tpegDirection")) {
                        current.direction = parser.nextText();

                    } else if (tag.equals("overallStartTime")) {
                        current.startTime = parser.nextText();

                    } else if (tag.equals("overallEndTime")) {
                        current.endTime = parser.nextText();

                    } else if (tag.equals("delaysType")) {
                        current.delayType = parser.nextText();

                    } else if (tag.equals("numberOfLanesRestricted")) {
                        current.restrictedLanes = parser.nextText().trim();

                    } else if (tag.equals("numberOfOperationalLanes")) {
                        current.operationalLanes = parser.nextText().trim();

                    } else if (tag.equals("groupOfLocations")) {
                        insideLocations = true;

                    } else if (insideLocations
                            && (tag.equals("name") || tag.equals("ilc"))) {

                        insideLocationDescriptor = true;
                        locationDescriptor = "";
                        locationDescriptorType = "";

                    } else if (insideLocationDescriptor
                            && tag.equals("value")) {

                        locationDescriptor = parser.nextText();

                    } else if (insideLocationDescriptor
                            && (tag.equals("tpegOtherPointDescriptorType")
                            || tag.equals("tpegIlcPointDescriptorType"))) {

                        locationDescriptorType = parser.nextText();


                    } else if (tag.equals("from") && insideLocations) {
                        insideFrom = true;

                    } else if (tag.equals("to") && insideLocations) {
                        insideTo = true;

                    } else if (tag.equals("latitude") && insideLocations) {
                        Double latitude = coordinate(parser.nextText(), 90);

                        if (insideFrom) {
                            current.fromLatitude = latitude;
                        } else if (insideTo) {
                            current.toLatitude = latitude;
                        } else {
                            current.pointLatitude = latitude;
                        }

                    } else if (tag.equals("longitude") && insideLocations) {
                        Double longitude = coordinate(parser.nextText(), 180);

                        if (insideFrom) {
                            current.fromLongitude = longitude;
                        } else if (insideTo) {
                            current.toLongitude = longitude;
                        } else {
                            current.pointLongitude = longitude;
                        }
                    }


                }

            } else if (event == XmlPullParser.END_TAG) {
                String tag = parser.getName();


                if (tag.equals("generalPublicComment")) {
                    insidePublicComment = false;

                } else if (insideLocationDescriptor
                        && (tag.equals("name") || tag.equals("ilc"))) {

                    if (current != null
                            && current.roadName.isEmpty()
                            && (locationDescriptorType.equals("linkName")
                            || locationDescriptorType.equals("tpegIlcName1"))) {

                        current.roadName = locationDescriptor.trim();
                    }

                    insideLocationDescriptor = false;
                    locationDescriptor = "";
                    locationDescriptorType = "";


                } else if (tag.equals("from") && insideLocations) {
                    insideFrom = false;

                } else if (tag.equals("to") && insideLocations) {
                    insideTo = false;

                } else if (tag.equals("groupOfLocations")) {
                    insideLocations = false;
                    insideFrom = false;
                    insideTo = false;

                } else if (tag.equals("situation") && current != null) {
                    situations.add(current);
                    current = null;
                }

            }

            event = parser.next();
        }

        return situations;
    }

    private static Double coordinate(String text, int limit) {
        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) && Math.abs(value) <= limit ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
