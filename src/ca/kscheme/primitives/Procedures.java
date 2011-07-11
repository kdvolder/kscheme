package ca.kscheme.primitives;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SProcedure;
import ca.kscheme.data.SSymbol;
import ca.kscheme.data.SchemeValue;
import ca.kscheme.interp.Cont;
import ca.kscheme.interp.ErrorWithCont;
import ca.kscheme.interp.KSchemeAssert;
import ca.kscheme.interp.Trampoline;
import ca.kscheme.namespace.ClassFrame;
import ca.kscheme.namespace.HashFrame;
import ca.kscheme.namespace.SchemeName;

public class Procedures extends ClassFrame {
	
	public Procedures() throws KSchemeException {
		super();
	}
	
	public final SProcedure apply = new SProcedure() {
		@Override
		public Trampoline apply(Object rands, Cont k) {
			try {
				KSchemeAssert.assertEquals("number of argumens",2, length(rands));
				return apply(car(rands),cadr(rands), k);
			} catch (KSchemeException e) {
				throw new ErrorWithCont("Applying primitive #apply with rands "+rands,k,e);
			}
		}
	};

	public final SProcedure remainder = new SProcedure() {
		@Override
		public Trampoline apply(Object rands, Cont k) {
			try {
				KSchemeAssert.assertEquals("Number of rands", 2, length(rands));
				return k.applyCont(asInt(car(rands))%asInt(cadr(rands)));
			} catch (Exception e) {
				return k.raise("apply remainder "+rands, e);
			}
		}
	};
	public final SProcedure method = new SProcedure() {
		@Override
		public Trampoline apply(Object rands, Cont k) {
			try {
				KSchemeAssert.assertTrue("Number of args", length(rands)>=2);
				Class<?> cls = asClass(car(rands));
				String methodName = asSymbol(cadr(rands)).getName();
				Object argTypeList = cddr(rands);
				int numArgs = length(argTypeList); // number of arguments, not including receiver
				Class<?>[] parameterTypes = new Class<?>[numArgs];
				for (int i = 0; i < numArgs; i++) {
					parameterTypes[i] = asClass(car(argTypeList));
					argTypeList = cdr(argTypeList);
				}
				Method method = cls.getMethod(methodName, parameterTypes);
				if (Modifier.isStatic(method.getModifiers()))
					return k.applyCont(new StaticMethodInvoker(method));
				else 
					return k.applyCont(new InstanceMethodInvoker(method));
			} catch (Exception e) {
				throw new ErrorWithCont("apply #method "+rands, k, e);
			}
		}
	};
	
	public final SProcedure constructor = new SProcedure() {
		@Override
		public Trampoline apply(Object rands, Cont k) {
			Constructor<?> method;
			try {
				KSchemeAssert.assertTrue("Need at least one rand",length(rands)>=1);
				Class<?> cls = asClass(car(rands));
				Object argTypeList = cdr(rands);
				int numArgs = length(argTypeList); // number of arguments, not including receiver
				Class<?>[] parameterTypes = new Class<?>[numArgs];
				for (int i = 0; i < numArgs; i++) {
					parameterTypes[i] = asClass(car(argTypeList));
					argTypeList = cdr(argTypeList);
				}
				method = cls.getConstructor(parameterTypes);
			} catch (Exception e) {
				throw new ErrorWithCont("constructor "+rands, k, e);
			}
			return k.applyCont(new ConstructorInvoker(method));
		}
	};
	
	@SchemeName("call-with-current-continuation")
	public final SProcedure callCC = new SProcedure() {
		@Override
		public Trampoline apply(Object rands, final Cont k) {
			try {
				KSchemeAssert.assertEquals("Number of rands", 1, length(rands));
				return apply(car(rands), list(k), k);
			} catch (KSchemeException e) {
				throw new ErrorWithCont("apply call/cc "+rands, k, e);
			}
		}
	};
	
	@SchemeName("call-with-handler")
	public final SProcedure tryIt = new SProcedure() {

		@Override
		public Trampoline apply(Object rands, final Cont k) {
			try {
				KSchemeAssert.assertEquals("Number of rands", 2, length(rands));
				SProcedure body = asProcedure(car(rands));
				final SProcedure handler = asProcedure(cadr(rands));
				return body.apply(makeNull(), new Cont(null, k) {
					// This continuation is is the one that handles exceptions.
					@Override
					protected Trampoline applyNow(Object value) {
						//In non exceptional situation skip over this cont.
						return parent.applyCont(value);
					}
					
					@Override
					public Trampoline raise(Cont origin, Object info, Exception e) {
						// In exceptional situations... call the handler
						return handler.apply(list(e,origin),parent);
					}
				});
			} catch (KSchemeException e) {
				return k.raise("try: "+rands, e);
			}
		}
		
	};
	
	public final SProcedure error = new SProcedure() {
		@Override
		public Trampoline apply(Object rands, Cont k) {
			return k.raise(cons(error,rands),new KSchemeException("error"));
		}
	};
	

}
