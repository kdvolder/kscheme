package ca.kscheme.reader;

import java.net.URL;

import ca.kscheme.data.Proxy;
import ca.kscheme.data.SourcePosition;


/** 
 * A Scheme SyntaxObj is essentially a Scheme Object annotated with
 * additional information, such as the source location from where this
 * Scheme object was parsed.
 * <p>
 * The SyntaxObj can be treated as a Proxy for the object it wraps.
 */
public class SyntaxObj implements Proxy {
	
	private Object value;
	private SourcePosition startPos;
	private SourcePosition endPos;

	public SyntaxObj(Object value, SourcePosition startPos,
			SourcePosition endPos) {
		this.value = value;
		this.startPos = startPos;
		this.endPos = endPos;
	}

	@Override
	public String toString() {
		return ""+value;
	}

	@Override
	public Object unproxy() {
		return value;
	}

	public String toStringWithLocation() {
		return ""+startPos+"\n   "+value.toString();
	}
	
	public URL getSourceURL() {
		return startPos.getSourceURL();
	}

}
