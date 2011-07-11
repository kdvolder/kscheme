package ca.kscheme.interp;

/**
 * A Trampoline is used to import proper tail recursion
 * in this interpreter.
 * 
 * The idea is that when you want to make a tail call, 
 * e.g. to evaluate an expression, and you want the Java 
 * stack to be properly dropped down, instead of builduin up,
 * you return a trampoline which does the work contained in 
 * the tailcall rather than making the call directly.
 * 
 * It is the responsibility of the caller to force any
 * trampolines that may fall out of interpreting expressions.
 */
public abstract class Trampoline {

	public static final Trampoline theNull = new Trampoline() {

		@Override
		public Trampoline force1() {
			return this;
		}
		
	};

	public final void force()  {
		Trampoline result = this;
		while (result != Trampoline.theNull) {
			result = ((Trampoline) result).force1();
		}
	}

	public abstract Trampoline force1();
	
}
