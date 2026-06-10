package com.cubrid.cubridmigration.graph;

import java.util.List;

import com.cubrid.cubridmigration.core.dbobject.Column;
import com.cubrid.cubridmigration.core.dbobject.Index;
import com.cubrid.cubridmigration.core.dbobject.PK;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.sql.SQLHelper;
import com.cubrid.cubridmigration.cubrid.CUBRIDFormator;
import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.cubridmigration.graph.dbobj.Edge;
import com.cubrid.cubridmigration.graph.dbobj.Vertex;

public class GraphSQLHelper extends SQLHelper {
    private static final String NEWLINE = "\n";
    
    private List<Vertex> migratedVertexList;
    private List<Edge> migratedEdgeList;

    public List<Vertex> getMigratedVertexList() {
        return migratedVertexList;
    }

    public void setMigratedVertexList(List<Vertex> migratedVertexList) {
        this.migratedVertexList = migratedVertexList;
    }

    public List<Edge> getMigratedEdgeList() {
        return migratedEdgeList;
    }

    public void setMigratedEdgeList(List<Edge> migratedEdgeList) {
        this.migratedEdgeList = migratedEdgeList;
    }

    private final static GraphSQLHelper HELPER = new GraphSQLHelper();

    public static GraphSQLHelper getInstance(String version) {
        return HELPER;
    }
    
    public static GraphSQLHelper getInstance() {
        return HELPER;
    }

    public String getVertexDDL(Vertex v) {
        String ddl = getCreateVertex(v);
        return ddl;
    }

    public String getEdgeDDL(Edge e) {
        String ddl = new String();
        ddl = getCreateEdge(e);
        return ddl;
    }
    
    public String getTargetInsertVertex(Vertex v) {
        int supportColumCount = 0;
        StringBuffer buffer = new StringBuffer("INSERT INTO ");
        buffer.append(getQuotedObjName(v.getVertexLabel())).append(" VALUES (");
        
        List<Column> columns = v.getGraphColumnList();
        int len = columns.size();
        for (int i = 0; i < len; i++) {

            if (!columns.get(i).isSelected()) {
                continue;
            }

            supportColumCount++;

            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append('?');
        }

        if (supportColumCount == 0) {
            return null;
        }

        buffer.append(")");
        return buffer.toString();
    }
    
    public String getTargetInsertEdge(Edge e, int fkIdx) {
        List<String> fkColumns = e.getFKColumnNames();
        if (fkIdx >= fkColumns.size()) {
            return null;
        }
        String fkColName = fkColumns.get(fkIdx);
        String refColName = e.getREFColumnNames(fkColName);
        if (fkColName == null || refColName == null
                || e.getStartVertexName() == null || e.getEndVertexName() == null) {
            return null;
        }

        StringBuffer buf = new StringBuffer("INSERT EDGE INTO ").append(getQuotedObjName(e.getEdgeLabel()));
        buf.append(" SELECT n, m FROM ").append(getQuotedObjName(e.getStartVertexName()));
        buf.append(" n JOIN ").append(getQuotedObjName(e.getEndVertexName())).append(" m ON");
        buf.append(" n.").append(getQuotedObjName(fkColName)).append(" =");
        buf.append(" m.").append(getQuotedObjName(refColName));

        return buf.toString();
    }

    public String getTargetInsertJoinEdge(Edge e) {
        StringBuffer buffer = new StringBuffer();
        int fkIndex = 0;
        if (e.getStartVertexName().equals(e.getEndVertexName())) {
            fkIndex = 1;
        }
        
        buffer.append("INSERT EDGE");
        buffer.append(" FROM (");
        buffer.append(" SELECT ").append(getQuotedObjName(e.getStartVertexName()));
        buffer.append(" FROM ").append(getQuotedObjName(e.getStartVertexName()));
        buffer.append(" WHERE ").append(getQuotedObjName(e.getREFColumnNames(e.getFKColumnNames().get(fkIndex)))).append(" = ?)");
        
        buffer.append(" TO (");
        buffer.append(" SELECT ").append(getQuotedObjName(e.getEndVertexName()));
        buffer.append(" FROM ").append(getQuotedObjName(e.getEndVertexName()));
        buffer.append(" WHERE ").append(getQuotedObjName(e.getREFColumnNames(e.getFKColumnNames().get(1)))).append(" = ?)");
        
        buffer.append(" INTO ").append(getQuotedObjName(e.getEdgeLabel())).append(" VALUES (");
        
        if (e.getGraphColumnList() != null) {

            for (int i = 0; i < e.getGraphColumnList().size(); i++) {
                buffer.append('?');

                if (i < e.getGraphColumnList().size() - 1) {
                    buffer.append(", ");
                }
            }
        }

        buffer.append(")");
        return buffer.toString();
    }

