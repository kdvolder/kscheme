package ca.kscheme.data;

/**
 * This interface can be implemented by any class that is a kind
 * of indirect representation of some other object.
 * <p>
 * For example, when lazy evaluation is supported, lazy values
 * need to be forced to get their "actual" value.
 * <p>
 * Or a SyntaxObj is a wrapper around another Scheme object,
 * attaching extra information to that Scheme object.
 * In most contexts the syntax object should be treated
 * as equivalent to the object it wraps. This can be
 * done straightforwardly by implementing the {@link Proxy}
 * interface.
 * <p>
 * Generally operations applying to values that depend on the
 * specifics of a value should unproxy the value before using
 * it.
 * 
 * @author kdvolder
 */
public interface Proxy {
	
	Object unproxy();

}
