package ca.kscheme.interp;

import java.util.ArrayList;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SProcedure;
import ca.kscheme.data.SchemeValue;
import static ca.kscheme.data.SchemeValue.*;

public abstract class Cont extends SProcedure {
	
	/**
	 * The expression this continuation is a continuation of.
	 */
	private ArrayList<Object> exps = new ArrayList<Object>();
	
	/**
	 * Continuation that this continuation transfers control to after
	 * doing its own thing.
	 * 
	 * The parent may be null if this is the HALT cont.
	 */
	protected Cont parent;

	public Cont(Object exp, Cont parent) {
		this.exps.add(exp);
		this.parent = parent;
	}
	
	public final Trampoline applyCont(final Object value) {
		return new Trampoline() {
			@Override
			public Trampoline force1() {
				return applyNow(value);
			}
		};
	}
	
	protected abstract Trampoline applyNow(Object value);

	@Override
	public final String toString() {
		StringBuilder result = new StringBuilder();
		for (int i = exps.size()-1; i>=0 ; i--) {
			result.append(SchemeValue.toStringWithLocation(exps.get(i)) + "\n");
		}
		if (parent!=null)
			result.append(SchemeValue.toString(parent));
		return result.toString();
	}

	/**
	 * Add an expression for debug info. Since we have tail calling, some
	 * continuations will correspond to multiple expressions in the code that
	 * share that continuation. 
	 */
	public void setExp(Object addExp) {
		if (isSyntaxObj(addExp)) { // Ignore exps that don't have line number info, not that useful
			for (Object exp : exps) {
				if (exp==addExp) return; // only add if not yet there
			}
			exps.add(addExp);
		}
	}

	@Override
	public Trampoline apply(Object rands, Cont k) {
		try {
			KSchemeAssert.assertEquals("Number of rands", 1, length(rands));
			return this.applyCont(car(rands));
		} catch (KSchemeException e) {
			return k.raise("applyCont "+this, e);
		}
	}

	public Trampoline raise(Object info, Exception e) {
		return raise(this,info,e);
	}

	public Trampoline raise(Cont origin, Object info, Exception e) {
		if (parent==null)
			throw new ErrorWithCont(info, origin, e);
		else
			return parent.raise(origin, info, e);
	}
	
}
