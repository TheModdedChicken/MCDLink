package org.loganshaw.mcdlink.util.managers;

import org.loganshaw.mcdlink.MCDLink;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {
    Connection connection;
    private final MCDLink plugin;

    public DatabaseManager (MCDLink plugin) throws ClassNotFoundException, SQLException {
        this.plugin = plugin;

        File DBFile = new File(this.plugin.getDataFolder(), "database.db");
        if (!DBFile.exists()) {
            try {
                DBFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:plugins/" + this.plugin.getName() + "/database.db");


    }

    public void createTableLinking() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS links" + "(uuid varchar(36) PRIMARY KEY, nickname varchar(16), discord varchar(37), discord_id varchar(18), linking_date varchar(19))");
            preparedStatement.execute(); preparedStatement.close();
        } catch (SQLException e) {
            this.plugin.console.sendMessage(e.toString());
        }
    }
}
