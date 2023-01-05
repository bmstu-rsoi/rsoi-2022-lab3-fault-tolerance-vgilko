package ru.gilko.gatewayimpl.wrapper;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@Data
public class TryingWrapper {
    public static final int TIMEOUT = 10;
    private LocalDateTime timoutTimestamp;
    private LocalDateTime lastCall;
    private Supplier<Boolean> runnable;
    private UUID uuid;


    public TryingWrapper(Supplier<Boolean> runnable) {
        timoutTimestamp = LocalDateTime.now().plusSeconds(TIMEOUT);
        uuid = UUID.randomUUID();
        this.runnable = runnable;
    }
}
