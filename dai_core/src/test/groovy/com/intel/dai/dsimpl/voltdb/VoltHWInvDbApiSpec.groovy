package com.intel.dai.dsimpl.voltdb


import com.intel.logging.Logger
import org.voltdb.client.Client
import spock.lang.Specification

class VoltHWInvDbApiSpec extends Specification {
    VoltHWInvDbApi api
    Logger logger = Mock(Logger)

    def setup() {
        VoltDbClient.voltClient = Mock(Client)
        String[] servers = ["localhost"]
        api = new VoltHWInvDbApi(logger, servers)
    }

    def "initialize"() {
        when: api.initialize()
        then: notThrown Exception
    }
}
