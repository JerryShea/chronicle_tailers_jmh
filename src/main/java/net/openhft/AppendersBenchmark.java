package net.openhft;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by Jerry Shea on 12/04/18.
 */
@State(Scope.Thread)
public class AppendersBenchmark {

    final ExcerptAppender appender = SingleChronicleQueueBuilder.binary(OS.TARGET + "/data_append/append/").build().acquireAppender();
    final ExcerptAppender appenderPretouch = SingleChronicleQueueBuilder.binary(OS.TARGET + "/data_append/append_pt/").build().acquireAppender();
    long index = 0;

    static {
        IOTools.deleteDirWithFiles(OS.TARGET + "/data_append/", 10);
    }

    public AppendersBenchmark () {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Jvm.pause(100);
                appenderPretouch.pretouch();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Benchmark
    @BenchmarkMode({Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long tailerRO_data() {
        return append(appender);
    }

    private long append(ExcerptAppender tailer) {
        try (DocumentContext dc = tailer.writingDocument()) {
            dc.wire().write("1").int64(index);
        }
        return ++index;
    }
}
