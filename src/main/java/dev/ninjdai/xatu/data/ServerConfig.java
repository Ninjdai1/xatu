package dev.ninjdai.xatu.data;

import discord4j.common.util.Snowflake;
import reactor.util.annotation.Nullable;

public class ServerConfig {
    public Snowflake server_id;
    public int fetch_cron;
    public String repo_name;
    public Snowflake channel_id;
    @Nullable public Snowflake second_channel_id;

    @Override
    public String toString() {
        return "ServerConfig{" +
                "server_id=" + server_id +
                ", fetch_cron=" + fetch_cron +
                ", repo_name='" + repo_name + '\'' +
                ", channel_id=" + channel_id +
                ", second_channel_id=" + second_channel_id +
                '}';
    }
}
