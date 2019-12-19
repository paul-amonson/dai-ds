// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.junit.Test;
import org.voltdb.Expectation;
import org.voltdb.SQLStmt;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class DiagGetDiagToolIdTest {
    class MockDiagGetDiagToolId extends DiagGetDiagToolId {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(new VoltTable.ColumnInfo("DiagToolId", VoltType.STRING),
                    new VoltTable.ColumnInfo("DiagListId", VoltType.STRING));
            result[0].addRow("tool_id", "list_id");
            return result;
        }
    }

    @Test
    public void run() {
        MockDiagGetDiagToolId proc = new MockDiagGetDiagToolId();
        proc.run("list_id");
    }
}