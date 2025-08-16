package dev.ninjdai.xatu.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ninjdai.xatu.Main;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.emoji.Emoji;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.*;
import discord4j.rest.util.AllowedMentions;
import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.github.GHIssue;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {
    public static final Pattern BACK_QUOTE_CONTENT_REGEX = Pattern.compile("`[^``]*`/g");
    public static final Pattern FORMATTED_LINK_CONTENT_REGEX = Pattern.compile("\\[(.*?)\\]\\(.*?\\)");
    public static final Pattern RHH_MATCHES_REGEX = Pattern.compile("(^|\\s)#\\d+");
    public static final Pattern PRET_MATCHES_REGEX = Pattern.compile("(^|\\s)pret#\\d+");
    public static final Pattern XKCD_MATCHES_REGEX = Pattern.compile("(^|\\s)xkcd#\\d+");

    public static final Emoji EMOJI_ISSUE_CLOSED = Emoji.of(1333157204429377566L, "issue_closed", false);
    public static final Emoji EMOJI_ISSUE_OPEN = Emoji.of(1333157206740439051L, "issue_open", false);
    public static final Emoji EMOJI_PR_MERGED = Emoji.of(1333157400399843339L, "pr_merged", false);
    public static final Emoji EMOJI_PR_OPEN = Emoji.of(1333157402954170369L, "pr_open", false);

    public static final Map<Snowflake, Snowflake> MESSAGE_REPLY_MAP = new HashMap<>();

    public static Mono<Void> execute(MessageCreateEvent event) {
        List<LayoutComponent> replyButtons = getMessageButtons(event.getMessage().getContent());
        if (!replyButtons.isEmpty()) {
            MessageChannel channel = event.getMessage().getChannel().block();
            if (channel != null) channel.createMessage(MessageCreateSpec.builder()
                .components(replyButtons)
                .messageReference(MessageReferenceData.builder().messageId(event.getMessage().getId().asLong()).build())
                .allowedMentions(AllowedMentions.builder().build())
                .build()).doOnSuccess(msg -> {
                    if (msg != null) MESSAGE_REPLY_MAP.put(event.getMessage().getId(), msg.getId());
                }).subscribe();
        }

        if (event.getMessage().getContent().toLowerCase().matches(".*thank.*xatu.*")) {
            event.getMessage().addReaction(Emoji.unicode("\uD83D\uDC9A")).subscribe();
        } else if (event.getMessage().getContent().toLowerCase().matches(".*hi.*xatu.*")) {
            event.getMessage().addReaction(Emoji.unicode("\uD83D\uDC4B")).subscribe();
        }
        return Mono.empty();
    }

    public static Mono<?> execute(MessageUpdateEvent event) {
        if (!event.isContentChanged()) return Mono.empty();
        Message message = event.getMessage().block();
        if (message == null || !MESSAGE_REPLY_MAP.containsKey(event.getMessageId())) return Mono.empty();
        List<LayoutComponent> replyButtons = getMessageButtons(message.getContent());
        if (!replyButtons.isEmpty()) {
            return Main.DISCORD_CLIENT.getMessageById(message.getChannelId(), MESSAGE_REPLY_MAP.get(event.getMessageId()))
                    .edit(MessageEditRequest.builder().components(getComponentsData(replyButtons)).build());
        }
        return Mono.empty();
    }

    public static Mono<Void> execute(MessageDeleteEvent event) {
        if (MESSAGE_REPLY_MAP.containsKey(event.getMessageId())) {
            Main.DISCORD_CLIENT.getMessageById(event.getChannelId(), MESSAGE_REPLY_MAP.get(event.getMessageId())).delete("OP removed the relevant message").block();
            MESSAGE_REPLY_MAP.remove(event.getMessageId());
        }
        return Mono.empty();
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
        Matcher xkcdMatcher = XKCD_MATCHES_REGEX.matcher(messageContent);
        while (issues.size()<5 && xkcdMatcher.find()) {
            int xkcdNumber = Integer.parseInt(xkcdMatcher.group().replace("#", "").replace("xkcd", "").strip());
            issues.add(Pair.of("xkcd", xkcdNumber));
        }

        LayoutComponent[] replyButtons = new LayoutComponent[issues.size()];
        issues.parallelStream().forEach(pair -> {
            if (Objects.equals(pair.getLeft(), "rhh")) {
                GHIssue issue = GithubHandler.getIssue("rh-hideout/pokeemerald-expansion", pair.getRight());
                if (issue != null) {
                    String btnName = "#%d - %s".formatted(pair.getRight(), issue.getTitle());
                    replyButtons[issues.indexOf(pair)] = ActionRow.of(Button.link(issue.getHtmlUrl().toString(), getIssueEmoji(issue), btnName.length() > 80 ? btnName.substring(0, 77) + "..." : btnName));
                }
            } else if (Objects.equals(pair.getLeft(), "pret")) {
                GHIssue issue = GithubHandler.getIssue("pret/pokeemerald", pair.getRight());
                if (issue != null) {
                    String btnName = "pret#%d - %s".formatted(pair.getRight(), issue.getTitle());
                    replyButtons[issues.indexOf(pair)] = ActionRow.of(Button.link(issue.getHtmlUrl().toString(), getIssueEmoji(issue), btnName.length() > 80 ? btnName.substring(0, 77) + "..." : btnName));
                }
            } else if (Objects.equals(pair.getLeft(), "xkcd")) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    JsonNode xkcdJson = mapper.readTree(URI.create(String.format("https://xkcd.com/%d/info.0.json", pair.getRight())).toURL());
                    if (xkcdJson != null) {
                        String btnName = "xkcd#%d - %s".formatted(pair.getRight(), xkcdJson.get("safe_title").asText());
                        replyButtons[issues.indexOf(pair)] = ActionRow.of(Button.link(String.format("https://xkcd.com/%d/", pair.getRight()), Emoji.unicode("\uD83D\uDC40"), btnName.length() > 80 ? btnName.substring(0, 77) + "..." : btnName));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return Arrays.stream(replyButtons).toList();
    }

    public static Emoji getIssueEmoji(GHIssue issue) {
        if (issue.isPullRequest()) {
            if (issue.getClosedAt() != null) return EMOJI_PR_MERGED;
            else return EMOJI_PR_OPEN;
        } else {
            if (issue.getClosedAt() != null) return EMOJI_ISSUE_CLOSED;
            else return EMOJI_ISSUE_OPEN;
        }
    }
}
