package ca.kscheme.namespace;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SSymbol;

public class AppendFrame extends Frame {

	private Frame l;
	private Frame r;

	/**
	 * Create a composite Frame consisting out of two Frames.
	 * The rightMost (most recently added) frame takes 
	 * precedence in lookup of names.
	 */
	public AppendFrame(Frame l, Frame r) {
		this.l = l; this.r = r;
	}

	@Override
	public Reference<Object> lookup(SSymbol sym) throws KSchemeException {
		Reference<Object> result = r.lookup(sym);
		if (result==null)
			result = l.lookup(sym);
		return result;
	}

	@Override
	public void def(SSymbol var, Reference<Object> ref) throws KSchemeException {
		r.def(var, ref);
	}

	@Override
	public String toString() {
		return l.toString()+"\n"+r.toString();
	}

	@Override
	public Iterable<Binding> exportedBindings() {
		return r.exportedBindings();
	}

}
