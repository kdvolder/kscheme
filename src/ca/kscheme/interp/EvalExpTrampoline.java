package ca.kscheme.interp;

import ca.kscheme.namespace.Env;


public class EvalExpTrampoline extends Trampoline {

	private CoreInterpreter interp;
	private Env env;
	private Object exp;
	private Cont k;

	public EvalExpTrampoline(CoreInterpreter schemeInterpreter, Env env, Object valExp, Cont k) {
		this.interp = schemeInterpreter;
		this.env = env;
		this.exp = valExp;
		this.k = k;
	}

	@Override
	public Trampoline force1() {
		return interp.eval(exp, env, k);
	}

	@Override
	public String toString() {
		return "Tramp("+exp+")";
	}
	
}
