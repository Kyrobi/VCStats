package me.Kyrobi;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Objects;
public class DatabaseHandler {

    static String url;
    static String username;
    static String password;

    public static HikariDataSource dataSource;

    public DatabaseHandler() throws IOException {
        System.out.println("Calling constructor");
        Configurations configs = new Configurations();
        try
        {
            Configuration config = configs.properties(new File("config.properties"));
            url = config.getString("database.url");
            username = config.getString("database.username");
            password = config.getString("database.password");
        }
        catch (ConfigurationException cex)
        {
            String dataToWrite = """
                    database.url = jdbc:mysql://192.168.0.1:3306/kyrobi_myDatabase
                    database.username = Kyrobi
                    database.password = myAwesomePassword123
                    """;
            System.out.println("Error reading login credentials. Creating a new config file. Please update the credentials!");

            // Assume config.properties doesn't exist and create a new one and exit program
            FileWriter myWriter = new FileWriter("config.properties");
            myWriter.write(dataToWrite);
            myWriter.close();
            System.exit(1);
        }

        HikariConfig Hconfig = new HikariConfig();
        Hconfig.setLeakDetectionThreshold(6000);

        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        createNewTable();
    }

    public static void closeDatabaseConnectionPool(){
        dataSource.close();
        System.out.println("Closing database pool...");
    }

    public void createNewTable(){
        String create_stats_table = "CREATE TABLE IF NOT EXISTS stats (" +
                "userID BIGINT NOT NULL DEFAULT 0, " +
                "time BIGINT NOT NULL DEFAULT 0, " +
                "serverID BIGINT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (userID, serverID), " +
                "UNIQUE KEY unique_user_server (userID, serverID)" +
                ");";

//        String create_stats_table = "CREATE TABLE IF NOT EXISTS stats (" +
//                "userKey VARCHAR(255) NOT NULL, " +
//                "userID BIGINT NOT NULL DEFAULT 0, " +
//                "time BIGINT NOT NULL DEFAULT 0, " +
//                "serverID BIGINT NOT NULL DEFAULT 0, " +
//                "PRIMARY KEY (userKey)" +
//                ");";

        try(Connection conn = dataSource.getConnection()){
            Statement stmt = conn.createStatement(); // Formulate the command to execute
            stmt.execute(create_stats_table);  //Execute said command
        }
        catch (SQLException error){
            System.out.println(error.getMessage());
        }

        System.out.println("Database does not exist. Creating a new one at " + url);
    }

    //Insert a new value into the database
    public static void insert(long userID, long time, long serverID){

        String sqlcommand = "INSERT INTO stats (userID, serverID, time) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE time = time + ?";

        try(Connection conn = dataSource.getConnection()){
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
        try(Connection conn = dataSource.getConnection()){
            // PreparedStatement update = conn.prepareStatement("UPDATE stats SET time = ? WHERE userID = ? AND serverID = ?");

            PreparedStatement updateOrInsertStatement = conn.prepareStatement(
                    "INSERT INTO stats (userID, serverID, time) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE time = time + ?");

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

    //Checks to see if a user exists in the database
    public static Boolean exists(long userID, long serverID){
        // String to get all the values from the database
        int count = 0;

        try(Connection conn = dataSource.getConnection()){
            //System.out.println("Connecting...");
            PreparedStatement ifexists = conn.prepareStatement("SELECT * FROM stats WHERE userID = ? AND serverID = ?");

            ifexists.setLong(1, userID);
            ifexists.setLong(2, serverID);

            ResultSet rs = ifexists.executeQuery(); // Execute the command


            //We loop through the database. If the userID matches, we break out of the loop
            while(rs.next()){
                //System.out.println("ID: " + rs.getString("userId") + " Amount: " + rs.getInt("amount"));
                if(Objects.equals(rs.getLong("userID"), userID)){
                    ++count;
                    rs.close();
                    conn.close();
                    break; // Breaks out of the loop once the value has been found. No need to loop through the rest of the database
                }
            }
        }
        catch(SQLException e){
            e.printStackTrace();
            System.out.println("Error code: " + e.getMessage());
        }

        if(count != 0){
            return true;
        }
        else{
            return false;
        }
    }


    public static long getTime(long userId, long serverID){
        long amount = 0;

        try (Connection conn = dataSource.getConnection()){
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
