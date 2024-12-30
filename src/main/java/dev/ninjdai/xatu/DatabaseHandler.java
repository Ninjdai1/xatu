package dev.ninjdai.xatu;

import dev.ninjdai.xatu.data.Details;
import dev.ninjdai.xatu.data.ServerConfig;
import discord4j.common.util.Snowflake;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
                "fetch_cron"	TEXT,
                "repo_name"	TEXT,
                "channel_id"	INTEGER,
                "big_feature_freeze_timestamp"	INTEGER,
                "merge_freeze_timestamp"	INTEGER,
                PRIMARY KEY("server_id")
            ) WITHOUT ROWID;""";

        try {
            databaseConnection = DriverManager.getConnection(url);
            Statement stmt = databaseConnection.createStatement();
            stmt.execute(github_data_query);
            stmt.execute(discord_data_query);
        } catch (SQLException e) {
            Main.LOGGER.error("Error initializing database", e);
        } finally {
            Main.LOGGER.info("Database initialized");
        }
    }

    public static ArrayList<ServerConfig> getServers() {
        String sql = "SELECT * FROM discord_data";
        ArrayList<ServerConfig> serverConfigs = new ArrayList<>();
        try(PreparedStatement pstmt = databaseConnection.prepareStatement(sql)) {
            var rs = pstmt.executeQuery();
            while (rs.next()) {
                ServerConfig config = new ServerConfig();
                config.server_id = Snowflake.of(rs.getString("server_id"));
                config.fetch_cron = rs.getString("fetch_cron");
                config.repo_name = rs.getString("repo_name");
                config.channel_id = Snowflake.of(rs.getString("server_id"));
                config.big_feature_freeze_timestamp = rs.getInt("big_feature_freeze_timestamp");
                config.merge_freeze_timestamp = rs.getInt("merge_freeze_timestamp");
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
