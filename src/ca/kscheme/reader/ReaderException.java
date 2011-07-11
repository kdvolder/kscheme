package ca.kscheme.reader;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SInputPort;

public class ReaderException extends KSchemeException {

	public ReaderException(String msg, SInputPort input) {
		super(input.getPosition()+" -> "+msg);
	}

}
