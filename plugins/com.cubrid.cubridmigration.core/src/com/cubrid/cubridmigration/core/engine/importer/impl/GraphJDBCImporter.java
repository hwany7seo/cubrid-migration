/*
 * Copyright (C) 2009 Search Solution Corporation. All rights reserved by Search Solution. 
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met: 
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer. 
 *
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution. 
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software without 
 *   specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE. 
 *
 */
package com.cubrid.cubridmigration.core.engine.importer.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.cubrid.common.log.LogUtil;
import com.cubrid.cubridmigration.core.common.Closer;
import com.cubrid.cubridmigration.core.common.DBUtils;
import com.cubrid.cubridmigration.core.dbobject.Column;
import com.cubrid.cubridmigration.core.dbobject.FK;
import com.cubrid.cubridmigration.core.dbobject.Grant;
import com.cubrid.cubridmigration.core.dbobject.Index;
import com.cubrid.cubridmigration.core.dbobject.PK;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlProcedure;
import com.cubrid.cubridmigration.core.dbobject.Record;
import com.cubrid.cubridmigration.core.dbobject.Record.ColumnValue;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.engine.JDBCConManager;
import com.cubrid.cubridmigration.core.engine.MigrationContext;
import com.cubrid.cubridmigration.core.engine.ThreadUtils;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.core.engine.config.SourceTableConfig;
import com.cubrid.cubridmigration.core.engine.event.ImportGraphRecordsEvent;
import com.cubrid.cubridmigration.core.engine.event.SingleRecordErrorEvent;
import com.cubrid.cubridmigration.core.engine.exception.JDBCConnectErrorException;
import com.cubrid.cubridmigration.core.engine.exception.NormalMigrationException;
import com.cubrid.cubridmigration.core.engine.importer.Importer;
import com.cubrid.cubridmigration.cubrid.stmt.CUBRIDParameterSetter;
import com.cubrid.cubridmigration.graph.GraphSQLHelper;
import com.cubrid.cubridmigration.graph.dbobj.Edge;
import com.cubrid.cubridmigration.graph.dbobj.Vertex;
import com.cubrid.cubridmigration.graph.stmt.GraphParameterSetter;

public class GraphJDBCImporter extends Importer {
    
    private static final Logger LOG = LogUtil.getLogger(GraphJDBCImporter.class);

    private final JDBCConManager connectionManager;
    private final MigrationConfiguration config;
    private final CUBRIDParameterSetter parameterSetter;
    private final GraphParameterSetter graphParameterSetter;
    private final GraphSQLHelper sqlHelper;

    public GraphJDBCImporter(MigrationContext mrManager) {
        super(mrManager);
        this.parameterSetter = mrManager.getParamSetter();
        this.graphParameterSetter = mrManager.getGraphParamSetter();
        this.config = mrManager.getConfig();
        this.connectionManager = mrManager.getConnManager();
        this.sqlHelper = GraphSQLHelper.getInstance(); 
    }

    @Override
    public void createVertex(Vertex v) {
        String sql = sqlHelper.getVertexDDL(v);
        try {
            executeDDL(sql);
            createObjectSuccess(v);
        } catch (RuntimeException e) {
            createObjectFailed(v, e);
            return;
        }
    }
    
    @Override
    public int importVertexs(Vertex v, List<Record> records) {
        int retryCount = 0;
        mrManager.getStatusMgr().addImpCount(v.getOwner(), v.getVertexLabel(), records.size());
        while (true) {
            try {
                return simpleVertexImportRecords(v, records);
            } catch (JDBCConnectErrorException ex) {
                if (retryCount < 3) {
                    retryCount++;
                    ThreadUtils.threadSleep(2000, eventHandler);
                } else {
                    eventHandler.handleEvent(new ImportGraphRecordsEvent(v, records.size(), ex, null));
                    return 0;
                }
            } catch (Exception e) {
                eventHandler.handleEvent(new ImportGraphRecordsEvent(v, records.size(), e, null));
                return 0;
            }
        }
    }
    
    @Override
    public void createEdge(Edge e) {
        String sql = GraphSQLHelper.getInstance(null).getEdgeDDL(e);
        try {
            executeDDL(sql);
            addTargetTableInConfig(e);
            createObjectSuccess(e);
        } catch (RuntimeException ex) {
            createObjectFailed(e, ex);
            return;
        }
    }

