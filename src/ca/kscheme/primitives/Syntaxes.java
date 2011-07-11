package ca.kscheme.primitives;

import static ca.kscheme.data.SchemeValue.cons;
import ca.kscheme.data.IPair;
import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SchemeValue;
import ca.kscheme.interp.Cont;
import ca.kscheme.interp.CoreInterpreter;
import ca.kscheme.interp.ErrorWithCont;
import ca.kscheme.interp.KSchemeAssert;
import ca.kscheme.interp.SSyntax;
import ca.kscheme.interp.Trampoline;
import ca.kscheme.namespace.ClassFrame;
import ca.kscheme.namespace.Env;
import ca.kscheme.namespace.SchemeName;

/**
 * Provides implementations for "basic" scheme syntaxes:
 * <code>begin</code>, <code>define</code>, <code>if</code>, 
 * <code>lambda</code>, <code>quote</code>, <code>set!</code> 
 * 
 * @author kdvolder
 */
public class Syntaxes extends ClassFrame {
	
	public Syntaxes() throws KSchemeException {
		super();
	}

	public static final SSyntax begin = new SSyntax() {
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
	};
	
	public static final SSyntax define = new SSyntax("define") {
		@Override
		public Trampoline applySyntax(final CoreInterpreter interpreter, Env env, IPair defExp, Cont k) {
			try {
				Object var = cadr(defExp);
				Object valExp = caddr(defExp);
				if (isPair(var)) { // expand (define (f args) ...) syntax
					return defineIt(interpreter, env, car(var), makeLambda(cdr(var), cddr(defExp)), k);
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
						env.define(asSymbol(var), value);
					} catch (KSchemeException e) {
						throw new ErrorWithCont("(define "+var+ " "+value+")",this,e);
					}
					return k.applyCont(value);
				}

//				@Override
//				public String toString() {
//					return "(define "+var+"*)\n"+k;
//				}
				
			});
		}
	};
	@SchemeName("if")
	public static final SSyntax iff = new SSyntax("if") {
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

//					@Override
//					public String toString() {
//						return "(if * "+thn+" "+els+")\n"+k;
//					}
					
				});
			} catch (KSchemeException e) {
				throw new ErrorWithCont(""+ifExp,k,e);
			}
		}
	};
	public static final SSyntax lambda = new SSyntax("lambda") {
		@Override
		public Trampoline applySyntax(CoreInterpreter interp, Env env, IPair exp, Cont k) {
			try {
				return k.applyCont(interp.makeProcedure(cadr(exp), cddr(exp), env));
			} catch (KSchemeException e) {
				throw new ErrorWithCont("applySyntax:"+exp,k,e);
			}
		}
	};
	public static final SSyntax quote = new SSyntax("quote") {
		@Override
		public Trampoline applySyntax(CoreInterpreter interpreter, Env env, IPair exp, Cont k) {
			try {
				KSchemeAssert.assertTrue("Mallformed 'quote' expression",isNull(cddr(exp)));
				return k.applyCont(cadr(exp));
			} catch (KSchemeException e) {
				throw new ErrorWithCont(""+exp,k,e);
			}
		}
	};
	@SchemeName("set!")
	public static final SSyntax set = new SSyntax("set!") {
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
	};
	
	protected static SchemeValue makeLambda(Object formals, Object body) {
		return cons(lambda,cons(formals, body));
	}

}
