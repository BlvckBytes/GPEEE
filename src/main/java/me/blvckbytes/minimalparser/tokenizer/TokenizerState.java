package me.blvckbytes.minimalparser.tokenizer;

import java.util.Stack;

public class TokenizerState {

  public int row, col, charIndex;
  public Stack<Integer> colStack;

  public TokenizerState() {
    this.colStack = new Stack<>();
  }

  public TokenizerState copy() {
    TokenizerState copy = new TokenizerState();

    copy.row = this.row;
    copy.col = this.col;
    copy.charIndex = this.charIndex;
    copy.colStack = this.colStack;

    return copy;
  }
}
