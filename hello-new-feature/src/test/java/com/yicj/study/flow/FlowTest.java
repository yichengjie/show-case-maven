package com.yicj.study.flow;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SubmissionPublisher;

class FlowTest {

    @Test
    void test(){
        IntPublisher intPublisher = new IntPublisher() ;
        IntSubscriber intSubscriber = new IntSubscriber() ;
        intPublisher.subscribe(intSubscriber);
    }


    @Test
    void test2() throws InterruptedException {
        // 定义有一个线程的线程池，使生产者和消费者在两个线程上工作
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // SubmissionPublisher第二个参数控制缓存数据的个数（内部有个计算的公式）
        SubmissionPublisher<Integer> sb = new SubmissionPublisher<>(executorService, 10);
        System.out.println("getMaxBufferCapacity: " + sb.getMaxBufferCapacity());

        IntSubscriber intSubscriber = new IntSubscriber();
        sb.subscribe(intSubscriber);
        for (int i = 0; i < 20; i++) {
            System.out.println(Thread.currentThread().getName() + " |  发布数据: " + i);
            sb.submit(i);
        }
        sb.close();
        executorService.shutdown();
        Thread.sleep(10000);
    }
}
