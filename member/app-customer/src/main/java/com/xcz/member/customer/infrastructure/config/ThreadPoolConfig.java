package com.xcz.member.customer.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync // 开启 Spring 的异步执行功能
public class ThreadPoolConfig {

    @Bean("ThreadExecutor") // 为你的线程池起一个唯一的名字
    public Executor stockSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        //核心线程数：线程池里常驻的线程数量
        executor.setCorePoolSize(10);

        //最大线程数：当队列满了之后，线程池最多能开多少个线程
        executor.setMaxPoolSize(20);

        //队列容量：核心线程满了以后，任务会先进入队列排队
        executor.setQueueCapacity(500);

        //线程前缀名：方便在日志中排查问题，一眼看出是哪个线程池报的错
        executor.setThreadNamePrefix("my-sync-thread-");

        //允许线程空闲时间：超过核心线程数的线程，空闲多久后被销毁（默认 60 秒）
        executor.setKeepAliveSeconds(60);

        //拒绝策略：当队列满了、最大线程也满了，新任务来时的处理方式
        // CallerRunsPolicy: 不丢弃任务，哪来的回哪去，让提交任务的线程（比如主线程）自己执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        //初始化线程池
        executor.initialize();
        return executor;
    }
}
