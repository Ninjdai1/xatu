package dev.ninjdai.xatu.manager;

import dev.ninjdai.xatu.Main;
import dev.ninjdai.xatu.interaction.command.Command;
import dev.ninjdai.xatu.interaction.modal.Modal;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class InteractionHandler {
    private static final Map<String, Command> COMMANDS = new HashMap<>();
    private static final Map<String, Modal> MODALS = new HashMap<>();
    public static void init() {
        long applicationId = Main.DISCORD_CLIENT.getApplicationId().block();

        ServiceLoader<Command> commandServiceLoader = ServiceLoader.load(Command.class);
        for (Command command : commandServiceLoader) {
            for (String alias: command.getName()) {
                COMMANDS.put(alias, command);
                Main.DISCORD_CLIENT.getApplicationService()
                    .createGlobalApplicationCommand(applicationId, command.getCommandBuilder().name(alias).build())
                    .subscribe();
            }
            Main.LOGGER.info("Registered /%s command (%d aliases)".formatted(command.getName()[0], command.getName().length - 1));
        }
    }

    public static Mono<Void> execute(ApplicationCommandInteractionEvent event) {
        if (COMMANDS.containsKey(event.getCommandName())) {
            return COMMANDS.get(event.getCommandName()).execute(event);
        } else {
            return event.reply("Command not found, please contact the developers").withEphemeral(true);
        }
    }

    public static Mono<Void> execute(ModalSubmitInteractionEvent event) {
        if (MODALS.containsKey(event.getCustomId())) {
            return MODALS.get(event.getCustomId()).execute(event);
        } else {
            return event.reply("Modal not found, please contact the developers").withEphemeral(true);
        }
    }
}
