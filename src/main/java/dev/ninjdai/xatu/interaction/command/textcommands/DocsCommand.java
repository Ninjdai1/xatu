package dev.ninjdai.xatu.interaction.command.textcommands;

import com.google.auto.service.AutoService;
import dev.ninjdai.xatu.interaction.command.Command;
import dev.ninjdai.xatu.interaction.command.SimpleButtonCommand;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.emoji.Emoji;

@AutoService(Command.class)
public class DocsCommand extends SimpleButtonCommand {
    @Override
    public String[] getName() {
        return new String[]{"docs", "documentation"};
    }

    @Override
    public String getMessageContent() {
        return "Check out the documentation !";
    }

    @Override
    public ActionRow getButtons() {
        return ActionRow.of(Button.link("https://rh-hideout.github.io/pokeemerald-expansion/", Emoji.unicode("\uD83D\uDD17"), "Link"));
    }
}
