package ca.kscheme.interp;

import ca.kscheme.data.SchemeValue;

public class ErrorWithCont extends Error {

	private static final long serialVersionUID = -648313217393752710L;
	private Cont k;

	public ErrorWithCont(Object exp, Cont k, Throwable e) {
		super(SchemeValue.toStringWithLocation(exp)+"\n"+k, e);
	}

}
