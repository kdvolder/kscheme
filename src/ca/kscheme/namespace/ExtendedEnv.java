//package ca.kscheme.namespace;
//
//import org.junit.Assert;
//
//import ca.kscheme.data.KSchemeException;
//import ca.kscheme.data.SSymbol;
//import ca.kscheme.data.SchemeValue;
//
//import static ca.kscheme.data.SchemeValue.*;
//
//public class ExtendedEnv extends Env {
//
//	private Frame frame;
//	private Env next = null;
//
//	public ExtendedEnv(Env env) {
//		this.frame = new HashFrame();
//		this.next = env;
//	}
//
//	public ExtendedEnv(Env env, Frame frame) {
//		Assert.assertNotNull(frame);
//		this.next = env;
//		this.frame = frame;
//	}
//
//	@Override
//	public void define(Object var, Object object) throws KSchemeException {
//		frame.put(var, object);
//		if (object instanceof SchemeValue) {
//			((SchemeValue) object).gotName(var);
//		}
//	}
//
//	@Override
//	public Object lookup(Object sym) throws KSchemeException {
//		if (frame.containsKey(sym)) 
//			return frame.get(sym);
//		else if (next==null) 
//			throw new KSchemeException("Unbound identifier: ", sym);
//		else
//			return next.lookup(sym);
//	}
//
//	@Override
//	public void assign(Object var, Object object) throws KSchemeException {
//		if (frame.containsKey(var)) {
//			frame.put(var, object);
//			if (object instanceof SchemeValue) {
//				((SchemeValue)object).gotName(var);
//			}
//		}
//		else if (next!=null)
//			next.assign(var, object);
//		else
//			throw new KSchemeException("Unbound identifier: ",var);
//	}
//	
//	@Override
//	public String toString() {
//		StringBuilder result = new StringBuilder();
//		result.append("Env{\n");
//		Env current = this;
//		while (!current.isEmpty()) {
//			ExtendedEnv ext = (ExtendedEnv) current;
//			result.append("   " + ext.frame+"\n");
//			current = ext.next;
//		}
//		result.append("}\n");
//		return result.toString();
//	}
//
//	@Override
//	public boolean isEmpty() {
//		return false;
//	}
//
//	@Override
//	public Env extend(Frame module) {
//		if (frame.isEmpty()) {
//			return next.extend(module);
//		}
//		else
//			return super.extend(module);
//	}
//}
