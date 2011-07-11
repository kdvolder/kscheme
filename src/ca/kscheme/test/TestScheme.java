package ca.kscheme.test;

import static ca.kscheme.data.SchemeValue.cons;
import static ca.kscheme.data.SchemeValue.isEqual;
import static ca.kscheme.data.SchemeValue.makeNull;
import static ca.kscheme.data.SchemeValue.makeNumber;
import static ca.kscheme.data.SchemeValue.makeSymbol;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ca.kscheme.KScheme;
import ca.kscheme.data.KSchemeException;
import ca.kscheme.data.SchemeValue;
import ca.kscheme.interp.CoreInterpreter;

public class TestScheme {
	
	protected KScheme scheme;

	@Before public void setup() throws Exception {
		scheme = CoreInterpreter.getDefault();
	}
	
	private void require(String string) throws KSchemeException {
		scheme.parseAndRun("(require '"+string+")");
	}

	@Test
	public void testTryCatch() throws Exception {
		testEval(makeSymbol("ok"),
				"(let ((cause-problem (lambda (x) (error x))))\n" +
				"  (call-with-handler (lambda() (cause-problem 'boohoo))\n" +
				"                     (lambda e 'ok)))");
		testEval(makeSymbol("ok"), 
				"(call-with-handler (lambda () (quotient 3 0))\n" +
				"     (lambda e 'ok))");
		testEval(makeSymbol("ok"), 
				"(call-with-handler (lambda () (1 0))\n" +
				"     (lambda e 'ok))");
		testEval(makeSymbol("ok"), 
				"(call-with-handler (lambda () (/ 3 0))\n" +
				"     (lambda e 'ok))");
		testEval(makeSymbol("ok"),
				"(call-with-handler (lambda () (error 'bad))" +
				"                   (lambda e 'ok))");
	}
	
	@Test
	public void testLocalDefine() throws Exception {
		testEval(44,  "(begin (define (foo x)" +
				"                  (define flub x)" +
				"                  flub)" +
				"               (foo 44))");
	}

	@Test
	public void testAnd() throws Exception {
		testEval(true,  "(and (= 1 1))");
		testEval(true,  "(and)");
		testEval(true,  "(and (= 1 1) (= 2 2))");
		testEval(false, "(and (= 1 1) (= 3 2))");
	}
	
	@Test
	public void testAppend() throws Exception {
		testEval(scheme.parseAndRun("'(1 2 3 4)"), "(append '(1 2) '(3 4))");
	}

	@Test public void testBegin() throws Exception {
		testEval(5, "(begin (define x 5) x)");
	}
	
	@Test public void testCase() throws Exception {
		testEval("'composite", 
				"(case (* 2 3)" +
				"   ((2 3 5 7) 'prime)" +
				"   ((1 4 6 8 9) 'composite))");
		testEval("'consonant",
				"(case (car '(c d))" +
			    "   ((a e i o u) 'vowel)" +
			    "   ((w y) 'semivowel)" +
			    "   (else 'consonant))");
	}
	@Test
	public void testChar() throws Exception {
		testEval('a', "#\\a");
		testEval('"', "#\\\"");
	}
	@Test
	public void testCharString() throws Exception {
		testEval("#\\?", "(string-ref \"?abc\" 0)"); 
		testEval("#\\a", "(string-ref \"?abc\" 1)"); 
		testEval("#\\b", "(string-ref \"?abc\" 2)"); 
		testEval("#\\c", "(string-ref \"?abc\" 3)"); 
	}
	@Test
	public void testCond() throws Exception {
		testEval(1, "(cond (#t 1) (else 2))");
		testEval(2, "(cond (#f 1) (else 2))");
		testEval(3, "(cond (#f 1) (#f 2) (else 3))");
		testEval(2, "(cond (#f 1) (#t 2) (else 3))");
		testEval(1, "(cond (#t 1) (#t 2) (else 3))");
		testEval("'((1 . 101) (2 . 202) (3 . 303) (4 . 304))", 
				"(let ((b #f))" +
				"  (let ((f (lambda (a)" +
				"             (cond ((= a 1) (set! b (+ a 100)) (cons a b))" +
				"                   ((= a 2) (set! b (+ a 200)) (cons a b))" +
				"                   (else (set! b (+ a 300)) (cons a b))))))" +
				"    (map f '(1 2 3 4))))");
	}
	@Test public void testDefFun() throws Exception {
		testEval(25, "(begin (define (square x) (* x x)) " +
				            "(square 5))");
	}
	@Test public void testDisplay() throws Exception {
		testOutput("1", "(display 1)");
	}
	@Test public void testEqTrueTrue() throws Exception {
		testEval(true, "(eq? (eqv? 4 4) (eqv? 4 4))");
		// This test fails because when a method returning a boolean is called,
		// apparantly the Java reflection API creates a new Boolean instance each
		// time.
	}
	@Test public void testEqv() throws Exception {
		testEval(true, "(eqv? (+ 4 5) (+ 5 4))");
		testEval(false, "(eqv? (cons 1 2) (cons 1 2))");
		testEval(false, "(eqv? (list 1 2) (list 1 2))");
		testEval(true, "(eqv? #\\a #\\a)");
		testEval(true, "(eqv? 'abc 'abc)");
		testEval(true, "(eqv? #t #t)");
		testEval(true, "(eqv? #t (= 2 2))");
		testEval(true, "(eqv? #f #f)");
		testEval(true, "(eqv? () ())");
	}
	private void testEval(boolean b, String exp) throws Exception {
		testEval(SchemeValue.makeBoolean(b), exp);
	}
	private void testEval(char c, String exp) throws Exception {
		testEval(SchemeValue.makeChar(c), exp);
	}
	
