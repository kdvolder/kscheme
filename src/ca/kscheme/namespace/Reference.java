package ca.kscheme.namespace;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SSymbol;
import ca.kscheme.data.SchemeValue;

/**
 * An abstract base class for anything that represents a reference to
 * an object of a given type. 
 * @author kdvolder
 */
public abstract class Reference<T> {
	
	private SSymbol name;
	
	public abstract void set(T newValue) throws KSchemeException;
	public abstract T get() throws KSchemeException;
	public void gotName(SSymbol name) {
		try {
			Object val = this.get();
			if (val instanceof SchemeValue)
				((SchemeValue) val).gotName(name);
		} catch (KSchemeException e) {
			//ignore... this is not an essential operation. It is ok to fail silently.
		}
	}

}
