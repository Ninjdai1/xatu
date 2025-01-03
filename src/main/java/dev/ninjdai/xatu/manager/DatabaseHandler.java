package dev.ninjdai.xatu.manager;

import dev.ninjdai.xatu.Main;
import dev.ninjdai.xatu.data.Details;
import dev.ninjdai.xatu.data.ServerConfig;
import dev.ninjdai.xatu.data.ServerMetadata;
import discord4j.common.util.Snowflake;
import reactor.util.annotation.NonNull;

import java.sql.*;
import java.util.ArrayList;

public class DatabaseHandler {
    public static Connection databaseConnection;

    public static void init(String url) {
        String github_data_query = """
            CREATE TABLE IF NOT EXISTS "github_data" (
                "timestamp"	INTEGER NOT NULL,
                "repo"	TEXT NOT NULL,
                "opened_pr_1"	INTEGER,
                "opened_pr_7"	INTEGER,
                "opened_pr_30"	INTEGER,
                "opened_pr_365"	INTEGER,
                "opened_pr_all"	INTEGER,
                "merged_pr_1"	INTEGER,
                "merged_pr_7"	INTEGER,
                "merged_pr_30"	INTEGER,
                "merged_pr_365"	INTEGER,
                "merged_pr_all"	INTEGER,
                "opened_issue_1"	INTEGER,
                "opened_issue_7"	INTEGER,
                "opened_issue_30"	INTEGER,
                "opened_issue_365"	INTEGER,
                "opened_issue_all"	INTEGER,
                "closed_issue_1"	INTEGER,
                "closed_issue_7"	INTEGER,
                "closed_issue_30"	INTEGER,
                "closed_issue_365"	INTEGER,
                "closed_issue_all"	INTEGER,
                PRIMARY KEY("timestamp","repo")
            );""";
        String discord_data_query = """
            CREATE TABLE IF NOT EXISTS "discord_data" (
                "server_id"	INTEGER NOT NULL UNIQUE,
                "fetch_cron"	INTEGER,
                "repo_name"	TEXT,
                "channel_id"	INTEGER,
                "second_channel_id"	INTEGER,
                PRIMARY KEY("server_id")
            ) WITHOUT ROWID;""";
        String metadata_query = """
            CREATE TABLE IF NOT EXISTS "metadata" (
                "server_id"	INTEGER NOT NULL UNIQUE,
                "current_version"	TEXT,
                "minor_release_timestamp"	INTEGER,
                "patch_release_timestamp"	INTEGER,
                PRIMARY KEY("server_id")
            ) WITHOUT ROWID;""";

        try {
            databaseConnection = DriverManager.getConnection(url);
            Statement stmt = databaseConnection.createStatement();
            stmt.execute(github_data_query);
            stmt.execute(discord_data_query);
            stmt.execute(metadata_query);
        } catch (SQLException e) {
            Main.LOGGER.error("Error initializing database", e);
        } finally {
            Main.LOGGER.info("Database initialized");
        }
    }

    public static void addServerMetadata(@NonNull ServerMetadata metadata) {
        ServerMetadata oldMetadata = getServerMetadata(metadata.server_id);
        if (oldMetadata == null) {
            String sql = "INSERT INTO metadata VALUES(?,?,?,?)";
            try (PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
                pstmt.setLong(1, metadata.server_id.asLong());
                pstmt.setString(2, metadata.current_version);

                if (metadata.minor_release_timestamp == null) pstmt.setNull(3, Types.INTEGER);
                else pstmt.setLong(3, metadata.minor_release_timestamp);

                if (metadata.patch_release_timestamp==null) pstmt.setNull(4, Types.INTEGER);
                else pstmt.setLong(4, metadata.patch_release_timestamp);

                pstmt.executeUpdate();
            } catch (SQLException e) {
                Main.LOGGER.error("Error adding server config to database", e);
            }
        } else {
            String sql = "UPDATE metadata SET current_version=?, minor_release_timestamp=?, patch_release_timestamp=? WHERE server_id=?";
            try (PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
                pstmt.setString(1, metadata.current_version == null ? oldMetadata.current_version : metadata.current_version);

                if (metadata.minor_release_timestamp == null) {
                    if (oldMetadata.minor_release_timestamp == null) pstmt.setNull(2, Types.NULL);
                    else pstmt.setLong(2, oldMetadata.minor_release_timestamp);
                } else pstmt.setLong(2, metadata.minor_release_timestamp);

                if (metadata.patch_release_timestamp == null) {
                    if (oldMetadata.patch_release_timestamp == null) pstmt.setNull(3, Types.NULL);
                    else pstmt.setLong(3, oldMetadata.patch_release_timestamp);
                } else pstmt.setLong(3, metadata.patch_release_timestamp);
                pstmt.setLong(4, metadata.server_id.asLong());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                Main.LOGGER.error("Error updating server config in database", e);
            }
        }
    }

