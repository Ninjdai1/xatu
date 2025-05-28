package dev.ninjdai.xatu.interaction.command;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import reactor.core.publisher.Mono;

public abstract class SimpleTextCommand implements Command {
    @Override
    public ImmutableApplicationCommandRequest.Builder getCommandBuilder() {
        return ApplicationCommandRequest.builder().description(getName()[0]);
    }

    @Override
    public Mono<Void> execute(ApplicationCommandInteractionEvent event) {
        return event.reply(getMessageContent());
    }

    public abstract String getMessageContent();
}
