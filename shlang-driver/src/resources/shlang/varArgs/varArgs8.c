
struct Vector {
    long x;
    long y;
    long z;
};

extern int printf(const char *fmt, ...);

void my_printf(const char *fmt, ...) {
    __builtin_va_list args;
    __builtin_va_start(args, fmt);
    struct Vector first = __builtin_va_arg(args, struct Vector);
    printf(fmt, first.x, first.y, first.z);
    __builtin_va_end(args);
}

int main() {
    my_printf("Vector: (%d, %d, %d)\n", (struct Vector){1, 2, 3});
    return 0;
}