package ca.kscheme.namespace;

import ca.kscheme.data.KSchemeException;

public class ImmutableRef<T> extends Reference<T> {

	private T obj;

	public ImmutableRef(T obj) {
		this.obj = obj;
	}

	@Override
	public T get() {
		return obj;
	}

	@Override
	public void set(T newValue) throws KSchemeException {
		throw new KSchemeException("set: The reference to "+obj+" is immutable");
	}

}
