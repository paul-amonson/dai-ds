// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.google.gson.Gson;
import com.intel.dai.dsapi.HWInvDbApi;
import com.intel.dai.dsapi.pojo.Dimm;
import com.intel.dai.dsapi.pojo.FruHost;
import com.intel.dai.dsapi.pojo.NodeInventory;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface that allows HW inventory to be stored in an underlying DB.  The HW
 * inventory is encoded in canonical form which is a list of HW locations.  The DB stores
 * each HW location as a row.  If the location is occupied, the row contains a index into
 * the FRU table.  Each entry of the FRU table describes a FRU that ever occupied a HW
 * location.
 */
public class VoltHWInvDbApi implements HWInvDbApi {

    public VoltHWInvDbApi(Logger logger, String[] servers) {
        this.logger = logger;
        this.servers = servers;
    }

    /**
     * <p> Initialize a client connection to the online tier database. </p>
     */
    @Override
    public void initialize() {
        VoltDbClient.initializeVoltDbClient(servers);
        client = VoltDbClient.getVoltClientInstance();
    }

    public int ingest(String id, Dimm dimm) throws DataStoreException {
        try {
            return upsertRawDimm(id, dimm);
        } catch (IOException e) {
            logger.error("IOException:%s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        } catch (ProcCallException e) {
            logger.error("ProcCallException:%s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        }
    }

    public int ingest(String id, FruHost fruHost) throws DataStoreException {
        try {
            return upsertRawFruHost(id, fruHost);
        } catch (IOException | ProcCallException e) {
            logger.error(e.getMessage());
            throw new DataStoreException(e.getMessage());
        }
    }

    public int ingest(NodeInventory nodeInventory) throws DataStoreException {
        try {
            return insertNodeInventoryHistory(nodeInventory);
        } catch (IOException e) {
            logger.error("IOException:%s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        } catch (ProcCallException e) {
            logger.error("ProcCallException:%s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        }
    }

    int upsertRawDimm(String id, Dimm dimm) throws IOException, ProcCallException {
        String source = gson.toJson(dimm);
        ClientResponse cr = client.callProcedure("Raw_DIMM_Insert",
                id, dimm.serial, dimm.mac, dimm.locator, source, dimm.timestamp);

        if (cr.getStatus() != ClientResponse.SUCCESS) {
            logger.error("upsertRawDimm(id=%s) => %d", id, cr.getStatus());
            return 0;
        }
        guaranteeDbUpdatedTimestampUniqueness();
        return 1;
    }

    int upsertRawFruHost(String id, FruHost fruHost) throws IOException, ProcCallException {
        String source = gson.toJson(fruHost);
        ClientResponse cr = client.callProcedure("Raw_FRU_Host_Insert",
                id, fruHost.boardSerial, fruHost.mac, source, fruHost.timestamp);

        if (cr.getStatus() != ClientResponse.SUCCESS) {
            logger.error("upsertRawFruHost(id=%s) => %d", id, cr.getStatus());
            return 0;
        }
        guaranteeDbUpdatedTimestampUniqueness();
        return 1;
    }

    private void guaranteeDbUpdatedTimestampUniqueness() {
        try {
            Thread.sleep(2);    // ensures DbUpdatedTimestamp uniqueness
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    int insertNodeInventoryHistory(NodeInventory nodeInventory) throws IOException, ProcCallException {
        String source = gson.toJson(nodeInventory);
        ClientResponse cr = client.callProcedure("Raw_Node_Inventory_History_insert", source);

        if (cr.getStatus() != ClientResponse.SUCCESS) {
            logger.error("insertNodeInventoryHistory(source=%s) => %d", source, cr.getStatus());
            return 0;
        }
        guaranteeDbUpdatedTimestampUniqueness();
        return 1;
    }

    public List<FruHost> enumerateFruHosts() {
        try {
            ClientResponse cr = client.callProcedure("Get_FRU_Hosts");
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error("cr.getStatusString(): %d", cr.getStatusString());
                return null;
            }
            VoltTable tuples = cr.getResults()[0];
            logger.info("Number of FRUs = %s", tuples.getRowCount());
            tuples.resetRowPosition();
            ArrayList<FruHost> fruHosts = new ArrayList<>();
            long numberFrusEnumerated = 0;
            while (tuples.advanceRow()) {
                numberFrusEnumerated += 1;
                String source = tuples.getString(3);
                logger.debug("%d: %s%n", numberFrusEnumerated, source);
                fruHosts.add(gson.fromJson(source, FruHost.class));
            }
            return fruHosts;
        } catch (IOException | ProcCallException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public Map<String, String> getDimmJsonsOnFruHost(String fruHostMac) {
        try {
            ClientResponse cr = client.callProcedure("Get_Dimms_on_FRU_Host", fruHostMac);
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error(cr.getStatusString());
                return null;
            }
            VoltTable tuples = cr.getResults()[0];
            logger.info("Number of DIMM associated with %s = %d", fruHostMac, tuples.getRowCount());
            tuples.resetRowPosition();
            HashMap<String, String> dimmMap = new HashMap<>();
            int i = 0;
            while (tuples.advanceRow()) {
                i += 1;
                String locator = tuples.getString(3);
                String source = tuples.getString(4);

                logger.debug("%d: %s", i, source);
                dimmMap.put(locator, source);
            }
            return dimmMap;
        } catch (IOException | ProcCallException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public FruHost getFruHostByMac(String dimmMac) {
        try {
            ClientResponse cr = client.callProcedure("Get_FRU_Host_by_DIMM_Mac", dimmMac);
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error(cr.getStatusString());
                return null;
            }
            VoltTable tuples = cr.getResults()[0];
            int numberFruHostByMac = tuples.getRowCount();
            if (numberFruHostByMac != 1) {
                logger.error("Expected 1 FRU host associated with %s but has %d", dimmMac, numberFruHostByMac);
                return null;
            }
            tuples.resetRowPosition();
            if (tuples.advanceRow()) {
                String id = tuples.getString(0);
                String source = tuples.getString(3);
                long docTimestamp = tuples.getLong(4);
                logger.debug("id: %s, docTimestamp: %d, source: %s", id, docTimestamp, source);
                return gson.fromJson(source, FruHost.class);
            }
            return null;
        } catch (IOException | ProcCallException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private final static Gson gson = new Gson();
    private final Logger logger;
    private final String[] servers;
    private Client client = null;
}
