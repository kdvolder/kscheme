package ca.kscheme.data;

import ca.kscheme.interp.Cont;
import ca.kscheme.interp.Trampoline;


public abstract class SProcedure extends SchemeValue {
	
	protected Object name;

	protected SProcedure() { super(); }
	
	@Override
	public void gotName(Object name) {
		if (this.name==null)
			this.name = name;
	}
	
	@Override
	public String toString() {
		if (name==null)
			return super.toString();
		return "#proc:"+name;
	}
	
	abstract public Trampoline apply(Object rands, Cont k);
		
}
