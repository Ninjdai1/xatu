package dev.ninjdai.xatu.data;

import discord4j.core.spec.MessageCreateSpec;

public record RepoData(String repo, long timestamp, MessageCreateSpec embed, Details details) {
}
