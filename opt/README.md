# Opt
Yet another compiler backend

## Features
- [x] LLVM like IR
- [x] Optimizations: Mem2Reg, DCE, Constant folding
- [x] Code generation: x86_64 only
- [x] Linear Scan Register allocation

## Usage
See examples in `example` module. It shows how to use the `opt` IR in Kotlin code.

## Available optimizations
### Mem2Reg
Converts `alloc` instructions to SSA variables and insert `phi` functions. This is similar to the LLVM's Mem2Reg pass by concept.
The `alloc` instruction will be replaced if it is satisfied following conditions:
- The `alloc` value is primitive type (int, float, etc.)
- The value which produces the `alloc` is not escaped to the another function, global variable etc. (seek `Escape Analysis pass`)

### Constant folding & Dead code elimination
It removes instructions that are not used in the program and replaces constant expressions with their values. 
Currently, it supports only simple expressions like arithmetic operations and comparisons. 

## Implementation details
Generally, `opt` IR is similar to LLVM IR. The compilation is based on modules. Each module contains a list of function data, global variables and other metadata.
Each function contains a list of basic blocks. The basic block contains a list of instructions and every last instruction is a terminator instruction. 
Some terminator instructions produces control flow edge to another basic block, like `br` instruction. 
Compared to LLVM IR, all types of `call` instruction produces a control flow edge to the next basic block. 
It is assumed that only one exit basic block is allowed in the function. Seek `VerifySSA` pass to find out more limitations of the IR.
  
There are several steps to convert the IR into machine code: optimization, lowering, code generation.
### Optimization
The optimization phase performs the optimization pipeline for each function data in the module. 
Currently, the compiler doesn't perform any inter-procedural optimizations, so each function is optimized separately. 
This is optional step and it is run only if the `-O3` flag is specified.

### Lowering
The lowering phase converts the IR into a lower-level representation that is closer to the machine code.
This is complicated step to describe it here, I just mention some steps of it:
- It does SSA deconstruction steps: "parallel" copy insertion for `phi` operands, split critical edges inside the CFG.
- Put `copy` instructions for function arguments and some instruction operands.
- Transform `alloc` instructions to stack allocation instructions.
- Etc.

### Code generation
The code generation phase converts the lowered IR into X86_64 machine code. 
This step also performs register allocation and instruction selection. 
It creates a list of AT&T assembly instructions in text format. 
The compiler relies on the `as` assembler from GCC toolchain to convert the assembly code into machine code.

## References
- [LLVM](https://llvm.org/)
- [Poletto linear scan](https://dl.acm.org/doi/10.1145/237721.237727)
- [SSA Book](https://www.cs.utexas.edu/~lin/cs380c/wegman.pdf)
- [S. Muchnick. Advanced Compiler Design Implementation](https://www.amazon.com/Advanced-Compiler-Design-Implementation-Muchnick/dp/1558603204)
- [C. Wimmer. Linear Scan Register Allocation for the Java HotSpot™ Client Compiler (2008)](https://www.researchgate.net/publication/221012814_Linear_Scan_Register_Allocation_for_the_Java_HotSpot_Client_Compiler)
- [Design of an SSA Register Allocator](https://compilers.cs.uni-saarland.de/projects/ssara/hack_ssara_ssa09.pdf)
- [Value Numbering](https://softlib.rice.edu/pub/CRPC-TRs/reports/CRPC-TR94517-S.pdf)