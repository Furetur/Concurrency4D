package client.coroutines;

import me.furetur.concurrency4d.Coroutine;
import me.furetur.concurrency4d.ReceiveChannel;
import me.furetur.concurrency4d.SendChannel;

import java.util.LinkedList;
import java.util.List;

public class CoCollector<T> extends Coroutine {
    ReceiveChannel<T> input;
    SendChannel<List<T>> output;

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
            System.out.println("collector: collected " + result);
            msg = input.receive();
        }
        System.out.println("collector: close");
        output.send(result);
    }
}
