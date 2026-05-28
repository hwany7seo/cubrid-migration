package com.cubrid.cubridmigration.graph;

import java.util.List;

import com.cubrid.cubridmigration.core.dbobject.Column;
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

    public String getEdgeDDL(Edge e) {
        String ddl = new String();
        ddl = getCreateEdge(e);
        return ddl;
    }

    private String getCreateVertex(Vertex v) {
        StringBuffer buffer = new StringBuffer("CREATE VERTEX TABLE ");
        buffer.append(getQuotedObjName(v.getVertexLabel())).append(" (");
        buffer.append(v.getUniqueIDName());
        buffer.append(" BIGINT ");
        buffer.append(" AUTO_INCREMENT PRIMARY KEY");
        
        List<Column> columns = v.getColumnList();
        int len = columns.size();
        for (int i = 0; i < len; i++) {
            String columnName = columns.get(i).getName();
            String columnType = columns.get(i).getDataType();
            
            buffer.append(", ");

            buffer.append(columnName).append(' ');
            buffer.append(columnType);
        }
        buffer.append(")");
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

    @Override
    public String getQuotedObjName(String objectName) {
        return new StringBuffer("[").append(objectName).append("]").toString();
    }

    @Override
    public String getTestSelectSQL(String sql) {
        return sql;
    }
}
