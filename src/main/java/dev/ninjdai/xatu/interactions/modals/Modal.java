package dev.ninjdai.xatu.interactions.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;

public interface Modal {
    String getCustomId();
    void execute(ModalSubmitInteractionEvent event);
}
