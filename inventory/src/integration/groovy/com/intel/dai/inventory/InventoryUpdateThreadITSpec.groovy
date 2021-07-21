package com.intel.dai.inventory

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.InventorySnapshot
import com.intel.dai.dsapi.InventoryTrackingApi

import com.intel.dai.dsimpl.voltdb.VoltHWInvDbApi
import com.intel.dai.inventory.utilities.*
import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.logging.Logger
import spock.lang.Specification

// Use the following in place of Mock Logger to get traces on stdout
// def logger = LoggerFactory.getInstance(AdapterInventoryNetworkBase.ADAPTER_TYPE, "inventory", "console")

class InventoryUpdateThreadITSpec extends Specification {
    Logger logger = Mock Logger
    DataStoreFactory dsClientFactory = Mock DataStoreFactory
    String[] voltDbServers = ['css-centos-8-00.ra.intel.com']

    def setup() {
        println Helper.testStartMessage(specificationContext)
        "./src/integration/resources/scripts/drop_inventory_data.sh".execute().text
        dsClientFactory.createHWInvApi() >> new VoltHWInvDbApi(logger, voltDbServers)
        dsClientFactory.createInventoryTrackingApi() >> Mock(InventoryTrackingApi)
    }

    def cleanup() {
        println Helper.testEndMessage(specificationContext)
    }

//    def "run - near line server unavailable"() {
//        def ts = new InventoryUpdateThread(Mock(Logger), Mock(NetworkListenerConfig))
//        when:
//        ts.run()
//        then:
//        notThrown Exception
//    }
}

class DatabaseSynchronizerITSpec extends Specification {
    Logger logger = Mock Logger
    NetworkListenerConfig config = Mock NetworkListenerConfig
    DataStoreFactory dsClientFactory = Mock DataStoreFactory
    String[] voltDbServers = ['css-centos-8-00.ra.intel.com']

    DatabaseSynchronizer ts

    def setup() {
        println Helper.testStartMessage(specificationContext)
        "./src/integration/resources/scripts/drop_inventory_data.sh".execute().text
        dsClientFactory.createHWInvApi() >> new VoltHWInvDbApi(logger, voltDbServers)
        dsClientFactory.createInventoryTrackingApi() >> Mock(InventoryTrackingApi)
        dsClientFactory.createInventorySnapshotApi() >> Mock(InventorySnapshot)

        ts = Spy(DatabaseSynchronizer, constructorArgs: [logger,
//                                                         dsClientFactory,
                                                         config])
        ts.waitForDataMoverToFinishMovingRawFruHosts() >> {}
        ts.areEmptyInventoryTablesInPostgres() >> true
        ts.getLastHWInventoryHistoryUpdate() >> ''  // initial loading
    }

    def cleanup() {
        println Helper.testEndMessage(specificationContext)
    }

//    def "updateDaiInventoryTables - near line server unavailable"() {
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [logger, dsClientFactory])
//        ts.getLastHWInventoryHistoryUpdate() >> null    //near line server unavailable
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        notThrown Exception
//    }

    /**
     * We need to use a Spy because getLastHWInventoryHistoryUpdate() does not work in the
     * absence of Postgres.
     */
//    def "updateDaiInventoryTables - initial loading"() {
//        setup:
//        ts.setElasticsearchServerAttributes("cmcheung-centos-7.ra.intel.com", 9200,
//                "elkrest", "elkdefault");
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        ts.totalNumberOfInjectedDocuments == 34 + 243 + 8
//    }

    /**
     * We need to use a Spy because getLastHWInventoryHistoryUpdate() does not work in the
     * absence of Postgres.
     */
//    def "updateDaiInventoryTables - patching"() {
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [logger, dsClientFactory])
//        ts.getLastHWInventoryHistoryUpdate() >> '2020-07-27T21:10:49.745223Z'  // patching
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        notThrown Exception
//    }
//
//    def "updateDaiInventoryTables - history unavailable"() {
//        def dsFactory = new DataStoreFactoryImpl(null as String, Mock(Logger))
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [Mock(Logger), dsFactory])
//        ts.getLastHWInventoryHistoryUpdate() >> '2222-07-27T21:10:49.745223Z'  // future; history unavailable
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        notThrown Exception
//    }
//
//    def "updateDaiInventoryTables - not a timestamp"() {
//        def dsFactory = new DataStoreFactoryImpl(null as String, Mock(Logger))
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [Mock(Logger), dsFactory])
//        ts.getLastHWInventoryHistoryUpdate() >> 'notATimeStamp' // not a timestamp
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        notThrown Exception
//    }
}
