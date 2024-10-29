
extern int printf(const char *fmt, ...);

int my_printf(const char *fmt, ...) {
    int val = 33;
    __builtin_va_list args;
    __builtin_va_start(args, fmt);
    int v2 = 55;
    int first = __builtin_va_arg(args, int);
    printf(fmt, first);
    __builtin_va_end(args);
    return val + v2;
}

int main() {
    return my_printf("Number: %d\n", 1);
}