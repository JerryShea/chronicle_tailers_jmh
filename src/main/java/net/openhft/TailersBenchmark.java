package net.openhft;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

/**
 * Created by Jerry Shea on 12/04/18.
 */
@State(Scope.Thread)
public class TailersBenchmark {

    // queue directory exists but no .cq4 file
    ExcerptTailer tailerRW_nodata = SingleChronicleQueueBuilder.binary(OS.TARGET + "/data/nodata/").build().createTailer();
    // as above but read only
    ExcerptTailer tailerRO_nodata = SingleChronicleQueueBuilder.binary(OS.TARGET + "/data/nodata/").readOnly(true).build().createTailer();
    // queue directory does not exist, read only
    ExcerptTailer tailerRO_empty = SingleChronicleQueueBuilder.binary(OS.TARGET + "/data/empty/").readOnly(true).build().createTailer();
    // queue directory exists, 1 entry in queue
    ExcerptTailer tailerRW_data = SingleChronicleQueueBuilder.binary(OS.TARGET + "/data/data/").build().createTailer();
    // as above but read only
    ExcerptTailer tailerRO_data = SingleChronicleQueueBuilder.binary(OS.TARGET + "/data/data/").readOnly(true).build().createTailer();

    @BenchmarkMode({Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long tailerRW_nodata() {
        return readFromTailer(tailerRW_nodata);
    }

    @Benchmark
    @BenchmarkMode({Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long tailerRO_nodata() {
        return readFromTailer(tailerRO_nodata);
    }

    @Benchmark
    @BenchmarkMode({Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long tailerRW_data() {
        return readFromTailer(tailerRW_data);
    }

    @Benchmark
    @BenchmarkMode({Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long tailerRO_empty() {
        return readFromTailer(tailerRO_empty);
    }

    @Benchmark
    @BenchmarkMode({Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long tailerRO_data() {
        return readFromTailer(tailerRO_data);
    }

    private long readFromTailer(ExcerptTailer tailer) {
        try (DocumentContext dc = tailer.readingDocument()) {
            if (! dc.isPresent())
                return Long.MIN_VALUE;
            return dc.wire().read("1").int64();
        }
    }

    public static void prepareData() {
        ExcerptTailer tailerRW_data = SingleChronicleQueueBuilder.binary(OS.TARGET + "/data/data/").build().createTailer();
        ChronicleQueue queue = tailerRW_data.queue();
        ExcerptAppender app = queue.acquireAppender();
        try (DocumentContext dc = app.writingDocument()) {
            dc.wire().write("1").int64(System.currentTimeMillis());
        }
    }

    public static void main(String[] args) throws Exception {

        IOTools.deleteDirWithFiles(OS.TARGET + "/data/", 10);
        prepareData();

        Options opt = new OptionsBuilder()
                .include(TailersBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(5))
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
