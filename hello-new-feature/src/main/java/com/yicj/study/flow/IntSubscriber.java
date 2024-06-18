package com.yicj.study.flow;

import lombok.SneakyThrows;

import java.util.concurrent.Flow;

public class IntSubscriber implements Flow.Subscriber<Integer>{

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {

        System.out.println(Thread.currentThread().getName() + " | 订阅，开始请求数据");
        this.subscription = subscription;

        // 第一次请求获取数据的个数，如果不行将不会获取数据
        this.subscription.request(1);
    }

    @SneakyThrows
    @Override
    public void onNext(Integer item) {

        // 消费者5秒消费一个
        Thread.sleep(5000);
        System.out.println(Thread.currentThread().getName() + " | 订阅者接收数据: " + item);

        // 这里只在item等于0的时候使用request请求五次，以演示`request`的作用
        if (item == 0){
            // 下一次请求获取数据的个数，如果这里不写将不在请求数据
            this.subscription.request(5);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("订阅者接收数据出现异常：" + throwable);
    }

    @Override
    public void onComplete() {

        System.out.println(Thread.currentThread().getName() + " | 订阅完成");
    }
}