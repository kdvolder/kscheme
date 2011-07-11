package ca.kscheme.data;

import ca.kscheme.interp.Cont;
import ca.kscheme.interp.ErrorWithCont;
import ca.kscheme.interp.SSyntax;
import ca.kscheme.interp.CoreInterpreter;
import ca.kscheme.interp.Trampoline;
import ca.kscheme.namespace.Env;
import ca.kscheme.primitives.Syntaxes;

public class SMacro extends SSyntax {

	private SProcedure transformer;

	protected SMacro(SProcedure transformerProc) {
		this.transformer = transformerProc;
	}

	@Override
	public Trampoline applySyntax(final CoreInterpreter interp, final Env env, final IPair exp, final Cont k) {
		return transformer.apply(exp, new Cont(exp, k) {
			@Override
			protected Trampoline applyNow(Object transformed) {
				try {
					if (isPair(transformed)) {
						setCar(exp, car(transformed));
						setCdr(exp, cdr(transformed));
					}
					else {
						setCar(exp, Syntaxes.begin);
						setCdr(exp, cons(transformed, makeNull()));
					}
				} catch (KSchemeException e) {
					k.raise("Problem caching expanded syntax: "+exp+"\n expanded = "+transformed,e);
				}
				return interp.eval(transformed, env, k);
			}

//			@Override
//			public String toString() {
//				return "(eval *)\n"+k;
//			}
		});
	}
}