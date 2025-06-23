
extern int printf(const char *fmt, ...);

void my_printf(const char *fmt, ...) {
    __builtin_va_list args;
    __builtin_va_start(args, fmt);
    int first = __builtin_va_arg(args, int);
    int second = __builtin_va_arg(args, int);
    int third = __builtin_va_arg(args, int);
    int fourth = __builtin_va_arg(args, int);
    int fifth = __builtin_va_arg(args, int);
    int sixth = __builtin_va_arg(args, int);
    printf(fmt, first, second, third, fourth, fifth, sixth);
}

int main() {
    my_printf("Numbers: %d %d %d %d %d %d\n", 1, 2, 3, 4, 5, 6);
    return 0;
}