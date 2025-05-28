package dev.ninjdai.xatu.interaction.command;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Mono;

public abstract class SimpleButtonCommand extends SimpleTextCommand {
    @Override
    public Mono<Void> execute(ApplicationCommandInteractionEvent event) {
        return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .content(getMessageContent())
                        .components(getButtons())
                .build());
    }

    public abstract ActionRow getButtons();
}
