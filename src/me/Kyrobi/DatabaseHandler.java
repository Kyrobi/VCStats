package me.Kyrobi;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Objects;
public class DatabaseHandler {

    public static String CONNECTION_STRING = "jdbc:sqlite:";
    public static String DATABASE_FILE_NAME = "database.db";

    public DatabaseHandler() throws IOException {

        File file = new File(DATABASE_FILE_NAME);

        // Check if the file exists
        if (!file.exists()) {
            try {
                // Create the file if it doesn't exist
                if (file.createNewFile()) {
                    System.out.println("File created: " + file.getName());
                } else {
                    System.out.println("File creation failed.");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        } else {
            System.out.println("File already exists.");
        }

        CONNECTION_STRING = CONNECTION_STRING + DATABASE_FILE_NAME;

        createNewTable();
    }

    public void createNewTable(){
        String create_stats_table = "CREATE TABLE IF NOT EXISTS stats (" +
                "userID INTEGER NOT NULL DEFAULT 0, " +
                "time INTEGER NOT NULL DEFAULT 0, " +
                "serverID INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (userID, serverID), " +
                "UNIQUE (userID, serverID)" +
                ");";


        try(Connection conn = DriverManager.getConnection(CONNECTION_STRING)){
            Statement stmt = conn.createStatement(); // Formulate the command to execute
            stmt.execute(create_stats_table);  //Execute said command
        }
        catch (SQLException error){
            System.out.println(error.getMessage());
        }

        System.out.println("Database does not exist. Creating a new one at " + CONNECTION_STRING);
    }

    //Insert a new value into the database
    public static void insert(long userID, long time, long serverID){

        String sqlcommand = "INSERT INTO stats (userID, serverID, time) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT(userID, serverID) DO UPDATE SET time = time + ?;";

        try(Connection conn = DriverManager.getConnection(CONNECTION_STRING)){
            PreparedStatement stmt = conn.prepareStatement(sqlcommand);
            stmt.setLong(1, userID); // The first column will contain the ID
            stmt.setLong(2, serverID); // The second column will contain the amount
            stmt.setLong(3, time);
            stmt.setLong(4, time);
            stmt.executeUpdate();
            conn.close();
        }
        catch(SQLException error){
            System.out.println(error.getMessage());
        }
    }

    public static void bulkInsert(List<Long> userIDs, List<Long>times, List<Long> serverIDs){
        try(Connection conn = DriverManager.getConnection(CONNECTION_STRING)){
            // PreparedStatement update = conn.prepareStatement("UPDATE stats SET time = ? WHERE userID = ? AND serverID = ?");

            PreparedStatement updateOrInsertStatement = conn.prepareStatement(
                    "INSERT INTO stats (userID, serverID, time) " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT(userID, serverID) DO UPDATE SET time = time + ?;"
            );

            // Disable auto-commit to enable batch processing
            conn.setAutoCommit(false);


            for (int i = 0; i < userIDs.size(); i++){
                long userID = userIDs.get(i);
                long serverID = serverIDs.get(i);
                long time = times.get(i);

                updateOrInsertStatement.setLong(1, userID);
                updateOrInsertStatement.setLong(2, serverID);
                updateOrInsertStatement.setLong(3, time);
                updateOrInsertStatement.setLong(4, time);
                updateOrInsertStatement.addBatch();
            }

            // Execute the batch
            updateOrInsertStatement.executeBatch();


            // Commit the changes
            conn.commit();


            // Enable auto-commit again
            conn.setAutoCommit(true);

        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }


    public static long getTime(long userId, long serverID){
        long amount = 0;

        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)){
            //System.out.println("Connecting...");
            PreparedStatement getAmount = conn.prepareStatement("SELECT * FROM stats WHERE userID = ? AND serverID = ?");

            getAmount.setLong(1, userId);
            getAmount.setLong(2, serverID);

            ResultSet rs = getAmount.executeQuery(); // Used with prepared statement
            if(rs.next()){
                amount = rs.getLong("time");
            }
            rs.close();
            conn.close();
        }
        catch(SQLException se){
            System.out.println(se.getMessage());
        }
        return amount;
    }
}
