package com.metarhia.jstp.benchmarks;

import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class JSParserBenchmark {

  private JSSerializerWorker serializerWorker;

  private JSParserWorker parserWorker;

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(JSParserBenchmark.class.getSimpleName())
//        .addProfiler(StackProfiler.class)
//        .addProfiler(GCProfiler.class)
        .build();

    new Runner(opt).run();
  }

  @Setup
  public void setup(final Blackhole bh) {
    parserWorker = new JSParserWorker(Data.JSTP_COMPLEX_OBJECT, bh);
    try {
      Object parsed = JSParser.parse(Data.JSTP_COMPLEX_OBJECT);
      serializerWorker = new JSSerializerWorker(parsed, bh);
    } catch (JSParsingException e) {
      throw new RuntimeException("Cannot parse data for serialization benchmark");
    }
  }

  @Benchmark
  @BenchmarkMode({Mode.Throughput, Mode.AverageTime})
  @Warmup(iterations = 15)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  @Fork(2)
  public void parserBenchmark() {
    parserWorker.work();
  }

  @Benchmark
  @BenchmarkMode({Mode.Throughput, Mode.AverageTime})
  @Warmup(iterations = 15)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  @Fork(2)
  public void serializerBenchmark() {
    serializerWorker.work();
  }
}
