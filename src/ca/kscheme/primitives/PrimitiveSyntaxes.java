package ca.kscheme.primitives;

import static ca.kscheme.data.SchemeValue.cons;
import static ca.kscheme.data.SchemeValue.makeSymbol;
import ca.kscheme.data.SSymbol;

public class PrimitiveSyntaxes {

	public static SSymbol begin = makeSymbol("begin");
	public static SSymbol lambda = makeSymbol("lambda");

	public static Object makeBegin(Object body) {
		return cons(begin, body);
	}

	public static Object makeLambda(Object formals, Object body) {
		return cons(lambda,cons(formals, body));
	}
	
}