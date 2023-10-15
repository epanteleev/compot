.global main

main:
    pushq %rbp
    movq %rsp, %rbp
    subq $24, %rsp
    pushq %r15
    pushq %r14
    movq %r15, %r14
    movl $5, %r15d
    subq $8, %rsp
    callq printIntArray
    addq $8, %rsp
    movl $0, %eax
    popq %r14
    popq %r15
    leave
    ret