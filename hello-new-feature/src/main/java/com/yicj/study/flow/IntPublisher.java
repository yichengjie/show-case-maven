package com.yicj.study.flow;

import java.util.concurrent.Flow;

public class IntPublisher implements Flow.Publisher<Integer>{
 
    @Override
    public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
        for(int i = 1; i <= 5; i++) {
            System.out.println(Thread.currentThread().getName() + " Publishing = " + i);
            // 将数据发给订阅者
            subscriber.onNext(i);
        }
        // 发出完成信号
        subscriber.onComplete();
    }
}