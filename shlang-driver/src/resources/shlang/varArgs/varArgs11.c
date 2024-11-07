
struct Point {
    int x;
    int y;
};

extern int printf(const char *fmt, ...);

void my_printf(const char *fmt, ...) {
    __builtin_va_list args;
    __builtin_va_start(args, fmt);
    struct Point first;
    first = __builtin_va_arg(args, struct Point);
    printf(fmt, first.x, first.y);
    __builtin_va_end(args);
}

int main() {
    my_printf("Point: (%d, %d)\n", (struct Point){1, 2});
    return 0;
}