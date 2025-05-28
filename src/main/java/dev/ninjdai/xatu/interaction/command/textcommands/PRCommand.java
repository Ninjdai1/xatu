package dev.ninjdai.xatu.interaction.command.textcommands;

import com.google.auto.service.AutoService;
import dev.ninjdai.xatu.interaction.command.Command;
import dev.ninjdai.xatu.interaction.command.SimpleButtonCommand;
import dev.ninjdai.xatu.interaction.command.SimpleTextCommand;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.emoji.Emoji;

@AutoService(Command.class)
public class PRCommand extends SimpleButtonCommand {
    @Override
    public String[] getName() {
        return new String[]{"pr", "pull_request", "pullrequest"};
    }

    @Override
    public String getMessageContent() {
        return "If you'd like to contribute to the project, open a PR on GitHub";
    }

    @Override
    public ActionRow getButtons() {
        return ActionRow.of(
                Button.link("https://github.com/rh-hideout/pokeemerald-expansion/blob/master/CONTRIBUTING.md#pull-requests", Emoji.unicode("\uD83D\uDCD6"), "Instructions")
        );
    }
}
