package dev.ninjdai.xatu;

import dev.ninjdai.xatu.data.RepoData;
import dev.ninjdai.xatu.data.ServerConfig;
import dev.ninjdai.xatu.manager.DatabaseHandler;
import dev.ninjdai.xatu.manager.GithubHandler;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.entity.RestChannel;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class DataSendingJob implements Job {
    public void execute(JobExecutionContext jobExecutionContext) {
        ServerConfig serverConfig = (ServerConfig) jobExecutionContext.getMergedJobDataMap().get("server_config");
        Main.LOGGER.info("Running server job {}-{} with server config {}", serverConfig.server_id.asString(), serverConfig.repo_name, serverConfig);
        RepoData data = GithubHandler.getRepoData(serverConfig);
        RestChannel channel = Main.DISCORD_CLIENT.getChannelById(serverConfig.channel_id);

        if (data != null) {
            Button detailsBtn = Button.primary("details:%s:%d".formatted(data.repo(), data.timestamp()), "Show details");
            MessageCreateSpec message = MessageCreateSpec.builder()
                .addEmbed(data.embed())
                .addComponent(ActionRow.of(detailsBtn))
                .build();
            channel.createMessage(message.asRequest()).subscribe();
            if (serverConfig.second_channel_id != null) Main.DISCORD_CLIENT.getChannelById(serverConfig.second_channel_id).createMessage(message.asRequest()).subscribe();
            DatabaseHandler.registerDetails(data.repo(), data.timestamp(), data.details());
        }
    }
}
