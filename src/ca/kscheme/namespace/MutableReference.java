package ca.kscheme.namespace;

import ca.kscheme.data.KSchemeException;

public class MutableReference<T> extends Reference<T> {

	private T obj;

	public MutableReference(T obj) {
		this.obj = obj;
	}

	@Override
	public T get() {
		return obj;
	}

	@Override
	public void set(T newValue) throws KSchemeException {
		obj = newValue;
	}

}
