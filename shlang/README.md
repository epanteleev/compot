# Shlang
...isn't Clang, but it's a compiler for C language. It's based on [opt](../opt/README.md) IR.
The compiler is written in Kotlin and uses recursive descent parser.  

It's still in development, so it's not ready for production use, but you can try it out.  
Shlang implements a subset of C11 standard, so that it can compile some large projects, for example [Chibicc](https://github.com/rui314/chibicc),
[Zlib](https://zlib.net/),[libpng](https://www.libpng.org/pub/png/libpng.html), [json-c](https://github.com/json-c/json-c) and others.

## Features
There are some features that are already implemented:
- [x] Own preprocessor, lexical analyzer and recursive descent parser
- [x] Aggregates (structs and unions), floating-point types, enums
- [x] Pointers, arrays, function pointers
- [x] Control flow statements (if, while, do-while, for, switch, goto)
- [x] VarArgs
- [x] Initializer lists, compound literals and designated initializers

Shlang doesn't support:
- [ ] f80 long double type, complex types
- [ ] _Generic keyword
- [ ] _Alignas, _Alignof, _Static_assert
- [ ] _Thread_local storage class specifier
- [ ] _Atomic type specifier
- And many other features...

## References
- [C11 standard](https://port70.net/~nsz/c/c11/n1570.html)