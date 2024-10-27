
extern int printf(const char *fmt, ...);

void my_printf(const char *fmt, ...) {
    __builtin_va_list args;
    __builtin_va_start(args, fmt);
    int first = __builtin_va_arg(args, int);
    printf(fmt, first);
}

int main() {
    my_printf("Number: %d\n", 1);
    return 0;
}