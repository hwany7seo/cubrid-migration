package com.cubrid.cubridmigration.graph;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cubrid.cubridmigration.core.dbobject.Column;
import com.cubrid.cubridmigration.core.dbobject.Record;
import com.cubrid.cubridmigration.core.sql.SQLHelper;
import com.cubrid.cubridmigration.graph.dbobj.Edge;
import com.cubrid.cubridmigration.graph.dbobj.Vertex;

public class GraphSQLHelper extends SQLHelper {
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

    public String getVertexDDL(Vertex v) {
        String ddl = getCreateVertex(v);
        return ddl;
    }

    public String getVertexInsert(Vertex v) {
        String ddl = getTargetInsertVertex(v);
        return ddl;
    }

    public String getEdgeDDL(Edge e) {
        String ddl = new String();
        ddl = getCreateEdge(e);
        return ddl;
    }

    public String getEdgeInsert(Edge e, int index) {
        String ddl = new String();
        ddl = getTargetInsertEdge(e, index);
        return ddl;
    }

    public String getEdgeInsert(Edge e) {
        String ddl = new String();
        if (e.getEdgeType() == Edge.JOINTABLE_TYPE) {
            ddl = getTargetInsertJoinEdge(e);
        }
        return ddl;
    }

    private String getCreateVertex(Vertex v) {
        StringBuffer buffer = new StringBuffer("CREATE VERTEX TABLE ");
        buffer.append(getQuotedObjName(v.getVertexLabel())).append(" (");
        List<Column> columns = v.getColumnList();
        int len = columns.size();
        for (int i = 0; i < len; i++) {
            String columnName = columns.get(i).getName();
            String columnType = columns.get(i).getDataType();

            if (i > 0) {
                buffer.append(", ");
            }

            buffer.append(columnName).append(' ');
            buffer.append(columnType);

        }
        buffer.append(")");
        return buffer.toString();

    }

    private String getTargetInsertVertex(Vertex v) {
        int supportColumCount = 0;
        StringBuffer buffer = new StringBuffer("INSERT VERTEX INTO ").append(v.getVertexLabel()).append(" VALUES (");
        List<Column> columns = v.getColumnList();
        int len = columns.size();
        for (int i = 0; i < len; i++) {

            if (!columns.get(i).isSelected()) {
                continue;
            }

            supportColumCount++;

            if (i > 0) {
                buffer.append(", ");
            }
            String columnName = columns.get(i).getName();
            columnName = columnName.replaceAll("\"", "");
            buffer.append(columnName).append(':');

            buffer.append('?');
        }

        if (supportColumCount == 0) {
            return null;
        }

        buffer.append("}");
        buffer.append(")");
        buffer.append(" return n");
        return buffer.toString();
    }

    private String getCreateEdge(Edge e) {
        StringBuffer buffer = new StringBuffer("CREATE EDGE TABLE ");
        buffer.append(getQuotedObjName(e.getEdgeLabel()));
        List<Column> columns = e.getColumnList();
        int len = columns.size();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                String columnName = columns.get(i).getName();
                String columnType = columns.get(i).getDataType();

                if (i > 0) {
                    buffer.append(", ");
                } else {
                    buffer.append(" (");
                }

                buffer.append(columnName).append(' ');
                buffer.append(columnType);

            }
            buffer.append(")");
        }
        
        return buffer.toString();
    }

    private String getTargetInsertEdge(Edge e, int index) {
        StringBuffer buffer = new StringBuffer("MATCH (n:").append(e.getStartVertexName()).append("),");
        buffer.append("(m:").append(e.getEndVertexName()).append(")");
        buffer.append(" where ");
        buffer.append("n.").append(e.getFKColumnNames().get(index)).append(" = ");
        buffer.append("m.").append(e.getREFColumnNames(e.getFKColumnNames().get(index))).append(" ");
        buffer.append("create (n)-[r:").append(e.getEdgeLabel()).append("]->(m) return count(r)");

        return buffer.toString();
    }

    private String getTargetInsertJoinEdge(Edge e) {

        StringBuffer buffer = new StringBuffer();

        return buffer.toString();
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
