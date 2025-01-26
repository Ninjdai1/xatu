package dev.ninjdai.xatu.manager;

import dev.ninjdai.xatu.Main;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.*;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.entity.RestMessage;
import discord4j.rest.util.AllowedMentions;
import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.github.GHIssue;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {
    public static final Pattern BACK_QUOTE_CONTENT_REGEX = Pattern.compile("/`[^``]*`/g");
    public static final Pattern FORMATTED_LINK_CONTENT_REGEX = Pattern.compile("/\\[(.*?)\\]\\(.*?\\)/g");
    public static final Pattern RHH_MATCHES_REGEX = Pattern.compile("(^|\\s)#\\d+");
    public static final Pattern PRET_MATCHES_REGEX = Pattern.compile("(^|\\s)pret#\\d+");

    public static final ReactionEmoji EMOJI_ISSUE_CLOSED = ReactionEmoji.of(1333157204429377566L, "issue_closed", false);
    public static final ReactionEmoji EMOJI_ISSUE_OPEN = ReactionEmoji.of(1333157206740439051L, "issue_open", false);
    public static final ReactionEmoji EMOJI_PR_MERGED = ReactionEmoji.of(1333157400399843339L, "pr_merged", false);
    public static final ReactionEmoji EMOJI_PR_OPEN = ReactionEmoji.of(1333157402954170369L, "pr_open", false);

    public static final Map<Snowflake, Snowflake> MESSAGE_REPLY_MAP = new HashMap<>();

    public static void execute(MessageCreateEvent event) {
        List<LayoutComponent> replyButtons = getMessageButtons(event.getMessage().getContent());
        if (!replyButtons.isEmpty()) {
            MessageChannel channel = event.getMessage().getChannel().block();
            if (channel != null) {
                Message message = channel.createMessage(MessageCreateSpec.builder()
                        .components(replyButtons)
                        .messageReference(event.getMessage().getId())
                        .allowedMentions(AllowedMentions.builder().build())
                        .build()).block();
                if (message != null) MESSAGE_REPLY_MAP.put(event.getMessage().getId(), message.getId());
            }
        }
    }

    public static void execute(MessageUpdateEvent event) {
        if (!event.isContentChanged()) return;
        Message message = event.getMessage().block();
        if (message == null || !MESSAGE_REPLY_MAP.containsKey(event.getMessageId())) return;
        List<LayoutComponent> replyButtons = getMessageButtons(message.getContent());
        if (!replyButtons.isEmpty()) {
            Main.DISCORD_CLIENT.getMessageById(message.getChannelId(), MESSAGE_REPLY_MAP.get(event.getMessageId()))
                    .edit(MessageEditRequest.builder().components(getComponentsData(replyButtons)).build()).block();
        }
    }

    public static void execute(MessageDeleteEvent event) {
        if (MESSAGE_REPLY_MAP.containsKey(event.getMessageId())) {
            Main.DISCORD_CLIENT.getMessageById(event.getChannelId(), MESSAGE_REPLY_MAP.get(event.getMessageId())).delete("OP removed the relevant message").block();
            MESSAGE_REPLY_MAP.remove(event.getMessageId());
        }
    }

    private static List<ComponentData> getComponentsData(List<LayoutComponent> layoutComponents) {
        List<ComponentData> list = new ArrayList<>();
        for (LayoutComponent layoutComponent: layoutComponents) {
            list.add(layoutComponent.getData());
        }
        return list;
    }

    private static List<LayoutComponent> getMessageButtons(String initialMessageContent) {
        String messageContent = initialMessageContent.replaceAll(BACK_QUOTE_CONTENT_REGEX.pattern(), "").replaceAll(FORMATTED_LINK_CONTENT_REGEX.pattern(), "");
        Matcher rhhMatcher = RHH_MATCHES_REGEX.matcher(messageContent);
        List<Pair<String, Integer>> issues = new ArrayList<>();
        while (issues.size()<5 && rhhMatcher.find()) {
            int issueNumber = Integer.parseInt(rhhMatcher.group().replace("#", "").strip());
            if (issueNumber <= 20) continue;
            issues.add(Pair.of("rhh", issueNumber));
        }
        Matcher pretMatcher = PRET_MATCHES_REGEX.matcher(messageContent);
        while (issues.size()<5 && pretMatcher.find()) {
            int issueNumber = Integer.parseInt(pretMatcher.group().replace("#", "").replace("pret", "").strip());
            if (issueNumber <= 20) continue;
            issues.add(Pair.of("pret", issueNumber));
        }

        LayoutComponent[] replyButtons = new LayoutComponent[issues.size()];
        issues.parallelStream().forEach(pair -> {
            if (Objects.equals(pair.getLeft(), "rhh")) {
                GHIssue issue = GithubHandler.getIssue("rh-hideout/pokeemerald-expansion", pair.getRight());
                if (issue != null) {
                    String btnName = "#%d - %s".formatted(pair.getRight(), issue.getTitle());
                    replyButtons[issues.indexOf(pair)] = ActionRow.of(Button.link(issue.getHtmlUrl().toString(), getIssueEmoji(issue), btnName.length() > 80 ? btnName.substring(0, 77) + "..." : btnName));
                }
            } else {
                GHIssue issue = GithubHandler.getIssue("pret/pokeemerald", pair.getRight());
                if (issue != null) {
                    String btnName = "pret#%d - %s".formatted(pair.getRight(), issue.getTitle());
                    replyButtons[issues.indexOf(pair)] = ActionRow.of(Button.link(issue.getHtmlUrl().toString(), getIssueEmoji(issue), btnName.length() > 80 ? btnName.substring(0, 77) + "..." : btnName));
                }
            }
        });
        return Arrays.stream(replyButtons).toList();
    }

    public static ReactionEmoji getIssueEmoji(GHIssue issue) {
        if (issue.isPullRequest()) {
            if (issue.getClosedAt() != null) return EMOJI_PR_MERGED;
            else return EMOJI_PR_OPEN;
        } else {
            if (issue.getClosedAt() != null) return EMOJI_ISSUE_CLOSED;
            else return EMOJI_ISSUE_OPEN;
        }
    }
}
