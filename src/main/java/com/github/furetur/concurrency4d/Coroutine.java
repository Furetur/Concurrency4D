package com.github.furetur.concurrency4d;

import java.util.List;

public abstract class Coroutine extends AsyncCoroutine {
    public Coroutine(List<ReceiveChannel<?>> inputs, List<SendChannel<?>> outputs) {
        super(inputs, outputs);
    }
}
