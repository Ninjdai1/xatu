package dev.ninjdai.xatu.interaction.command.textcommands;

import com.google.auto.service.AutoService;
import dev.ninjdai.xatu.interaction.command.Command;
import dev.ninjdai.xatu.interaction.command.SimpleButtonCommand;
import dev.ninjdai.xatu.interaction.command.SimpleTextCommand;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.emoji.Emoji;

@AutoService(Command.class)
public class IssueCommand extends SimpleButtonCommand {
    @Override
    public String[] getName() {
        return new String[]{"issue", "bug", "bug_report", "bugreport"};
    }

    @Override
    public String getMessageContent() {
        return "Please file a bug or issue on GitHub!";
    }

    @Override
    public ActionRow getButtons() {
        return ActionRow.of(
                Button.link("https://github.com/rh-hideout/pokeemerald-expansion/issues/new", Emoji.unicode("\uD83D\uDD17"), "Link"),
                Button.link("https://github.com/rh-hideout/pokeemerald-expansion/blob/master/CONTRIBUTING.md#bug-reports", Emoji.unicode("\uD83D\uDCD6"), "Instructions")
        );
    }
}
