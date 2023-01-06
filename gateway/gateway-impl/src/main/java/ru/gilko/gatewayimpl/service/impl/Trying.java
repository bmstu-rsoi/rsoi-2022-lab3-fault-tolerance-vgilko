package ru.gilko.gatewayimpl.service.impl;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class Trying {

    public static final int SLEEP_TIME = 2000;
    public static final int CALL_TIMEOUT = 2000;

    private final Queue<TryingWrapper> requests = new LinkedBlockingDeque<>();
    private final Set<Integer> currentRequestHashes = new HashSet<>();

    public void addRequest(Integer hash, Supplier<Boolean> runnable) {
        int fullHash = buildFullHash(hash, runnable);

        if (!currentRequestHashes.contains(fullHash)) {
            currentRequestHashes.add(fullHash);
            TryingWrapper request = new TryingWrapper(runnable, fullHash);

            requests.add(request);
            log.debug("Added new request {}", request.getFullHash());
        }
    }

    private int buildFullHash(Integer hash, Supplier<Boolean> runnable) {
        return (hash.toString() + runnable.hashCode()).hashCode();
    }

    @Async
    public void resendRequests() throws InterruptedException {
        log.debug("Start resending requests.");

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

        if (!isTimeout(request, currentTime)) {
            if (lastCall == null || isNeedCall(lastCall, currentTime)) {
                boolean isValidRequest = request.getRunnable().get();
                request.setLastCall(currentTime);

                if (!isValidRequest) {
                    requests.add(request);

                    log.debug("Request {} should be reprocessed.", request.getFullHash());
                } else {
                    log.debug("Request {} was processed", request.getFullHash());
                    currentRequestHashes.remove(request.getFullHash());
                }
            } else {
                requests.add(request);
            }
        } else {
            log.debug("Request {} was deleted from resending queue due timeout", request.getFullHash());
        }
    }

    private boolean isTimeout(TryingWrapper request, LocalDateTime currentTime) {
        return currentTime.isAfter(request.getTimoutTimestamp());
    }

    private boolean isNeedCall(LocalDateTime lastCall, LocalDateTime currentTime) {
        return ChronoUnit.NANOS.between(lastCall, currentTime) > CALL_TIMEOUT;
    }
}
