.global sum8
.global main

sum8:
    pushq %rbp
    movq %rsp, %rbp
    subq $88, %rsp
    pushq %r12
    pushq %rbx
    pushq %r14
    pushq %r13
    pushq %r15
    movq 24(%rbp), %r15
    movq 16(%rbp), %r14
    movq %r9, %r13
    movq %r8, %r12
    movq %rcx, %rbx
    movq %rdx, %r11
    movq %rsi, -80(%rbp)
    movq %rdi, -88(%rbp)
    movq -88(%rbp), %r10
    movq %r10, -16(%rbp)
    movq -80(%rbp), %r10
    movq %r10, -24(%rbp)
    movq %r11, -32(%rbp)
    movq %rbx, -40(%rbp)
    movq %r12, -48(%rbp)
    movq %r13, -56(%rbp)
    movq %r14, -64(%rbp)
    movq %r15, -72(%rbp)
    movq -16(%rbp), %r15
    movq -24(%rbp), %r14
    movq %r15, %r13
    addq %r14, %r13
    movq -32(%rbp), %r14
    movq %r13, %r15
    addq %r14, %r15
    movq -40(%rbp), %r13
    movq %r15, %r14
    addq %r13, %r14
    movq -48(%rbp), %r15
    movq %r14, %r13
    addq %r15, %r13
    movq -56(%rbp), %r15
    movq %r13, %r14
    addq %r15, %r14
    movq -64(%rbp), %r15
    movq %r14, %r13
    addq %r15, %r13
    movq -72(%rbp), %r15
    movq %r13, %r14
    addq %r15, %r14
    movq %r14, %r13
    callq printUlong
    movq %r14, -8(%rbp)
    movq -8(%rbp), %r14
    movq %r14, %rax
    popq %r15
    popq %r13
    popq %r14
    popq %rbx
    popq %r12
    leave
    ret
main:
    pushq %rbp
    movq %rsp, %rbp
    pushq %r15
    movq $1, %rdi
    movq $2, %rsi
    movq $3, %rdx
    movq $4, %rcx
    movq $5, %r8
    movq $6, %r9
    movq $7, (%rsp)
    movq $8, -8(%rsp)
    subq $8, %rsp
    callq sum8
    addq $8, %rsp
    movq %rax, %r15
    movq %r15, %rax
    popq %r15
    leave
    ret