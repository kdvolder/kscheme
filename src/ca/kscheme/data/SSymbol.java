package ca.kscheme.data;

import org.apache.commons.collections.map.ReferenceMap;

public class SSymbol extends SchemeValue {
	
	private static ReferenceMap symbolTable = new ReferenceMap();
	private String string;

	private SSymbol(String string) {
		this.string = string;
	}

	public static SSymbol intern(String string) {
		string = new String(string.toCharArray()); //copy!
		   // copy: not needed if Strings are immutable.
		   // but for the sake of Scheme string-set! we have hacked
		   // around String immutability with Java reflection!
		SSymbol sym = (SSymbol) symbolTable.get(string);
		if (sym==null) {
			sym = new SSymbol(string);
			symbolTable.put(string, sym);
		}
		return sym;
	}
	
	@Override
	public String toString() {
		return string;
	}

	public String getName() {
		return string;
	}
	
	public SchemeValue gensym() {
		return new SSymbol(string);
	}
	
}
