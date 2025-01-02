package dev.ninjdai.xatu;

import dev.ninjdai.xatu.data.Details;
import dev.ninjdai.xatu.data.ServerConfig;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.Color;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static Map<Snowflake, ServerConfig> SERVER_CONFIGS;
    public static DiscordClient DISCORD_CLIENT;
    public static Logger LOGGER = LoggerFactory.getLogger("Xatu");

    public static void main(String[] args) {
        DISCORD_CLIENT = DiscordClient.create(System.getenv("DISCORD_TOKEN"));
        GithubHandler.init(System.getenv("GITHUB_TOKEN"));
        DatabaseHandler.init("jdbc:sqlite:%s/xatu.db".formatted(System.getenv("DB_DIR") != null ? System.getenv("DB_DIR") : "."));
        SchedulerManager.init();

        {
            List<ServerConfig> serverConfigList = DatabaseHandler.getServers();
            SERVER_CONFIGS = new HashMap<>(serverConfigList.size());
            for (ServerConfig serverConfig : serverConfigList) {
                SchedulerManager.addServerJob(serverConfig);
            }
            LOGGER.info("{} servers loaded", SERVER_CONFIGS.size());
        }

        Mono<Void> login = DISCORD_CLIENT.withGateway((GatewayDiscordClient gateway) -> {
            Mono<Void> printOnLogin = gateway.on(ReadyEvent.class, event ->
                            Mono.fromRunnable(() -> {
                                final User self = event.getSelf();
                                InteractionHandler.init();
                                SchedulerManager.start();
                                LOGGER.info("Logged in as {} and ready to fetch !", self.getUsername());
                            })).doOnError(throwable -> Main.LOGGER.error("Error on ready event", throwable))
                    .then();

            Mono<Void> handleComponentInteractions = gateway.on(ComponentInteractionEvent.class, event -> {
                if (event.getCustomId().startsWith("details:")) { // details:repo_owner/repo:timestamp
                    String[] interactionArgs = event.getCustomId().split(":");
                    String repo = interactionArgs[1];
                    long timestamp = Long.parseLong(interactionArgs[2]);
                    Details details = DatabaseHandler.getDetails(repo, timestamp);
                    EmbedCreateSpec embed = makeAddendumeEmbed(details, timestamp);
                    InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
                            .addEmbed(embed)
                            .ephemeral(true)
                            .build();
                    event.reply(spec).subscribe();
                }
                return Mono.empty();
            }).then();

            Mono<Void> handleCommandInteractions = gateway.on(ApplicationCommandInteractionEvent.class, event -> {
                InteractionHandler.execute(event);
                return Mono.empty();
            }).then();
            Mono<Void> handleModalInteractions = gateway.on(ModalSubmitInteractionEvent.class, event -> {
                InteractionHandler.execute(event);
                return Mono.empty();
            }).then();

            return printOnLogin.and(handleComponentInteractions).and(handleCommandInteractions).and(handleModalInteractions);
        });

        login.block();
    }

    static EmbedCreateSpec makeAddendumeEmbed(Details details, long timestamp) {
        ZoneId zoneId = ZoneId.of("UTC");
        Instant fetchInstant = Instant.ofEpochSecond(timestamp);

        String dateTimeNow = ZonedDateTime.ofInstant(fetchInstant, zoneId).toLocalDate().toString();
        String dateTimeDay = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp - Utils.Durations.DAY.duration), zoneId).toLocalDate().toString();
        String dateTimeWeek = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp - Utils.Durations.WEEK.duration), zoneId).toLocalDate().toString();
        String dateTimeMonth = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp - Utils.Durations.MONTH.duration), zoneId).toLocalDate().toString();
        String dateTimeYear = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp - Utils.Durations.YEAR.duration), zoneId).toLocalDate().toString();

        return EmbedCreateSpec.builder()
                .color(Color.of(0, 176, 244))
                .title("Expansion Issue Report")
                .url("https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%3Aopen+")
                .author("Xatu", "https://github.com/Ninjdai1/xatu", "https://raw.githubusercontent.com/PMDCollab/SpriteCollab/master/portrait/0178/Inspired.png")
                .addField(
                    "STATS",
                    "All stats are displayed as:\n\n**Metric**: yesterday | last 7 days | last 30 days | last 365 days | all time.\n\nRate is \"For every X created, how many are completed?\". For example, 2 means \"For every bug that came in this month, we solved two of them\".\n\nGrowth is how many more of these occured in this time period. For example, -14 means \"This week we merged/closed 14 PRs\".",
                    false
                )
                .addField(
                        "Opened PRs",
                        String.format("[%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+sort%%3Aupdated-asc+created%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+sort%%3Aupdated-asc+created%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+sort%%3Aupdated-asc+created%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+sort%%3Aupdated-asc+created%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+sort%%3Aupdated-asc)\n\n",
                                details.opened_pr_1, dateTimeDay, dateTimeNow, details.opened_pr_7, dateTimeWeek, dateTimeNow, details.opened_pr_30, dateTimeMonth, dateTimeNow, details.opened_pr_365, dateTimeYear, dateTimeNow, details.opened_pr_all
                        ),
                        false
                )
                .addField(
                        "Merged PRs",
                        String.format("[%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse+merged%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse+merged%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse+merged%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse+merged%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse)\n\n",
                                details.merged_pr_1, dateTimeDay, dateTimeNow, details.merged_pr_7, dateTimeWeek, dateTimeNow, details.merged_pr_30, dateTimeMonth, dateTimeNow, details.merged_pr_365, dateTimeYear, dateTimeNow, details.merged_pr_all
                        ),
                        true
                )
                .addField(
                        "PR Metrics",
                        String.format("Merge Rate: %.2g | %.2g | %.2g | %.2g | %.2g\nGrowth: %d | %d | %d | %d | %d",
                                ((float)details.merged_pr_1) / ((float)details.opened_pr_1), ((float)details.merged_pr_7) / ((float)details.opened_pr_7), ((float)details.merged_pr_30) / ((float)details.opened_pr_30), ((float)details.merged_pr_365) / ((float)details.opened_pr_365), ((float)details.merged_pr_all) / ((float)details.opened_pr_all),
                                details.opened_pr_1 - details.merged_pr_1, details.opened_pr_7 - details.merged_pr_7, details.opened_pr_30 - details.merged_pr_30, details.opened_pr_365 - details.merged_pr_365, details.opened_pr_all - details.merged_pr_all
                        ),
                        false
                )
                .addField(
                        "Opened Issues",
                        String.format("[%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+sort%%3Aupdated-asc+created%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+sort%%3Aupdated-asc+created%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+sort%%3Aupdated-asc+created%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+sort%%3Aupdated-asc+created%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+sort%%3Aupdated-asc)\n\n",
                                details.opened_issue_1, dateTimeDay, dateTimeNow, details.opened_issue_7, dateTimeWeek, dateTimeNow, details.opened_issue_30, dateTimeMonth, dateTimeNow, details.opened_issue_365, dateTimeYear, dateTimeNow, details.opened_issue_all
                        ),
                        false
                )
                .addField(
                        "Closed Issues",
                        String.format("[%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse+merged%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse+merged%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse+merged%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse+merged%%3A%s..%s) | [%d](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+is%%3Amerged+sort%%3Aupdated-asc+draft%%3Afalse)\n\n",
                                details.closed_issue_1, dateTimeDay, dateTimeNow, details.closed_issue_7, dateTimeWeek, dateTimeNow, details.closed_issue_30, dateTimeMonth, dateTimeNow, details.closed_issue_365, dateTimeYear, dateTimeNow, details.closed_issue_all
                        ),
                        true
                )
                .addField(
                        "Issues Metrics",
                        String.format("Resolution Rate: %.2g | %.2g | %.2g | %.2g | %.2g\nGrowth: %d | %d | %d | %d | %d",
                                ((float)details.closed_issue_1) / ((float)details.opened_issue_1), ((float)details.closed_issue_7) / ((float)details.opened_issue_7), ((float)details.closed_issue_30) / ((float)details.opened_issue_30), ((float)details.closed_issue_365) / ((float)details.opened_issue_365), ((float)details.closed_issue_all) / ((float)details.opened_issue_all),
                                details.opened_issue_1 - details.closed_issue_1, details.opened_issue_7 - details.closed_issue_7, details.opened_issue_30 - details.closed_issue_30, details.opened_issue_365 - details.closed_issue_365, details.opened_issue_all - details.closed_issue_all
                        ),
                        false
                )
                .timestamp(fetchInstant)
                .footer("Written with ❤️ by Ninjdai", "https://archives.bulbagarden.net/media/upload/e/eb/BT178.png")
                .build();
    }
}