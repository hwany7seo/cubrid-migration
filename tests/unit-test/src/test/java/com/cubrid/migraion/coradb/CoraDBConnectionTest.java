package com.cubrid.migraion.coradb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CoraDBConnectionTest {
	public static void main(String[] args) {
		String driver = "coradb.jdbc.driver.CORADBDriver";
		String url = "jdbc:CoraDB:192.168.3.130:33000:demodb:dba::";
		String userid = "dba";
		String password = "";

		try {
			Class.forName(driver);
			try {
				Connection conn = DriverManager.getConnection(url, userid, password);
				String sql = "SELECT * FROM db_class";

				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery();
				
				while (rs.next()) {
	                String class_name = rs.getString("class_name"); 

	                System.out.println(" Class Name: " + class_name);
	            }

				conn.close();
			} catch (Exception e) {
				System.out.println("SQLException:" + e.getMessage());
			}
		} catch (ClassNotFoundException e1) {
			System.out.println("Can't find driver:" + e1.getMessage());
			e1.printStackTrace();
		}
	}
}
