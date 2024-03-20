.global sumLoop2
.global main

.text
sumLoop2:
    pushq %rbp
    movq %rsp, %rbp
    subq $24, %rsp
    pushq %r15
    pushq %r14
    # %gen0x5 = gen i32
    # %gen0x6 = gen i32
    # %gen0x7 = gen i32
    # %gen0x8 = gen i32
    # %gen0x9 = gen i32
    # move i32 %gen0x5 %0
    movl %edi, -4(%rbp)
    # move i32 %gen0x6 %1
    movl %esi, -8(%rbp)
    # move i32 %gen0x7 0
    movl $0, -12(%rbp)
    # move i32 %gen0x8 0
    movl $0, -16(%rbp)
    # br label L1
    jmp .L0.1
.L0.1:
    # %1x3 = copy i32 %gen0x8
    movl -16(%rbp), %r15d
    # %1x4 = copy i32 %gen0x5
    movl -4(%rbp), %r14d
    # %cmp1x2 = icmp lt i32 %1x3, %1x4
    cmpl %r14d, %r15d
    # br u1 %cmp1x2 label %L2, label %L3 
    jge .L0.3
.L0.2:
    # move i32 %gen0x9 0
    movl $0, -20(%rbp)
    # br label L4
    jmp .L0.4
.L0.4:
    # %4x3 = copy i32 %gen0x9
    movl -20(%rbp), %r15d
    # %4x4 = copy i32 %gen0x6
    movl -8(%rbp), %r14d
    # %cmp4x2 = icmp lt i32 %4x3, %4x4
    cmpl %r14d, %r15d
    # br u1 %cmp4x2 label %L5, label %L6 
    jge .L0.6
.L0.5:
    # %5x5 = copy i32 %gen0x7
    movl -12(%rbp), %r15d
    # %5x6 = copy i32 %gen0x8
    movl -16(%rbp), %r14d
    # %5x2 = add i32 %5x5, %5x6
    addl %r15d, %r14d
    # %5x7 = copy i32 %gen0x9
    movl -20(%rbp), %r15d
    # %5x4 = add i32 %5x2, %5x7
    addl %r15d, %r14d
    # move i32 %gen0x7 %5x4
    movl %r14d, -12(%rbp)
    # br label L7
    jmp .L0.7
.L0.7:
    # %7x2 = copy i32 %gen0x9
    movl -20(%rbp), %r14d
    # %7x1 = add i32 %7x2, 1
    addl $1, %r14d
    # move i32 %gen0x9 %7x1
    movl %r14d, -20(%rbp)
    # br label L4
    jmp .L0.4
.L0.6:
    # br label L8
    jmp .L0.8
.L0.8:
    # %8x2 = copy i32 %gen0x8
    movl -16(%rbp), %r14d
    # %8x1 = add i32 %8x2, 1
    addl $1, %r14d
    # move i32 %gen0x8 %8x1
    movl %r14d, -16(%rbp)
    # br label L1
    jmp .L0.1
.L0.3:
    # %3x1 = copy i32 %gen0x7
    movl -12(%rbp), %r15d
    # ret i32 %3x1
    movl %r15d, %eax
    popq %r14
    popq %r15
    leave
    ret
main:
    pushq %rbp
    movq %rsp, %rbp
    pushq %r15
    # downstackframe [i32 @sumLoop2(i32, i32)]
    subq $8, %rsp
    # %0x1 = copy i32 3
    movl $3, %edi
    # %0x2 = copy i32 5
    movl $5, %esi
    # %0x0 = call i32 @sumLoop2(%0x1:i32, %0x2:i32)
    callq sumLoop2
    movl %eax, %r15d
    # upstackframe [i32 @sumLoop2(i32, i32)]
    addq $8, %rsp
    # downstackframe [void @printInt(i32)]
    subq $8, %rsp
    # %0x3 = copy i32 %0x0
    movl %r15d, %edi
    # call void @printInt(%0x3:i32)
    callq printInt
    # upstackframe [void @printInt(i32)]
    addq $8, %rsp
    # ret u64 0
    movq $0, %rax
    popq %r15
    leave
    ret