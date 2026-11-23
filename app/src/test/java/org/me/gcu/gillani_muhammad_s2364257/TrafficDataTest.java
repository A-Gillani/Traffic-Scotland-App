package org.me.gcu.gillani_muhammad_s2364257;

import org.junit.Test;
import org.kxml2.io.KXmlParser;
import java.io.StringReader;
import java.util.List;
import static org.junit.Assert.*;

public class TrafficDataTest {
    private List<TrafficSituation> parse(String body) throws Exception {
        String xml = "<d2LogicalModel xmlns='urn:datex' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>"
                + body + "</d2LogicalModel>";
        return new TrafficXmlParser().parse(new StringReader(xml), "Incident", new KXmlParser());
    }

    private String situation(String id, String contents) {
        return "<situation id='" + id + "'><situationRecord xsi:type='VehicleObstruction'>"
                + contents + "</situationRecord></situation>";
    }

    @Test public void separatesRecordsAndPreservesBothLaneCounts() throws Exception {
        String road = "<groupOfLocations><name><value>M8</value>"
                + "<tpegOtherPointDescriptorType>linkName</tpegOtherPointDescriptorType></name></groupOfLocations>";
        String comment = "<generalPublicComment><comment><value>Queue at the junction</value></comment></generalPublicComment>";
        List<TrafficSituation> records = parse(situation("first", comment + road
                + "<impact><numberOfLanesRestricted>1</numberOfLanesRestricted>"
                + "<numberOfOperationalLanes>2</numberOfOperationalLanes></impact>") + situation("second", ""));
        assertEquals(2, records.size());
        TrafficSituation item = records.get(0);
        assertEquals("M8", item.roadName);
        assertEquals("VehicleObstruction", item.type);
        assertEquals("Queue at the junction", item.description);
        assertEquals("1", item.restrictedLanes);
        assertEquals("2", item.operationalLanes);
        assertTrue(item.details().contains("Operational lanes\n2"));
        assertEquals("second", records.get(1).id);
        assertTrue(records.get(1).description.isEmpty());
    }

    @Test public void readsPointAndSegmentCoordinates() throws Exception {
        String point = "<pointCoordinates><latitude>55.86</latitude><longitude>-4.25</longitude></pointCoordinates>";
        String area = "<groupOfLocations><from>" + point + "</from><to>"
                + point.replace("55.86", "55.90") + "</to></groupOfLocations>";
        List<TrafficSituation> records = parse(situation("segment", area)
                + situation("point", "<groupOfLocations>" + point + "</groupOfLocations>"));
        assertTrue(records.get(0).hasStart());
        assertEquals(55.90, records.get(0).toLatitude, 0.00001);
        assertTrue(records.get(1).hasPoint());
        assertEquals(-4.25, records.get(1).pointLongitude, 0.00001);
    }

    @Test public void skipsInvalidCoordinatesAndKeepsTheFollowingRecord() throws Exception {
        String bad = "<groupOfLocations><from><latitude>missing</latitude><longitude>200</longitude></from></groupOfLocations>";
        List<TrafficSituation> records = parse(situation("bad-location", bad) + situation("next", ""));
        assertEquals(2, records.size());
        assertNull(records.get(0).fromLatitude);
        assertNull(records.get(0).fromLongitude);
        assertFalse(records.get(0).hasStart());
        assertEquals("next", records.get(1).id);
    }

    @Test public void durationThresholdUsesTheExactElapsedTime() {
        TrafficSituation item = new TrafficSituation();
        item.startTime = "2026-11-20T09:00:00Z";
        item.endTime = "2026-11-21T10:00:00+01:00";
        assertFalse(item.isLongWork(item.worksDuration()));
        item.endTime = "2026-11-21T09:00:01Z";
        assertTrue(item.isLongWork(item.worksDuration()));
        item.endTime = "2026-11-19T09:00:00Z";
        assertNull(item.worksDuration());
        item.endTime = "";
        assertNull(item.worksDuration());
    }

    @Test public void roadAndCategoryFiltersCombineWithoutMatchingOtherRoads() {
        TrafficSituation item = new TrafficSituation();
        item.roadName = "M8";
        item.category = "Roadworks";
        assertTrue(item.matches(" m8 ", ""));
        assertTrue(item.matches("", "Roadworks"));
        assertFalse(item.matches("M80", ""));
        assertFalse(item.matches("M8", "Incident"));
    }
}
