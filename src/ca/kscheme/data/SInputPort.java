package ca.kscheme.data;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

/** A Scheme Input Port, is a wrapper around a Java Reader */
public class SInputPort extends SchemeValue {
	
	private URL sourceFile;
	private int line = 1;
	private int col = 0;
	private int pos = 0;
	
	private Reader input;
	private Object peekedChar;
	private boolean isPeeked = false;
	private boolean isCaseSensitive = false;
	
	public SInputPort(Reader input, URL sourceUrl) {
		this.sourceFile = sourceUrl;
		this.input = input;
	}
	
	public Object readChar() throws KSchemeException {
		Object result;
		if (isPeeked) {
			result = peekedChar;
			isPeeked = false;
		}
		else {
			result = getc();
		}
		return result;
	}

	private Object getc() throws KSchemeException {
		int result;
		try {
			result = input.read();
			pos++;
			if (result=='\n') {
				line++;
				col = 0;
			}
			else
				col++;
		} catch (IOException e) {
			throw new KSchemeException("getc", e);
		}
		if (result==-1)
			return SchemeValue.makeEOF();
		else {
			//System.out.append((char)result);
			return SchemeValue.makeChar((char)result);
		}
	}

	public Object peekChar() throws KSchemeException {
		if (!isPeeked) {
			peekedChar = getc();
			isPeeked = true;
		}
		return peekedChar;
	}

	@Override
	public boolean equals(Object obj) {
		return this==obj;
	}

	@Override
	public int hashCode() {
		return input.hashCode();
	}
	
	public SourcePosition getPosition() {
		return new SourcePosition(sourceFile,line,col);
	}
	
	public URL getSourceFile() {
		return sourceFile;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	public synchronized void close() throws IOException {
		if (input!=null) {
			input.close();
			input = null;
		}
	}

	public boolean isCaseSensitive() {
		return isCaseSensitive;
	}
	public void setCaseSensitive(boolean isCaseSensitive) {
		this.isCaseSensitive = isCaseSensitive;
	}
	
	@Override
	public String toString() {
		return "SInputPort("+getPosition()+")";
	}

}
