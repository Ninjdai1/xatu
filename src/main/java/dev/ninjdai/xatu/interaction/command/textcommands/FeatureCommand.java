package dev.ninjdai.xatu.interaction.command.textcommands;

import com.google.auto.service.AutoService;
import dev.ninjdai.xatu.interaction.command.Command;
import dev.ninjdai.xatu.interaction.command.SimpleButtonCommand;
import dev.ninjdai.xatu.interaction.command.SimpleTextCommand;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.emoji.Emoji;

@AutoService(Command.class)
public class FeatureCommand extends SimpleButtonCommand {
    @Override
    public String[] getName() {
        return new String[]{"feature", "request", "feature_request", "featurerequest"};
    }

    @Override
    public String getMessageContent() {
        return "If you have a request for a feature, please submit it on GitHub!";
    }

    @Override
    public ActionRow getButtons() {
        return ActionRow.of(
                Button.link("https://github.com/rh-hideout/pokeemerald-expansion/issues/new", Emoji.unicode("\uD83D\uDD17"), "Link"),
                Button.link("https://github.com/rh-hideout/pokeemerald-expansion/blob/master/CONTRIBUTING.md#feature-requests", Emoji.unicode("\uD83D\uDCD6"), "Instructions")
        );
    }
}
