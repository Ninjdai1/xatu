package dev.ninjdai.xatu.interaction.command;

import com.google.auto.service.AutoService;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

@AutoService(Command.class)
public class IssueCommand implements Command {
    private static final Map<String, Pair<String, String>> TEMPLATE_LINKS = Map.of(
            "battle_engine", Pair.of("01_battle_engine_bugs.yaml", "bug%2Cstatus%3A+unconfirmed%2Ccategory%3A+battle-mechanic"),
            "battle_ai", Pair.of("02_battle_ai_issues.yaml", "bug%2Cstatus%3A+unconfirmed%2Ccategory%3A+battle-ai"),
            "feature_request", Pair.of("03_feature_requests.yaml", "feature-request"),
            "other", Pair.of("04_other_errors.yaml", "bug%2Cstatus%3A+unconfirmed")
    );

    @Override
    public ApplicationCommandRequest getCommand() {
        return ApplicationCommandRequest.builder()
                .name("issue")
                .description("Create an issue")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(STRING_OPTION_TYPE)
                        .name("type")
                        .description("The type of issue")
                        .choices(List.of(
                                ApplicationCommandOptionChoiceData.builder().name("⚔️ Battle Engine mechanical bugs 🐛").value("battle_engine").build(),
                                ApplicationCommandOptionChoiceData.builder().name("🧠 Battle AI bugs 🐛").value("battle_ai").build(),
                                ApplicationCommandOptionChoiceData.builder().name("🙏 Feature Request 🙏").value("feature_request").build(),
                                ApplicationCommandOptionChoiceData.builder().name("💾 Other errors 🖥️").value("other").build()
                        ))
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .type(STRING_OPTION_TYPE)
                        .name("title")
                        .description("Quick description of the issue")
                        .build()
                )
                .build();
    }

    @Override
    public void execute(ApplicationCommandInteractionEvent event) {
        Pair<String, String> template = TEMPLATE_LINKS.get(event.getInteraction().getCommandInteraction().get().getOption("type").get().getValue().get().asString());
        String issueTitle = event.getInteraction().getCommandInteraction().get().getOption("title").get().getValue().get().asString();
        String issueURL = "https://github.com/rh-hideout/pokeemerald-expansion/issues/new?assignees=&labels="
                + template.getRight() // Labels
                + "&projects=&template="
                + template.getLeft() // Template name
                + "&contact="
                + event.getInteraction().getUser().getUsername()
                + "&title="
                + issueTitle.replaceAll(" ", "+");

        event.reply(InteractionApplicationCommandCallbackSpec.builder()
                .content("To report your issue, click on the button below and fill out the issues form !")
                .addComponent(ActionRow.of(Button.link(issueURL, issueTitle.length() > 80 ? issueTitle.substring(0, 77) + "..." : issueTitle)))
                .build()).block();
    }
}
