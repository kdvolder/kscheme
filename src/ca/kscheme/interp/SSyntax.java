package ca.kscheme.interp;

import ca.kscheme.data.IPair;
import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SSymbol;
import ca.kscheme.data.SchemeValue;
import ca.kscheme.namespace.Env;

public abstract class SSyntax extends SchemeValue {
	
	private SSymbol name;
	
	public SSyntax() {
		name = null;
	}
	
	public SSyntax(String name) {
		this.name = makeSymbol(name);
	}

	@Override
	public void gotName(Object var) {
		if (name==null)
			try {
				name = asSymbol(var);
			} catch (KSchemeException e) {
			}
	}

	public abstract Trampoline applySyntax(CoreInterpreter interpreter, Env env, IPair exp, Cont k);

	@Override
	public String toString() {
		if (name!=null) {
			return "#synt:"+name;
		}
		else 
			return "#<syntax>";
	}

	public SSymbol getName() {
		return name;
	}

}
