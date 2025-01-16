# Shlang
...isn't Clang, but it's a compiler for C language. It's based on [opt](../opt/README.md) IR.
The compiler is written in Kotlin and uses recursive descent parser.  

It's still in development, so it's not ready for production use, but you can try it out.  
Shlang implements a subset of C11 standard, so that it can compile some large projects, for example [Chibicc](https://github.com/rui314/chibicc).

## Features
There are some features that are already implemented:
- [x] Preprocessor
- [x] Aggregates (structs and unions), floating-point types, enums
- [x] Pointers, arrays, function pointers
- [x] Control flow statements (if, while, do-while, for, switch)
- [x] VarArgs

Shlang doesn't support:
- [ ] f80 long double type, complex types
- [ ] _Generic keyword
- [ ] Atomic operations, thread-local storage
- And many other features...

## References
- [C11 standard](https://port70.net/~nsz/c/c11/n1570.html)
- [Chibicc](https://github.com/rui314/chibicc)