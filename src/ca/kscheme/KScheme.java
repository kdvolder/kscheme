package ca.kscheme;

import static ca.kscheme.data.SchemeValue.asString;
import static ca.kscheme.data.SchemeValue.isEofObject;
import static ca.kscheme.data.SchemeValue.list;
import static ca.kscheme.data.SchemeValue.makeInputPort;
import static ca.kscheme.data.SchemeValue.makeStringInputPort;
import static ca.kscheme.data.SchemeValue.makeSymbol;

import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SInputPort;
import ca.kscheme.namespace.Frame;
import ca.kscheme.reader.SchemeReader;

/**
 * An abstract superclass with as few methods as possible to be able
 * to run the TestSuite.
 */
public abstract class KScheme {

	public KScheme(SchemeReader reader) {
		this.reader = reader;
	}

	/**
	 * Add an expression to the program to compile / interpret. Implementation
	 * may execute the expression immediately, or convert into some executable
	 * form for later execution.
	 */
	public abstract void compile(Object exp);
	
	/**
	 * Return the result of execution all pending compiled expressions.
	 * <p>
	 * The runtime environment created by executing these expressions is 
	 * retained for future compiled expressions, but the side effects
	 * will not be repeated.
	 */
	public abstract Object execute();
	
	/**
	 * Verify whether a given symbol is defined in the current environment in
	 * which this KScheme implementation compiles / executes expressions.
	 * @throws KSchemeException 
	 */
	public abstract boolean isDefined(String name) throws KSchemeException;

	/**
	 * Create a new global env frame so that redefinitions of identifiers do
	 * not affect references to the original identifier.
	 * 
	 * TODO: This method should be removed. We should make this the default
	 * behavior for every load to protect its load-time environment.
	 */
	public abstract void protectEnv();
	
	/**
	 * Add bindgins from an already compiled environment Frame to the Global 
	 * KScheme environment.
	 */
	public abstract void require(Class<? extends Frame> forName) throws KSchemeException;


	
	/**
	 * Every Scheme implementation, compiler or reader or whatever
	 * needs a reader.
	 */
	private SchemeReader reader = SchemeReader.getDefault();

	final public Object read(String string) throws KSchemeException {
		return read(makeStringInputPort(string));
	}

	final public Object parseAndRun(String string) throws KSchemeException {
		SInputPort input = makeStringInputPort(string);
		return load(input);
	}

	public Object read(SInputPort input) throws KSchemeException {
		return reader.read(input);
	}

	final public Object load(URL resource) throws KSchemeException {
		return load(makeInputPort(resource));
	}

	final public Object load(SInputPort input) throws KSchemeException {
		Object exp = read(input);
		while (!isEofObject(exp)) {
			compile(exp);
			exp = read(input);
		}
		return execute();
	}

	final public Object load(String string) throws KSchemeException {
		try {
			return load(new File(string).toURI().toURL());
		} catch (MalformedURLException e) {
			throw new KSchemeException("loading "+string,e);
		}
	}

	/**
	 * Define a "reader constant". A reader constant is a symbol that is
	 * replaced by some value by the reader.
	 */
	public void readerConstant(String string, Object value) {
		reader.readerConstant(string, value);
	}
	
	public void readEvalPrintLoop() {
		SInputPort input = makeInputPort(new InputStreamReader(System.in), null);
		Object val = null;
		while (!isEofObject(val)) {
			System.out.print("> ");
			try {
				Object exp = read(input);
				val = run(exp);
				System.out.println("-> " + toString(val));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public String toString(Object val) throws KSchemeException {
		if (isDefined("->string")) 
			return asString(run(list(makeSymbol("->string"), 
					list(makeSymbol("quote"), val))));
		else {
			return ""+val;
		}
	}

	public Object run(Object exp) {
		compile(exp);
		return execute();
	}
}
