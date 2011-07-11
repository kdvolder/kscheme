package ca.kscheme.namespace;

import java.util.Iterator;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SSymbol;
import ca.kscheme.data.SchemeValue;


public abstract class Frame {

	public static class Binding {
		public Binding(SSymbol name, Reference<Object> value) {
			this.name = name;
			this.value = value;
		}
		public final SSymbol name;
		public final Reference<Object> value;
	}

	/**
	 * Allocate a new location in this frame. The value stored at this location
	 * is initially undefined.
	 * <p>
	 * Returns a reference to the newly allocated location.
	 */
	public final Reference<Object> def(final SSymbol sym) throws KSchemeException {
		Reference<Object> loc = new MutableReference<Object>(SchemeValue.makeUndefined());
		def(sym, loc);
		return loc;
	}
	
	/**
	 * Bind a name in this frame to an already existing location.
	 * @throws KSchemeException 
	 */
	public abstract void def(SSymbol name, Reference<Object> ref) throws KSchemeException;
	
	/**
	 * Retrieve the location of a given name in this Frame. 
	 */
	public abstract Reference<Object> lookup(SSymbol sym) throws KSchemeException;

	public abstract Iterable<Binding> exportedBindings();

}