	private void testEval(int i, String exp) throws Exception {
		testEval(SchemeValue.makeNumber(i), exp);
	}

	private void testEval(Object expected, String exp) throws Exception {
		Assert.assertTrue(isEqual(expected, scheme.parseAndRun(exp)));
	}
	private void testEval(String exp1, String exp2) throws Exception {
		Assert.assertTrue(isEqual(scheme.parseAndRun(exp1), scheme.parseAndRun(exp2)));
	}


	@Test
	public void testFunnySymbols() throws Exception {
		require("common-list-functions");
		testEval(makeSymbol("Ljava.lang.Object;"), "'|Ljava.lang.Object;|");
		testEval(true,  "(every symbol? '(a b c))");
		testEval(true,  "(every symbol? '(+ - ... !.. $.+ %.- &.! *.: /:. :+. <-. =. >. ?. ~. _. ^.))");
		testEval(makeSymbol(";"), "'|;|");
	}
	
	@Test
	public void testIf() throws Exception {
		testEval(1,  "(if (> 2 1) 1 0)");
		testEval(0,  "(if (> 1 2) 1 0)");
		testEval(1,  "(if () 1 0)");
		testEval(1,  "(if 'ok 1 0)");
		testEval(1,  "(if #t 1 0)");
		testEval(0,  "(if #f 1 0)");
	}
	
	@Test public void testIfMatch() throws Exception {
		testEval("'bad",
				"(if-match (?nam ?ini ?upd) '(a b) " +
				"       (% ?nam ?ini)" +
				"       'bad)");
		testEval("(if-match ?x 'test ?x 'bad)", "'test");
		testEval("(if-match (if ?x ?y) '(if 1 2) (list ?x ?y) 'bad)", "'(1 2)");
		testEval("(if-match (if ?x ?y) '(iff 1 2) (list ?x ?y) 'bad)", "'bad");
	}
	@Test
	public void testIsList() throws Exception {
		testEval(true,  "(list-of? number? '(1 2 3))");
		testEval(false, "(list-of? number? '(1 2 . 3))");
		testEval(false, "(list-of? number? '(1 2 a))");
	}

	@Test
	public void testIsProcedure() throws Exception {
		testEval(true, "(procedure? +)");
		testEval(false, "(procedure? 5)");
	}
	
	@Test
	public void testIsString() throws Exception {
		testEval(true, "(string? \"Hello world!\")");
		testEval(false, "(string? 5)");
		testEval(false, "(string? 'abc)");
	}
	
	@Test
	public void testIsVector() throws Exception {
		testEval(true,  "(vector? #(1 2 3))");
		testEval(false, "(vector? '(1 2 3))");
	}
	
	@Test public void testJavaClass() throws Exception {
		testEval(String.class, "(|getClass| \"java.lang.String\")");
	}
	
	@Test public void testLambda1() throws Exception {
		testEval(10, "((lambda (x) (+ x x)) 5)");
	}
	
	@Test public void testLambda2() throws Exception {
		testEval(-1, "((lambda (x y) (- x y)) 5 6)");
	}
	
	
	@Test public void testLambdaAll() throws Exception {
		testEval(scheme.read("(5 6)"), "((lambda x x) 5 6)");
	}	
	
	@Test
	public void testLet() throws Exception {
		testEval(3, "(let ((x (+ 1 2))) x)");
		testEval(13, "(let ((x (+ 1 2)) " +
				"           (y 10))" +
				"       (+ x y))");
	}	
	
