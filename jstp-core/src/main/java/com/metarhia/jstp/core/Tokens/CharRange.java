package com.metarhia.jstp.core.Tokens;

/**
 * Created by lundibundi on 7/4/16.
 */
public final class CharRange {

  private int beg;
  private int end;

  public CharRange(int beg, int end) {
    this.beg = beg;
    this.end = end;
  }

  public boolean contains(int ch) {
    return ch >= beg && ch <= end;
  }
}
