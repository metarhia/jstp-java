package com.metarhia.jstp.benchmarks;

import com.metarhia.jstp.core.JSSerializer;
import org.openjdk.jmh.infra.Blackhole;

public class JSSerializerWorker implements Worker {

  private final Blackhole bh;

  private Object inputData;

  public JSSerializerWorker(Object input, Blackhole bh) {
    this.bh = bh;
    this.inputData = input;
  }

  @Override
  public void work() {
    bh.consume(JSSerializer.stringify(inputData));
  }
}
