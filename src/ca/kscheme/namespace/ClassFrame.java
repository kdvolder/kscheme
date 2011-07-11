package ca.kscheme.namespace;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import ca.kscheme.data.ImpossibleError;
import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SProcedure;
import ca.kscheme.data.SSymbol;
import ca.kscheme.data.SchemeValue;
import ca.kscheme.interp.Cont;
import ca.kscheme.interp.ErrorWithCont;
import ca.kscheme.interp.KSchemeAssert;
import ca.kscheme.interp.Trampoline;

/**
 * ClassFrame is an abstract class from which a user can inherit to create
 * a Frame that maps kscheme identifiers to methods and fields of the 
 * class. 
 * <p>
 * To the user, a method will look like an immutable variable bound to a procedure.
 * Fields will look like variables that can be mutated.
 */
public abstract class ClassFrame extends HashFrame {
	
	public ClassFrame() throws KSchemeException {
		Class<?> cls = this.getClass();
		Field[] fields = cls.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];
			if (Modifier.isPublic(f.getModifiers())) {
				SSymbol name = getName(f);
				KSchemeAssert.assertTrue("Name clash for "+name, lookup(name)==null);
				def(name, makeFieldReference(f));
			}
		}
		Method[] methods = cls.getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			Method m = methods[i];
			if (Modifier.isPublic(m.getModifiers())) {
				SSymbol name = getName(m);
				KSchemeAssert.assertTrue("Name clash for "+name, lookup(name)==null);
				def(name, makeMethodReference(m));
			}
		}
	}

	private SSymbol getName(AccessibleObject m) {
		SchemeName annot = m.getAnnotation(SchemeName.class);
		String name;
		if (annot==null) {
			if (m instanceof Field)
				name = ((Field)m).getName();
			else if (m instanceof Method)
				name = ((Method)m).getName();
			else
				throw new ImpossibleError("Only Fields or Methods");
		}
		else {
			name = annot.value();
		}
		return SchemeValue.makeSymbol(name);
	}

	private Reference<Object> makeMethodReference(final Method m) {
		return new ImmutableRef<Object>(new SProcedure() {
			@Override
			public Trampoline apply(Object rands, Cont k) {
				try {
					return k.applyCont(m.invoke(ClassFrame.this, SchemeValue.toUnproxiedArray(rands)));
				} catch (Exception e) {
					throw new ErrorWithCont("invoking "+m,k,e);
				}
			}
		});
	}

	private Reference<Object> makeFieldReference(final Field f) {
		return new Reference<Object>() {
			@Override
			public Object get() throws KSchemeException {
				try {
					return f.get(ClassFrame.this);
				} catch (Exception e) {
					throw new KSchemeException("Accessing "+f, e);
				}
			}

			@Override
			public void set(Object newValue) throws KSchemeException {
				try {
					f.set(ClassFrame.this, newValue);
				} catch (Exception e) {
					throw new KSchemeException("Setting "+f, e);
				}
			}
			
			@Override
			public String toString() {
				return "FieldRef("+f.getDeclaringClass().getName()+"."+f.getName()+")";
			}
		};
	}


}