	@Test
	public void testLetrec() throws Exception {
		testEval(13, "(letrec ((x 13)) x)");
		testEval(true, 
				"(letrec ((even?" +
				"		   (lambda (n) (if (zero? n) #t (odd? (- n 1)))))" +
				"		  (odd?" +
				"		   (lambda (n) (if (zero? n) #f (even? (- n 1))))))" +
				"	(even? 88))");
	}	
	
	@Test
	public void testCallCC() throws Exception {
		testEval(-3, 
				"(call-with-current-continuation" +
				   "(lambda (exit)" +
					  "(for-each (lambda (x)" +
					     "(if (negative? x)" +
						     "(exit x)))" +
						     "'(54 0 37 -3 245 19))" +
				     "#t))");
	}	
	
	@Test
	public void testMakeString() throws Exception {
		testEval(true,  "(string? (make-string 4 #\\a))");
		testEval((Object)SchemeValue.makeString("aaaa"), "(make-string 4 #\\a)");
	}
	

	@Test public void testMatchCase() throws Exception {
		testEval(
				"(match-case '(1 2 3)" +
				"   ((?x)    1)" +
				"   ((?x ?y) 2) " +
				"   ((?x ?y ?z) (list ?z ?y ?x)))",
				"'(3 2 1)");
	}
	@Test public void testMatchCase2() throws Exception {
		testEval("'(a b)",
			"(match-case '(a b)" +
			"  ((?nam ?ini ?upd) (% ?nam ?ini))" +
			"  ((?nam ?ini)      (% ?nam ?ini)))");
	}
	@Test
	public void testNamedLetScope() throws Exception {
		testEval("-1",  "(let ((f -)) (let f ((n (f 1))) n))");
	}
	
	@Test
	public void testNewline() throws Exception {
		testEval('\n', "#\\newline");
	}

	@Test public void testNumber() throws Exception {
		testEval(13, "13");
	}
	
	private void testOutput(String expected, String exp) throws Exception {
		PrintStream oldOut = System.out;
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			PrintStream out = new PrintStream(bytes);
			System.setOut(out);
			scheme.parseAndRun(exp);
			out.close();
			Assert.assertEquals(expected, bytes.toString());
		}
		finally {
			System.setOut(oldOut);
		}
	}
	
	@Test
	public void testQuasiquote() throws Exception {
		testEval("'((foo 7) . cons)", 
				 "`((foo ,(- 10 3)) ,@(cdr '(c)) . ,(car '(cons)))");
	}

	@Test public void testQuote() throws Exception {
		testEval(makeSymbol("xyz"), "'xyz");
		testEval(cons(makeNumber(1), makeSymbol("x")),
				"'(1 . x)");
		testEval(cons(makeNumber(1),
				 cons(makeSymbol("x"),
				 cons(makeSymbol("a"),
				 makeNull()))),
				"'(1 x a)");
	}
	
	@Test
	public void testQuote2() throws Exception {
		testEval(7, "'7");
		testEval("(list 1 2)", "'(1 2)");
		testEval("(list '+ 1 2)", "'(+ 1 2)");
		testEval(true, "'#t");
		testEval(false, "'#f");
	}
	@Test public void testR4RS() throws Exception {
		scheme.load("r4rstest.scm");
		scheme.parseAndRun("(test-cont)");
		scheme.parseAndRun("(test-sc4)");
	}

	@Test public void testSelf() throws Exception {
		scheme.load("self-test.scm");
	}
	
	@Test public void testSendNonStatic() throws Exception {
		testEval((Object)"abc", "((method (|getClass| \"java.lang.Object\") '|toString|) 'abc)");
		testEval((Object)"abcdef", 
				"(let* ((String (|getClass| \"java.lang.String\"))" +
				"       (concat (method String 'concat String)))" +
				"  (concat \"abc\" \"def\"))");
	}
	
	@Test public void testSendStatic() throws Exception {
		testEval(123, "((method (|getClass| \"java.lang.Integer\") " +
				"               '|parseInt| " +
				"               (|getClass| \"java.lang.String\")) " +
				"       \"123\")");
	}

	@Test public void testSum() throws Exception {
		testEval(9, "(+ 4 5)");
		testEval(0, "(+)");
		testEval(6, "(+ 1 2 3)");
	}
	@Test
	public void testVectorEqual() throws Exception {
		testEval(true,  "(equal? #(1 #(2) 3) #(1 #(2) 3))");
		testEval(true,  "(equal? #(1 2 3) #(1 2 3))");
		testEval(true,  "(equal? #() #())");
	}
	
	@Test
	public void testVectorLength() throws Exception {
		testEval(3,  "(vector-length #(1 2 3))");
		testEval(0,  "(vector-length #())");
	}
	
}
