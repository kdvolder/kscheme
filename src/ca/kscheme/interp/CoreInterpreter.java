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
import ca.kscheme.data.SMacro;
import ca.kscheme.data.SProcedure;
import ca.kscheme.data.SSymbol;
import ca.kscheme.data.SchemeValue;
import ca.kscheme.namespace.Env;
import ca.kscheme.namespace.Frame;
import ca.kscheme.primitives.PrimitiveSyntaxes;
import ca.kscheme.reader.SchemeReader;

/**
 * An interpeter for something close to R4RS. This interpreter performs macro expansion
 * 'on the fly' whenever it comes across the use of a macro.
 * <p>
 * It is inefficient, but relatively easy to implement and can be used to bootstrap
 * more efficient implementations.
 * 
 * @author kdvolder
 */
public class CoreInterpreter extends KScheme {
	
	private Env globalEnv;
	private Map<Object, Frame> modules;
	private Object lastResult;
	
	private Map<SSymbol, SSyntax> specialForms = new HashMap<SSymbol, SSyntax>();

	public CoreInterpreter() {
		this(new Env(), SchemeReader.getDefault(), new HashMap<Object, Frame>());
	}

	public static KScheme getDefault() throws Exception {
		CoreInterpreter interp = new CoreInterpreter();
		interp.setup();
		return interp;
	}
	
