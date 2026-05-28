package com.cubrid.migraion.coradb;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class CoraDBMetaData {
	public static void main(String[] args) {
		String driver = "coradb.jdbc.driver.CORADBDriver";
		String url = "jdbc:CoraDB:192.168.3.131:33000:demodb:dba::";
		String userid = "dba";
		String password = "";

		try {
			Class.forName(driver);
			try {
				Connection conn = DriverManager.getConnection(url, userid, password);
				DatabaseMetaData metaData = conn.getMetaData();
	            
	            System.out.println("getDatabaseProductName: " + metaData.getDatabaseProductName());
	            System.out.println("getDatabaseProductVersion " + metaData.getDatabaseProductVersion());
	            System.out.println("--------------------------------------------------");

	            String[] types = {"TABLE", "VIEW"};

	            try (ResultSet rs = metaData.getTables(null, null, "%", null)) {
	                
	                while (rs.next()) {
	                    String tableCat   = rs.getString("TABLE_CAT");
	                    String tableSchem = rs.getString("TABLE_SCHEM");
	                    String tableName  = rs.getString("TABLE_NAME");
	                    String tableType  = rs.getString("TABLE_TYPE");
	                    String remarks    = rs.getString("REMARKS");
	                    System.out.printf("%-15s | %-15s | %-30s | %-10s%n", tableCat, tableSchem, tableName, tableType);
	                }
	            }
			} catch (Exception e) {
				System.out.println("SQLException:" + e.getMessage());
			}
		} catch (ClassNotFoundException e1) {
			System.out.println("Can't find driver:" + e1.getMessage());
			e1.printStackTrace();
		}
		
	}
}
