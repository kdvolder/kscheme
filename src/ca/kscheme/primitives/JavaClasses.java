package ca.kscheme.primitives;

import ca.kscheme.data.ImpossibleError;
import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SProcedure;
import ca.kscheme.data.SSymbol;
import ca.kscheme.namespace.ClassFrame;
import ca.kscheme.namespace.HashFrame;
import ca.kscheme.namespace.Reference;
import static ca.kscheme.data.SchemeValue.*;


public class JavaClasses extends ClassFrame {

	public JavaClasses() throws KSchemeException {
		super();
	}

	public Class<?> intClass = int.class;
	public Class<?> charClass = char.class;
	public Class<?> booleanClass = boolean.class;
	
	public Class<?> getClass(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}
}
