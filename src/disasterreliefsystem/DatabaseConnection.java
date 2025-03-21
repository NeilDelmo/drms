/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package disasterreliefsystem;

/**
 *
 * @author Neil
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/disaster_relief";
    private static final String USER = "root";
    private static final String PASS = "";
    
    public static Connection getConnection(){
        Connection connection =  null;
        try{
            connection = DriverManager.getConnection(URL,USER,PASS);
            System.out.println("Database Connected Successfully");
            
        }catch(SQLException e){
            e.printStackTrace();
        }
        return connection;
        
    }
}
