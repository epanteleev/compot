# Compot
Compiler for C language. It's based on [opt](../opt/README.md) IR.
The compiler is written in Kotlin and uses own recursive descent parser.  
 
Compot implements a subset of C11 standard, so that it can compile some large projects, for example [Chibicc](https://github.com/rui314/chibicc),
[Zlib](https://zlib.net/),[libpng](https://www.libpng.org/pub/png/libpng.html), [json-c](https://github.com/json-c/json-c) and others.

## Features
There are some features that are already implemented:
- [x] Own preprocessor, lexical analyzer and recursive descent parser
- [x] Aggregates (structs and unions), floating-point types, enums
- [x] Pointers, arrays, function pointers
- [x] Control flow statements (if, while, do-while, for, switch, goto)
- [x] VarArgs
- [x] Initializer lists, compound literals and designated initializers
- [x] Position-independent code (PIC) generation (unstable)

Compot doesn't support:
- [ ] Inline assembly
- [ ] Debugging information
- [ ] f80 long double type, complex types
- [ ] _Generic keyword
- [ ] _Alignas, _Alignof, _Static_assert
- [ ] _Thread_local storage class specifier
- [ ] _Atomic type specifier
- And many other features...

## Implementation details
The compiler is written in Kotlin. There are no external libraries except for Kotlin and Java standard libraries, 
but you should have GCC toolchain installed to assemble and link the generated code.
The implementation is split into several phases: tokenization, preprocessing, parsing, semantic analysis, IR generation, optimization and code generation.
The compiler supports two modes of work: without optimization and with optimization `-O3`.

### Tokenization
The tokenization phase reads the input source code and converts it into a stream of tokens. The stream of tokens is represented as a list of tokens.
Each tokens store the information about its location in the source code, string representation and type of the token.

### Preprocessing
The preprocessing phase processes the source code and expands macros. 
It always starts with *.c file and recursively processes all included header files. 
As a result, it produces a single stream of tokens that represents the preprocessed source code.

### Parsing
The parsing phase reads the stream of tokens and converts it into an syntax tree. 
The compiler uses a handwritten recursive descent parser that is based on the grammar of the C11 language standard, 
thus not all features of the C11 standard are supported.
During parsing, the compiler also partially performs semantic analysis, such as type checking and scope resolution.
There are no separate stages for semantic analysis, it is done during parsing and IR generation lazily.

### IR Generation
The IR generation phase converts the AST into the intermediate representation (IR) that is used by own compiler backend.
More information about the IR can be found in the [opt](../opt/README.md) documentation.

## Testing
The compiler is tested using a set of tests inside the project, but some large C projects are also used to check the correctness of the code generation.
See [compot-driver](../compot-driver/README.md).

## Performance
Although the compiler has some optimizations, currently the project is focused on the correctness of the code generation.
The optimizations might make the codebase of the compiler more complex, so that it may increase the time of development and maintenance.
It is planned to implement some optimizations in the future, after adding more real-world applications to the test suite.
Currently, I estimate the performance of the generated code to be around x3-x5 times slower than GCC or Clang.

## References
- [C11 standard](https://port70.net/~nsz/c/c11/n1570.html)