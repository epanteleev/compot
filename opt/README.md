# Opt
Yet another compiler backend

## Features
- [x] LLVM like IR
- [x] Optimizations: Mem2Reg
- [x] Code generation: x86_64 only
- [x] Register allocation: Poletto linear scan

## Usage
See examples in `example` module.

## Available optimizations
- Mem2Reg
- Constant folding & Dead code elimination

## References
- [LLVM](https://llvm.org/)
- [Poletto linear scan](https://dl.acm.org/doi/10.1145/237721.237727)
- [SSA Book](https://www.cs.utexas.edu/~lin/cs380c/wegman.pdf)
- [S. Muchnick. Advanced Compiler Design Implementation](https://www.amazon.com/Advanced-Compiler-Design-Implementation-Muchnick/dp/1558603204)
- [C. Wimmer. Linear Scan Register Allocation for the Java HotSpot™ Client Compiler (2008)](https://www.researchgate.net/publication/221012814_Linear_Scan_Register_Allocation_for_the_Java_HotSpot_Client_Compiler)
- [Design of an SSA Register Allocator](https://compilers.cs.uni-saarland.de/projects/ssara/hack_ssara_ssa09.pdf)