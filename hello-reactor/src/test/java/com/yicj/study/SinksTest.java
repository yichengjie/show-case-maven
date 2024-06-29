package com.yicj.study;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * SinksTest
 * </p>
 *
 * @author yicj
 * @since 2024年06月29日 22:52
 */
@Slf4j
public class SinksTest {

    @Test
    public void testSinks() throws InterruptedException, IOException {
//        Sinks.many(); //发送Flux数据
//        Sinks.one(); //发送Mono数据

        // Sinks： 接受器，数据管道，所有数据顺着这个管道往下走的

        Sinks.many().unicast(); //单播：  这个管道只能绑定单个订阅者（消费者）
        Sinks.many().multicast();//多播： 这个管道能绑定多个订阅者
        Sinks.many().replay();//重放： 这个管道能重放元素。 是否给后来的订阅者把之前的元素依然发给它；

        // 单播模式
        Sinks.Many<Object> many = Sinks.many().unicast().onBackpressureBuffer(new PriorityQueue<>(2));

        for (int i = 0; i < 5; i++) {
            many.tryEmitNext(i); // 将元素放入管道
        }

        // 单播只能订阅一次，二次订阅会报错，如允许多个订阅者可用 多播模式
        many.asFlux().subscribe(item -> System.out.println("单播模式：" + item));

        // 重放模式：底层利用队列进行缓存之前数据
        Sinks.Many<Object> limit = Sinks.many().replay().limit(2); // 缓存两个

        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    limit.tryEmitNext(i); // 将元素放入管道
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        limit.asFlux().subscribe(item -> System.out.println("重放模式订阅1：" + item)); // 0、1、2、3、4

        TimeUnit.SECONDS.sleep(4);

        limit.asFlux().subscribe(item -> System.out.println("重放模式订阅2：" + item)); // 2、3、4

        System.in.read();
    }

}
