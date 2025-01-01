package dev.ninjdai.xatu.interactions.commands;

import dev.ninjdai.xatu.DatabaseHandler;
import dev.ninjdai.xatu.Main;
import dev.ninjdai.xatu.SchedulerManager;
import dev.ninjdai.xatu.data.ServerConfig;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

public class TriggerCommand implements Command{
    @Override
    public ApplicationCommandRequest getCommand() {
        return ApplicationCommandRequest.builder()
            .name("trigger")
            .description("Triggers a report fetch and send for testing purposes")
            .build();
    }

    @Override
    public void execute(ApplicationCommandInteractionEvent event) {
        if (event.getInteraction().getGuildId().isEmpty() || event.getInteraction().getMember().isEmpty()) return;
        if (!event.getInteraction().getMember().get().getBasePermissions().block().contains(Permission.ADMINISTRATOR)) return;
        ServerConfig serverConfig = DatabaseHandler.getServer(event.getInteraction().getGuildId().get());
        if (serverConfig == null) {
            event.reply("No server config found, please first add a configuration").withEphemeral(true).block();
            return;
        }
        event.reply("Launching a report fetch").withEphemeral(true).block();
    try {
            SchedulerManager.SCHEDULER.triggerJob(JobKey.jobKey("job:" + serverConfig.server_id.asString() + ":" + serverConfig.repo_name, "fetch-schedules"));
        } catch (SchedulerException e) {
            Main.LOGGER.error("Error during manual job trigger", e);
        }
    }
}
