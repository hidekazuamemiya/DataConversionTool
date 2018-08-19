package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class Token {

    public String kind;
    public String value;
    public Token left;
    public Token right;
    public Token ident;
    public List<Token> params;
    public List<Token> block;
    public List<Token> blockOfElse;

    @Override
    public String toString() {
        return getKind() + " \"" + getValue() + "\"";
    }

    public String indent(String parentIndent) {
        String indent = "  ";
        StringBuilder b = new StringBuilder();
        b.append(parentIndent).append(toString()).append("\n");
        if (getLeft() != null) {
            b.append(parentIndent).append("[left]\n").append(getLeft().indent(parentIndent + indent));
        }
        if (getRight() != null) {
            b.append(parentIndent).append("[right]\n").append(getRight().indent(parentIndent + indent));
        }
        if (getIdent() != null) {
            b.append(parentIndent).append("[ident]\n").append(getIdent().indent(parentIndent + indent));
        }
        if (getParams() != null) {
            b.append(parentIndent).append("[params]\n");
            for (Token param : getParams()) {
                b.append(param.indent(parentIndent + indent));
            }
        }
        if (getBlock() != null) {
            b.append(parentIndent).append("[block]\n");
            for (Token expr : getBlock()) {
                b.append(expr.indent(parentIndent + indent));
            }
        }
        if (getBlockOfElse() != null) {
            b.append(parentIndent).append("[blockOfElse]\n");
            for (Token expr : getBlockOfElse()) {
                b.append(expr.indent(parentIndent + indent));
            }
        }
        return b.toString();
    }

    public String paren() {
        if (getLeft() == null && getRight() == null && getParams() == null) {
            return getValue();
        } else if (getLeft() != null && getRight() == null && getParams() == null) {
            return getValue() + getLeft().paren();
        } else {
            StringBuilder b = new StringBuilder();
            b.append("(");
            if (getLeft() != null) {
                b.append(getLeft().paren()).append(" ");
            }
            b.append(getValue());
            if (getRight() != null) {
                b.append(" ").append(getRight().paren());
            }
            if (getParams() != null) {
                if (getParams().size() > 0) {
                    b.append(getParams().get(0).paren());
                    for (int i = 1; i < getParams().size(); ++i) {
                        b.append(", ").append(getParams().get(i).paren());
                    }
                }
                b.append(")");
            }
            b.append(")");
            return b.toString();
        }
    }

}