package dev.ninjdai.xatu.manager;

import dev.ninjdai.xatu.DataSendingJob;
import dev.ninjdai.xatu.Main;
import dev.ninjdai.xatu.data.ServerConfig;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.TimeZone;

public class SchedulerManager {
    public static Scheduler SCHEDULER;

    public static void init() {
        try {
            SCHEDULER = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            Main.LOGGER.error("Error setting up scheduler", e);
        }
    }

    public static void start() {
        try {
            SCHEDULER.start();
        } catch (SchedulerException e) {
            Main.LOGGER.error("Error starting scheduler", e);
        }
    }

    public static void addServerJob(ServerConfig serverConfig) {
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + serverConfig.server_id.asString() + "-" + serverConfig.repo_name, "fetch-schedules")
                .withSchedule(CronScheduleBuilder
                        .dailyAtHourAndMinute(serverConfig.fetch_cron, 0)
                        .inTimeZone(TimeZone.getTimeZone("GMT"))
                )
                .build();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("server_config", serverConfig);

        JobDetail job = JobBuilder.newJob(DataSendingJob.class)
                .withIdentity("job:" + serverConfig.server_id.asString() + ":" + serverConfig.repo_name, "fetch-schedules")
                .usingJobData(jobDataMap)
                .build();
        try {
            if(SCHEDULER.checkExists(job.getKey())) SCHEDULER.deleteJob(job.getKey());
            SCHEDULER.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            Main.LOGGER.error("Error adding server job to scheduler", e);
        }
    }

    public static void removeServerJob(ServerConfig serverConfig) {
        try {
            SCHEDULER.deleteJob(JobKey.jobKey("trigger-" + serverConfig.server_id.asString() + "-" + serverConfig.repo_name, "fetch-schedules"));
        } catch (SchedulerException e) {
            Main.LOGGER.error("Error removing server job from scheduler", e);
        }
    }
}
