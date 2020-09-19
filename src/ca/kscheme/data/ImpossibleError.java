package ca.kscheme.data;

/**
 * This unchecked Exception gets used to rethrow for exceptions that
 * are being caught which *should* be impossible unless there are bugs
 * in the implementation.
 * 
 * @author kdvolder
 */
public class ImpossibleError extends Error {

	public ImpossibleError(Exception e) {
		super(e);
	}

	public ImpossibleError(String string) {
		super(string);
	}

}
