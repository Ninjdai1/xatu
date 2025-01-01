package dev.ninjdai.xatu;

import dev.ninjdai.xatu.interactions.commands.Command;
import dev.ninjdai.xatu.interactions.commands.ConfigCommand;
import dev.ninjdai.xatu.interactions.commands.ReleaseCommand;
import dev.ninjdai.xatu.interactions.commands.TriggerCommand;
import dev.ninjdai.xatu.interactions.modals.ConfigModal;
import dev.ninjdai.xatu.interactions.modals.Modal;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;

import java.util.HashMap;
import java.util.Map;

public class InteractionHandler {
    private static final Map<String, Command> COMMANDS = new HashMap<>();
    private static final Map<String, Modal> MODALS = new HashMap<>();
    public static void init() {
        long applicationId = Main.DISCORD_CLIENT.getApplicationId().block();

        COMMANDS.put("config", new ConfigCommand());
        COMMANDS.put("release", new ReleaseCommand());
        COMMANDS.put("trigger", new TriggerCommand());
        //MODALS.put("config", new ConfigModal());

        for (Command command: COMMANDS.values()) {
            Main.DISCORD_CLIENT.getApplicationService()
                .createGlobalApplicationCommand(applicationId, command.getCommand())
                .subscribe();
        }
    }

    public static void execute(ApplicationCommandInteractionEvent event) {
        if (COMMANDS.containsKey(event.getCommandName())) {
            COMMANDS.get(event.getCommandName()).execute(event);
        } else {
            event.reply("Command not found, please contact the developers").withEphemeral(true).subscribe();
        }
    }

    public static void execute(ModalSubmitInteractionEvent event) {
        if (MODALS.containsKey(event.getCustomId())) {
            MODALS.get(event.getCustomId()).execute(event);
        } else {
            event.reply("Modal not found, please contact the developers").withEphemeral(true).subscribe();
        }
    }
}
