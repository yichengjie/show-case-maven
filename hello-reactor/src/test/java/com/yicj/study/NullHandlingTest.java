package com.yicj.study;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 * <p>
 * NullHandlingTest
 * </p>
 *
 * @author yicj
 * @since 2024年06月30日 10:00
 */
@Slf4j
public class NullHandlingTest {
    @Test
    void hello(){
        Flux<String> flux = Flux.just("event1", null, "event2", null, "event3");
        flux.flatMap(event -> {
                if (event == null) {
                    // 返回一个特定的错误事件或者一个空的事件流
                    return Flux.error(new NullPointerException("Event is null"));
                }
                // 处理非null的事件
                return Flux.just(handleEvent(event));
            })
//            .onErrorContinue((throwable, item) -> {
//                System.out.println("发生了异常：" + throwable.getMessage());
//                System.out.println("导致异常发生的值：" + throwable.getMessage());
//            })
            .onErrorResume(e -> {
                // 错误处理，例如记录日志或者返回默认值
                System.err.println("Error: " + e.getMessage());
                // 返回一个空的事件流作为错误处理的结果
                return Flux.empty();
            })
            .subscribe(System.out::println);
    }

    private static String handleEvent(String event) {
        // 事件处理逻辑
        return "Handled: " + event;
    }
}
