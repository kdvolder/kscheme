package ca.kscheme.interp;

import ca.kscheme.data.KSchemeException;

public class KSchemeAssert {

	public static void assertEquals(String msg, int expected, int actual) throws KSchemeException {
		if (expected!=actual) {
			throw new KSchemeException("Assertion failed: "+msg+" expected: "+expected+ " but was "+actual, null);
		}
	}

	public static void assertTrue(String msg, boolean b) throws KSchemeException {
		if (!b)
			throw new KSchemeException("Assertion failed: "+msg, null);
	}

}
