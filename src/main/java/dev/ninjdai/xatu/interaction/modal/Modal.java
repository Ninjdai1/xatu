package dev.ninjdai.xatu.interaction.modal;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import reactor.core.publisher.Mono;

public interface Modal {
    String getCustomId();
    Mono<Void> execute(ModalSubmitInteractionEvent event);
}
