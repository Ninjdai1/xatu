package dev.ninjdai.xatu.interaction.command;

import dev.ninjdai.xatu.manager.DatabaseHandler;
import dev.ninjdai.xatu.data.ServerConfig;
import dev.ninjdai.xatu.data.ServerMetadata;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigCommand implements Command {
    @Override
    public ApplicationCommandRequest getCommand() {
        return ApplicationCommandRequest.builder()
                .name("config")
                .description("Configure the bot")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(SUBCOMMAND_OPTION_TYPE)
                        .name("setup")
                        .description("Set up basic repository and discord info")
                        .addOption(ApplicationCommandOptionData.builder()
                                .type(STRING_OPTION_TYPE)
                                .name("repository")
                                .description("Repository to fetch (eg \"rh-hideout/pokeemerald-expansion\")")
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .type(INTEGER_OPTION_TYPE)
                                .name("fetch_time")
                                .description("Hour at which the fetching will happen (GMT, 24 hours format)")
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .type(CHANNEL_OPTION_TYPE)
                                .name("channel")
                                .description("Channel in which the reports will be sent")
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .type(CHANNEL_OPTION_TYPE)
                                .addChannelType(GUILD_TEXT_CHANNEL_TYPE)
                                .name("second_channel")
                                .description("Second channel in which to send the reports")
                                .required(false)
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .type(SUBCOMMAND_OPTION_TYPE)
                        .name("schedules")
                        .description("Define schedules and current_version info")
                        .addOption(ApplicationCommandOptionData.builder()
                                .type(STRING_OPTION_TYPE)
                                .name("current_version")
                                .description("Set master's current current_version, eg \"4.2.9\"")
                                .required(false)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .type(INTEGER_OPTION_TYPE)
                                .name("minor_release_timestamp")
                                .description("Unix timestamp of the next minor release")
                                .required(false)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .type(INTEGER_OPTION_TYPE)
                                .name("patch_release_timestamp")
                                .description("Unix timestamp of the next patch release")
                                .required(false)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void execute(ApplicationCommandInteractionEvent event) {
        {
            if (event.getInteraction().getGuildId().isEmpty()) {
                event.reply("Command must be used in a guild").withEphemeral(true).subscribe();
                return;
            }
            List<Role> roles = event.getInteraction().getMember().get().getRoles().collectList().block();
            if (event.getInteraction().getMember().isEmpty() || roles == null || roles.stream().noneMatch(role -> role.getId().asString().equals("1077007974666621039"))) {
                event.reply("You are not an expansion senate member ! Can't do that >.<").withEphemeral(true).subscribe();
                return;
            }
        }

        Optional<ApplicationCommandInteractionOption> setupOption = event.getInteraction().getCommandInteraction().get().getOption("setup");
        Optional<ApplicationCommandInteractionOption> schedulesOption = event.getInteraction().getCommandInteraction().get().getOption("schedules");
        if (setupOption.isPresent()) {
            ServerConfig config = new ServerConfig();
            config.server_id = event.getInteraction().getGuildId().get();
            config.repo_name = setupOption.get().getOption("repository").get().getValue().get().asString();
            long fetch_time = setupOption.get().getOption("fetch_time").get().getValue().get().asLong();
            if (fetch_time < 0 || fetch_time > 24) {
                event.reply("Fetch time must be between 0 and 24 ! %d is invalid".formatted(fetch_time)).block();
                return;
            }
            config.fetch_cron = (int) fetch_time;
            config.channel_id = setupOption.get().getOption("channel").get().getValue().get().asChannel().block().getId();
            setupOption.get().getOption("second_channel").ifPresent(option ->
                    config.second_channel_id = option.getValue().get().asChannel().block().getId());

            DatabaseHandler.addServer(config);
            event.reply("Configuration complete !\nThe github repo `%s` will be fetched every day at %s:00 GMT and the recap will be sent in <#%d>".formatted(config.repo_name, config.fetch_cron, config.channel_id.asLong()))
                    .withEphemeral(true)
                    .block();
        } else if (schedulesOption.isPresent()) {
            ServerMetadata metadata = new ServerMetadata();
            metadata.server_id = event.getInteraction().getGuildId().get();
            AtomicReference<String> reply = new AtomicReference<>("Configuration complete !");
            Optional<ApplicationCommandInteractionOption> current_version = schedulesOption.get().getOption("current_version");
            Optional<ApplicationCommandInteractionOption> minor_release_timestamp = schedulesOption.get().getOption("minor_release_timestamp");
            Optional<ApplicationCommandInteractionOption> patch_release_timestamp = schedulesOption.get().getOption("patch_release_timestamp");
            if (current_version.isPresent()) {
                metadata.current_version = current_version.get().getValue().get().asString();
                reply.set(reply + "\n* Master version set to `%s`".formatted(metadata.current_version));
            }
            if (minor_release_timestamp.isPresent()) {
                metadata.minor_release_timestamp = minor_release_timestamp.get().getValue().get().asLong();
                reply.set(reply + "\n* Next minor release date set to <t:%d:D> (<t:%d:R>)".formatted(metadata.minor_release_timestamp, metadata.minor_release_timestamp));
            }
            if (patch_release_timestamp.isPresent()) {
                metadata.patch_release_timestamp = patch_release_timestamp.get().getValue().get().asLong();
                reply.set(reply + "\n* Next patch release date set to <t:%d:D> (<t:%d:R>)".formatted(metadata.patch_release_timestamp, metadata.patch_release_timestamp));
            }

            DatabaseHandler.addServerMetadata(metadata);
            event.reply(reply.get())
                    .withEphemeral(true)
                    .block();
        }
    }
}
