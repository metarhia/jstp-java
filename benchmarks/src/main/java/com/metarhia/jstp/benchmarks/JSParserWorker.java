package com.metarhia.jstp.benchmarks;


import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Created by lundibundi on 5/20/17.
 */
class JSParserWorker implements Worker {

  private final Blackhole bh;

  private String inputData;

  private JSParser parser;

  public JSParserWorker(String input, Blackhole bh) {
    this.bh = bh;
    this.inputData = input;
    this.parser = new JSParser();
//    this.parser.setJsObjectClass(ArrayMap.class);
  }

  @Override
  public void work() {
    parser.setInput(inputData);
    try {
      bh.consume(parser.parse());
    } catch (JSParsingException e) {
      e.printStackTrace();
    }
  }
}
