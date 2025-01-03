package dev.ninjdai.xatu.interaction.modal;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;

public interface Modal {
    String getCustomId();
    void execute(ModalSubmitInteractionEvent event);
}
