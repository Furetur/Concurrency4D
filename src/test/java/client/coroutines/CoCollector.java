package client.coroutines;

import com.github.furetur.concurrency4d.Coroutine;
import com.github.furetur.concurrency4d.ReceiveChannel;
import com.github.furetur.concurrency4d.SendChannel;
import com.github.furetur.concurrency4d.Log;


import java.util.LinkedList;
import java.util.List;

public class CoCollector<T> extends Coroutine {
    ReceiveChannel<T> input;
    SendChannel<List<T>> output;

    Log log = new Log(this);

    public CoCollector(ReceiveChannel<T> input, SendChannel<List<T>> output) {
        super(List.of(input), List.of(output));
        this.input = input;
        this.output = output;
    }

    @Override
    protected void run() {
        var result = new LinkedList<T>();

        var msg = input.receive();
        while (msg.isValue()) {
            result.addLast(msg.value());
            log.debug(() -> "collected " + result);
            msg = input.receive();
        }
        log.debug(() -> "closing " + output);
        output.send(result);
    }
}
