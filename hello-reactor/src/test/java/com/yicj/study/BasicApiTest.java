package com.yicj.study;

import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * BasicApiTest
 * </p>
 *
 * @author yicj
 * @since 2024年06月24日 22:11
 */
@Slf4j
// https://blog.csdn.net/gnwu1111/article/details/136917938
class BasicApiTest {

    //Logger log = LoggerFactory.getLogger(BasicApiTest.class) ;
    @Test
    void hello(){
        log.info("hello world");
        Flux.just(1, 2, 3, 45, 0, 8).doOnNext(value -> {
            System.out.println("一起玩" + value + Thread.currentThread());
        }).subscribe(new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                System.out.println("绑定成功" + Thread.currentThread());
                request(1); // 向发布者请求 1 次数据，n 表示请求 n 次数据
                // requestUnbounded(); // 请求无限次数据，用了该方法则 onNext 中无需再写 request(1)
            }
            @Override
            protected void hookOnNext(Integer value) {
                //System.out.println("当前数据:" + value + Thread.currentThread());
                log.info("当前数据:{}", value);
                if (value == 45) {
                    cancel(); // 取消流
                }
                //request(1); // 继续要数据
            }
        });
    }

    @Test
    void testThread() throws Exception {
        System.out.println(Thread.currentThread().getName());
        Scheduler publisherScheduler = Schedulers.newParallel("Publisher");
        Scheduler subscriberScheduler = Schedulers.newParallel("Subscriber");
        Flux.range(1, 3)
                .doOnNext(item -> System.out.println("Publisher Default: [" + item + "] "+ Thread.currentThread().getName()))
                .publishOn(publisherScheduler)
                .doOnNext(item -> System.out.println("Publisher publishOn: [" + item + "] "+ Thread.currentThread().getName()))
                .subscribeOn(subscriberScheduler)
                .doOnNext(item -> System.out.println("Publisher subscribeOn: [" + item + "] "+Thread.currentThread().getName()))
                .subscribe(item -> System.out.println("Subscriber default: [" + item + "] "+ Thread.currentThread().getName()));
        Thread.sleep(1000);
    }

    @Test
    void testDelayElements() throws IOException, InterruptedException {
        Flux.range(0,5).delayElements(Duration.ofMillis(500)).log().subscribe();
        Thread.sleep(1000);
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


    /**
     * 日志显示：log()，下面为对应打印的解释
     * onSubscribe：流被订阅
     * request(unbounded)：请求无限数据
     * onNext(2): 每个数据到达
     * onComplete：流结束
     */
    @Test
    public void testLog() {
        Flux.just(1, 2, 3, 45, 0, 8)
//				.log()
                .filter(i -> i % 2 == 0)
                .log()
                .subscribe();
    }

    /**
     * 同步环境 生成 0~10 的序列
     * sink 是通道，sink.next(obj) 向下游发布 obj 对象
     * sink.complete() 迭代完成
     */
    @Test
    public void testGenerate() {
        Flux.generate( () -> 0, (state, sink) -> {
            sink.next(state);
            if (state == 10) sink.complete();
            return state + 1;
        }).log().subscribe();
    }

    /**
     * 多线程环境 create：常用于监听事件，并将事件信息传入管道
     * [ INFO] (main) onSubscribe(FluxCreate.BufferAsyncSink)
     * [ INFO] (main) request(unbounded)
     * 做家务
     * [ INFO] (main) onNext(家务)
     */
    @Test
    public void testCreate() {
        interface Listener {
            void afterDoSomeThing(String event);
        };
        class DoSomeThing {

            public void doSomeThing(String thing) {
                System.out.println("做" + thing);
                for (Listener listener : afterListenerList) {
                    listener.afterDoSomeThing(thing);
                }
            }

            private final List<Listener> afterListenerList = new ArrayList<>();

            public void register(Listener listener) {
                afterListenerList.add(listener);
            }
        }

        DoSomeThing doSomeThing = new DoSomeThing();
        Flux.create(sink -> doSomeThing.register(sink::next)).log().subscribe();
        doSomeThing.doSomeThing("家务");
    }

    /**
     * 当不使用缓冲区时，每有 1 个元素便会直接发给订阅者
     * buffer(n)：缓冲区，凑够数量 n 再发送给订阅者，订阅者接收到的将是 n 个元素组成的 ArrayList 集合
     * request(n)含义：找发布者请求 n 次数据，每次请求 bufferSize 个数据，总共能得到 n * bufferSize 个数据
     * [ INFO] (main) onNext([1, 2, 3])
     * [ INFO] (main) onNext([4, 5, 6])
     * [ INFO] (main) onNext([7, 8, 9])
     * [ INFO] (main) onNext([10])
     */
    @Test
    public void testBuffer() {
        Flux.range(1, 10).buffer(3).log().subscribe();
    }

    /**
     * 预请求
     * limitRate(n)：首次 request(n)，请求了 75% * n(四舍五入) 次后直接请求 request(75% * n) ，且后续均 request(75% * n)
     */
    @Test
    public void testLimitRate() {
        Flux.range(1, 10).log().limitRate(4).subscribe();
    }

    /**
     * map：一一映射
     */
    @Test
    public void testMap() {
        Flux.range(1, 10).map(value -> value + 1).log().subscribe();
    }

    /**
     * handle：类似于 map 可用于实现一对一映射，但更加强大的是sink.next()可以传不同类型
     */
    @Test
    public void testHandle() {
        Flux.range(1, 10)
                .handle((value, sink) -> {
                    if (value % 2 == 0) sink.next(value);
                    else sink.next("字符串" + value);
                }).log().subscribe();
    }

    /**
     * 扁平化处理：{ "张 三", "李 四"} 变为 { "张", "三", "李", "四"}
     */
    @Test
    public void testFlatMap() {
        Flux.just("张 三", "李 四")
                .flatMap(item -> {
                    String[] strings = item.split(" ");
                    return Flux.fromArray(strings);
                })
                .log()
                .subscribe();
    }

    /**
     * 流连接
     * concatWith：两个流元素类型要求一致
     * concat：静态方法，元素类型无要求
     * concatMap：将流中单个元素映射成其他流，再将所有流组合成一个流
     */
    @Test
    public void testConcat() {
        Flux.just(1, 2).concatWith(Flux.just(3,4)).log().subscribe();

        Flux.concat(Flux.just(1, 2), Flux.just("a",4)).log().subscribe();

        Flux.just(1, 2).concatMap(i -> Flux.just("key" + i, "value" + i)).log().subscribe();
    }

    /**
     * 把流变形成新数据
     * transform：无状态转换; 原理，无论多少个订阅者，transform只执行一次
     * transformDeferred：有状态转换; 每个订阅者transform都只执行一次
     */
    @Test
    public void testTransformDeferred() {
        AtomicInteger atomicInteger = new AtomicInteger(1);
        Flux<String> flux = Flux.just("a", "b", "c").transformDeferred(
                items -> {
                    System.out.println("调用次数：" + atomicInteger.getAndIncrement());
                    return items.map(String::toUpperCase);
                });
        flux.subscribe(System.out::println);
        flux.subscribe(System.out::println);
    }

    @Test
    void testTransform() {
        AtomicInteger atomicInteger = new AtomicInteger(1);
        Flux<String> flux = Flux.just("a", "b", "c").transform(
                items -> {
                    System.out.println("调用次数：" + atomicInteger.getAndIncrement());
                    return items.map(String::toUpperCase);
                });
        flux.subscribe(System.out::println);
        flux.subscribe(System.out::println);
    }


    /**
     * 空流是 Flux.empty(); Flux.just(null) 会报空指针异常
     * switchIfEmpty：如果是空流则转换成其他流
     * defaultIfEmpty：如果是空流则传入单个元素
     */
    @Test
    public void testEmpty() {
        Flux.empty().switchIfEmpty(Flux.empty()).defaultIfEmpty("a").log().subscribe();
    }

    /**
     * Flux.merge：按照流中每个元素发布的时间顺序合并流
     * Flux.mergeSequential：按照流发布的时间顺序合并流，如流1中有多个元素且流1元素最先发布，则流1中元素会被合并到最开头
     */
    @Test
    public void testMerge() throws IOException {
        Flux.merge(
                Flux.range(0, 2).delayElements(Duration.ofMillis(300)),
                Flux.range(5, 2).delayElements(Duration.ofMillis(100)),
                Flux.range(10, 2).delayElements(Duration.ofMillis(200))
        ).log().subscribe();
        System.in.read();
    }

    /**
     * zip：将两个流压缩成元组，多余单个元素会被忽略
     * Tuple：元组，[value1, value2]
     */
    @Test
    public void testZip() {
        Flux.range(0, 2).zipWith(Flux.range(0,3)).log().subscribe();
    }

    /**
     * 重试，会从头重试
     */
    @Test
    public void testRetry() throws IOException {
        Flux.just(1)
                .log()
                .delayElements(Duration.ofSeconds(5))
                .timeout(Duration.ofSeconds(1))
                .retry(2)
                .onErrorReturn(999)
                .subscribe(value -> System.out.println("value = " + value));
        System.in.read();
    }

    @Test
    void testOnErrorReturn() {
        Flux.just(1,2,3)
                .log()
                .map(i -> i / 0)
                .doOnError(e -> System.out.println("e = " + e))
                .timeout(Duration.ofSeconds(1))
                .onErrorReturn(999)
                .subscribe(value -> System.out.println("value = " + value));
    }

    @Test
    public void testCache() throws IOException {
        Flux<Integer> cache = Flux.range(1, 10)
                .delayElements(Duration.ofSeconds(1))
                .cache(2);// 缓存，不传参表示缓存所有元素
        cache.log().subscribe(); // 会输出 1~10
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // 缓存后用子线程输出缓存的3和4，之后和上面订阅者一样使用同一线程池输出
            // 如果不使用 cache 缓存，则默认会每10秒逐个输出 1~10
            cache.subscribe(item -> System.out.println("子线程输出：" + item));
        }).start();
        System.in.read();
    }

    /**
     * 阻塞式 API
     */
    @Test
    public void testBlock() {
        List<Integer> list = Flux.range(1, 1000).collectList().block();
        System.out.println(list);
    }

    /**
     * 响应式中的ThreadLocal，响应式编程中 ThreadLocal机制失效
     */
    @Test
    public void testContextAPI () {
        Flux.just(1,2,3)
                .transformDeferredContextual((flux,context)->{
                    System.out.println("flux = " + flux);
                    System.out.println("context = " + context);
                    return flux.map(i->i+"==>"+context.get("prefix"));
                })
                //上游能拿到下游的最近一次数据
                .contextWrite(Context.of("prefix","哈哈"))
                //ThreadLocal共享了数据，上游的所有人能看到; Context由下游传播给上游
                .subscribe(v-> System.out.println("v = " + v));
    }

}
