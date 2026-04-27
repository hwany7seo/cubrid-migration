package com.cubrid.cubridmigration.graph;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.connection.IConnHelper;
import com.cubrid.cubridmigration.core.datatype.DBDataTypeHelper;
import com.cubrid.cubridmigration.core.dbtype.DBConstant;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.core.sql.SQLHelper;
import com.cubrid.cubridmigration.graph.export.GraphExportHelper;
import com.cubrid.cubridmigration.graph.meta.GraphSchemaFetcher;

public class GraphDatabase extends DatabaseType {

	public static int dbVersion;
	
	public GraphDatabase() {
		super(DBConstant.DBTYPE_CORADB,
				DBConstant.DB_NAMES[DBConstant.DBTYPE_CORADB],
				new String[] { DBConstant.JDBC_CLASS_CORADB },
				DBConstant.DEF_PORT_CUBRID, 
				new GraphSchemaFetcher(),
				new GraphExportHelper(), 
				new GraphConnHelper(), 
				true);
	}

	@Override
	public SQLHelper getSQLHelper(String version) {
		//GDB GraphDatabase getSQLHelper
		return GraphSQLHelper.getInstance(version);
	}

	@Override
	public DBDataTypeHelper getDataTypeHelper(String version) {
		//GDB GraphDatabase data type helper
		return null;
	}

	private static class GraphConnHelper implements IConnHelper {

		public String makeUrl(ConnParameters connParameters) {
			String cubridJdbcURLPattern = "jdbc:CoraDB:%s:%s:%s:::";
            String url =
                    String.format(
                            cubridJdbcURLPattern,
                            connParameters.getHost(),
                            connParameters.getPort(),
                            connParameters.getDbName());
            String charSet = connParameters.getCharset();
            if (StringUtils.isNotBlank(charSet)) {
                url += "?charset=" + charSet;
            }
            return url;
		}

		public Connection createConnection(ConnParameters conParam)
				throws SQLException {
			try {
                Driver driver = conParam.getDriver();
                if (driver == null) {
                    throw new RuntimeException("JDBC driver can't be null.");
                }
                // can't get connection throw DriverManger
                Properties props = new Properties();
                props.put("user", conParam.getConUser());
                props.put("password", conParam.getConPassword());
                props.put("charset", conParam.getCharset());
                Connection conn;
                if (StringUtils.isBlank(conParam.getUserJDBCURL())) {
                    conn = driver.connect(makeUrl(conParam), props);
                } else {
                    conn = driver.connect(conParam.getUserJDBCURL(), props);
                }
                if (conn == null) {
                    throw new SQLException("Can not connect database server.");
                }
                dbVersion =
                        conn.getMetaData().getDatabaseMajorVersion() * 10
                                + conn.getMetaData().getDatabaseMinorVersion();
                conn.setAutoCommit(false);
                return conn;
            } catch (SQLException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
		}
		
		private void checkDatabase(Connection conn) 
		        throws SQLException {
			try {
                DatabaseMetaData metaData = conn.getMetaData();
                if (metaData != null) {
                    String projectVersion= metaData.getDatabaseProductVersion();
                    if (projectVersion.equals("Unknown")) {
                        throw new SQLException("Unable to read database information., Please check name or all setting of database");
                    }
                }
            } catch (SQLException e) {
                throw e;
            }
		}
	}
}
