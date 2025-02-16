package dev.ninjdai.xatu.interaction.command;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public interface Command {
    ApplicationCommandRequest getCommand();
    Mono<Void> execute(ApplicationCommandInteractionEvent event);

    int SUBCOMMAND_OPTION_TYPE = 1;
    int STRING_OPTION_TYPE = 3;
    int INTEGER_OPTION_TYPE = 4;
    int BOOLEAN_OPTION_TYPE = 5;
    int USER_OPTION_TYPE = 6;
    int CHANNEL_OPTION_TYPE = 7;
    int ROLE_OPTION_TYPE = 8;

    int GUILD_TEXT_CHANNEL_TYPE = 0;
}