    private String getCreateVertex(Vertex v) {
        StringBuffer buffer = new StringBuffer("CREATE VERTEX TABLE ");
        buffer.append(getQuotedObjName(v.getVertexLabel()));
        
        List<Column> columns = v.getGraphColumnList();
        int len = columns.size();
        for (int i = 0; i < len; i++) {
            Column column = columns.get(i);

            if (i > 0) {
                buffer.append(",").append(NEWLINE);
            } else {
                buffer.append(" (").append(NEWLINE);
            }

            buffer.append(getColumnDDL(column)).append(' ');
        }
        buffer.append(")");
        return buffer.toString();

    }

    private String getCreateEdge(Edge e) {
        StringBuffer buffer = new StringBuffer("CREATE EDGE TABLE ");
        buffer.append(getQuotedObjName(e.getEdgeLabel()));
        List<Column> columns = e.getGraphColumnList();
        int len = columns.size();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                Column column = columns.get(i);

                if (i > 0) {
                    buffer.append(",").append(NEWLINE);
                } else {
                    buffer.append(" (");
                }

                buffer.append(getColumnDDL(column)).append(' ');

            }
            buffer.append(")");
        }
        
        return buffer.toString();
    }
    
    private String getColumnDDL(Column column) {
        StringBuffer bf = new StringBuffer();
        bf.append(getQuotedObjName(column.getName()));
        bf.append(" ").append(column.getShownDataType());

        boolean autoInc = column.isAutoIncrement();
        String value = column.getDefaultValue();
        if (autoInc) {
            bf.append(" AUTO_INCREMENT");

            final Long autoIncSeedVal = column.getAutoIncSeedVal();
            Long autoIncIncVal = column.getAutoIncIncrVal();
            autoIncIncVal = autoIncIncVal == null ? 1 : autoIncIncVal;
            if (autoIncSeedVal > 0) {
                bf.append(" (");
                bf.append(autoIncSeedVal);
                bf.append(",");
                bf.append(autoIncIncVal);
                bf.append(")");
            }
        } else if (column.isShared()) {
            String defaultv =
                    CUBRIDFormator.format(column.getDataTypeInstance(), column.getSharedValue())
                            .getFormatResult();
            bf.append(" SHARED ").append(defaultv);
        } else if ((column.getDataType().equals("datetime")
                        || column.getDataType().equals("timestamp"))
                && CUBRIDTimeUtil.validateDateTimeFunction(value)) {
            bf.append(" DEFAULT ").append(value);
        } else {
            String defaultv = column.getDefaultValue();
            if (!column.isDefaultIsExpression()) {
                defaultv =
                        CUBRIDFormator.format(
                                        column.getDataTypeInstance(), column.getDefaultValue())
                                .getFormatResult();
            }
            if (defaultv != null) {
                bf.append(" DEFAULT ").append(defaultv);
            }
        }
        // add for bug484
        
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            bf.append(" COMMENT \'" + column.getComment() + "\'");
        }
//        if (column.getTableOrView() instanceof Table) {
//            boolean flag = false;
//            for (Index idx : ((Table) column.getTableOrView()).getIndexes()) {
//                if (!idx.isUnique()) {
//                    continue;
//                }
//                if (idx.getColumnNames().indexOf(column.getName()) >= 0) {
//                    flag = true;
//                }
//            }
//            if (!flag) {
//                bf.append(" UNIQUE ");
//            }
//        }

        return bf.toString();
    }
    
    @Override
    public String getQuotedObjName(String objectName) {
        return new StringBuffer("[").append(objectName).append("]").toString();
    }

    @Override
    public String getTestSelectSQL(String sql) {
        return sql;
    }
}
