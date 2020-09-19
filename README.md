KScheme
=======

KScheme is a simple interpreter for a simple dialect of Scheme written in Java.

KScheme is mostly a project done for fun and such it is minimalistic in nature.

- It supports only R4RS (with the exception that kscheme strings are immutable).
- It does support call/cc in its full generality.
- Interpreter supports proper tail-call optimisation (it uses Trampolines internally to achieve
  this.
- R4RS does not have macros but KScheme has a simple (non-hiegenic) macro system.
  Much of kscheme syntax is implemented using this.


