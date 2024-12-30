package dev.ninjdai.xatu.data;

import discord4j.core.spec.EmbedCreateSpec;

public record RepoData(String repo, long timestamp, EmbedCreateSpec embed, Details details) {
}