    @Override
    public int importEdges(Edge e, List<Record> records) {
        int retryCount = 0;
        while (true) {
            try {
                if (e.getEdgeType() == Edge.JOINTABLE_TYPE) {
                    return createJoinEdgeImport(e, records);
                }
                return createEdgeImport(e);
            } catch (JDBCConnectErrorException ex) {
                if (retryCount < 3) {
                    retryCount++;
                    ThreadUtils.threadSleep(2000, eventHandler);
                } else {
                    eventHandler.handleEvent(new ImportGraphRecordsEvent(e, e.getfkCol2RefMappingSize(), ex, null));
                    return 0;
                }
            } catch (Exception exception) {
                eventHandler.handleEvent(new ImportGraphRecordsEvent(e, e.getfkCol2RefMappingSize(), exception, null));
                return 0;
            }
        }
    }

    private int createEdgeImport(Edge e) throws SQLException {
        int result = 0;
        int resultTotal = 0;
        boolean prvAutoCommitStatus = false;
        Connection conn = connectionManager.getTargetConnection();
        if (conn.getAutoCommit()) {
            prvAutoCommitStatus = true;
            conn.setAutoCommit(false);
        }
        PreparedStatement stmt = null;
        try {
            for (int i=0 ; i < e.getfkCol2RefMappingSize(); i++) {
                String sql = sqlHelper.getTargetInsertEdge(e, i);
                stmt = conn.prepareStatement(sql);

                result = stmt.executeUpdate();
                resultTotal += result;

                DBUtils.commit(conn);
                if (resultTotal > 0) {
                    eventHandler.handleEvent(new ImportGraphRecordsEvent(e, resultTotal));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();

            if (isConnectionCutDown(ex)) {
                throw new JDBCConnectErrorException(ex);
            }
            DBUtils.rollback(conn);
            // If SQL has errors, write the records to a SQL files.
        } catch (Exception eee) {
            eee.printStackTrace();
        } finally {
            Closer.close(stmt);
            if (prvAutoCommitStatus) {
                conn.setAutoCommit(true);
            }
            connectionManager.closeTar(conn);
        }
        return resultTotal;
    }

    private int createJoinEdgeImport(Edge e, List<Record> records) throws SQLException {
        int result = 0;
        int resultTotal = 0;
        boolean prvAutoCommitStatus = false;
        Connection conn = connectionManager.getTargetConnection();
        if (conn.getAutoCommit()) {
            prvAutoCommitStatus = true;
            conn.setAutoCommit(false);
        }
        PreparedStatement stmt = null;
        String sql = sqlHelper.getTargetInsertJoinEdge(e);
        try {
            if (sql == null) {
                try {
                    Exception ex = new Exception("There is not a single supported column in the table.");
                    throw ex;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    eventHandler.handleEvent(new SingleRecordErrorEvent(null, ex));
                }
            }
            stmt = conn.prepareStatement(sql);

            for (Record rc : records) {
                if (rc == null) {
                    continue;
                }

                graphParameterSetter.setEdgeRecord2Statement(e, rc, stmt);

                result = stmt.executeUpdate();
                resultTotal += result;

                stmt.clearParameters();
            }
            DBUtils.commit(conn);
            if (resultTotal > 0) {
                eventHandler.handleEvent(new ImportGraphRecordsEvent(e, resultTotal));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();

            if (isConnectionCutDown(ex)) {
                throw new JDBCConnectErrorException(ex);
            }
            DBUtils.rollback(conn);
            // If SQL has errors, write the records to a SQL files.
        } catch (Exception eee) {
            eee.printStackTrace();
        } finally {
            Closer.close(stmt);
            if (prvAutoCommitStatus) {
                conn.setAutoCommit(true);
            }
            connectionManager.closeTar(conn);
        }
        return resultTotal;
    }

    /**
     * Import with no retry.
     * 
     * @param Vertex  Table List
     * @param records List<Record>
     * @return success record count
     * @throws SQLException when SQL error
     */
    private int simpleVertexImportRecords(Vertex v, List<Record> records) throws SQLException {
        // Auto commit is false by default.
        Connection conn = connectionManager.getTargetConnection(); // NOPMD
        boolean prvAutoCommitStatus = false;
        if (conn.getAutoCommit()) {
            prvAutoCommitStatus = true;
            conn.setAutoCommit(false);
        }
        PreparedStatement stmt = null; // NOPMD
        int result = 0;
        try {
            String sql = sqlHelper.getTargetInsertVertex(v);
            try {
                stmt = conn.prepareStatement(sql);

                if (sql == null) {
                    try {
                        Exception e = new Exception("There is not a single supported column in the table.");
                        throw e;
                    } catch (Exception e) {
                        eventHandler.handleEvent(new SingleRecordErrorEvent(null, e));
                    }
                }

                for (Record rc : records) {
                    if (rc == null) {
                        continue;
                    }
                    try {
                        Record trec = createTargetRecord(v, rc);
                        parameterSetter.setRecord2Statement(trec, stmt);
                        stmt.addBatch();
                    } catch (SQLException ex) {
                        ex.printStackTrace();

                        if (isConnectionCutDown(ex)) {
                            throw new JDBCConnectErrorException(ex);
                        }
                        eventHandler.handleEvent(new SingleRecordErrorEvent(rc, ex));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        eventHandler.handleEvent(new SingleRecordErrorEvent(rc, ex));
                    }
                }
                int[] exers = stmt.executeBatch();
                DBUtils.commit(conn);
                for (int rs : exers) {
                    result += rs;
                }
                if (result != records.size()) {
                    eventHandler.handleEvent(new ImportGraphRecordsEvent(v, records.size() - result,
                            new NormalMigrationException(ERROR_RECORD_MSG), null));
                }
                if (result > 0) {
                    eventHandler.handleEvent(new ImportGraphRecordsEvent(v, result));
                }
            } catch (SQLException ex) {

                ex.printStackTrace();

                if (isConnectionCutDown(ex)) {
                    throw new JDBCConnectErrorException(ex);
                }
                DBUtils.rollback(conn);
                // If SQL has errors, write the records to a SQL files.
                // String file = null;
                if (config.isWriteErrorRecords()) {
                    List<Record> errorRecords = new ArrayList<Record>();
                    for (Record rc : records) {
                        if (rc == null) {
                            continue;
                        }
                        Record trec = createTargetRecord(v, rc);
                        if (trec != null) {
                            errorRecords.add(trec);
                        }
                    }
                }
            }
        } finally {
            Closer.close(stmt);
            if (prvAutoCommitStatus) {
                conn.setAutoCommit(true);
            }
            connectionManager.closeTar(conn);
        }
        return result;
    }

    /**
     * If database connect is closed by server, it needs retry 5 times
     * 
     * @param ex the exception raised.
     * @return true:need retry.
     */
    private boolean isConnectionCutDown(SQLException ex) {
        String message = ex.getMessage();
        return message.indexOf("Connection or Statement might be closed") >= 0
                || message.indexOf("Cannot communicate with the broker") >= 0 || ex.getErrorCode() == -2019
                || ex.getErrorCode() == -21003 && ex.getErrorCode() == -2003;
    }
    
    private void addTargetTableInConfig(Edge e) {
        Table table = new Table();
        table.setName(e.getName());
        table.setColumns(e.getColumnList());
        config.addTargetTableSchema(table);
    }
    
    /**
     * Create a target record by source record
     * 
     * @param v    Vertex
     * @param rrec source record
     * @return Target record
     */
//      Record trec = new Record();
    private Record createTargetRecord(Vertex v, Record rrec) {
        ArrayList<Column> columnList = (ArrayList<Column>) v.getColumnList();
        ArrayList<ColumnValue> recordList = (ArrayList<ColumnValue>) rrec.getColumnValueList();
        ArrayList<String> colNameList = new ArrayList<String>();

        Record newRec = new Record();

        for (Column col : columnList) {
            if (col.isSelected()) {
                colNameList.add(col.getName());
            }
        }

        for (ColumnValue colVal : recordList) {
            if (colNameList.contains(colVal.getColumn().getName())) {
                newRec.addColumnValue(colVal);
            }
        }

        return newRec;
    }

    @Override
    public void executeDDL(String sql) {
        Connection conn = connectionManager.getTargetConnection();
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (Exception e) {
            throw new NormalMigrationException(e);
        } finally {
            DBUtils.commit(conn);
            Closer.close(stmt);
            connectionManager.closeTar(conn);
        }
    }

    @Override
    public void createFK(FK fk) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createPlcsqlFunctionHeader(PlcsqlFunction plcsqlFunction) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createPlcsqlFunctionBody(PlcsqlFunction plcsqlFunction) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createIndex(Index index) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createPK(PK pk) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createPlcsqlProcedureHeader(PlcsqlProcedure plcsqlProcedure) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createPlcsqlProcedureBody(PlcsqlProcedure plcsqlProcedure) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createSequence(Sequence sq) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createSynonym(Synonym sn) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createGrant(Grant gr) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createSchema(Schema schema) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createTable(Table table) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createView(View view) {
        // TODO Auto-generated method stub

    }

    @Override
    public void alterView(View view) {
        // TODO Auto-generated method stub

    }

    @Override
    public int importRecords(SourceTableConfig stc, List<Record> records) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int importQuickScript() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int importVertexsCsv(Vertex v) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int importEdgeCsv(Edge e) {
        // TODO Auto-generated method stub
        return 0;
    }
    
}