//package ca.kscheme.namespace;
//
//import ca.kscheme.data.KSchemeException;
//import ca.kscheme.data.SSymbol;
//
//
//public class EmptyEnv extends Env {
//
//	@Override
//	public void assign(Object var, Object object) throws KSchemeException {
//		throw new KSchemeException("set! not allowed: EmptyEnv is immutable");
//	}
//
//	@Override
//	public void define(Object var, Object object) throws KSchemeException {
//		throw new KSchemeException("Define in empty env: ", var);
//	}
//
//	@Override
//	public Object lookup(Object sym) throws KSchemeException {
//		throw new KSchemeException("Unbound identifier: ", sym);
//	}
//
//	@Override
//	public String toString() {
//		return "Env{}";
//	}
//
//	@Override
//	public boolean isEmpty() {
//		return true;
//	}
//
//}
