package ca.kscheme.data;

import static ca.kscheme.data.SchemeValue.toStringWithLocation;

/**
 * A checked exception that should be thrown by methods that detect an error
 * condition but do not have access to a continuation.
 */
public class KSchemeException extends Exception {

	public KSchemeException(String string, Throwable e) {
		super(string, e);
	}

	public KSchemeException(String string) {
		super(string);
	}

	public KSchemeException(String string, Object obj) {
		super(string+toStringWithLocation(obj));
	}


}
