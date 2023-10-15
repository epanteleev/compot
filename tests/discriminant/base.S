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
    subq $16, %rsp
    pushq %r15
    pushq %r14
    pushq %r13
    pushq %r12
    movl %edx, %r15d
    movl %esi, %r14d
    movl %edi, %r13d
    movl %r13d, -4(%rbp)
    movl %r14d, -8(%rbp)
    movl %r15d, -12(%rbp)
    movl -8(%rbp), %r15d
    movl -8(%rbp), %r14d
    movl %r15d, %r13d
    imull %r14d, %r13d
    movl -4(%rbp), %r15d
    movl $4, %r14d
    imull %r15d, %r14d
    movl -12(%rbp), %r15d
    movl %r14d, %r12d
    imull %r15d, %r12d
    movl %r13d, %r15d
    subl %r12d, %r15d
    movl %r15d, %eax
    popq %r12
    popq %r13
    popq %r14
    popq %r15
    leave
    ret