package ru.gilko.gatewayimpl.service.impl;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.gilko.gatewayimpl.wrapper.TryingWrapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;

@Component
public class Trying {

    public static final int SLEEP_TIME = 1000;
    public static final int CALL_TIMEOUT = 1000;

    private final Queue<TryingWrapper> requests = new LinkedBlockingDeque<>();
    private final Set<Integer> currentRequestHashes = new HashSet<>();

    public void addRequest(Integer hash, Supplier<Boolean> runnable) {
        int fullHash = buildFullHash(hash, runnable);

        if (!currentRequestHashes.contains(fullHash)) {
            currentRequestHashes.add(fullHash);
            requests.add(new TryingWrapper(runnable));
        }
    }

    private int buildFullHash(Integer hash, Supplier<Boolean> runnable) {
        return (hash.toString() + runnable.hashCode()).hashCode();
    }

    @Async
    public void resendRequests() throws InterruptedException {
        while (true) {
            TryingWrapper request = requests.peek();

            if (request != null) {
                processRequest(request);
            } else {
                Thread.sleep(SLEEP_TIME);
            }
        }
    }

    private void processRequest(TryingWrapper request) {
        LocalDateTime lastCall = request.getLastCall();
        LocalDateTime currentTime = LocalDateTime.now();

        requests.poll();

        if (isTimeout(request, currentTime)) {
            request.getRunnable().get();
        } else {
            if (lastCall == null || isNeedCall(lastCall, currentTime)) {
                boolean isValidRequest = request.getRunnable().get();

                if (!isValidRequest) {
                    request.setLastCall(currentTime);
                    requests.add(request);
                }
            } else {
                requests.add(request);
            }
        }
    }

    private boolean isTimeout(TryingWrapper request, LocalDateTime currentTime) {
        return currentTime.isAfter(request.getTimoutTimestamp());
    }

    private boolean isNeedCall(LocalDateTime lastCall, LocalDateTime currentTime) {
        return ChronoUnit.NANOS.between(lastCall, currentTime) > CALL_TIMEOUT;
    }
}
