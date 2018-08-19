package com.example.demo.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.dto.Token;

/**
 * 字句解析ユーティリティ
 */

@Component
public class LexerUtil {

	private String text;
	private int i;
	private List<String> functionList;

	public LexerUtil init(String text, List<String> functionList) {
		i = 0;
		this.text = text;
		this.functionList = functionList;
		return this;
	}

	private boolean isEOT() {
		return text.length() <= i;
	}

	private char c() throws Exception {
		if (isEOT()) {
			throw new Exception("No more character");
		}
		return text.charAt(i);
	}

	private char next() throws Exception {
		char c = c();
		++i;
		return c;
	}

	private void skipSpace() throws Exception {
		while (!isEOT() && Character.isWhitespace(c())) {
			next();
		}
	}

	private boolean isSignStart(char c) {
		return c == '=' || c == '+' || c == '-' || c == '*' || c == '/' || c == '!' || c == '<' || c == '>' || c == '&'
				|| c == '|';
	}

	private boolean isParenStart(char c) {
		return c == '(' || c == ')';
	}

//	private boolean isCurlyStart(char c) {
//		return c == '{' || c == '}';
//	}

	private boolean isSymbolStart(char c) {
		return c == ',';
	}

	private boolean isDigitStart(char c) throws Exception {
		return Character.isDigit(c);
	}

	private boolean isStringStart(char c) {
		return c == '"';
	}

	private boolean isIdentStart(char c) throws Exception {
		return Character.isAlphabetic(c);
	}

//	// add
//	private boolean isVariableStart(char c) throws Exception {
//		return Character.isAlphabetic(c);
//	}
//
//	private Token variable() throws Exception {
//		StringBuilder b = new StringBuilder();
//		b.append(next());
//		while (!isEOT() && (Character.isAlphabetic(c()) || Character.isDigit(c()) || String.valueOf(c()).contains("."))) {
//			b.append(next());
//		}
//		Token t = new Token();
//		t.kind = "variable";
//		t.value = b.toString();
//		return t;
//	}
//	// add

	private Token sign() throws Exception {
		Token t = new Token();
		t.kind = "sign";
		char c1 = next();
		char c2 = (char) 0;
		if (!isEOT()) {
			if (c1 == '=' || c1 == '!' || c1 == '<' || c1 == '>') {
				if (c() == '=') {
					c2 = next();
				}
			} else if (c1 == '&') {
				if (c() == '&') {
					c2 = next();
				}
			} else if (c1 == '|') {
				if (c() == '|') {
					c2 = next();
				}
			}
		}
		String v;
		if (c2 == (char) 0) {
			v = Character.toString(c1);
		} else {
			v = Character.toString(c1) + Character.toString(c2);
		}
		t.value = v;
		return t;
	}

	private Token paren() throws Exception {
		Token t = new Token();
		t.kind = "paren";
		t.value = Character.toString(next());
		return t;
	}

//	private Token curly() throws Exception {
//		Token t = new Token();
//		if (c() == '{') {
//			t.kind = "curly";
//		} else {
//			t.kind = "eob";
//		}
//		t.value = Character.toString(next());
//		return t;
//	}

//	private Token symbol() throws Exception {
//		Token t = new Token();
//		t.kind = "symbol";
//		t.value = Character.toString(next());
//		return t;
//	}

	private Token digit() throws Exception {
		StringBuilder b = new StringBuilder();
		b.append(next());
		while (!isEOT() && Character.isDigit(c())) {
			b.append(next());
		}
		Token t = new Token();
		t.kind = "digit";
		t.value = b.toString();
		return t;
	}

	private Token string() throws Exception {
		StringBuilder b = new StringBuilder();
		next();
		while (c() != '"') {
			if (c() != '\\') {
				b.append(next());
			} else {
				next();
				char c = c();
				if (c == '"') {
					b.append('"');
					next();
				} else if (c == '\\') {
					b.append('\\');
					next();
				} else if (c == '/') {
					b.append('/');
					next();
				} else if (c == 'b') {
					b.append('\b');
					next();
				} else if (c == 'f') {
					b.append('\f');
					next();
				} else if (c == 'n') {
					b.append('\n');
					next();
				} else if (c == 'r') {
					b.append('\r');
					next();
				} else if (c == 't') {
					b.append('\t');
					next();
				} else {
					throw new Exception("string error");
				}
			}
		}
		next();
		Token t = new Token();
		t.kind = "string";
		t.value = b.toString();
		return t;
	}

	private Token ident() throws Exception {
		StringBuilder b = new StringBuilder();
		b.append(next());
		while (!isEOT() && (Character.isAlphabetic(c()) || Character.isDigit(c())
							|| isStringStart(c()) || isParenStart(c()) || isSymbolStart(c())
							|| String.valueOf(c()).contains("."))) {
			b.append(next());
		}
		Token t = new Token();

		// ファンクション判定
		t.kind = "ident";
		if (b.toString().indexOf("(") > -1) {
			String functionName = b.toString().substring(0, b.toString().indexOf("("));
			if (functionList.contains(functionName)) {
				t.kind = "function";
			}
		}
		t.value = b.toString();
		return t;
	}

	public Token nextToken() throws Exception {
		skipSpace();
		if (isEOT()) {
			return null;
		} else if (isSignStart(c())) {
			return sign();
		} else if (isDigitStart(c())) {
			return digit();
		} else if (isStringStart(c())) {
			return string();
		} else if (isIdentStart(c())) {
			return ident();
		} else if (isParenStart(c())) {
			return paren();
//		} else if (isCurlyStart(c())) {
//			return curly();
//		} else if (isSymbolStart(c())) {
//			return symbol();
		} else {
			throw new Exception("Not a character for tokens");
		}
	}

	public List<Token> tokenize() throws Exception {
		List<Token> tokens = new ArrayList<>();
		Token t = nextToken();
		while (t != null) {
			tokens.add(t);
			t = nextToken();
		}
		return tokens;
	}


	public static boolean isCharacter(String s) {
		try {
			Integer.parseInt(s);
			return false;
		} catch (NumberFormatException e) {
			return true;
		}
	}
}
