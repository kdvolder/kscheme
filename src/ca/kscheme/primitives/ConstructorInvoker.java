package ca.kscheme.primitives;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SProcedure;
import ca.kscheme.interp.Cont;
import ca.kscheme.interp.ErrorWithCont;
import ca.kscheme.interp.Trampoline;

public class ConstructorInvoker extends SProcedure {

	private Constructor<?> constructor;

	public ConstructorInvoker(Constructor<?> constructor) {
		this.constructor = constructor;
	}

	@Override
	public Trampoline apply(final Object rands, final Cont k) {
		try {
			final Object[] args = toUnproxiedArray(rands);
			return new Trampoline() {
				@Override
				public Trampoline force1() {
					try {
						return k.applyCont(constructor.newInstance(args));
					} catch (Exception e) {
						throw new ErrorWithCont("Invoke constructor "+constructor+"\n rands = "+rands, k, e);
					}
				}
			};
		} catch (KSchemeException e) {
			throw new ErrorWithCont("Converting rands to array: rands ="+rands, k, e);
		}
	}
	
	@Override
	public String toString() {
		if (name!=null) 
			return "#new:"+name;
		else
			return "#new<"+constructor+">";
	}

}
