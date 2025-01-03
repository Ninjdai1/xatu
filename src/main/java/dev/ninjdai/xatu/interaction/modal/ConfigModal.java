package dev.ninjdai.xatu.interaction.modal;

import dev.ninjdai.xatu.manager.DatabaseHandler;
import dev.ninjdai.xatu.data.ServerConfig;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;

public class ConfigModal implements Modal {
    @Override
    public String getCustomId() {
        return "config";
    }

    @Override
    public void execute(ModalSubmitInteractionEvent event) {
        if (event.getInteraction().getGuildId().isEmpty()) {
            event.reply("Modal must be used in a guild").withEphemeral(true).subscribe();
            return;
        };
        ServerConfig config = new ServerConfig();
        for (TextInput component : event.getComponents(TextInput.class)) {
            if ("repo_name".equals(component.getCustomId())) {
                config.repo_name = component.getValue().orElse("");
            } else if ("fetch_cron".equals(component.getCustomId())) {
                config.fetch_cron = Integer.parseInt(component.getValue().orElse("0"));
            } else if ("channel_id".equals(component.getCustomId())) {
                config.channel_id = Snowflake.of(component.getValue().orElse(event.getInteraction().getChannelId().asString()));
            }
        }
        config.server_id = event.getInteraction().getGuildId().get();
        DatabaseHandler.addServer(config);
        event.reply("Configuration complete !\nThe github repo `%s` will be fetched every day at %s:00 GMT and the recap will be sent in <#%d>".formatted(config.repo_name, config.fetch_cron, config.channel_id.asLong()))
                .withEphemeral(true)
                .block();
    }
}
