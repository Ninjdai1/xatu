package dev.ninjdai.xatu.data;

public class Details{
    public long timestamp = 0;

    public int opened_pr_1 = 0;
    public int opened_pr_7 = 0;
    public int opened_pr_30 = 0;
    public int opened_pr_365 = 0;
    public int opened_pr_all = 0;
    
    public int merged_pr_1 = 0;
    public int merged_pr_7 = 0;
    public int merged_pr_30 = 0;
    public int merged_pr_365 = 0;
    public int merged_pr_all = 0;
    
    public int opened_issue_1 = 0;
    public int opened_issue_7 = 0;
    public int opened_issue_30 = 0;
    public int opened_issue_365 = 0;
    public int opened_issue_all = 0;
    
    public int closed_issue_1 = 0;
    public int closed_issue_7 = 0;
    public int closed_issue_30 = 0;
    public int closed_issue_365 = 0;
    public int closed_issue_all = 0;

    public Details(){};
    public Details(long timestamp,
            int opened_pr_1, int opened_pr_7, int opened_pr_30, int opened_pr_365, int opened_pr_all,
            int merged_pr_1, int merged_pr_7, int merged_pr_30, int merged_pr_365, int merged_pr_all,
            int opened_issue_1, int opened_issue_7, int opened_issue_30, int opened_issue_365, int opened_issue_all,
            int closed_issue_1, int closed_issue_7, int closed_issue_30, int closed_issue_365, int closed_issue_all) {
        this.timestamp = timestamp;

        this.opened_pr_1 = opened_pr_1;
        this.opened_pr_7 = opened_pr_7;
        this.opened_pr_30 = opened_pr_30;
        this.opened_pr_365 = opened_pr_365;
        this.opened_pr_all = opened_pr_all;

        this.merged_pr_1 = merged_pr_1;
        this.merged_pr_7 = merged_pr_7;
        this.merged_pr_30 = merged_pr_30;
        this.merged_pr_365 = merged_pr_365;
        this.merged_pr_all = merged_pr_all;

        this.opened_issue_1 = opened_issue_1;
        this.opened_issue_7 = opened_issue_7;
        this.opened_issue_30 = opened_issue_30;
        this.opened_issue_365 = opened_issue_365;
        this.opened_issue_all = opened_issue_all;

        this.closed_issue_1 = closed_issue_1;
        this.closed_issue_7 = closed_issue_7;
        this.closed_issue_30 = closed_issue_30;
        this.closed_issue_365 = closed_issue_365;
        this.closed_issue_all = closed_issue_all;
    }
}
