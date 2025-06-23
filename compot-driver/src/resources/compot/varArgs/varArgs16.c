#include <stdio.h>
#include <stdarg.h>
#include <stdbool.h>

int fsnprintf(bool string, void *stream, int size, const char *format, ...)
{
    va_list args;
    va_start(args, format);

    int res = string ? vsnprintf((char *)stream, size, format, args) : vfprintf((FILE *)stream, format, args);

    va_end(args);
    return res;
}

int main() {
    char buf[100];
    fsnprintf(true, buf, 100, "Hello, %s!\n", "world");
    printf("%s", buf);
    return 0;
}