	private void setup() throws KSchemeException {
		require(Require.class);
		initSpecialForms();
		load(KScheme.class.getResource("bootstrap.scm"));
		protectEnv();
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
				return evalPair(asPair(exp), env, k);
			else if (isSymbol(exp))
				return k.applyCont(env.lookup(asSymbol(exp)));
			else
				return k.applyCont(exp);
		}
		catch (KSchemeException e) {
			return k.raise(exp, e);
		}
	}

	private Trampoline evalPair(final IPair exp, final Env env, final Cont k) {
		try {
			Object ratorExp = car(exp);
			SSyntax specialForm = getSpecialForm(ratorExp);
			if (specialForm!=null) {
				return specialForm.applySyntax(this, env, exp, k);
			}
			return eval(ratorExp, env, new Cont(ratorExp, k) {
				@Override
				protected Trampoline applyNow(final Object rator) {
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
			});
		} catch (KSchemeException e) {
			return k.raise(exp, e);
		}
	}

	private SSyntax getSpecialForm(Object exp) throws KSchemeException {
		if (isSymbol(exp)) {
			return specialForms.get(asSymbol(exp));
		} else if (exp instanceof SSyntax) {
			return (SSyntax) exp;
		}
		return null;
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
			return tEval(PrimitiveSyntaxes.makeBegin(body), newEnv, k);
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

	private void initSpecialForms() throws KSchemeException {
		specialForm("begin", new SSyntax() {
			@Override
			public Trampoline applySyntax(final CoreInterpreter interpreter, Env env, IPair arg, final Cont k) {
				try {
					return evalSeq(interpreter, env, cdr(arg), k);
				} catch (KSchemeException e) {
					throw new ErrorWithCont("applySyntax: "+arg,k,e);
				}
			}

			private Trampoline evalSeq(final CoreInterpreter interpreter, final Env env, final Object exps, final Cont k) {
				try {
					if (isNull(exps)) {
						Object result = SchemeValue.makeUndefined();
						return k.applyCont(result);
					}
					else if (isNull(cdr(exps))) {
						return interpreter.eval(car(exps), env, k);
					}
					else {
						final Object rest = cdr(exps);
						return interpreter.eval(car(exps), env, new Cont(car(exps),k) {

							@Override
							protected Trampoline applyNow(Object value) {
								return evalSeq(interpreter, env, rest, k);
							}
						});
					}
				} catch (KSchemeException e) {
					throw new ErrorWithCont("evalSeq: "+exps,k,e);
				}
			}
		});
		
		specialForm("define", new SSyntax() {
			@Override
			public Trampoline applySyntax(final CoreInterpreter interpreter, Env env, IPair defExp, Cont k) {
				try {
					Object var = cadr(defExp);
					Object valExp = caddr(defExp);
					if (isPair(var)) { // expand (define (f args) ...) syntax
						return defineIt(interpreter, env, car(var), PrimitiveSyntaxes.makeLambda(cdr(var), cddr(defExp)), k);
					}
					else
						return defineIt(interpreter, env, var, valExp, k);
				} catch (KSchemeException e) {
					throw new ErrorWithCont("expanding: "+defExp,k,e);
				}
			}

			private Trampoline defineIt(final CoreInterpreter interpreter, final Env env, final Object var, Object valExp, final Cont k) {
				return interpreter.eval(valExp, env, new Cont(valExp,k) {

					@Override
					protected Trampoline applyNow(Object value) {
						try {
							if (value instanceof SMacro) {
								specialForm(asSymbol(var).getName(), (SMacro)value);
							}
							env.define(asSymbol(var), value);
						} catch (KSchemeException e) {
							throw new ErrorWithCont("(define "+var+ " "+value+")",this,e);
						}
						return k.applyCont(value);
					}

//					@Override
//					public String toString() {
//						return "(define "+var+"*)\n"+k;
//					}
					
				});
			}
		});
		
		specialForm("if", new SSyntax() {
			@Override
			public Trampoline applySyntax(final CoreInterpreter interpreter, final Env env, IPair ifExp, final Cont k) {
				try {
					int len = length(ifExp);
					KSchemeAssert.assertTrue("If must have 2 or 3 arguments", len==3||len==4);
					Object test = cadr(ifExp);
					final Object thn  = caddr(ifExp);
					final Object els  = len==3 ? SchemeValue.makeUndefined() : cadddr(ifExp);
					
					return interpreter.eval(test, env, new Cont(test,k) {

						@Override
						protected Trampoline applyNow(Object value) {
							if (isFalse(value)) {
								return interpreter.eval(els, env, k);
							}
							else {
								return interpreter.eval(thn, env, k);
							}
						}

//						@Override
//						public String toString() {
//							return "(if * "+thn+" "+els+")\n"+k;
//						}
						
					});
				} catch (KSchemeException e) {
					throw new ErrorWithCont(""+ifExp,k,e);
				}
			}
		});
		
		specialForm("lambda", new SSyntax() {
			@Override
			public Trampoline applySyntax(CoreInterpreter interp, Env env, IPair exp, Cont k) {
				try {
					return k.applyCont(interp.makeProcedure(cadr(exp), cddr(exp), env));
				} catch (KSchemeException e) {
					throw new ErrorWithCont("applySyntax:"+exp,k,e);
				}
			}
		});
		
		specialForm("quote", new SSyntax() {
			@Override
			public Trampoline applySyntax(CoreInterpreter interpreter, Env env, IPair exp, Cont k) {
				try {
					KSchemeAssert.assertTrue("Mallformed 'quote' expression",isNull(cddr(exp)));
					return k.applyCont(cadr(exp));
				} catch (KSchemeException e) {
					throw new ErrorWithCont(""+exp,k,e);
				}
			}
		});

		specialForm("set!", new SSyntax() {
			@Override
			public Trampoline applySyntax(final CoreInterpreter interpreter, final Env env, final IPair exp, final Cont k) {
				try {
					KSchemeAssert.assertEquals("number of arguments", 3, length(exp));
					return interpreter.eval(caddr(exp), env, new Cont(caddr(exp), k) {
						@Override
						protected Trampoline applyNow(Object value) {
							try {
								env.assign(asSymbol(cadr(exp)), value);
							} catch (KSchemeException e) {
								throw new ErrorWithCont(exp,k,e);
							}
							return k.applyCont(value);
						}
					});
				} catch (KSchemeException e) {
					throw new ErrorWithCont(""+exp, k, e);
				}
			}
		});
		
	}

	/** Adds a special form to this interpreter */
	private void specialForm(String name, SSyntax formInterpreter) throws KSchemeException {
		KSchemeAssert.assertTrue("Multiply defined special form: "+name, !specialForms.containsKey(name));
		SSymbol sym = makeSymbol(name);
		specialForms.put(sym, formInterpreter);
		globalEnv.define(sym, formInterpreter);
		formInterpreter.gotName(sym);
	}
	
	public static abstract class SSyntax extends SchemeValue {
		
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


}
