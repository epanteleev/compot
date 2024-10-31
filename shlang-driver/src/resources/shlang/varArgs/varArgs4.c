
extern int printf(const char *fmt, ...);

void my_printf(const char *fmt, int first, ...) {
    __builtin_va_list args;
    __builtin_va_start(args, fmt);
    int second = __builtin_va_arg(args, int);
    printf(fmt, first, second);
    __builtin_va_end(args);
}

int main() {
    my_printf("Numbers: %d %d\n", 1, 2);
    return 0;
}