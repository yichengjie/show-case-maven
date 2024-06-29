package com.yicj.study;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 * <p>
 * ErrorHandlerTest
 * </p>
 *
 * @author yicj
 * @since 2024年06月29日 22:35
 */
@Slf4j
public class ErrorHandlerTest {

    @Test
    public void onErrorReturn() {
        Flux.just(1,0,2)
                .map(item -> 2 / item)
                .onErrorReturn(999)
                .log() // 输出 onNext(2)，onNext(999)，onComplete()
                .subscribe();
    }

    @Test
    public void onErrorResume() {
        Flux.just(1,0,2)
                .map(item -> 2 / item)
                .onErrorResume(throwable -> {
                    System.out.println("异常：" + throwable.getMessage());
                    return Flux.just(9,0,9);
                })
                .log() // onNext(2)，onNext(9)，onNext(0)，cancel()
                .map(item -> 3 / item)
                .onErrorResume(throwable -> Flux.error(new RuntimeException(throwable)))
                .subscribe(
                        item -> log.info("onErrorMap item : {}", item),
                        throwable -> log.error("onErrorMap error : {}", throwable.getMessage())
                );
    }

    @Test
    public void onErrorMap() {
        Flux.just(1,0,2)
                .map(item -> 2 / item)
                .onErrorMap(throwable -> new RuntimeException(throwable))
                .subscribe(
                    item -> log.info("onErrorMap item : {}", item),
                    throwable -> log.error("onErrorMap error : {}", throwable.getMessage())
                );
        log.info("onErrorMap end ..." );
    }

    /**
     * doOnError ：发生异常时会执行该方法
     * onErrorContinue ：发生异常后会吃掉异常继续执行
     * doFinally ：必定执行
     */
    @Test
    public void onErrorContinue() {
        Flux.just(1,0,2,3)
                .log() // request(unbounded),onNext(1),onNext(0),request(1),onNext(2),onNext(3),onComplete()
                .map(item -> 2 / item)
                .doOnError(throwable -> {
                    System.out.println("可获取到异常" + throwable.getMessage());
                })
                .onErrorContinue((throwable, item) -> {
                    System.out.println("发生了异常：" + throwable.getMessage());
                    System.out.println("导致异常发生的值：" + throwable.getMessage());
                })
                .doFinally(signalType -> {
                    System.out.println("流信号类型" + signalType);
                })
                .subscribe(value -> log.info("value: {}", value)) ;
    }

    @Test
    void hello(){
        Flux.just("1", "2", null, "4", "5")
                .onErrorContinue((throwable, item) -> {
                    System.out.println("发生了异常：" + throwable.getMessage());
                    System.out.println("导致异常发生的值：" + throwable.getMessage());
                })
                .subscribe(value -> log.info("value: {}", value)) ;
    }

}
