package dev.ninjdai.xatu.data;

import discord4j.common.util.Snowflake;

public class ServerConfig {
    public Snowflake server_id;
    public String fetch_cron;
    public String repo_name;
    public Snowflake channel_id;

    public int big_feature_freeze_timestamp;
    public int merge_freeze_timestamp;
}
