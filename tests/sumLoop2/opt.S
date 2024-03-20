.global sumLoop2
.global main

.text
sumLoop2:
    pushq %rbp
    movq %rsp, %rbp
    pushq %rbx
    pushq %r14
    pushq %r15
    pushq %r12
    pushq %r13
    # %0x5 = copy i32 0
    movl $0, %r15d
    # %0x6 = copy i32 0
    movl $0, %r14d
    # br label L1
    jmp .L0.1
.L0.1:
    # %phi1x5 = phi i32 [%0x5: entry, %8x2: L8]
    # %phi1x4 = phi i32 [%0x6: entry, %8x3: L8]
    # %cmp1x2 = icmp lt i32 %phi1x5, %0
    cmpl %edi, %r15d
    # br u1 %cmp1x2 label %L2, label %L3 
    jge .L0.3
.L0.2:
    # %2x0 = copy i32 %phi1x4
    movl %r14d, %r13d
    # %2x1 = copy i32 0
    movl $0, %r12d
    # br label L4
    jmp .L0.4
.L0.4:
    # %phi4x4 = phi i32 [%2x0: L2, %7x2: L7]
    # %phi4x3 = phi i32 [%2x1: L2, %7x3: L7]
    # %cmp4x2 = icmp lt i32 %phi4x3, %1
    cmpl %esi, %r12d
    # br u1 %cmp4x2 label %L5, label %L6 
    jge .L0.6
.L0.5:
    # %5x2 = add i32 %phi4x4, %phi1x5
    movl %r13d, %ebx
    addl %r15d, %ebx
    # %5x4 = add i32 %5x2, %phi4x3
    addl %r12d, %ebx
    # br label L7
    jmp .L0.7
.L0.7:
    # %7x1 = add i32 %phi4x3, 1
    leal 1(%r12), %r14d
    # %7x2 = copy i32 %5x4
    movl %ebx, %r13d
    # %7x3 = copy i32 %7x1
    movl %r14d, %r12d
    # br label L4
    jmp .L0.4
.L0.6:
    # br label L8
    jmp .L0.8
.L0.8:
    # %8x1 = add i32 %phi1x5, 1
    leal 1(%r15), %r11d
    # %8x2 = copy i32 %8x1
    movl %r11d, %r15d
    # %8x3 = copy i32 %phi4x4
    movl %r13d, %r14d
    # br label L1
    jmp .L0.1
.L0.3:
    # ret i32 %phi1x4
    movl %r14d, %eax
    popq %r13
    popq %r12
    popq %r15
    popq %r14
    popq %rbx
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