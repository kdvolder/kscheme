package ca.kscheme.primitives;

import java.lang.reflect.Method;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.Proxy;
import ca.kscheme.data.SProcedure;
import ca.kscheme.interp.Cont;
import ca.kscheme.interp.ErrorWithCont;
import ca.kscheme.interp.Trampoline;

public class InstanceMethodInvoker extends SProcedure {

	private Method method;
	private boolean proxyAware;

	public InstanceMethodInvoker(Method method) {
		this.method = method;
		proxyAware = Proxy.class.isAssignableFrom(method.getDeclaringClass());
	}

	@Override
	public Trampoline apply(Object rands, final Cont k) {
		try {
			final Object rcvr = getReceiver(rands);
			final Object[] args = getArgs(rands);
			return new Trampoline() {
				@Override
				public Trampoline force1() {
					try {
						return k.applyCont(method.invoke(rcvr, args));
					} catch (Throwable e) {
						throw new ErrorWithCont("Could not call "+method+"\n rcvr = "
								+rcvr+"\n args ="+args, k, e );
					}
				}
			};
		} catch (KSchemeException e) {
			throw new ErrorWithCont("MethodInvoker.apply parsing rands: "+rands, k, e);
		}
	}
	
	private Object getReceiver(Object rands) throws KSchemeException {
		if (proxyAware)
			return car(rands);
		else 
			return unproxy(car(rands));
	}
	private Object[] getArgs(Object rands) throws KSchemeException {
		if (proxyAware)
			return toArray(cdr(rands));
		else 
			return toUnproxiedArray(cdr(rands));
	}

	@Override
	public String toString() {
		if (name!=null)
			return "#meth:"+name;
		else
			return "#meth<"+method+">";
	}

}
