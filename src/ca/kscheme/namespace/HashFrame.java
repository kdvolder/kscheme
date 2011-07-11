package ca.kscheme.namespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.org.apache.bcel.internal.generic.AllocationInstruction;

import junit.framework.Assert;
import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SSymbol;
import ca.kscheme.data.SchemeValue;

/**
 * An implementation of Frame backed by a (Hash)Map
 * @author kdvolder
 */
public class HashFrame extends Frame {

	private static final boolean allowRedefinitions = true;
	private Map<SSymbol, Reference<Object>> map = new HashMap<SSymbol, Reference<Object>>();
	
	public HashFrame() {}
	
	public HashFrame(Map<SSymbol, Object> bindings) {
		for (Map.Entry<SSymbol, Object> entry : bindings.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	@Override
	public String toString() {
		return "HashFrame"+map.keySet().toString();
	}

	@Override
	public void def(SSymbol sym, Reference<Object> ref) throws KSchemeException {
		if (allowRedefinitions || map.get(sym)==null)
			map.put(sym, ref);
		else
			throw new KSchemeException("Variable "+sym+" already defined in scope");
	}
	
	@Override
	public Reference<Object> lookup(SSymbol sym) throws KSchemeException {
		return map.get(sym);
	}

	/**
	 * For use in subclasses to initialize the frame with automatically defined symbols.
	 */
	protected void put(SSymbol sym, Object val) {
		Assert.assertFalse(map.containsKey(sym));
		map.put(sym, new ImmutableRef<Object>(val));
	}

	@Override
	public Iterable<Binding> exportedBindings() {
		ArrayList<Frame.Binding> exported = new ArrayList<Binding>();
		for (Map.Entry<SSymbol, Reference<Object>> entry : map.entrySet()) {
			exported.add(new Binding(entry.getKey(), entry.getValue()));
		}
		return exported;
	}

}
