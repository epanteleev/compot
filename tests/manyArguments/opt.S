.global sum8
.global main

sum8:
    pushq %rbp
    movq %rsp, %rbp
    subq $24, %rsp
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
    movq %rsi, -8(%rbp)
    movq %rdi, -16(%rbp)
    movq -16(%rbp), %rax
    movq %rax, %r10
    addq -8(%rbp), %r10
    movq %r10, -24(%rbp)
    movq -24(%rbp), %rax
    movq %rax, %r10
    addq %r11, %r10
    movq %r10, -16(%rbp)
    movq -16(%rbp), %rax
    movq %rax, %r11
    addq %rbx, %r11
    movq %r11, %rbx
    addq %r12, %rbx
    movq %rbx, %r12
    addq %r13, %r12
    movq %r12, %r13
    addq %r14, %r13
    movq %r13, %r14
    addq %r15, %r14
    movq %r14, %r13
    callq printUlong
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