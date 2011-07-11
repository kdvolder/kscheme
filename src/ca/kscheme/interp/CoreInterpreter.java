package ca.kscheme.interp;

import static ca.kscheme.data.SchemeValue.asPair;
import static ca.kscheme.data.SchemeValue.asSymbol;
import static ca.kscheme.data.SchemeValue.car;
import static ca.kscheme.data.SchemeValue.cdr;
import static ca.kscheme.data.SchemeValue.isNull;
import static ca.kscheme.data.SchemeValue.isPair;
import static ca.kscheme.data.SchemeValue.isSymbol;
import static ca.kscheme.data.SchemeValue.makeSymbol;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import ca.kscheme.KScheme;
import ca.kscheme.data.IPair;
import ca.kscheme.data.ImpossibleError;
import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SProcedure;
import ca.kscheme.namespace.Env;
import ca.kscheme.namespace.Frame;
import ca.kscheme.primitives.Syntaxes;
import ca.kscheme.reader.SchemeReader;

/**
 * An interpeter for something close to R4RS.
 * 
 * @author kdvolder
 */
public class CoreInterpreter extends KScheme {
	
	private Env globalEnv;
	private Map<Object, Frame> modules;
	private Object lastResult;

	public CoreInterpreter() {
		this(new Env(), SchemeReader.getDefault(), new HashMap<Object, Frame>());
	}

	public static KScheme getDefault() throws Exception {
		CoreInterpreter interp = new CoreInterpreter();
		interp.setup();
		return interp;
	}
	
	private CoreInterpreter(
			Env iniEnv, 
			SchemeReader reader, 
			Map<Object,Frame> modules) {
		super(reader);
		this.globalEnv = iniEnv;
		this.modules = modules;
	}
	
	public Trampoline eval(Object exp, Env env, Cont k) {
		k.setExp(exp);
		try {
			if (isPair(exp)) 
				return evalApp(asPair(exp), env, k);
			else if (isSymbol(exp))
				return k.applyCont(env.lookup(asSymbol(exp)));
			else
				return k.applyCont(exp);
		}
		catch (KSchemeException e) {
			return k.raise(exp, e);
		}
	}

	private Trampoline evalApp(final IPair exp, final Env env, final Cont k) {
		final CoreInterpreter thiss = this;
		return eval(exp.car(), env, new Cont(exp.car(), k) {
			@Override
			public Trampoline applyNow(final Object rator) {
				if (rator instanceof SSyntax)
					return ((SSyntax)rator).applySyntax(thiss, env, exp, k);
				else {
					return evalList(exp.cdr(), env, new Cont(exp.cdr(), k) {
						@Override
						public Trampoline applyNow(Object rands) {
							try {
								return asProcedure(rator).apply(rands, k);
							} catch (KSchemeException e) {
								return k.raise(exp,e);
							}
						}
					});
				}
			}
		});
	}

	private Trampoline evalList(final Object exps, final Env env, final Cont k) {
		try {
			if (isNull(exps))
				return k.applyCont(exps);
			else if (isPair(exps)) {
				final Object rest = cdr(exps);
				return eval(car(exps), env, new Cont(car(exps), k) {
					@Override
					public Trampoline applyNow(final Object carVal) {
						return evalList(rest, env, new Cont(rest, k) {
							@Override
							public Trampoline applyNow(Object cdrVal) {
								return k.applyCont(cons(carVal, cdrVal));
							}
						});
					}
				});
			}
			else 
				throw new ErrorWithCont("Improper list in evalList: "+exps, k, null);
		} catch (KSchemeException e) {
			throw new ErrorWithCont("evalList: "+exps, k, e);
		}
	}

	public Trampoline tEval(Object valExp, Env env, Cont k) {
		return new EvalExpTrampoline(this, env, valExp, k);
	}

	public Object makeProcedure(Object formals, Object body, Env env) {
		return new SLambdaProcedure(formals, body, env);
	}
	private class SLambdaProcedure extends SProcedure {
		
		private Object formals;
		private Object body;
		private Env env;

		public SLambdaProcedure(Object formals, Object body,
				 Env env) {
			this.formals = formals;
			this.body = body;
			this.env = env;
		}
		
		@Override
		public String toString() {
			if (name!=null)
				return super.toString();
			else
				return "#proc<"+formals+" "+body+">";
		}

		@Override
		public Trampoline apply(Object rands, Cont k) {
			Env newEnv;
			try {
				newEnv = env.extend(formals, rands);
			} catch (KSchemeException e) {
				throw new ErrorWithCont("apply: bad rands?\n proc = "+this+"\n rands = "+rands, k, e);
			}
			return tEval(cons(Syntaxes.begin, body), newEnv, k);
		}

	}

	/**
	 * Create a new Environment frame. Thus, redefinitions of identifiers made after
	 * calling protectEnv will not affect references to those identifiers made before
	 * the call to protectEnv. The "redefined" identifier will be a local identifier shadowing 
	 * the original one, while letting previously define procedures keep accessing the
	 * old id.
	 */
	public void protectEnv() {
		globalEnv = globalEnv.extend(); 
	}

	/**
	 * Evaluate an expression in the globalEnv. Instead of actually doing the eval
	 * immediately, it returns a Trampoline to support proper tail call behavior for
	 * this call to the evaluator.
	 */
	public Trampoline tGlobalEval(Object exp, Cont k) {
		return tEval(exp, globalEnv, k);
	}

	private static class CaptureResultCont extends Cont {
		
		public CaptureResultCont(Object exp) {
			super(exp+"=>HALT",null);
		}

		private Object res = null;
		private boolean called = false;

		@Override
		public Trampoline applyNow(Object value) {
			res = value;
			called = true;
			return Trampoline.theNull;
		}

		public Object getResult() {
			if (!called)
				//This is "impossible" if the interpeter is in proper
				//continuation passing style. 
				//Note: maybe not impossible: what if continuation
				//is not called because of some magic with call/cc
				throw new ImpossibleError("The CaptureResultCont was not called?");
			return res;
		}

//		@Override
//		public String toString() {
//			return "Halt";
//		}
	}
	/////////// The following methods must be provided to implement abstract methods ////
	
	@Override
	public void compile(Object exp) {
		CaptureResultCont k = new CaptureResultCont(exp);
		eval(exp, globalEnv, k).force();
		lastResult =  k.getResult();
	}

	@Override
	public Object execute() {
		return lastResult;
	}

	@Override
	public boolean isDefined(String name) throws KSchemeException {
		return globalEnv.lookupRef(makeSymbol(name))!=null;
	}

	public final void require(Class<? extends Frame> clss) throws KSchemeException {
		Frame module = modules.get(clss);
		if (module==null) {
			try {
				try {
					Constructor<? extends Frame> cons = clss.getConstructor();
					module = cons.newInstance();
				}
				catch (NoSuchMethodException e) {
					try {
						Constructor<? extends Frame> cons = clss.getConstructor(KScheme.class);
						module = cons.newInstance(this);
					} catch (NoSuchMethodException e2) {
						Constructor<? extends Frame> cons = clss.getConstructor(this.getClass());
						module = cons.newInstance(this);
					}
				}
			} catch (Exception e) {
				throw new KSchemeException("Could not initialize module: "+clss, e);
			}
		}
		for (Frame.Binding binding : module.exportedBindings()) {
			globalEnv.defineRef(binding.name, binding.value);
		}
	}

}
