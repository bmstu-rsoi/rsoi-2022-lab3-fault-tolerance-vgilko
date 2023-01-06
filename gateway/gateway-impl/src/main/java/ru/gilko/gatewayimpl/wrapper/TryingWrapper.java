package ru.gilko.gatewayimpl.wrapper;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Data
public class TryingWrapper {
    public static final int TIMEOUT = 50;
    private LocalDateTime timoutTimestamp;
    private LocalDateTime lastCall;
    private Supplier<Boolean> runnable;
    private int fullHash;


    public TryingWrapper(Supplier<Boolean> runnable, int fullHash) {
        timoutTimestamp = LocalDateTime.now().plusSeconds(TIMEOUT);
        this.fullHash = fullHash;
        this.runnable = runnable;
    }
}
