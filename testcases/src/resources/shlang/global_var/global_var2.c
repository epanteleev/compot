#ifndef VALUE_TYPE
#define VALUE_TYPE int
#define VALUE_FMT "%d"
#endif

extern void printf(const char *format, ...);

VALUE_TYPE variable = 50 + 50;

int main() {
    printf(VALUE_FMT, variable);
    return 0;
}