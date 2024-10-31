
extern int printf(const char *fmt, ...);

void my_printf(double first, ...) {
    __builtin_va_list args;
    __builtin_va_start(args, first);
    const char *fmt = __builtin_va_arg(args, const char*);
    printf(fmt, first);
    __builtin_va_end(args);
}

int main() {
    my_printf(1.0, "Number: %lf\n");
    return 0;
}