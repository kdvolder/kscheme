;; bootstrap.scm
;; 
;; This file is loaded by KScheme during initialization.

#case-sensitive

; Initially only one identifier is define:
;   :require string
; It loads a precompiled or manually code environment Frame into the
; KScheme environment.

(:require "ca.kscheme.primitives.JavaClasses")
(:require "ca.kscheme.primitives.Procedures")
(:require "ca.kscheme.primitives.InterpreterProcedures")

; The idea of the minimal interpreter is that it does not provide many primitives.
; Instead it provides a few hooks to interoperate with Java.
;
;  (getClass <full-qualified-classname-string>)
;        Returns the result of call Java's Class.forName(String)
;
;  (method <class> '<method-name> <class>*)
;        Returns a procedure that applies a Java method to a receiver
;        and a number of arguments.
;
;  (constructor <class> <class>*)
;        Returns a procedure that applies a Java Constructor to a number 
;        of arguments, creating an instance of a given class.
;        First argument: the class to be created.
;        Remaining arguments: the argument types for the constructor.

;A few shortcuts to important Java library classes
(define Array         (getClass "java.lang.reflect.Array"))
(define Character     (getClass "java.lang.Character"))
(define Boolean       (getClass "java.lang.Boolean"))
(define Class         (getClass "java.lang.Class"))
(define Comparable    (getClass "java.lang.Comparable"))
(define Field         (getClass "java.lang.reflect.Field"))
(define FileOutputStream (getClass "java.io.FileOutputStream"))
(define FileReader    (getClass "java.io.FileReader"))
(define HashSet       (getClass "java.util.HashSet"))
(define Integer       (getClass "java.lang.Integer"))
(define InputStream   (getClass "java.io.InputStream"))
(define InputStreamReader  (getClass "java.io.InputStreamReader"))
(define Modifier      (getClass "java.lang.reflect.Modifier"))
(define Number        (getClass "java.lang.Number"))
(define Object        (getClass "java.lang.Object"))
(define OutputStream  (getClass "java.io.OutputStream"))
(define Reader        (getClass "java.io.Reader"))
(define Set           (getClass "java.util.Set"))
(define String        (getClass "java.lang.String"))
(define StringBuilder (getClass "java.lang.StringBuilder"))
(define System        (getClass "java.lang.System"))

(define File         (getClass "java.io.File"))
(define URL          (getClass "java.net.URL"))
(define URI          (getClass "java.net.URI"))
(define URLConnection (getClass "java.net.URLConnection"))

(define Object[]   (getClass "[Ljava.lang.Object;"))
(define char[]     (getClass "[C"))

(define int intClass)
(define boolean booleanClass)
(define char charClass)

;A few shortcuts to important KScheme classes
(define SchemeInterpreter (getClass "ca.kscheme.interp.CoreInterpreter"))
(define SInputPort        (getClass "ca.kscheme.data.SInputPort"))
(define SMacro            (getClass "ca.kscheme.data.SMacro"))
(define SPair             (getClass "ca.kscheme.data.IPair"))
(define SProcedure        (getClass "ca.kscheme.data.SProcedure"))
(define SSymbol           (getClass "ca.kscheme.data.SSymbol"))
(define SchemeValue       (getClass "ca.kscheme.data.SchemeValue"))
(define SyntaxObj         (getClass "ca.kscheme.reader.SyntaxObj"))

; - - helpers to work with Java - - - - - - - - - - - - - - - - - - - - - - - - 

;Instance of testing
(define instance-of? (method Class 'isInstance Object))
(define (instance-of class)
  (lambda (obj) (instance-of? class obj)))

;Accessing an Object's fields
(define .getField (method Class 'getDeclaredField String))
(define Field.get (method Field 'get Object))
(define Field.getModifiers (method Field 'getModifiers))
(define Modifier.isStatic (method Modifier 'isStatic int))
(define (Field.isStatic field)
  (Modifier.isStatic (Field.getModifiers field)))
(define (get-field class field-name)
  (define field (.getField class (symbol->string field-name)))
  ((method Field 'setAccessible boolean) field #t)
  (if (Field.isStatic field)
      (lambda ()
        (Field.get field '()))
      (lambda (obj)
        (Field.get field obj))))
(define Object.getClass (method Object 'getClass))

; - - procedures for manipulating Scheme data - - - - - - - - - -

; Equality tests

;Shortcut to Java's equals method. Care should be taken not to call this on
;a null pointer.
(define .equals (method Object 'equals Object))

(define (eqv? x y)
  ;Eqv is equivalent to Java's equals method, except, the equals method can 
  ;not be invoked on null pointers.
  (if (null? x) 
      (null? y)
      (.equals x y)))
  
(define eq? (method SchemeValue 'isEqv Object Object))
   ; Not using == comparison because Java reflection may cause multiple Boolean
   ; instances that represent true or false and these must be treated as being
   ; eq? to meet the R4RS standard.

; equal?

(define (equal? x y)
  (cond ((pair? x)
         (and (pair? y)
              (equal? (car x) (car y))
              (equal? (cdr x) (cdr y))))
        ((vector? x)
         (and (vector? y)
              (vector-equal? x y)))
        (else (eqv? x y))))


; Procedures
(define procedure? (instance-of SProcedure))

; Symbols
(define symbol?  (instance-of SSymbol))
(define gensym   (method SSymbol 'gensym))
(define symbol->string (method SSymbol 'toString))
(define string->symbol (method SSymbol 'intern String))

; Null 
(define (null? x) (eq? x ()))

; Booleans
(define boolean? (instance-of Boolean))
(define (not x) (if x #f #t))

; Pairs
(define pair?    (instance-of SPair))
(define cons     (method SchemeValue 'cons Object Object))
(define car      (method SPair 'car))
(define cdr      (method SPair 'cdr))
(define set-car! (method SPair 'setCar Object))
(define set-cdr! (method SPair 'setCdr Object))

(define (caar x) (car (car x)))  
(define (cadr x) (car (cdr x)))  
(define (cdar x) (cdr (car x)))  
(define (cddr x) (cdr (cdr x)))  
(define (cadar x) (car (cdar x)))
(define (caddr x) (car (cddr x)))  

(define (map1 f l)
  (if (null? l)
      l
      (cons (f (car l))
            (map1 f (cdr l)))))
  
(define (map f . lists)
  (if (null? (car lists))
      '()
      (cons (apply f (map1 car lists))
            (apply map (cons f (map1 cdr lists))))))

(define (list . args) args)

; Fixnum integers
(define =int (method SchemeValue 'sameInt    int int))
(define >int (method SchemeValue 'greaterInt int int))
(define (<int a b) (>int b a))
(define (<=int a b) (not (>int a b)))
(define (>=int a b) (<=int b a))

(define +int (method SchemeValue 'add int int))
(define -int (method SchemeValue 'sub int int))
(define *int (method SchemeValue 'mul int int))

(define SchemeValue.parseInt
  (method SchemeValue 'parseInt String int))

(define (string->number s . radix)
  (if (null? radix)
      (set! radix 10)
      (if (null? (cdr radix))
          (set! radix (car radix))
          (error 'string->number "Too many arguments: " (cons s radix))))
  (SchemeValue.parseInt s radix))

(define Integer.string (method Integer 'toString))
(define Integer.string2 (method Integer 'toString int int))
(define (number->string num . radix)
  (match-case radix
     (() (Integer.string num))
     ((?radix) (Integer.string2 num ?radix))))

(define (zero? x) (=int x 0))
(define (make-comparison bin-op)
  (define (comp a1 a2 . rest)
    (and (bin-op a1 a2)
         (or (null? rest)
             (apply comp (cons a2 rest)))))
  comp)

(define =  (make-comparison =int))
(define >  (make-comparison >int))
(define <  (make-comparison <int))
(define <= (make-comparison <=int))
(define >= (make-comparison >=int))

(define (negative? x) (< x 0))
(define (positive? x) (> x 0))

(define (foldl z f)
  (lambda args
    (let loop ((acc z)
               (args args))
      (if (null? args)
          acc
          (loop (f acc (car args))
                (cdr args))))))

(define * (foldl 1 *int))
(define + (foldl 0 +int))

(define (- a . rest)
  (define (loop acc args)
    (if (null? args)
        acc
        (loop (-int acc (car args)) (cdr args)))) 
  (if (null? rest)
      (-int 0 a)
      (loop a rest)))
(define (/ a b)
  (if (= (remainder a b) 0)
      (quotient a b)
      (error "Unsupported division: " `(/ ,a ,b))))
  
(define quotient (method SchemeValue 'quotient int int)) 
  
(define (modulo x y)
  (let ((r (remainder x y)))
    (if (or (zero? r) (= (sign y) (sign r)))
        r
        (+ r y))))
  
(define (gcd2 x y)
  (cond ((zero? y)
         (abs x))
        (else 
         (gcd2 y (remainder x y)))))
  
(define (gcd . args)
  (cond ((null? args)
         0)
        ((null? (cdr args))
         (car args))
        (else
         (apply gcd (cons (gcd2 (car args) (cadr args)) (cddr args))))))
  
(define (lcm2 x y)
  (abs (* (quotient x (gcd2 x y)) y)))
  
(define (lcm . args)
  (cond ((null? args)
         1)
        (else
         (apply (boot:fold1 lcm2) args))))
        
(define (odd? x) (not (zero? (remainder x 2))))
(define (even? x) (zero? (remainder x 2)))

(define (boot:fold1 bin-op)
  (define (op a1 . rest)
    (if (null? rest)
        a1
        (apply op (cons (bin-op a1 (car rest)) (cdr rest)))))
  op)
  
(define max (boot:fold1 (lambda (a b) (if (> a b) a b))))
(define min (boot:fold1 (lambda (a b) (if (> a b) b a))))

(define (abs x)
  (if (< x 0) (- x) x))

(define (sqr x) (* x x))

(define (sign x)
  (cond ((> x 0) 1)
        ((< x 0) -1)
        (else
         0)))

(define (expt base power)
   (cond ((= 1 base) base)
         ((= 0 power)
          1)
         ((< 0 power)
          (if (even? power)
              (sqr (expt base (quotient power 2)))
              (* base (expt base (- power 1)))))
         ((= -1 base)
          (expt base (- power)))
         (else
          (error `(expt ,base ,power) "Not implemented, we only have integers"))))

; Skeleton of the "numeric tower" when only fixnums exist
(define integer? (instance-of Integer))
(define rational? integer?)
(define real? integer?)
(define complex? integer?)
(define number? integer?)

(define (exact? x) 
   (or (integer? x) (rational? x)))
(define (inexact? x)
   (not (exact? x)))

; Chars
(define char? (instance-of Character))
(define char-upcase (method Character 'toUpperCase char))
(define char-downcase (method Character 'toLowerCase char))
  
(define .compareTo (method Comparable 'compareTo Object))
(define (Comparable-comparer comp)
  (lambda (x y)
    (comp (.compareTo x y) 0)))

(define (make-char-ci-comparer comp)
  (lambda (x y)
    (comp (.compareTo (char-upcase x) (char-upcase y)) 0)))
  
(define char=?  (Comparable-comparer =int))
(define char<?  (Comparable-comparer <int))
(define char>?  (Comparable-comparer >int))
(define char<=? (Comparable-comparer <=int))
(define char>=? (Comparable-comparer >=int))

(define char-ci=?  (make-char-ci-comparer =int))
(define char-ci<?  (make-char-ci-comparer <int))
(define char-ci>?  (make-char-ci-comparer >int))
(define char-ci<=? (make-char-ci-comparer <=int))
(define char-ci>=? (make-char-ci-comparer >=int))

(define (and/f . preds)
  (if (null? preds) 
      ; no preds
      (lambda () #t)
      (if (null? (cdr preds))
          ; one pred
          (car preds)
          (begin 
            (define first (car preds))
            (define  rest (apply and/f (cdr preds)))
            (lambda args
              (and (apply first args)
                   (apply rest args)))))))
  
(define char-alphabetic? (and/f char? (method Character 'isLetter char)))
(define char-numeric?    (and/f char? (method Character 'isDigit char)))
(define char-whitespace? (and/f char? (method Character 'isWhitespace char)))
(define char-upper-case? (and/f char?  (method Character 'isUpperCase char)))
(define char-lower-case? (and/f char?  (method Character 'isLowerCase char)))

(define integer->char (method SchemeValue 'makeChar int))   
(define char->integer (method SchemeValue 'toInt char))

; - - the most used R4RS syntax defined by means of macros - - - - - - - - - - - - - - - -

(define macro (method SchemeValue 'makeMacro SProcedure))

(define let
  (begin 
    (define (#expand-named-let name bindings body)
      (list let bindings
            (list letrec (list (list name (cons lambda (cons (map car bindings) body))))
                  (cons name (map car bindings)))))
    
    (macro
     (lambda (let bindings . body)
       (if (symbol? bindings)
           (#expand-named-let bindings (car body) (cdr body))
           (cons (cons 'lambda (cons (map car bindings) body))
                 (map cadr bindings)))))))

(define letrec
  (macro 
   (lambda (xxx bindings . body)
     (cons let
           (cons (map (lambda (b)
                        (list (car b) #f))
                      bindings)
                 (append (map (lambda (b)
                                (list set! (car b) (cadr b)))
                              bindings)
                         body))))))

(define let*
  (macro 
   (lambda (let* bindings . body)
     (cond ((null? bindings)
            (append (list let (list)) body))
           ((null? (cdr bindings))
            (cons let (cons bindings body)))
           (else
            (list let (list (list (caar bindings) (cadar bindings)))
                  (cons let* 
                        (cons (cdr bindings)
                              body))))))))

(define cond
  (macro ;(traced 'cond
   (lambda (cond . clauses)
     (if (null? clauses)
         #f
         (let ((first (car clauses))
               (rest  (cons cond (cdr clauses)))
               (test-var (gensym 'test)))
           (if (null? (cdr first)) 
               ;(cond (test) ...)
               (list let (list (list test-var (car first)))
                     (list if test-var test-var rest))
               (if (eqv? (cadr first) '=>)
                   ;(cond (test => ...)
                   (list let (list (list test-var (car first)))
                         (list if test-var (list (caddr first) test-var)
                               rest))
                   ;(cond (test exp1 exp2 ...)
                   (list if (car first)
                         (cons begin (cdr first))
                         rest))))))));)

(define else #t)

(define and 
  (macro 
   (lambda (and . args)
     (cond ((null? args)
            #t)
           ((null? (cdr args))
            (car args))
           (else
            (let ((tmp (gensym 'tmp)))
              (list let (list (list tmp (car args)))
                    (list if tmp (cons and (cdr args)) 
                          tmp))))))))

(define or 
  (macro 
   (lambda (or . args)
     (cond ((null? args)
            #f)
           ((null? (cdr args))
            (car args))
           (else
            (let ((tmp (gensym 'tmp)))
              (list let (list (list tmp (car args)))
                    (list if tmp tmp 
                          (cons or (cdr args))))))))))

; Less used R4RS Macros

(define case
  (macro 
   (lambda (case val . clauses)
     (list (#case-clauses clauses) val))))

(define (#case-clauses clauses)
  (define val (gensym 'case-val))
  (cond ((null? clauses)
         (list lambda (list val)
               #f))
        (else
         (let ((clause (car clauses)))
           (cons lambda 
                 (cons (list val)
                       (if (eqv? (car clause) 'else)
                           (cdr clause)
                           (list (list if (list 'memv val (list quote (car clause))) 
                                       (cons begin (cdr clause))
                                       (list (#case-clauses (cdr clauses)) val))))))))))

(define % 
  (macro
   (lambda (% . template)
     (cond ((null? template)
            ())
           ((#unquote? template)
            (cadr template))
           ((pair? template)
            (list cons (cons % (car template)) (cons % (cdr template))))
           (else
            template)))))

(define (#unquote? x)
  (and (pair? x) (eqv? (car x) 'unquote)))         

(define do
  (macro 
   (lambda (xxx clauses finish . body)
     (define initialize
       (lambda (clause)
       (match-case clause
          ((?nam ?ini ?upd) (% ?nam ?ini))
          ((?nam ?ini)      (% ?nam ?ini)))))
     (define (update clause)
       (match-case clause
          ((?nam ?ini ?upd) ?upd)
          ((?nam ?ini)      ?nam)))
     (let ((loop (gensym 'do-loop)))
       (% let loop ,(map initialize clauses)
          ,(match-case finish 
              ((?test ?ret) (% if ?test ?ret 
                               (begin (begin . body) 
                                 (loop . ,(map update clauses)))))
              ((?test)      (let ((tmp-test (gensym 'test)))
                              (% let ((tmp-test ?test))
                                 (if tmp-test tmp-test
                                     (begin (begin . body) 
                                       (loop . ,(map update clauses)))))))))))))

(define if-match
  (macro 
   (let ((var? (lambda (x) (and (symbol? x) 
                                (eqv? #\?
                                      (string-ref (symbol->string x) 0))))))
     (lambda (if-match pattern obj thn els)
       (cond ((var? pattern)
              (% let ((pattern obj)) thn))
             ((pair? pattern)
              (let ((tmp-car (gensym 'car))
                    (tmp-cdr (gensym 'cdr))
                    (tmp-els (gensym 'els))
                    (tmp-obj (gensym 'obj)))
                (% let ((tmp-obj obj)) 
                   (if (pair? tmp-obj)
                       (let ((tmp-els (lambda () els))
                             (tmp-car (car tmp-obj))
                             (tmp-cdr (cdr tmp-obj)))
                         (if-match ,(car pattern) tmp-car
                                   (if-match ,(cdr pattern) tmp-cdr
                                             thn
                                             (tmp-els))
                                   (tmp-els)))
                       els))))
             (else
              (% if (equal? 'pattern obj) thn els)))))))

(define match-case
  (macro
   (lambda (xxx obj . cases)
               (if-match ((?pat ?thn . ?thns) . ?cases) cases
                         (let ((tmp-obj (gensym 'obj)))
                           (% let ((tmp-obj obj))
                              (if-match ?pat tmp-obj (begin ?thn . ?thns)
                                        (match-case tmp-obj . ?cases))))
                         (% error "Syntax error")))))

(define quasiquote
  (macro
   (let ((quasiquote       'quasiquote)
         (unquote-splicing 'unquote-splicing)
         (unquote          'unquote)
         (quote            'quote))
     (lambda (qq pattern)
       (let expand ((pattern pattern)
                    (level 0))
         (match-case pattern
           (() 
            ())
           ((unquote ?stuff)
            (if (= level 0)
                ?stuff
                (% cons ,(expand (car pattern) level)
                   ,(expand (cdr pattern) (- level 1)))))
           (((unquote-splicing ?stuff) . ?rest)
            (if (= level 0)
                (% append ?stuff ,(expand ?rest level))
                (% cons ,(expand (car pattern) (- level 1))
                   ,(expand (cdr pattern) level))))
           ((quasiquote ?stuff)
            (% cons ,(expand (car pattern) level) ,(expand (cdr pattern) (+ level 1))))
           ((?a . ?b)
            (% cons ,(expand ?a level) ,(expand ?b level)))
           (?else
            (cond ((vector? pattern)
                   (let ((pattern (vector->list pattern)))
                     (% list->vector ,(expand pattern level))))
                  (else
                   (% quote pattern))))))))))

; - - Strings - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

(define string?    (instance-of String))
(define string-ref (method String 'charAt int))
(define (string=? s1 s2)
  (.equals s1 s2))
(define .newString (constructor String char[]))       
(define (make-string len . c)
  (set! c (if (null? c) #\space (car c)))
  (let ((contents (.newArray char len)))
    (vector-fill! contents c)
    (.newString contents)))
(define (string . chars)
  (let* ((len      (length chars))
         (contents (.newArray char len)))
    (let loop ((i 0)
               (chars chars))
      (cond ((< i len)
             (vector-set! contents i (car chars))
             (loop (+ i 1) (cdr chars)))
            (else
             (.newString contents))))))
(define string-upcase (method String 'toUpperCase))
  
(define string-length (method String 'length))
(define String.concat (method String 'concat String))
(define String.toCharArray (method String 'toCharArray))

(define (string-append . strings)
   (cond ((null? strings) 
          "")
         ((null? (cdr strings)) 
          (car strings))
         (else
          (String.concat (car strings) (apply string-append (cdr strings))))))

(define (substring s start end)
  (if (>= start end)
      ""
      (let ((sub (.newArray char (- end start))))
        (let loop ((start start)
                   (i 0))
          (if (< start end)
              (begin 
                (vector-set! sub i (string-ref s start)) 
                (loop (+ start 1)
                      (+ i 1)))
              (.newString sub))))))

(define string-ends-with?
  (method String 'endsWith String))

; TODO? R4RS Strings are mutable. 
; kscheme Strings are not mutable. This is because since we represent 
; them as just plain java strings. These are not mutable.
; The implementation below used to work, but... doesn't anymore.
; (define string-set!
;   (lambda (str n c)
;     (define String.value (get-field String 'value))
;     (let ((chars (String.value str)))
;       (vector-set! chars n c))))

(define (string-map char-fun s)
  (let ((buf (make-string-builder))
        (len (string-length s)))
    (let loop ((i 0))
      (cond ((< i len)
             (string-builder-append! buf (char-fun (string-ref s i)))
             (loop (+ i 1)))
            (else (->string buf))))))

(define string<? (Comparable-comparer <int))
(define string>? (Comparable-comparer >int))
(define string>=? (Comparable-comparer >=int))
(define string<=? (Comparable-comparer <=int))
  
(define (make-string-ci-comparer comp)
  (lambda (s1 s2)
    (comp (string-upcase s1) (string-upcase s2))))

(define (string->list str)
  (vector->list (String.toCharArray str)))
  
(define string-ci=? (make-string-ci-comparer string=?))
(define string-ci<? (make-string-ci-comparer string<?))
(define string-ci>? (make-string-ci-comparer string>?))
(define string-ci>=? (make-string-ci-comparer string>=?))
(define string-ci<=? (make-string-ci-comparer string<=?))

; - - lists - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

(define (make-assoc eq?)
  (begin 
    (define (assoc obj alist)
      (cond ((null? alist)
             #f)
            ((eq? (caar alist) obj)
             (car alist))
            (else
             (assoc obj (cdr alist)))))
    assoc))

(define assq (make-assoc eq?))
(define assv (make-assoc eqv?))
(define assoc (make-assoc equal?))

(define (make-member eq?)
  (begin
    (define (member val lis)
      (cond ((null? lis)
             #f)
            ((eq? (car lis) val) lis)
            (else
             (member val (cdr lis)))))
    member))

(define memq   (make-member eq?))
(define memv   (make-member eqv?))
(define member (make-member equal?))

; lists 

(define (append . lists)
  (cond ((null? lists)
	 '())
	((null? (cdr lists))
	 (car lists))
	((null? (car lists))
	 (apply append (cdr lists)))
	(else
	 (cons (caar lists)
	       (apply append (cons (cdar lists) (cdr lists)))))))

(define (reverse lis)
  (let loop ((rev '())
             (lis lis))
    (if (null? lis) rev
        (loop (cons (car lis) rev)
              (cdr lis)))))

(define (list-ref lis i)
  (if (zero? i) 
      (car lis)
      (list-ref (cdr lis) (- i 1))))

(define new.HashSet      (constructor HashSet))
(define Set.contains?    (method Set 'contains Object))
(define Set.add!         (method Set 'add Object))

(define (list? x)
  (define *seen* (new.HashSet)) ;; for circular "lists"
  (let loop ((x x))
    (and (not (Set.contains? *seen* x))
         (or (null? x)
             (and (pair? x)
                  (begin (Set.add! *seen* x)
                         (loop (cdr x))))))))

(define (length l)
  (let loop ((ct 0)
	     (l l))
    (if (null? l) ct
	(loop (+ ct 1) (cdr l)))))

(define (for-each proc . lis)
  (if (pair? (car lis))
      (begin (apply proc (map car lis))
             (apply for-each (cons proc (map cdr lis))))))

; vectors

(define vector? (instance-of Object[]))
(define vector-length (method Array 'getLength Object))
(define vector-ref    (method Array 'get Object int))
(define vector-set!   (method Array 'set Object int Object))
(define .newArray (method Array 'newInstance Class int))
(define (vector-fill! vector el)
  (let loop ((i 0))
     (if (< i (vector-length vector))
         (begin (vector-set! vector i el)
                (loop (+ i 1)))))
  vector)
(define (make-vector len . el)
   (let ((el (if (null? el) el (car el)))
         (vec (.newArray Object len)))
     (vector-fill! vec el)
     vec))

(define (vector-equal? v1 v2)
  (and (= (vector-length v1) (vector-length v2))
       (let loop ((i 0))
         (or (>= i (vector-length v1))
             (and (equal? (vector-ref v1 i) (vector-ref v2 i))
                  (loop (+ i 1)))))))
  
(define (vector->list v)
  (let loop ((i (- (vector-length v) 1))
             (l '()))
    (if (>= i 0)
        (loop (- i 1)
              (cons (vector-ref v i) l))
        l)))
  
(define (vector . args)
  (list->vector args))
  
(define (list->vector args)
  (let ((v (make-vector (length args))))
    (let loop ((i 0)
               (args args))
      (if (null? args)
          v
          (begin
            (vector-set! v i (car args))
            (loop (+ i 1)
                  (cdr args)))))))

; - - some helper code for SLib configuration, including the "vicinity" stuff ---

(define URL? (instance-of URL))
(define URI->File (constructor File URI))
(define URL->URI (method URL 'toURI))
(define (URL->File f) (URI->File (URL->URI f)))

;Like slib's "in-vicinity", but returns the result as a URL object.
(define in-vicinity-URL (constructor URL URL String))
  
(define string->file-URL 
  (let ((make-url (constructor URL String String String)))
    (lambda (file-name)
      (make-url "file" #null file-name))))
  
(define System.getProperty (method System 'getProperty String))

(define make-vicinity (constructor URL String)) 
  
(define (user-vicinity)
  (string->file-URL (string-append (System.getProperty "user.dir") "/")))
  
(define (home-vicinity)
  (string->file-URL (string-append (System.getProperty "user.home") "/")))
  
(define (library-vicinity)
  (sub-vicinity (implementation-vicinity) "slib"))
  
(define (sub-vicinity vic name)
  (in-vicinity-URL vic (string-append name "/")))
  
(define program-vicinity
  (let ((SyntaxObj.getSourceFile (method SyntaxObj 'getSourceURL)))
    (macro (lambda (xxx)
             (in-vicinity-URL (SyntaxObj.getSourceFile xxx) ".")))))

(define URL->string (method URL 'toString))

(define (in-vicinity vic file-name)
  (URL->string (in-vicinity-URL vic file-name)))

(define (vicinity:suffix? c)
  (eqv? c #\/))
  
(define getResource (method Class 'getResource String))
(define (implementation-vicinity)
  (getResource SchemeInterpreter "/"))
  
(define pathname->vicinity (constructor URL String))

(define File.exists? (method File 'exists))
(define string->File (constructor File String))
  
(define (file-exists? x)
  (cond ((file-URL? x)
         (File.exists? (URL->File x)))
        ((URL? x)
         (let* ((conn (URL.openConnection x))
                (result (< 0 (URLConnection.getContentLength conn))))
           result))
        ((string? x)
         (file-exists? (in-vicinity-URL (user-vicinity) x)))
        ((File? x)
         (File.exists? x))
        (else
         (error "Don't know how to check existence for " x))))
  
(define File? (instance-of File))
(define File.delete (method File 'delete))  
(define (file-URL? f)
  (and (URL? f)
       (equal? (substring (->string f) 
                          0 5)
               "file:")))
  
(define (delete-file f)
  (cond ((File? f)
         (File.delete f))
        ((file-URL? f)
         (delete-file (URI->File (URL->URI f))))
        ((URL? f)
         #f)
        ((string? f)
         (delete-file (in-vicinity-URL (user-vicinity) f)))
        (else
         (error 'delete-file "Invalid argument" f))))

(define (list->string chars)
  (apply string chars)) 

; - - - IO - - - - - - - - - - - - - - - - - - - - - - - - - - - -

; input
(define *current-input-port* #f)

(define SInputPort.new (constructor SInputPort Reader URL))  
(define InputStreamReader.new (constructor InputStreamReader InputStream)) 
(define (InputStream->port is source-name)
  (SInputPort.new (InputStreamReader.new is) source-name))  

(define (current-input-port)
  (if (not *current-input-port*)
      (set! *current-input-port*
            (InputStream->port ((get-field System 'in))
                               #null)))
  *current-input-port*)

(define input-port? (instance-of SInputPort))

(define (call-with-input-file file-name proc)
  (let ((input (open-input-file file-name)))
    (let ((r (proc input)))
      (close-input-port input)
      r)))
  
(define peek-char (method SInputPort 'peekChar))
(define read-char (method SInputPort 'readChar))
  
(define URL.openStream (method URL 'openStream))
(define URL.openConnection (method URL 'openConnection))  
(define URLConnection.getContentLength (method URLConnection 'getContentLength))

(define (open-input-file file-name)
  (cond ((URL? file-name)
         (SInputPort.new (InputStreamReader.new (URL.openStream file-name))
                         file-name))
        ((string? file-name)
         (open-input-file (in-vicinity-URL (user-vicinity) file-name)))
        (else
         (error "Don't know how to open that: " file-name))))

(define close-input-port
  (method SInputPort 'close))
  
(define eof-object? (method SchemeValue 'isEofObject Object))
  
; output
(define current-output-port (get-field System 'out))
  
(define current-error-port (get-field System 'err))

(define output-port? (instance-of OutputStream))
  
(define close-output-port (method FileOutputStream 'close))
(define open-output-file/File  (constructor FileOutputStream File))
  
(define (open-output-file file)
  (cond ((string? file) 
         (open-output-file (in-vicinity-URL (user-vicinity) file)))
        ((URL? file)
         (open-output-file (URL->File file)))
        ((File? file)
         (open-output-file/File file))
        (else
         (error "Unknown/unsupported type of file reference: " file))))
  
(define (call-with-output-file file-name proc)
  (let ((out (open-output-file file-name)))
    (let ((r (proc out)))
      (close-output-port out)
      r)))
  
(define OutputStream.write-int (method OutputStream 'write int))
  
(define (write-char char . port)
  (if (null? port)
      (set! port (current-output-port))
      (set! port (car port)))
  (OutputStream.write-int port (char->integer char)))  
  
(define (display x . port)
  (if (null? port)
      (set! port (current-output-port))
      (set! port (car port)))
  (cond ((string? x)
         (let loop ((i 0))
           (if (< i (string-length x))
               (begin (write-char (string-ref x i) port)
                 (loop (+ 1 i))))))
        (else
         (display (->string x) port))))
  
(define (newline . args)
  (apply display (cons #\newline args)))

(define (write x . port)
  (if (null? port)
      (set! port (current-output-port))
      (set! port (car port)))
  (let ((display (lambda (x) (display x port)))
        (write-char (lambda (x) (write-char x port))))
    (let write ((x x))
      (cond ((number? x)
             (display (->string x)))
            ((boolean? x)
             (display (if x "#t" "#f")))
            ((vector? x)
             (display "#(")
             (let loop ((i 0))
               (if (< i (vector-length x))
                   (begin
                     (if (> i 0)
                         (display " "))
                     (write (vector-ref x i))
                     (loop (+ i 1)))))
             (display ")"))
            ((pair? x)
             (display "(")
             (write (car x))
             (let loop ((x (cdr x)))
               (cond ((pair? x)
                      (display " ")
                      (write (car x))
                      (loop (cdr x)))
                     ((null? x)
                      'done)
                     (else
                      (display " . ")
                      (write x))))
             (display ")"))
            ((string? x)
             (display #\")
             (let loop ((i 0))
               (if (< i (string-length x))
                   (let ((c (string-ref x i)))
                     (cond ((eqv? c #\")
                            (display #\\)
                            (display #\"))
                           (else
                            (write-char c)))
                     (loop (+ i 1)))))
             (display #\"))
            ((char? x)
             (display "#\\")
             (write-char x))
            (else
             (display x))))))

; - - kscheme specific procedures not part of slib or r4rs - - - - - 

(define (every pred? lis) 
  (cond ((null? lis)
         #t)
        ((null? (cdr lis))
         (pred? (car lis)))
        (else
         (and (pred? (car lis))
              (every pred? (cdr lis))))))

(define .toString  (method Object 'toString))
(define (->string x)
  (cond ((null? x) 
         "()")
        ((vector? x)
         (vector->string x))
        ((pair? x)
         (pair->string x))
        (else
         (.toString x))))

(define make-string-builder (constructor StringBuilder))
(define string-builder-append! (method StringBuilder 'append Object))

(define (pair->string x)
  (let ((str (make-string-builder)))
    
    (define (rest->string x)
      (cond ((null? x)
             'done)
            ((pair? x)
             (string-builder-append! str " ")
             (string-builder-append! str (->string (car x)))
             (rest->string (cdr x)))
            (else
             (string-builder-append! str " . ")
             (string-builder-append! str (->string x)))))
    
    (string-builder-append! str "(")
    (string-builder-append! str (->string (car x)))
    (rest->string (cdr x))
    (string-builder-append! str ")")
    
    (->string str)))

(define (vector->string vec)
  (let ((str (make-string-builder))
        (len (vector-length vec)))
    (string-builder-append! str "#(")
    (let loop ((i 0))
      (cond ((< i len)
             (if (> i 0)
                 (string-builder-append! str " "))
             (string-builder-append! str (->string (vector-ref vec i)))
             (loop (+ i 1)))))
    (string-builder-append! str ")")
    (->string str)))

(define (list-of? pred? l)
  (if (null? l)
      #t
      (if (pair? l)
          (and (pred? (car l))
               (list-of? pred? (cdr l)))
          #f)))

(define SMacro? (instance-of SMacro))

(define (defmacro? x)
  (let ((val (try (eval x)
                  (lambda e #f))))
    (SMacro? val)))

(define try
  (macro (lambda (xxx body handler)
           `(,call-with-handler (lambda () ,body) ,handler))))

(define (load f)
  (cond ((URL? f)
         (load/URL f))
        ((string? f)
         (load/URL (in-vicinity-URL (user-vicinity) f)))
        (else
         (error 'load "Unsupported type of argument" f))))

#eof    
  