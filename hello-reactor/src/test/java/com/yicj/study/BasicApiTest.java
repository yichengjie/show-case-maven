package com.yicj.study;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;

/**
 * <p>
 * BasicApiTest
 * </p>
 *
 * @author yicj
 * @since 2024年06月24日 22:11
 */
// https://blog.csdn.net/gnwu1111/article/details/136917938
class BasicApiTest {

    @Test
    void testThread() throws IOException {
        System.out.println(Thread.currentThread().getName());
        Scheduler publisherScheduler = Schedulers.newParallel("Publisher");
        Scheduler subscriberScheduler = Schedulers.newParallel("Subscriber");
        Flux.range(1, 3)
                .doOnNext(item -> System.out.println("Publisher Default:" + item + Thread.currentThread().getName()))
                .publishOn(publisherScheduler)
                .doOnNext(item -> System.out.println("Publisher publishOn:" + item + Thread.currentThread().getName()))
                .subscribeOn(subscriberScheduler)
                .doOnNext(item -> System.out.println("Publisher subscribeOn:" + item + Thread.currentThread().getName()))
                .subscribe(item -> System.out.println("Subscriber default:" + item + Thread.currentThread().getName()));
        System.in.read();
    }

    @Test
    public void testDelayElements() throws IOException {
        Flux.range(0,5).delayElements(Duration.ofMillis(500)).log().subscribe();
        System.in.read();
    }


    @Test
    void testParallel() throws IOException {
        // 将 1000 条数据，按每 10 个一组分片处理，用 4个线程跑该段逻辑，用 runOn 指定线程池
        Flux.range(1,1000)
                .buffer(10)
                .parallel(4)
                .runOn(Schedulers.newParallel("线程池"))
                .log()
                .flatMap(Flux::fromIterable)
                .collectSortedList(Integer::compareTo)
                .subscribe(v -> System.out.println("v=" + v));
        System.in.read();
    }

}
