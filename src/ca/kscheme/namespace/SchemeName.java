package ca.kscheme.namespace;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is for use together with Frame implementations that
 * use Java reflection to allow Java methods and fields to be made available
 * as "definitions" in a Scheme environment.
 * 
 * Normally, KScheme will just use a method or field's name for the Scheme 
 * variable name that corresponds to that method or field. However, if a
 * SchemeName annotation is present than the name specified in the annotations
 * attribute will be used instead.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemeName {
	String value();
}
