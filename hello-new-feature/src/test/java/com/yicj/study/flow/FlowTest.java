package com.yicj.study.flow;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
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


    @Test
    void test3() throws Exception {
        // 2. 定义订阅者：订阅者可订阅发布者发布的消息
        Flow.Subscriber<String> subscriber = new Flow.Subscriber<>() {
            // 保存绑定关系
            private Flow.Subscription subscription;
            // 绑定订阅消息时触发
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                System.out.println("订阅事件发生了");
                subscription.request(1); // 背压模式，订阅者向发布者请求发布信息
                // Thread[ForkJoinPool.commonPool-worker-1,5,main]
                System.out.println("订阅者线程：" + Thread.currentThread());
            }

            @Override
            public void onNext(String item) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("本轮：" + item);
                subscription.request(1);
                if (item.equals("原材料2")) {
						throw new RuntimeException("自控异常");
                }
                // Thread[ForkJoinPool.commonPool-worker-1,5,main]
                System.out.println("订阅者线程Next：" + Thread.currentThread());
            }
            @Override
            public void onError(Throwable throwable) {
                System.out.println("异常了：" + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("完成了");
            }
        };


        // 1. 定义发布者：可发布消息
        try (SubmissionPublisher<String> publisher = new SubmissionPublisher<>()){
            // 3. 发布者的订阅者列表中添加这名订阅者，后续发布信息会发送给订阅者
            publisher.subscribe(subscriber);
            // 4. 发布者发布消息
            for (int i = 0; i < 10; i++) {
                //
                System.out.println("发布者线程：" + Thread.currentThread() + ", 发布数据： " + i); // Thread[main,5,main]
                publisher.submit("原材料" + i);
            }
            System.out.println("主线程：" + Thread.currentThread()); // Thread[main,5,main]
            publisher.close(); // 这样才会回调 onComplete 方法
            System.in.read();
        }
    }



}
