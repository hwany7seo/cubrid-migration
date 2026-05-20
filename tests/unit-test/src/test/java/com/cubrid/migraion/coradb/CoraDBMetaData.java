package com.cubrid.migraion.coradb;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class CoraDBMetaData {
	public static void main(String[] args) {
		String driver = "coradb.jdbc.driver.CORADBDriver";
		String url = "jdbc:CoraDB:192.168.3.130:33000:mitdb:dba::";
		String userid = "dba";
		String password = "";

		try {
			Class.forName(driver);
			try {
				Connection conn = DriverManager.getConnection(url, userid, password);
				DatabaseMetaData metaData = conn.getMetaData();
	            
	            System.out.println("데이터베이스 제품명: " + metaData.getDatabaseProductName());
	            System.out.println("데이터베이스 버전: " + metaData.getDatabaseProductVersion());
	            System.out.println("--------------------------------------------------");

	            String[] types = {"TABLE", "VIEW"};

	            try (ResultSet rs = metaData.getTables(null, null, "%", types)) {
	                
	                while (rs.next()) {
	                    String tableCat   = rs.getString("TABLE_CAT");   // 카탈로그 (DB명)
	                    String tableSchem = rs.getString("TABLE_SCHEM"); // 스키마
	                    String tableName  = rs.getString("TABLE_NAME");  // 테이블/뷰 이름
	                    String tableType  = rs.getString("TABLE_TYPE");  // 타입 (TABLE, VIEW 등)
	                    String remarks    = rs.getString("REMARKS");     // 테이블에 대한 설명/코멘트
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
