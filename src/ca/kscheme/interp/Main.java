package ca.kscheme.interp;

import ca.kscheme.KScheme;



public class Main {
	
	public static void main(String[] args) throws Exception {
		KScheme kscheme = CoreInterpreter.getDefault();
		for (int i = 0; i < args.length; i++) {
			System.out.println("will load : "+args[i]);
			kscheme.load(args[i]);
		}
		kscheme.readEvalPrintLoop();
	}
	
	/*
(define (str-copy s)
  (let ((v (make-string (string-length s))))
    (do ((i (- (string-length v) 1) (- i 1)))
	((< i 0) v)
	  (display i)
	  (newline)
      (string-set! v i (string-ref s i)))))
	 */

}
