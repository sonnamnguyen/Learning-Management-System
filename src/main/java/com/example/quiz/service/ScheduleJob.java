package com.example.quiz.service;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

@Service
public class ScheduleJob {

    @Autowired
    private Scheduler scheduler;

    public void scheduleQuizOpenJob(Long quizId, LocalDateTime startTime) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(QuizStatusOpenUpdaterJob.class)
                .withIdentity("quizOpenJob-" + quizId.toString(), "quizJobs")
                .usingJobData("quizId", quizId)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("quizOpenTrigger-" + quizId.toString(), "quizTriggers")
                .startAt(Date.from(startTime.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void scheduleQuizCloseJob(Long quizId, LocalDateTime endTime) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(QuizStatusCloseUpdaterJob.class)
                .withIdentity("quizCloseJob-" + quizId.toString(), "quizJobs")
                .usingJobData("quizId", quizId)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("quizCloseTrigger-" + quizId.toString(), "quizTriggers")
                .startAt(Date.from(endTime.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
    public void scheduleClearCacheJob(Long quizId, LocalDateTime clearTime) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(QuizCacheClearJob.class)
                .withIdentity("quizCacheClearJob-" + quizId, "quizJobs")
                .usingJobData("quizId", quizId)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("quizCacheClearTrigger-" + quizId, "quizTriggers")
                .startAt(Date.from(clearTime.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

}