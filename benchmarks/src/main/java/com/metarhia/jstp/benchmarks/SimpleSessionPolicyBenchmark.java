package com.metarhia.jstp.benchmarks;

import static org.mockito.Mockito.mock;

import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.connection.Message;
import com.metarhia.jstp.connection.MessageType;
import com.metarhia.jstp.session.SimpleSessionPolicy;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class SimpleSessionPolicyBenchmark {

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(SimpleSessionPolicyBenchmark.class.getSimpleName())
//        .addProfiler(StackProfiler.class)
//        .addProfiler(HotspotRuntimeProfiler.class)
//        .addProfiler(HotspotMemoryProfiler.class)
//        .addProfiler(LinuxPerfProfiler.class)
//        .addProfiler(GCProfiler.class)
        .build();

    new Runner(opt).run();
  }

  @State(Scope.Thread)
  public static class BenchmarkState {

    @Param({"5", "10", "50", "100"})
    public int messagesNumber;
  }

  @Benchmark
  @BenchmarkMode({Mode.Throughput, Mode.AverageTime})
  @Warmup(iterations = 15)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  @Fork(1)
  public void putRestoreBenchmark(BenchmarkState state) {
    SimpleSessionPolicy policy = new SimpleSessionPolicy();
    policy.setConnection(mock(Connection.class));

    for (int i = 0; i < state.messagesNumber; ++i) {
      policy.onMessageSent(new Message(i, MessageType.CALL));
    }
    policy.restore(state.messagesNumber / 3);
  }
}
