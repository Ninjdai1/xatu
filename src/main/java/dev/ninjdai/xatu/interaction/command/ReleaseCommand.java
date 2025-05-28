package dev.ninjdai.xatu.interaction.command;

import com.google.auto.service.AutoService;
import dev.ninjdai.xatu.manager.DatabaseHandler;
import dev.ninjdai.xatu.Utils;
import dev.ninjdai.xatu.data.Semver;
import dev.ninjdai.xatu.data.ServerMetadata;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.Optional;

@AutoService(Command.class)
public class ReleaseCommand implements Command {
    @Override
    public String[] getName() {
        return new String[]{"release"};
    }

    @Override
    public ImmutableApplicationCommandRequest.Builder getCommandBuilder() {
        return ApplicationCommandRequest.builder()
                .description("Display the release schedules for this server")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(BOOLEAN_OPTION_TYPE)
                        .name("ephemeral")
                        .description("Only show the data to you")
                        .required(false)
                        .build());
    }

    @Override
    public Mono<Void> execute(ApplicationCommandInteractionEvent event) {
        if (event.getInteraction().getGuildId().isEmpty()) {
            return event.reply("Command must be used in a guild").withEphemeral(true);
        }
        ServerMetadata metadata = DatabaseHandler.getServerMetadata(event.getInteraction().getGuildId().get());
        if (metadata == null) {
            return event.reply("No release data found for your server").withEphemeral(true);
        }
        if (metadata.current_version == null) {
            return event.reply("Current version unknown, please contact a senate member").withEphemeral(true);
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
        return replyMono.withEphemeral(ephemeralOption.isEmpty() || ephemeralOption.get().getValue().get().asBoolean());
    }
}
