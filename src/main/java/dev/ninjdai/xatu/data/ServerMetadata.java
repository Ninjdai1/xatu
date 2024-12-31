package dev.ninjdai.xatu.data;

import discord4j.common.util.Snowflake;
import reactor.util.annotation.Nullable;

public class ServerMetadata {
    public Snowflake server_id;
    @Nullable public String current_version;
    @Nullable public Long minor_release_timestamp;
    @Nullable public Long patch_release_timestamp;

    @Override
    public String toString() {
        return "ServerMetadata{" +
                "server_id=" + server_id +
                ", current_version='" + current_version + '\'' +
                ", minor_release_timestamp=" + minor_release_timestamp +
                ", patch_release_timestamp=" + patch_release_timestamp +
                '}';
    }
}
