package ca.kscheme.namespace;

import static ca.kscheme.data.SchemeValue.asSymbol;
import static ca.kscheme.data.SchemeValue.car;
import static ca.kscheme.data.SchemeValue.cdr;
import static ca.kscheme.data.SchemeValue.isNull;
import static ca.kscheme.data.SchemeValue.isPair;
import static ca.kscheme.data.SchemeValue.makeNull;
import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SSymbol;
import ca.kscheme.data.SchemeValue;
import ca.kscheme.interp.KSchemeAssert;

public final class Env {

	private Frame frame;

	public Env() {
		this.frame = new HashFrame();
	}

	private Env(Frame frame) {
		this.frame = frame;
	}

	public String toString() {
		return "Env {\n" +
				frame.toString()+
			   "}\n";
	}

	public Env extend() {
		return extend(new HashFrame());
	}
	public Env extend(Frame addFrame) {
		return new Env(new AppendFrame(frame, addFrame));
	}

	public void define(SSymbol sym, Object val) throws KSchemeException {
		Reference<Object> loc = frame.def(sym);
		loc.set(val);
		if (val instanceof SchemeValue) {
			((SchemeValue)val).gotName(sym);
		}
	}

	public void defineRef(SSymbol name, Reference<Object> ref) throws KSchemeException {
		ref.gotName(name);
		frame.def(name, ref);
	}

	public void assign(SSymbol sym, Object val) throws KSchemeException {
		Reference<Object> loc = frame.lookup(sym);
		if (loc==null)
			throw new KSchemeException("Unbound identifier: "+sym);
		loc.set(val);
		if (val instanceof SchemeValue) {
			((SchemeValue)val).gotName(sym);
		}
	}

	public Object lookup(SSymbol sym) throws KSchemeException {
		Reference<Object> loc = lookupRef(sym);
		if (loc==null)
			throw new KSchemeException("Unbound identifier: "+sym);
		return loc.get();
	}
	public Reference<Object> lookupRef(SSymbol sym) throws KSchemeException {
		Reference<Object> loc = frame.lookup(sym);
		return loc;
	}

	public Env extend(Object formals, Object rands) throws KSchemeException {
		Env extEnv = this.extend();
		while (!isNull(formals)) {
			if (isPair(formals)) {
				extEnv.define(asSymbol(car(formals)), car(rands));
				formals = cdr(formals);
				rands = cdr(rands);
			}
			else {
				extEnv.define(asSymbol(formals), rands);
				formals = makeNull();
				rands = makeNull();
			}
		}
		KSchemeAssert.assertTrue("Too many rands",isNull(rands));
		return extEnv;
	}

}