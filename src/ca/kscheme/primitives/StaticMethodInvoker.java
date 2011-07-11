package ca.kscheme.primitives;

import java.lang.reflect.Method;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SProcedure;
import ca.kscheme.interp.Cont;
import ca.kscheme.interp.ErrorWithCont;
import ca.kscheme.interp.Trampoline;

public class StaticMethodInvoker extends SProcedure {

	private Method method;

	public StaticMethodInvoker(Method method) {
		this.method = method;
	}

	@Override
	public Trampoline apply(final Object rands, final Cont k) {
		try {
			final Object[] args = toUnproxiedArray(rands);
			return new Trampoline() {
				@Override
				public Trampoline force1() {
					try {
						return k.applyCont(method.invoke(null, args));
					} catch (Exception e) {
						return k.raise("StaticMethodInvoker "+method+"\n rand = "+rands, e);
					}
				}
			};
		} catch (KSchemeException e) {
			throw new ErrorWithCont("StaticMethodInvoker "+method+"\n rand = "+rands, k, e);
		}
	}
	
	@Override
	public String toString() {
		if (name!=null)
			return "#meth:"+name;
		else
			return "#meth<"+method.toString()+">";
	}

}
