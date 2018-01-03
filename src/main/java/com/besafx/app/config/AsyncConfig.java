package com.besafx.app.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "threadPoolEmailSender")
    public Executor threadPoolTaskExecutor() {
        log.info("Prepare threadPoolEmailSender...");
        return initThreadPool(1, 1, 500, "EmailSender-");
    }

//    @Bean(name = "threadPoolTwilioSMS")
//    public Executor threadPoolTwilioSMS() {
//        log.info("Prepare threadPoolTwilioSMS...");
//        return initThreadPool(5, 10, 500, "TwilioSMS-");
//    }

//    @Bean(name = "threadPoolBulkSMS")
//    public Executor threadPoolBulkSMS() {
//        log.info("Prepare threadPoolBulkSMS...");
//        return initThreadPool(5, 10, 500, "BulkSMS-");
//    }

    @Bean(name = "threadPoolFileUploader")
    public Executor threadPoolFileUploader() {
        log.info("Prepare threadPoolFileUploader...");
        return initThreadPool(5, 10, 500, "FileUploader-");
    }

    @Bean(name = "threadPoolFileSharing")
    public Executor threadPoolFileSharing() {
        log.info("Prepare threadPoolFileSharing...");
        return initThreadPool(5, 10, 500, "FileSharing-");
    }

    @Bean(name = "threadPoolReportGenerator")
    public Executor threadPoolReportGenerator() {
        log.info("Prepare threadPoolReportGenerator...");
        return initThreadPool(1, 1, 500, "FilePDFGenerate-");
    }

    @Bean(name = "threadPoolPaymentExcel")
    public Executor threadPoolPaymentExcel() {
        log.info("Prepare threadPoolPaymentExcel...");
        return initThreadPool(1, 1, 500, "PaymentExcel-");
    }

    @Bean(name = "JasperPrintGenerator")
    public Executor JasperPrintGenerator() {
        log.info("Prepare JasperPrintGenerator...");
        return initThreadPool(10, 10, 500, "JasperPrintGenerator-");
    }

    private ThreadPoolTaskExecutor initThreadPool(int corePoolSize, int maxPoolSize, int queueCapacity, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix(prefix);
        return executor;
    }

}
