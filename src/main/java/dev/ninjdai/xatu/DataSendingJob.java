package dev.ninjdai.xatu;

import dev.ninjdai.xatu.data.RepoData;
import dev.ninjdai.xatu.data.ServerConfig;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.entity.RestChannel;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class DataSendingJob implements Job {
    public void execute(JobExecutionContext jobExecutionContext) {
        ServerConfig serverConfig = (ServerConfig) jobExecutionContext.getMergedJobDataMap().get("server_config");
        Main.LOGGER.info("Running server job {}-{}", serverConfig.server_id.asString(), serverConfig.repo_name);
        RepoData data = GithubHandler.getRepoData(serverConfig);
        RestChannel channel = Main.DISCORD_CLIENT.getChannelById(Snowflake.of("875622508026544148"));
        if (data != null) {
            Button detailsBtn = Button.primary("details:%s:%d".formatted(data.repo(), data.timestamp()), "Show details");
            channel.createMessage(MessageCreateSpec.builder()
                .addEmbed(data.embed())
                .addComponent(ActionRow.of(detailsBtn))
                .build().asRequest()
            ).subscribe();
            DatabaseHandler.registerDetails(data.repo(), data.timestamp(), data.details());
        }
    }
}
