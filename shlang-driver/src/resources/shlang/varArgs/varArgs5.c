
extern int printf(const char *fmt, ...);

void my_printf(const char *fmt, ...) {
    __builtin_va_list args;
    __builtin_va_start(args, fmt);
    double first = __builtin_va_arg(args, double);
    printf(fmt, first);
    __builtin_va_end(args);
}

int main() {
    my_printf("Number: %lf\n", 1.0);
    return 0;
}