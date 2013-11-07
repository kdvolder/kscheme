package ca.kscheme.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import ca.kscheme.interp.Cont;
import ca.kscheme.interp.CoreInterpreter.SSyntax;
import ca.kscheme.interp.Trampoline;
import ca.kscheme.reader.SyntaxObj;

/** 
 * This is an abstract Class that may be used to subclass from to
 * create MyScheme compatible data types. 
 * 
 * However, there is no real need to create subclasses for this
 * purpose. Indeed, the idea is that any Java object should be
 * compatible with KScheme, including the null pointer "object".
 * 
 * This class therefore contains mostly static methods that can
 * be called on Objects of any type. Typically, they perform
 * a dynamic type check (by type casting) and dispatch to
 * a method of some specific type.
 */
public abstract class SchemeValue {
	
	private static final SchemeValue theEOFObject = makeSymbol("#EOF");
	
	public static int add(int n1, int n2) throws KSchemeException {
		return asInt(n1)+asInt(n2);
	}

	public static Trampoline apply(Object proc, Object rands, Cont k) {
		try {
			return asProcedure(proc).apply(rands, k);
		} catch (KSchemeException e) {
			return k.raise("apply \n proc="+proc+"\n rand = "+rands,e);
		}
	}
	public static boolean asBoolean(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!(obj instanceof Boolean))
			throw new KSchemeException("asBoolean: "+obj+" is not a Boolean");
		return (Boolean)(obj);
	}
	public static char asChar(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!(obj instanceof Character))
			throw new KSchemeException("asChar: "+obj+" is not an Character");
		return (Character)(obj);
	}
	public static double asDouble(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!(obj instanceof Double))
			throw new KSchemeException("asDouble: "+obj+" is not a Double");
		return (Double)(obj);
	}
	public static SInputPort asInputPort(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!(obj instanceof SInputPort))
			throw new KSchemeException("asInputPort: "+obj+" is not an SInputPort");
		return (SInputPort)(obj);
	}
	public static int asInt(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!(obj instanceof Integer))
			throw new KSchemeException("asInt: "+obj+" is not an Integer");
		return (Integer)(obj);
	}
	public static Number asNumber(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!isNumber(obj))
			throw new KSchemeException("asNumber: "+obj+" is not a Number");
		return (Number)(obj);
	}
	public static IPair asPair(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!(obj instanceof IPair))
			throw new KSchemeException("asPair: "+obj+" does not implement IPair", null);
		return (IPair)(obj);
	}
	
	public static String asString(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!isString(obj))
			throw new KSchemeException("asString: "+obj+" is not a String");
		return (String)(obj);
	}
	public static SyntaxObj asSyntaxObj(Object obj) throws KSchemeException {
		if (!isSyntaxObj(obj))
			throw new KSchemeException("asSyntaxObj: "+obj+" is not a SyntaxObj");
		return (SyntaxObj)obj;
	}
	public static SSymbol asSymbol(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!isSymbol(obj))
			throw new KSchemeException("asSymbol: "+obj+" is not a Symbol");
		return (SSymbol)(obj);
	}
	public static Object cadddr(Object cons) throws KSchemeException {
		return caddr(cdr(cons));
	}
	public static Object caddr(Object cons) throws KSchemeException {
		return cadr(cdr(cons));
	}
	public static Object cadr(Object cons) throws KSchemeException {
		return car(cdr(cons));
	}
	public static Object car(Object cons) throws KSchemeException {
		return asPair(cons).car();
	}
	public static Object cddr(Object cons) throws KSchemeException {
		return cdr(cdr(cons));
	}
	public static Object cdr(Object cons) throws KSchemeException {
		return asPair(cons).cdr();
	}
	
	public static int char2int(Object obj) throws KSchemeException {
		return asChar(obj);
	}
	public static SPair cons(Object car, Object cdr) {
		return new SPair(car,cdr);
	}
	
	public static SPair cons(Object car, Object cdr, boolean mutable) {
		return new SPair(car,cdr, mutable);
	}
	
	public static void display(Object obj, Object port) throws Exception {
		if (port instanceof PrintStream) {
			((PrintStream)port).print(obj);
		}
		else {
			((PrintWriter)port).print(obj);
		}
	}
	private static boolean equalArrays(Object o1, Object o2) {
		if (o1 instanceof Object[] && o2 instanceof Object[]) {
			Object[] arr1 = (Object[]) o1; 
			Object[] arr2 = (Object[]) o2; 
			if (arr1.length!=arr2.length) 
				return false;
			for (int i = 0; i < arr1.length; i++) {
				if (!isEqual(arr1[i], arr2[i]))
					return false;
			}
			return true;
		}
		else
			return false;
	}

	public static boolean sameInt(int n1, int n2) {
		return n1==n2;
	}
	public static boolean greaterInt(int n1, int n2) {
		return n1>n2;
	}
	public static int hashCode(Object obj) throws Exception {
		return obj==null ? 0 : obj.hashCode();
	}
	
	public static boolean isChar(Object val) {
		val = unproxy(val);
		return val instanceof Character;
	}
	public static boolean isCharNumeric(Object val) throws KSchemeException {
		return isChar(val) && Character.isDigit(asChar(val));
	}
	public static boolean isCharWhitespace(Object val) throws KSchemeException {
		return isChar(val) && Character.isWhitespace(asChar(val));
	}
	public static boolean isDouble(Object val) {
		val = unproxy(val);
		return val instanceof Double;
	}
	public static boolean isEofObject(Object val) {
		return isEqv(val,makeEOF());
	}
	public static boolean isEq(Object o1, Object o2) throws KSchemeException {
		o1 = unproxy(o1);
		o2 = unproxy(o2);
		return o1==o2;
	}

	public static boolean isEqual(Object o1, Object o2) {
		o1 = unproxy(o1);
		o2 = unproxy(o2);
		if (o1==null) 
			return o2==null;
		else if (o1 instanceof Object[]) 
		    return equalArrays(o1,o2);
		else if (o1 instanceof SchemeValue)
			return ((SchemeValue) o1).isEqual(o2);
		else 
			return o1.equals(o2);
	}

	public static Boolean isEqv(Object v1, Object v2) {
		v1 = unproxy(v1);
		v2 = unproxy(v2);
		return v1==v2 
		    || v1!=null && v1.equals(v2);
	}
	public static boolean isFalse(Object val) {
		val = unproxy(val);
		return isEqv(val, false);
	}
	public static boolean isInputPort(Object val) {
		val = unproxy(val);
		return val instanceof SInputPort;
	}
	public static boolean isInt(Object val) {
		val = unproxy(val);
		return val instanceof Integer;
	}
	public static boolean isNull(Object val) {
		val = unproxy(val);
		return val == SNull.the;
	}
	public static boolean isNumber(Object val) {
		val = unproxy(val);
		return val instanceof Number;
	}
	
	public static boolean isPair(Object val) {
		val = unproxy(val);
		return val instanceof IPair;
	}
	
	public static boolean isProcedure(Object val) {
		val = unproxy(val);
		return val instanceof SProcedure;
	}
	
	public static boolean isString(Object val) {
		val = unproxy(val);
		return val instanceof String;
	}
	
	public static boolean isSymbol(Object val) {
		val = unproxy(val);
		return val instanceof SSymbol;
	}
	
	public static boolean isSyntax(Object val) {
		val = unproxy(val);
		return val instanceof SSyntax;
	}
	
	public static boolean isSyntaxObj(Object obj) {
		return obj instanceof SyntaxObj;
	}
	public static boolean isVector(Object val) {
		val = unproxy(val);
		return val instanceof Object[];
	}
	public static int length(Object sexp) throws KSchemeException {
		if (isNull(sexp))
			return 0;
		else if (isPair(sexp)) 
			return 1+length(cdr(sexp));
		else
			throw new KSchemeException("length -- Not a proper list: "+sexp);
	}

	public static Object list(Object... obj) {
		Object result = makeNull();
		for (int i = obj.length-1; i >= 0; i--) {
			result = cons(obj[i], result);
		}
		return result;
	}
	public static Boolean makeBoolean(boolean b) {
		return Boolean.valueOf(b);
	}
	public static Character makeChar(char result) {
		return new Character(result);
	}
	public static Character makeChar(int i) {
		return new Character((char)i);
	}
	public static SchemeValue makeEOF() {
		return theEOFObject;
	}
	public static SInputPort makeInputPort(Reader reader, URL sourceFile) {
		return new SInputPort(reader, sourceFile);
	}
	public static SInputPort makeInputPort(URL resource) throws KSchemeException {
		try {
			return new SInputPort(new InputStreamReader(resource.openStream()),
								 resource);
		} catch (IOException e) {
			throw new KSchemeException("makeInputPort "+resource, e);
		}
	}
	public static SchemeValue makeMacro(SProcedure transformerProc) {
		return new SMacro(transformerProc);
	}
	public static Object makeNull() {
		return SNull.the;
	}
	public static Number makeNumber(int i) {
		return new Integer(i);
	}
	public static Object parseInt(String str, int radix) throws KSchemeException {
		try {
			return Integer.parseInt(str, radix);
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static String makeString(String string) {
		return string;
	}
	
	public static SInputPort makeStringInputPort(String string) {
		return new SInputPort(new StringReader(string), null);
	}
	public static SSymbol makeSymbol(String string) {
		return SSymbol.intern(string);
	}
	public static SSymbol makeSymbol(String string, boolean caseSensitive) {
		if (!caseSensitive)
			string = string.toLowerCase();
		return makeSymbol(string);
	}
	public static Object makeUndefined() {
		return makeNull();
	}
	public static Object makeVector(int len, Object element) {
		Object[] vector = new Object[len];
		for (int i = 0; i < vector.length; i++) {
			vector[i] = element;
 		}
		return vector;
	}
	public static Object makeVector(Object vectorElements) throws KSchemeException {
		Object[] vector = new Object[length(vectorElements)];
		for (int i = 0; i < vector.length; i++) {
			vector[i] = car(vectorElements);
			vectorElements = cdr(vectorElements);
 		}
		return vector;
	}
	public static int mul(int n1, int n2) throws KSchemeException {
		return n1*n2;
	}
	public static Number negate(Object n) throws KSchemeException {
		if (isInt(n))
			return -asInt(n);
		else if (isDouble(n)) {
			return -asDouble(n);
		}
		else
			throw new KSchemeException("negate -- unsupported operand type: "+n);
	}
	public static int quotient(int x,int y) throws KSchemeException {
		try {
			return x/y;
		} catch (ArithmeticException e) {
			throw new KSchemeException(""+x+"/"+y,e);
		}
	}
	public static int sub(int n1, int n2) throws KSchemeException {
		return n1-n2;
	}
	
	public static void setCar(Object cons, Object v) throws KSchemeException {
		asPair(cons).setCar(v);
	}
	
	public static void setCdr(Object cons, Object v) throws KSchemeException {
		asPair(cons).setCdr(v);
	}
	
	public static Object[] toUnproxiedArray(Object list) throws KSchemeException {
		Object[] arr = new Object[length(list)];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = unproxy(car(list));
			list = cdr(list);
		}
		return arr;
	}
	public static Object[] toArray(Object list) throws KSchemeException {
		Object[] arr = new Object[length(list)];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = car(list);
			list = cdr(list);
		}
		return arr;
	}
	public static String toString(Object obj) {
		return ""+obj;
	}
	public static String toStringWithLocation(Object obj) {
		if (isSyntaxObj(obj)) {
			try {
				return asSyntaxObj(obj).toStringWithLocation();
			} catch (KSchemeException e) {
				return obj.toString()+" ERROR: "+e.toString();
			}
		}
		else if (obj==null)
			return "()";
		else
			return obj.toString();
	}
	public static int toInt(char c) {
		return (int) c;
	}
	public static Object unproxy(Object val) {
		while (val instanceof Proxy) {
			val = ((Proxy)val).unproxy();
		}
		return val;
	}
	public static void vectorSet(Object ovector, Object oindex, Object oval) throws Exception {
		((Object[])ovector)[asInt(oindex)] = oval;
	}
	protected SchemeValue() {}
	public Class<?> asClass(Object car) throws KSchemeException {
		try {
			return (Class<?>)(car);
		} catch (Throwable e) {
			throw new KSchemeException("Not a class? "+car,e);
		}
	}
	public static SProcedure asProcedure(Object obj) throws KSchemeException {
		if (!isProcedure(obj))
			throw new KSchemeException("asProcedure: "+obj+" is not a Procedure");
		return (SProcedure)obj;
	}
	public static URL asURL(Object obj) throws KSchemeException {
		obj = unproxy(obj);
		if (!(obj instanceof URL))
			throw new KSchemeException("asURL: "+obj+" is not a URL");
		return (URL)obj;
	}
	/** 
	 * Some Scheme values may use their "name" to display themselves.
	 * However, SchemeValues do not really have a name, instead they are bound
	 * to variables. This method is called when a value was bound to a variable
	 * by a set! or define expression. Most Scheme values will ignore this.
	 * But those values who like to have a name, can override this method.
	 */
	public void gotName(Object var) {
	}
	protected boolean isEqual(Object o2) {
		return equals(o2);
	}
}
