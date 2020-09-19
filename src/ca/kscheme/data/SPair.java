package ca.kscheme.data;

import com.github.kdvolder.util.Assert;

public class SPair extends SchemeValue implements IPair {
	
	private Object car;
	private Object cdr;
	private boolean mutable = true;

	SPair(Object car2, Object cdr2) {
		this(car2,cdr2,true);
	}
	SPair(Object car2, Object cdr2, boolean mutable) {
		this.car = car2;
		this.cdr = cdr2;
		this.mutable = mutable;
	}

	@Override
	public boolean isEqual(Object obj) {
		if (obj==null) return false;
		if (!isPair(obj))
			return false;
		IPair other;
		try {
			other = asPair(obj);
		} catch (KSchemeException e) {
			throw new ImpossibleError(e);
		}
		return isEqual(this.car, other.car())
			&& isEqual(this.cdr, other.cdr());
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append('(');
		try {
			toStringRest(this, out);
		} catch (Exception e) {
			out.append(e);
		}
		out.append(')');
		return out.toString();
	}

	private void toStringRest(Object val, StringBuilder out) throws Exception {
		if (isPair(val)) {
			out.append(toString(car(val)));
			out.append(' ');
			toStringRest(cdr(val), out);
		}
		else if (isNull(val)) {}
		else {
			out.append(" . ");
			out.append(val.toString());
		}
	}

	/* (non-Javadoc)
	 * @see ca.kdvolder.myscheme.data.IPair#car()
	 */
	public Object car() {
		return car;
	}
	
	/* (non-Javadoc)
	 * @see ca.kdvolder.myscheme.data.IPair#cdr()
	 */
	public Object cdr() {
		return cdr;
	}
	
	public void setCar(Object v) {
		Assert.isLegalState(mutable);
		this.car = v;
	}
	public void setCdr(Object v) {
		Assert.isLegalState(mutable);
		this.cdr = v;
	}
	public void makeImmutable() {
		this.mutable = false;
	}
}
