package ca.kscheme.interp;

import ca.kscheme.KScheme;
import ca.kscheme.data.KSchemeException;
import ca.kscheme.namespace.ClassFrame;
import ca.kscheme.namespace.Frame;
import ca.kscheme.namespace.SchemeName;

public class Require extends ClassFrame {
	
	private KScheme interpreter;

	public Require(KScheme interp) throws KSchemeException { 
		this.interpreter = interp; 
	}
	
	@SchemeName(":require")
	public void require(String className) throws ClassNotFoundException, KSchemeException {
		//This built-in version of require is minimalistic. It expects that the
		//argument to require is a name of a Java class that implements Frame.
		interpreter.require((Class<? extends Frame>) Class.forName(className));
	}

}
