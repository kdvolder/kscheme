;;
;; Implementation of output String ports.
;; 

#case-sensitive

(define ByteArrayOutputStream (getClass "java.io.ByteArrayOutputStream"))
(define open-output-string (constructor ByteArrayOutputStream))

(define (call-with-output-string proc)
  (let ((port (open-output-string)))
    (proc port)
    (->string port)))
