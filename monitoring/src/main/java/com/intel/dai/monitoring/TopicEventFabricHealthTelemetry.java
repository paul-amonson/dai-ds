package com.intel.dai.monitoring;

import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.runtime_utils.TimeUtils;

import java.util.List;

public class TopicEventFabricHealthTelemetry extends TopicBaseProcessor {
    TopicEventFabricHealthTelemetry(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        final PropertyMap fields = map.getMapOrDefault("fields", new PropertyMap());
        if(!fields.isEmpty()) {
            for(String requiredKey : REQ_KEYS) {
                if(!fields.containsKey(requiredKey)) {
                    log_.error("Missing required argument in FabricHealthTelemetry data: '" + requiredKey + "'");
                    return;
                }
            }

            try {
                final String[] foreignLocationArray = fields.getStringOrDefault(LOCATION, "").split("/");
                final String foreignLocation = foreignLocationArray[foreignLocationArray.length - 1];
                final String location = CommonFunctions.convertForeignToLocation(foreignLocation);
                final long timestamp = TimeUtils.nSFromIso8601(fields.getString(TIMESTAMP));
                addToResults(data.topic, RAS_EVENT_NAME, data.originalJsonText, location, timestamp, results);
            } catch (Exception e) {
                log_.warn("cannot find xname location: %s", e.getMessage());
            }
        }
    }

    private final static String LOCATION = "Location";
    private final static String TIMESTAMP = "Timestamp";
    private final static String RAS_EVENT_NAME = "RasCrayFabricHealthTelemetry";

    private final static String[] REQ_KEYS = {LOCATION, TIMESTAMP};
}