    public static ServerMetadata getServerMetadata(Snowflake serverId) {
        String sql = "SELECT * FROM metadata WHERE server_id = ?";
        try(PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
            pstmt.setLong(1, serverId.asLong());
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                ServerMetadata metadata = new ServerMetadata();
                metadata.server_id = Snowflake.of(rs.getString("server_id"));
                metadata.current_version = rs.getString("current_version");
                metadata.minor_release_timestamp = rs.getLong("minor_release_timestamp");
                metadata.patch_release_timestamp = rs.getLong("patch_release_timestamp");
                return metadata;
            }
        } catch (SQLException e) {
            Main.LOGGER.error("Error reading server metadata from databases:", e);
        }
        return null;
    }

    public static void addServer(ServerConfig config) {
        ServerConfig oldConfig = getServer(config.server_id);
        if (oldConfig == null) {
            String sql = "INSERT INTO discord_data VALUES(?,?,?,?,?)";
            try (PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
                pstmt.setLong(1, config.server_id.asLong());
                pstmt.setInt(2, config.fetch_cron);
                pstmt.setString(3, config.repo_name);
                pstmt.setLong(4, config.channel_id.asLong());
                if (config.second_channel_id != null) pstmt.setLong(5, config.second_channel_id.asLong());
                else pstmt.setNull(5, Types.INTEGER);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                Main.LOGGER.error("Error adding server config to database", e);
            }
        } else {
            SchedulerManager.removeServerJob(oldConfig);
            String sql = "UPDATE discord_data SET fetch_cron=?, repo_name=?, channel_id=?, second_channel_id=? WHERE server_id=?";
            try (PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
                pstmt.setInt(1, config.fetch_cron);
                pstmt.setString(2, config.repo_name);
                pstmt.setLong(3, config.channel_id.asLong());
                pstmt.setLong(4, config.server_id.asLong());
                if (config.second_channel_id != null) pstmt.setLong(5, config.second_channel_id.asLong());
                else pstmt.setNull(5, Types.INTEGER);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                Main.LOGGER.error("Error updating server config in database", e);
            }
        }
        SchedulerManager.addServerJob(config);
    }

    public static ServerConfig getServer(Snowflake serverId) {
        String sql = "SELECT * FROM discord_data WHERE server_id = ?";
        try(PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
            pstmt.setLong(1, serverId.asLong());
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                ServerConfig config = new ServerConfig();
                config.server_id = Snowflake.of(rs.getString("server_id"));
                config.fetch_cron = rs.getInt("fetch_cron");
                config.repo_name = rs.getString("repo_name");
                config.channel_id = Snowflake.of(rs.getString("channel_id"));
                config.second_channel_id = Snowflake.of(rs.getString("second_channel_id"));
                return config;
            }
        } catch (SQLException e) {
            Main.LOGGER.error("Error reading server configuration from databases:", e);
        }
        return null;
    }

    public static ArrayList<ServerConfig> getServers() {
        String sql = "SELECT * FROM discord_data";
        ArrayList<ServerConfig> serverConfigs = new ArrayList<>();
        try(PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
            var rs = pstmt.executeQuery();
            while (rs.next()) {
                ServerConfig config = new ServerConfig();
                config.server_id = Snowflake.of(rs.getString("server_id"));
                config.fetch_cron = rs.getInt("fetch_cron");
                config.repo_name = rs.getString("repo_name");
                config.channel_id = Snowflake.of(rs.getString("channel_id"));
                config.second_channel_id = Snowflake.of(rs.getString("second_channel_id"));
                serverConfigs.add(config);
            }
        } catch (SQLException e) {
            Main.LOGGER.error("Error reading server configurations from databases:", e);
        }
        return serverConfigs;
    }

    public static void registerDetails(String repo, long timestamp, Details details) {
        String sql = "INSERT INTO github_data VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try(PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
            pstmt.setLong(1, timestamp);
            pstmt.setString(2, repo);

            pstmt.setInt(3, details.opened_pr_1);
            pstmt.setInt(4, details.opened_pr_7);
            pstmt.setInt(5, details.opened_pr_30);
            pstmt.setInt(6, details.opened_pr_365);
            pstmt.setInt(7, details.opened_pr_all);

            pstmt.setInt(8, details.merged_pr_1);
            pstmt.setInt(9, details.merged_pr_7);
            pstmt.setInt(10, details.merged_pr_30);
            pstmt.setInt(11, details.merged_pr_365);
            pstmt.setInt(12, details.merged_pr_all);

            pstmt.setInt(13, details.opened_issue_1);
            pstmt.setInt(14, details.opened_issue_7);
            pstmt.setInt(15, details.opened_issue_30);
            pstmt.setInt(16, details.opened_issue_365);
            pstmt.setInt(17, details.opened_issue_all);

            pstmt.setInt(18, details.closed_issue_1);
            pstmt.setInt(19, details.closed_issue_7);
            pstmt.setInt(20, details.closed_issue_30);
            pstmt.setInt(21, details.closed_issue_365);
            pstmt.setInt(22, details.closed_issue_all);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Main.LOGGER.error("Error registering details to database", e);
        }
    }

    public static Details getDetails(String repo, long timestamp) {
        var sql = "SELECT * FROM github_data WHERE repo = ? AND timestamp = ?";

        try (PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
            pstmt.setString(1, repo);
            pstmt.setLong(2, timestamp);

            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Details(
                    rs.getInt("opened_pr_1"),
                    rs.getInt("opened_pr_7"),
                    rs.getInt("opened_pr_30"),
                    rs.getInt("opened_pr_365"),
                    rs.getInt("opened_pr_all"),

                    rs.getInt("merged_pr_1"),
                    rs.getInt("merged_pr_7"),
                    rs.getInt("merged_pr_30"),
                    rs.getInt("merged_pr_365"),
                    rs.getInt("merged_pr_all"),

                    rs.getInt("opened_issue_1"),
                    rs.getInt("opened_issue_7"),
                    rs.getInt("opened_issue_30"),
                    rs.getInt("opened_issue_365"),
                    rs.getInt("opened_issue_all"),

                    rs.getInt("closed_issue_1"),
                    rs.getInt("closed_issue_7"),
                    rs.getInt("closed_issue_30"),
                    rs.getInt("closed_issue_365"),
                    rs.getInt("closed_issue_all")
                );
            }
        } catch (SQLException e) {
            Main.LOGGER.error("Error getting details from database", e);
        }
        return null;
    }
}
