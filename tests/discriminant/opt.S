.global main
.global discriminant

main:
    pushq %rbp
    movq %rsp, %rbp
    pushq %r15
    pushq %r14
    movl $8, %r15d
    movl $8, %r15d
    movl $8, %edx
    callq discriminant
    movl %eax, %r15d
    movl %r15d, %r14d
    callq printInt
    movq $0, %rax
    popq %r14
    popq %r15
    leave
    ret
discriminant:
    pushq %rbp
    movq %rsp, %rbp
    pushq %r14
    pushq %r13
    pushq %r15
    pushq %r12
    movl %edx, %r15d
    movl %esi, %r14d
    movl %edi, %r13d
    movl %r14d, %r12d
    imull %r14d, %r12d
    movl $4, %r14d
    imull %r13d, %r14d
    movl %r14d, %r13d
    imull %r15d, %r13d
    movl %r12d, %r15d
    subl %r13d, %r15d
    movl %r15d, %eax
    popq %r12
    popq %r15
    popq %r13
    popq %r14
    leave
    ret