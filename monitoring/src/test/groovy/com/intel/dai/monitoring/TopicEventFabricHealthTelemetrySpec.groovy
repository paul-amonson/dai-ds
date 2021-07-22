package com.intel.dai.monitoring

import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.dai.network_listener.CommonDataFormat
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import com.intel.runtime_utils.TimeUtils
import spock.lang.Specification

class TopicEventFabricHealthTelemetrySpec extends Specification {

    def underTest_
    ConfigIO parser_ = ConfigIOFactory.getInstance("json")

    void setup() {
        underTest_ = new TopicEventFabricHealthTelemetry(Mock(Logger), false)
    }

    def "ProcessTopic positive format"() {
        given:
        PropertyMap map = parser_.fromString(input)
        List<CommonDataFormat> results = new ArrayList<>()
        EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
        underTest_.processTopic(envelope, map, results)
        expect:
        results.size() == RESULT
        where:
        input || RESULT
        "{\"fields\":{\"Location\": \"http://test:8000/a1/a2/x0\", \"Timestamp\": \"2021-07-06T04:49:48.152Z\", \"Value\": \"Test message\",},\"name\": \"CrayFabricHealthTelemetry\",\"tags\": {},\"timestamp\": 1625546990}" || 1
        "{\"fields\":{\"Location\": \"http://test:8000/a1/a2/x1\", \"Timestamp\": \"2021-07-06T04:49:48.152Z\", \"Value\": \"Test message\",},\"name\": \"CrayFabricHealthTelemetry\",\"tags\": {},\"timestamp\": 1625546990}"|| 0

    }

    def "ProcessTopic negative format"() {
        given:
        PropertyMap map = parser_.fromString(format_negative)
        List<CommonDataFormat> results = new ArrayList<>()
        EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
        underTest_.processTopic(envelope, map, results)
        expect:
        results.size() == 0
    }

    private final String format_negative = "{\"fields\":{\"Timestamp\": \"2021-07-06T04:49:48.152Z\", \"Value\": \"Test message\",},\"name\": \"CrayFabricHealthTelemetry\",\"tags\": {},\"timestamp\": 1625546990}"
}
