package dev.ninjdai.xatu.interactions.commands;

import dev.ninjdai.xatu.DatabaseHandler;
import dev.ninjdai.xatu.Utils;
import dev.ninjdai.xatu.data.Semver;
import dev.ninjdai.xatu.data.ServerMetadata;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Optional;

public class ReleaseCommand implements Command {
    @Override
    public ApplicationCommandRequest getCommand() {
        return ApplicationCommandRequest.builder()
                .name("release")
                .description("Display the release schedules for this server")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(BOOLEAN_OPTION_TYPE)
                        .name("ephemeral")
                        .description("Only show the data to you")
                        .required(false)
                        .build())
                .build();
    }

    @Override
    public void execute(ApplicationCommandInteractionEvent event) {
        if (event.getInteraction().getGuildId().isEmpty()) {
            event.reply("Command must be used in a guild").withEphemeral(true).subscribe();
            return;
        }
        ServerMetadata metadata = DatabaseHandler.getServerMetadata(event.getInteraction().getGuildId().get());
        if (metadata == null) {
            event.reply("No release data found for your server").withEphemeral(true).block();
            return;
        }
        if (metadata.current_version == null) {
            event.reply("Current version unknown, please contact a senate member").withEphemeral(true).block();
            return;
        }

        Optional<ApplicationCommandInteractionOption> ephemeralOption = event.getInteraction().getCommandInteraction().get().getOption("ephemeral");
        long unixTime = System.currentTimeMillis() / 1000L;
        String reply = "";
        Semver currentSemver = new Semver(metadata.current_version);
        if (metadata.patch_release_timestamp != null && metadata.patch_release_timestamp > unixTime) {
            reply += "> The next **patch release**, version `%s`, is planned to release <t:%d:R> on <t:%d:f>\n".formatted(currentSemver.nextPatch(), metadata.patch_release_timestamp, metadata.patch_release_timestamp);
        }
        if (metadata.minor_release_timestamp != null && metadata.minor_release_timestamp > unixTime) {
            reply += "> The next **minor release**, version `%s`, is planned to release <t:%d:R> on <t:%d:f>\n".formatted(currentSemver.nextMinor(), metadata.minor_release_timestamp, metadata.minor_release_timestamp);
            reply += "> * Big features will not be merged after <t:%d:f>\n".formatted(metadata.minor_release_timestamp - Utils.DAY_LENGTH_IN_SECONDS * 30);
            reply += "> * Non-bugfixes will not be merged after <t:%d:f>\n".formatted(metadata.minor_release_timestamp - Utils.DAY_LENGTH_IN_SECONDS * 14);
        }
        InteractionApplicationCommandCallbackReplyMono replyMono = event.reply(reply);
        ephemeralOption.ifPresentOrElse(option -> replyMono.withEphemeral(option.getValue().get().asBoolean()).block(), () -> replyMono.withEphemeral(true).block());
    }
}
