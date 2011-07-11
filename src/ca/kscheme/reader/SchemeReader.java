package ca.kscheme.reader;

import static ca.kscheme.data.SchemeValue.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SInputPort;
import ca.kscheme.data.SProcedure;
import ca.kscheme.data.SSymbol;
import ca.kscheme.data.SchemeValue;
import ca.kscheme.data.SourcePosition;

public class SchemeReader {

	private static final SSymbol dotSymbol = SchemeValue.makeSymbol(".");
	private Map<String, Object> readerConstants = new HashMap<String, Object>();
	
	public SyntaxObj read(SInputPort input) throws KSchemeException {
		skipWhite(input);
		SourcePosition startPos = input.getPosition();
		Object value = readNaked(input);
		SourcePosition endPos = input.getPosition();
		return new SyntaxObj(value, startPos, endPos);
	}
	
	public Object readNaked(SInputPort input) throws KSchemeException {
		Object ch = input.peekChar();
		if (isEofObject(ch))
			return ch;
		else if (isCharNumeric(ch)) {
			return readNumber(input);
		}
		else if (ch.equals('-')) {
			input.readChar();
			if (isCharWhitespace(input.peekChar()) || input.peekChar().equals(')'))
				return SchemeValue.makeSymbol("-");
			Object rest = read(input);
			if (isNumber(rest))
				return negate(rest);
			else if (isSymbol(rest))
				return SchemeValue.makeSymbol(""+ch+asSymbol(rest).getName());
			else {
				throw new ReaderException("- in strange place", input);
			}
		}
		else if (ch.equals('(')) 
			return readList(input);
		else if (ch.equals('\'')) {
			SSymbol first = SchemeValue.makeSymbol("quote");
			input.readChar();
			return cons(first,cons(read(input),SchemeValue.makeNull()));
		}
		else if (ch.equals('`')) {
			SSymbol first = SchemeValue.makeSymbol("quasiquote");
			input.readChar();
			return cons(first,cons(read(input),SchemeValue.makeNull()));
		}
		else if (ch.equals(',')) {
			SSymbol first = SchemeValue.makeSymbol("unquote");
			input.readChar();
			if (input.peekChar().equals('@')) {
				input.readChar();
				first = SchemeValue.makeSymbol("unquote-splicing");
			}
			return cons(first,cons(read(input),SchemeValue.makeNull()));
		}
		else if (ch.equals('"')) {
			return readString(input, '"');
		}
		else if (ch.equals('|')) {
			return makeSymbol(readString(input, '|'));
		}
		else { // Assume symbol
			return readSymbol(input);
		}
	}

	private Object readSymbol(SInputPort input) throws KSchemeException {
		StringBuilder s = new StringBuilder();
		Object c, oc;
		boolean escaped = false;
		do {
			oc = c = input.readChar();
			s.append(asChar(c));
			c = input.peekChar();
			escaped = !escaped && isEqv(oc, '\\');
		} while (isCharPartOfSymbol(c) || escaped);
		String image = s.toString();
		if (image.startsWith("#")) {
			if (image.startsWith("#\\")&&image.length()==3)
				return SchemeValue.makeChar(image.charAt(2));
			else if (image.equals("#")) {
				Object vectorElements = read(input);
				return SchemeValue.makeVector(vectorElements);
			}
			else if (image.startsWith("#|")) {
				Object element = read(input);
				while (!isEofObject(element)) {
					element = read(input);
					if (element.equals(makeSymbol("|#"))) 
						return read(input);
				}
				return element; //EOF
			}
			else if (readerConstants.containsKey(image)) {
				return readerConstants.get(image);
			}
			else if (image.equals("#input")) {
				return input;
			}
			else if (image.equals("#case-sensitive")) {
				input.setCaseSensitive(true);
				return read(input);
			}
			else if (image.charAt(1)=='x'
				 ||  image.charAt(1)=='b'
			     ||  image.charAt(1)=='d'
				 ||  image.charAt(1)=='o') {
				int radix = 10;
				if (image.charAt(1)=='x') 
					radix = 16;
				else if (image.charAt(1)=='b') 
					radix = 2;
				else if (image.charAt(1)=='o') 
					radix = 8;
				return Integer.parseInt(image.substring(2), radix);
			}
		}
		if (input.isCaseSensitive())
			return SchemeValue.makeSymbol(s.toString());
		else
			return SchemeValue.makeSymbol(s.toString().toLowerCase());
	}

