package io.battery.util;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit-test")
@Disabled
public class ConcurrencyTest {
    private void waitAndDoNothing(String name, Duration duration) {
        Instant endsAt = Instant.now().plus(duration);
        System.out.println("Running " + name);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.print(".");
                System.out.flush();
                TimeUnit.MILLISECONDS.sleep(1000);
                if (Instant.now().isAfter(endsAt)) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Quitting " + name);
    }

    private void waitAndThrow(String name, Duration duration) {
        Instant endsAt = Instant.now().plus(duration);
        System.out.println("Running " + name);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.printf(".");
                System.out.flush();
                TimeUnit.MILLISECONDS.sleep(1000);
                if (Instant.now().isAfter(endsAt)) {
                    throw new IllegalStateException("Fail " + name);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Quitting " + name);
    }

    @Test
    public void givenChainedFutures_expectCancellation() {
        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() -> {
            waitAndDoNothing("f1", Duration.ofSeconds(5));
            return 1;
        }).handle((integer, throwable) -> {
            System.out.printf("f1 completed with %d and error %s%n", integer, throwable);
            return 0;
        });

        CompletableFuture<Integer> f2 = f1.thenApplyAsync(integer -> {
            waitAndThrow("f2", Duration.ofSeconds(5));
            return integer + 1;
        }).handle((integer, throwable) -> {
            System.out.printf("f2 completed with %d and error %s%n", integer, throwable);
            throw new UndeclaredThrowableException(throwable);
        });

        CompletableFuture<Integer> f3 = f2.thenApplyAsync(integer -> {
            waitAndDoNothing("f3", Duration.ofSeconds(5));
            return integer + 1;
        }).handle((integer, throwable) -> {
            System.out.printf("f3 completed with %d and error %s%n", integer, throwable);
            throw new UndeclaredThrowableException(throwable);
        });

        CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3);
        try {
            all.join();
        } catch (CancellationException e) {
            System.out.println("Cancelled");
            e.printStackTrace();
        } catch (CompletionException e) {
            System.out.println("Completion error");
            e.printStackTrace();
        }
    }
}
