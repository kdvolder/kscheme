package ca.kscheme.primitives;

import java.net.URL;

import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SProcedure;
import ca.kscheme.interp.Cont;
import ca.kscheme.interp.ErrorWithCont;
import ca.kscheme.interp.KSchemeAssert;
import ca.kscheme.interp.CoreInterpreter;
import ca.kscheme.interp.SSyntax;
import ca.kscheme.interp.Trampoline;
import ca.kscheme.namespace.ClassFrame;
import ca.kscheme.namespace.SchemeName;


/**
 * The Scheme Primitives provided by this Frame require access to the interpreter.
 * 
 * @author kdvolder
 */
public class InterpreterProcedures extends ClassFrame {
	
	public InterpreterProcedures(CoreInterpreter interp) throws KSchemeException {
		super();
		this.interp = interp;
	}

	private CoreInterpreter interp;
	
	public final SProcedure eval = new SProcedure() {
		@Override
		public Trampoline apply(Object rands, Cont k) {
			try {
				KSchemeAssert.assertEquals("Number of rands",1, length(rands));
				return interp.tGlobalEval(car(rands), k);
			} catch (KSchemeException e) {
				throw new ErrorWithCont("apply eval "+rands, k, e);
			}
		}
	};

	@SchemeName("load/URL")
	public final Object load(URL url) throws KSchemeException {
		return interp.load(url);
	};

	public final SProcedure read = new SProcedure() {
		@Override
		public Trampoline apply(Object rands, Cont k) {
			try {
				KSchemeAssert.assertEquals("Number of args", 1, length(rands));
				Object input = car(rands);
				return k.applyCont(interp.read(asInputPort(input)));
			} catch (KSchemeException e) {
				throw new ErrorWithCont("read "+rands,k,e);
			}
		}
	};
	
}
