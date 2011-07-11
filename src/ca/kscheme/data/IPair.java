package ca.kscheme.data;

/**
 * The idea is that we could make many things behave like Scheme pairs
 * by having them implement this interface.
 * 
 * For now we really only have one implementation: SPair.
 */
public interface IPair {

	public abstract Object car();

	public abstract Object cdr();

	public abstract void setCar(Object v);

	public abstract void setCdr(Object v);

}