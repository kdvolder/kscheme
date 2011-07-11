package ca.kscheme.data;

/** 
 * Class to represent Scheme null, which is different from Java's null
 */
public final class SNull {
	
	public static final SNull the = null;
	
	private SNull() {}
	
	@Override
	public String toString() {
		return "()";
	}
	
}
