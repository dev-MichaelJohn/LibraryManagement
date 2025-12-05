package config;

import java.sql.*;
import java.sql.DriverManager;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Singleton class for managing database connections and executing queries.
 * 
 * @author Darkuz69
 */
public class DatabaseConnection implements AutoCloseable {
    private final String URL = "jdbc:mysql://localhost:3306/test";
    private final String User = "root";
    private final String Password = "";
    private static Connection connection;

    private static DatabaseConnection instance;

    private DatabaseConnection() {
        try {
            connection = DriverManager.getConnection(this.URL, this.User, this.Password);
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to database: " + e.getMessage());
        }
    }

    /**
     * Retrieves the singleton instance of DatabaseConnection.
     * 
     * @return The DatabaseConnection instance.
     */
    public static synchronized DatabaseConnection GetInstance() {
        if(instance == null) instance = new DatabaseConnection();
        return instance;
    }

    /**
     * Creates a prepared statement with the given SQL and parameters.
     * 
     * @param statement The SQL statement.
     * @param args The parameters for the SQL statement.
     * @return The prepared statement.
     * @throws RuntimeException if statement creation fails.
     */
    private static PreparedStatement CreateStatement(String statement, Object... args) {
        if(statement == null || statement.trim().isEmpty()) return null;

        try {
            PreparedStatement newStatement = connection.prepareStatement(statement);
            if(newStatement == null) return null;
            
            if(args.length > 0) {
                for(int i = 0; i < args.length; i++) {
                    newStatement.setObject(i + 1, args[i]);
                }
            }

            return newStatement;
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create prepared statement: " + e.getMessage());
        }
    }

    /**
     * Extracts results from a ResultSet into a list of maps.
     * 
     * @param resultSet The ResultSet to extract data from.
     * @return A list of maps representing the rows in the ResultSet.
     * @throws RuntimeException if result extraction fails.
     */
    private static List<Map<String, Object>> ExtractResult(ResultSet resultSet) {
        if(resultSet == null) return null;
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            if(metaData == null) return null;

            int columnCount = metaData.getColumnCount();
            while(resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for(int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), resultSet.getObject(i));
                }
                results.add(row);
            }
            
            return results;
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to extract results: " + e.getMessage());
        }
    }

    /**
     * Executes a query and returns the results.
     * Useful for SELECT statements.
     * 
     * @param statement The SQL query to execute.
     * @param args The parameters for the SQL query.
     * @return A list of maps representing the query results.
     * @throws RuntimeException if query execution fails.
     */
    public List<Map<String, Object>> ExecuteQuery(String statement, Object... args) {
        PreparedStatement newStatement = CreateStatement(statement, args);
        if(newStatement == null) return null;

        ResultSet resultSet = null;
        try {
            resultSet = newStatement.executeQuery();
            List<Map<String, Object>> extractedResults = ExtractResult(resultSet);

            return extractedResults;
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute query: " + e.getMessage());
        } finally {
            try {
                if(newStatement != null) newStatement.close();
                if(resultSet != null) resultSet.close();
            } catch(SQLException ex) {
                ex.printStackTrace();
                throw new RuntimeException("Failed to close resources: " + ex.getMessage());
            }
        }
    }

    /**
     * Executes an update operation.
     * Useful for INSERT, UPDATE, DELETE statements.
     * 
     * @param statement The SQL statement to execute.
     * @param args The parameters for the SQL statement.
     * @return The number of rows affected, or -1 if an error occurred.
     * @throws RuntimeException if update execution fails.
     */
    public int ExecuteUpdate(String statement, Object... args) {
        PreparedStatement newStatement = CreateStatement(statement, args);

        try {
            connection.setAutoCommit(false);
            int rowsAffected = newStatement.executeUpdate();
            
            connection.commit();

            if(rowsAffected == -1) return -1;
            return rowsAffected;
        } catch(SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch(SQLException ex) {
                ex.printStackTrace();
                throw new RuntimeException("Failed to rollback transaction: " + ex.getMessage());
            }
            
            throw new RuntimeException("Failed to execute update: " + e.getMessage());
        } finally {
            try {
                if(newStatement != null) newStatement.close();
                connection.setAutoCommit(true);
            } catch(SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to close resources: " + e.getMessage());
            }
        }
    }

    /**
     * Closes the database connection.
     */
    @Override
    public void close() {
        try {
            if(connection == null || connection.isClosed()) return;
            connection.close();
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to close database connection: " + e.getMessage());
        }
    }
}
