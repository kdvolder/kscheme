(define let/cc 
  (macro (lambda (xxx k-name . body)
            `(call-with-current-continuation (lambda (,k-name) ,@body)))))
