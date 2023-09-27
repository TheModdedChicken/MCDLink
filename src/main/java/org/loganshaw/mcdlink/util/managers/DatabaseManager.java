package org.loganshaw.mcdlink.util.managers;

import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.util.PUID;
import org.loganshaw.mcdlink.util.PlayerLink;
import org.loganshaw.mcdlink.util.enums.PlatformType;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

public class DatabaseManager {
    Connection connection;
    private final MCDLink plugin;
    private final Logger logger;
    private final String links_table = "links";

    public DatabaseManager (MCDLink plugin) throws ClassNotFoundException, SQLException {
        this.plugin = plugin;
        this.logger = plugin.logger;

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

        createTableLinking();
    }

    public void createTableLinking() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS "
                    + links_table
                    + "(discord_id INTEGER UNIQUE PRIMARY KEY, java_uuid TEXT, bedrock_uuid TEXT, created DATE)"
            );
            preparedStatement.execute(); preparedStatement.close();
        } catch (SQLException e) {
            this.plugin.console.sendMessage(e.toString());
        }
    }

    public void setLink (PlayerLink link) {
        PlayerLink linkCheck = getPlayerLinkFromDiscordID(link.discordID);
        if (linkCheck == null) createLink(link);
        else if (link.javaUUID != null || link.bedrockUUID != null) updateLinkData(link.discordID, link);
        else removeLink(link.discordID);
    }

    public void createLink(PlayerLink link) {
        PreparedStatement statement = null;

        try {
            statement = this.connection.prepareStatement("INSERT INTO " + links_table + "(discord_id, java_uuid, bedrock_uuid, created) VALUES (?, ?, ?, ?)");
            statement.setLong(1, link.discordID);
            statement.setString(2, link.javaUUID != null ? link.javaUUID.toString() : null);
            statement.setString(3, link.bedrockUUID != null ? link.bedrockUUID.toString() : null);
            statement.setDate(4, new Date(new java.util.Date().getTime()));
            statement.executeUpdate(); statement.close();
        } catch (SQLException err) {
            this.logger.severe(err.getMessage());
        }
    }

    public void updateLinkData (long discordID, PlayerLink link) {
        PreparedStatement statement = null;

        try {
            statement = this.connection.prepareStatement("UPDATE " + links_table+ " SET discord_id=?, java_uuid=?, bedrock_uuid=? WHERE discord_id=?");
            statement.setLong(1, link.discordID);
            statement.setString(2, link.javaUUID != null ? link.javaUUID.toString() : null);
            statement.setString(3, link.bedrockUUID != null ? link.bedrockUUID.toString() : null);
            statement.setLong(4, link.discordID);
            statement.executeUpdate(); statement.close();
        } catch (SQLException err) {
            this.logger.severe(err.getMessage());
        }
    }

    public void removeLink(long discordID) {
        PreparedStatement statement = null;

        try {
            statement = this.connection.prepareStatement("DELETE FROM " + links_table +" WHERE discord_id=?");
            statement.setLong(1, discordID);
            statement.executeUpdate(); statement.close();
        } catch (SQLException err) {
            this.logger.severe(err.getMessage());
        }
    }

    public PlayerLink getPlayerLinkFromPUID (PUID puid) {
        return puid.platform == PlatformType.JAVA
                ? this.plugin.databaseManager.getPlayerLinkFromJavaUUID(puid.uuid)
                : this.plugin.databaseManager.getPlayerLinkFromBedrockUUID(puid.uuid);
    }

    public PlayerLink getPlayerLinkFromDiscordID (long discordID) { return getPlayerLinkFromField("discord_id", discordID); }
    public PlayerLink getPlayerLinkFromJavaUUID (UUID javaUUID) { return getPlayerLinkFromField("java_uuid", javaUUID.toString()); }
    public PlayerLink getPlayerLinkFromBedrockUUID (UUID bedrockUUID) { return getPlayerLinkFromField("bedrock_uuid", bedrockUUID.toString()); }

    private PlayerLink getPlayerLinkFromField (String field, Object value) {
        PreparedStatement statement = null;
        try {
            statement = this.connection.prepareStatement("SELECT * FROM " + links_table + " WHERE " + field + "=?");
            statement.setObject(1, value);
            ResultSet resultSet = statement.executeQuery();

            return getPlayerLinkFromResultSet(resultSet);
        } catch (SQLException err) {
            this.logger.severe(err.getMessage());
        }

        return null;
    }

    private PlayerLink getPlayerLinkFromResultSet(ResultSet result) {
        try {
            long discordID = result.getLong("discord_id");
            String javaUUID = result.getString("java_uuid");
            String bedrockUUID = result.getString("bedrock_uuid");
            Date created = result.getDate("created");

            if (result.next()) return new PlayerLink(
                    discordID,
                    javaUUID != null ? UUID.fromString(javaUUID) : null,
                    bedrockUUID != null ? UUID.fromString(bedrockUUID) : null,
                    created
            );

            result.close(); result.close();
        } catch (SQLException err) {
            this.logger.severe(err.getMessage());
        }

        return null;
    }
}
