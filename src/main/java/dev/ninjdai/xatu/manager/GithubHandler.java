package dev.ninjdai.xatu.manager;

import dev.ninjdai.xatu.Main;
import dev.ninjdai.xatu.Utils;
import dev.ninjdai.xatu.data.Details;
import dev.ninjdai.xatu.data.RepoData;
import dev.ninjdai.xatu.data.ServerConfig;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.kohsuke.github.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GithubHandler {
    private static GitHub GITHUB;
    public static void init(String githubToken) {
        try {
            GITHUB = new GitHubBuilder().withOAuthToken(githubToken).build();
        } catch (IOException e) {
            Main.LOGGER.error("Error initializing github api", e);
        }
    }

    public static GHIssue getIssue(String repoName, int number) {
        try {
            return GITHUB.getRepository(repoName).getIssue(number);
        } catch (IOException e) {
            return null;
        }
    }

    public static RepoData getRepoData(ServerConfig serverConfig) {
        if (GITHUB==null) return null;
        try {
            long timestamp = new Date().getTime() / 1000;
            GHRateLimit rateLimit = GITHUB.getRateLimit();
            Main.LOGGER.info("Ratelimits for {}-{}: {}/{} - resets at {}", serverConfig.server_id.asString(), serverConfig.repo_name, rateLimit.getRemaining(), rateLimit.getLimit(), rateLimit.getResetDate());
            GHRepository repository = GITHUB.getRepository(serverConfig.repo_name);
            GHLabel confirmedLabel = repository.getLabel("status: confirmed");
            GHLabel unconfirmedLabel = repository.getLabel("status: unconfirmed");
            GHLabel featureRequestLabel = repository.getLabel("feature-request");

            Main.LOGGER.debug("Starting to fetch issues");
            List<GHIssue> allList = repository.getIssues(GHIssueState.ALL)/*.stream().filter(issue -> {
                try {
                    return !Objects.equals(issue.getUser().getType(), "Bot");
                } catch (IOException e) {
                    return true;
                }
            }).toList()*/;
            Main.LOGGER.debug("Starting to filter issues");
            List<GHIssue> openIssueList = allList.stream().filter(issue -> !issue.isPullRequest() && issue.getState()==GHIssueState.OPEN).sorted((issue, t1) -> {
                try {
                    return issue.getUpdatedAt().compareTo(t1.getUpdatedAt());
                } catch (IOException e) {
                    return 0;
                }
            }).toList();
            List<GHIssue> recentIssueList = openIssueList.stream().sorted((t1, issue) -> {
                try {
                    return issue.getCreatedAt().compareTo(t1.getCreatedAt());
                } catch (IOException e) {
                    return 0;
                }
            }).toList();
            int confirmedBugs = openIssueList.stream().filter(issue -> issue.getLabels().contains(confirmedLabel)).toList().size();
            int unconfirmedBugs = openIssueList.stream().filter(issue -> issue.getLabels().contains(unconfirmedLabel)).toList().size();
            int featureRequests = openIssueList.stream().filter(issue -> issue.getLabels().contains(featureRequestLabel)).toList().size();

            Main.LOGGER.debug("Starting to fetch PRs");
            List<GHPullRequest> openPRList = repository.queryPullRequests().state(GHIssueState.OPEN).sort(GHPullRequestQueryBuilder.Sort.UPDATED).list().toList();
            Main.LOGGER.debug("Starting to filter PRs");
            List<GHPullRequest> recentPRList = openPRList.stream().sorted((t1, pr) -> {
                try {
                    return pr.getCreatedAt().compareTo(t1.getCreatedAt());
                } catch (IOException e) {
                    return 0;
                }
            }).toList();
            List<GHPullRequest> readyForReviewPRList = openPRList.stream().parallel().filter(pr -> {
                try {
                    return !pr.isDraft();
                } catch (IOException e) {
                    return false;
                }
            }).toList();
            int draftPR = openPRList.size() - readyForReviewPRList.size();

            Main.LOGGER.debug("Starting to create embed");
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .color(Color.of(0, 176, 244))
                    .title("Expansion Issue Report")
                    .url("https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%3Aopen+")
                    .author("Xatu", "https://github.com/Ninjdai1/xatu", "https://raw.githubusercontent.com/PMDCollab/SpriteCollab/master/portrait/0178/Inspired.png")
                    .addField(
                            "RAW STATS",
                            String.format("* [%d Issues](https://github.com/rh-hideout/pokeemerald-expansion/issues) ([%d Confirmed Bugs](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+is%%3Aopen+label%%3A\"status%%3A+confirmed\") / [%d Unconfirmed Bugs](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+is%%3Aopen+label%%3A\"status%%3A+unconfirmed\") / [%d Feature Requests](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+is%%3Aopen+label%%3Afeature-request)) \n* [%d Pull Requests](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Aopen) ([%d Ready for Review](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Aopen+draft%%3Afalse) / [%d Draft](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Aopen+draft%%3Atrue))",
                                    openIssueList.size(), confirmedBugs, unconfirmedBugs, featureRequests,
                                    openPRList.size(), readyForReviewPRList.size(), draftPR
                            ),
                            false)
                    .addField(
                            "STALES",
                            String.format("**[Pull Requests](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Aopen+draft%%3Afalse+sort%%3Aupdated-asc)**%s%s%s",
                                    renderStaleIssue(readyForReviewPRList.get(0)),
                                    renderStaleIssue(readyForReviewPRList.get(1)),
                                    renderStaleIssue(readyForReviewPRList.get(2))
                            ),
                            false
                    )
                    .addField(
                            "STALES",
                            String.format("**[Issues](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aopen+sort%%3Aupdated-asc)**%s%s%s",
                                    renderStaleIssue(openIssueList.get(0)),
                                    renderStaleIssue(openIssueList.get(1)),
                                    renderStaleIssue(openIssueList.get(2))
                            ),
                            true
                    )
                    .addField(
                            "LAST CREATED",
                            String.format("**[Pull Requests](https://github.com/rh-hideout/pokeemerald-expansion/pulls?q=is%%3Apr+is%%3Aopen+sort%%3Acreated-desc)**%s%s%s",
                                    renderRecentIssue(recentPRList.get(0)),
                                    renderRecentIssue(recentPRList.get(1)),
                                    renderRecentIssue(recentPRList.get(2))
                            ),
                            false
                    )
                    .addField(
                            "LAST CREATED",
                            String.format("**[Issues](https://github.com/rh-hideout/pokeemerald-expansion/issues?q=is%%3Aissue+is%%3Aopen+sort%%3Acreated-desc)**%s%s%s",
                                    renderRecentIssue(recentIssueList.get(0)),
                                    renderRecentIssue(recentIssueList.get(1)),
                                    renderRecentIssue(recentIssueList.get(2))
                            ),
                            false
                    )
                    .timestamp(Instant.now())
                    .footer("Written with ❤️ by Ninjdai", "https://archives.bulbagarden.net/media/upload/e/eb/BT178.png")
                    .build();

            Details details = new Details();
            for (GHIssue issue: allList) {
                if (issue.getClosedAt() == null || issue.getCreatedAt() == null) continue;
                long createdAt = issue.getCreatedAt().getTime()/1000;
                long closedAt = issue.getClosedAt().getTime()/1000;
                if (issue.isPullRequest()){
                    if (createdAt > timestamp - Utils.Durations.DAY.duration) details.opened_pr_1++;
                    if (createdAt > timestamp - Utils.Durations.WEEK.duration) details.opened_pr_7++;
                    if (createdAt > timestamp - Utils.Durations.MONTH.duration) details.opened_pr_30++;
                    if (createdAt > timestamp - Utils.Durations.YEAR.duration) details.opened_pr_365++;
                    details.opened_pr_all++;

                    if (issue.getState() == GHIssueState.CLOSED) {
                        if (closedAt > timestamp - Utils.Durations.DAY.duration) details.merged_pr_1++;
                        if (closedAt > timestamp - Utils.Durations.WEEK.duration) details.merged_pr_7++;
                        if (closedAt > timestamp - Utils.Durations.MONTH.duration) details.merged_pr_30++;
                        if (closedAt > timestamp - Utils.Durations.YEAR.duration) details.merged_pr_365++;
                        details.merged_pr_all++;
                    }
                } else {
                    if (createdAt > timestamp - Utils.Durations.DAY.duration) details.opened_issue_1++;
                    if (createdAt > timestamp - Utils.Durations.WEEK.duration) details.opened_issue_7++;
                    if (createdAt > timestamp - Utils.Durations.MONTH.duration) details.opened_issue_30++;
                    if (createdAt > timestamp - Utils.Durations.YEAR.duration) details.opened_issue_365++;
                    details.opened_issue_all++;

                    if (issue.getState() == GHIssueState.CLOSED) {
                        if (closedAt > timestamp - Utils.Durations.DAY.duration) details.closed_issue_1++;
                        if (closedAt > timestamp - Utils.Durations.WEEK.duration) details.closed_issue_7++;
                        if (closedAt > timestamp - Utils.Durations.MONTH.duration) details.closed_issue_30++;
                        if (closedAt > timestamp - Utils.Durations.YEAR.duration) details.closed_issue_365++;
                        details.closed_issue_all++;
                    }
                }
            }
            return new RepoData(serverConfig.repo_name, timestamp, embed, details);
        } catch (IOException e) {
            Main.LOGGER.error("Error while fetching repository data", e);
            return null;
        }
    }

    public static String renderStaleIssue(GHIssue issue) {
        Date lastUpdatedAt = new Date();
        try {
            lastUpdatedAt = issue.getUpdatedAt();
        } catch (IOException ignored) {}
        return String.format("\n* [#%d - %s](%s) | Last updated <t:%d:R>",
                issue.getNumber(),
                issue.getTitle().length() > 100 ? (issue.getTitle().substring(0, 100) + "...") : issue.getTitle(),
                issue.getHtmlUrl().toString(),
                (int) (lastUpdatedAt.getTime() / 1000)
        );
    }

    public static String renderRecentIssue(GHIssue issue) {
        Date createdAt = new Date();
        try {
            createdAt = issue.getCreatedAt();
        } catch (IOException ignored) {}
        return String.format("\n* [#%d - %s](%s) | Created <t:%d:R>",
                issue.getNumber(),
                issue.getTitle().length() > 100 ? (issue.getTitle().substring(0, 100) + "...") : issue.getTitle(),
                issue.getHtmlUrl().toString(),
                (int) (createdAt.getTime() / 1000)
        );
    }
}
