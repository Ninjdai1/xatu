package dev.ninjdai.xatu.interaction.command;

import com.google.auto.service.AutoService;
import dev.ninjdai.xatu.manager.DatabaseHandler;
import dev.ninjdai.xatu.Main;
import dev.ninjdai.xatu.manager.SchedulerManager;
import dev.ninjdai.xatu.data.ServerConfig;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

import java.util.List;

@AutoService(Command.class)
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
        {
            if (event.getInteraction().getGuildId().isEmpty() || event.getInteraction().getMember().isEmpty()) {
                event.reply("Command must be used in a guild").withEphemeral(true).subscribe();
                return;
            }
            List<Role> roles = event.getInteraction().getMember().get().getRoles().collectList().block();
            if (event.getInteraction().getMember().isEmpty() || roles == null || roles.stream().noneMatch(role -> role.getId().asString().equals("1077007974666621039"))) {
                event.reply("You are not an expansion senate member ! Can't do that >.<").withEphemeral(true).subscribe();
                return;
            }
        }
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