	private boolean isCharPartOfSymbol(Object o) {
		if (!isChar(o)) return false;
		char c = (Character)o;
		return !( Character.isWhitespace(c)
				|| c=='(' || c == ')' || c=='\'' || c =='"' 
			    || c==';');
	}

	private Object readList(SInputPort input) throws KSchemeException {
		input.readChar(); // skip '('
		return readRest(input);
	}
	
	private Object readRest(SInputPort input) throws KSchemeException {
		skipWhite(input);
		Object c = input.peekChar();
		if (c.equals(')')) {
			input.readChar();
			return SchemeValue.makeNull();
		}
		else if (isEofObject(c))
			throw new KSchemeException("Unexpected end of file:"+input.getPosition());
		else {
			Object first = read(input);
			if (isEqual(dotSymbol, first)) {
				Object result = read(input);
				skipWhite(input);
				expect(input, ')');
				return result;
			}
			else 
				return cons(first,readRest(input));
		}
	}

	private String readString(SInputPort input, char terminator) throws KSchemeException {
		input.readChar(); // skip quote
		Object c;
		StringBuilder image = new StringBuilder();
		while (!(c = input.readChar()).equals(terminator)) {
			if (isEofObject(c)) 
				throw new KSchemeException("Unexpected end of file");
			if (c.equals('\\')) {
				//TODO: handle escape sequences?
				c = input.readChar();
			}
			image.append(asChar(c));
		}
		return SchemeValue.makeString(image.toString());
	}

	private void expect(SInputPort input, char c) throws KSchemeException {
		Object got = input.peekChar();
		if (got.equals(c)) {
			input.readChar();
		}
		else 
			throw new KSchemeException("Error while reading: expected a '"+c+"' but got a '"+got);
	}

	private void skipWhite(SInputPort input) throws KSchemeException {
		Object c = input.peekChar();
		while (isCharWhitespace(c) || c.equals(';')) {
			input.readChar();
			if (c.equals(';')) skipToEol(input); //single line comment
			c = input.peekChar();
		}
	}

	private void skipToEol(SInputPort input) throws KSchemeException {
		Object c = input.readChar();
		while (!(c.equals('\n')||c.equals('\r')||isEofObject(c)))
			c = input.readChar();
	}

	private Object readNumber(SInputPort input) throws KSchemeException {
		Object ch = input.peekChar();
		boolean isFloat = false;
		StringBuilder image = new StringBuilder();
		while (isCharNumeric(ch)) {
			image.append(ch);
			input.readChar(); // we are processing the char, so skip it
			ch = input.peekChar();
		}
		if (isEqv(ch, '.')) {
			isFloat = true;
			image.append(ch);
			input.readChar();
			ch = input.peekChar();
			while (isCharNumeric(ch)) {
				image.append(ch);
				input.readChar(); // we are processing the char, so skip it
				ch = input.peekChar();
			}
			if (isEqv(ch,'e') || isEqv(ch,'E')) {
				image.append(ch);
				input.readChar(); // we are processing the char, so skip it
				ch = input.peekChar();
				if (isEqv(ch, '-') || isEqv(ch, '+')) {
					image.append(ch);
					input.readChar();
					ch = input.peekChar();
				}
				while (isCharNumeric(ch)) {
					image.append(ch);
					input.readChar(); // we are processing the char, so skip it
					ch = input.peekChar();
				}
			}
		}
		if (!isFloat)
			return Integer.parseInt(image.toString());
		else
			return Double.parseDouble(image.toString());
	}

	/**
	 * Define a "reader constant". A reader constant is a symbol that is
	 * replaced by some value by the reader.
	 */
	public void readerConstant(String string, Object value) {
		readerConstants.put(string, value);
	}
	
	public static SchemeReader getDefault() {
		SchemeReader reader = new SchemeReader();
		reader.readerConstant("#t", SchemeValue.makeBoolean(true));
		reader.readerConstant("#f", SchemeValue.makeBoolean(false));
		reader.readerConstant("#\\newline", SchemeValue.makeChar('\n'));
		reader.readerConstant("#\\space", SchemeValue.makeChar(' '));
		reader.readerConstant("#\\Space", SchemeValue.makeChar(' '));
		reader.readerConstant("#eof", SchemeValue.makeEOF());
		reader.readerConstant("#null", null);
		return reader;
	}

}
