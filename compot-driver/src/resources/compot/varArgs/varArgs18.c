#include <stdio.h>
#include <stdarg.h>

void variadic(char* r1, int r2, int r3, int r4, int r5, int r_last, ...)
{
    va_list ap;

    va_start(ap, r_last);
    printf("variadic: %d %d %d %d\n", r2, r3, r4, r5);
    vfprintf(stdout, r1, ap);
    va_end(ap);
}

int main()
{
    int r2 = 1;
    int r3 = 2;
    int r4 = 3;
    int r5 = 4;
    int r6 = 5;
    char* fmt = "Numbers: %d %d %d %d\n";
    variadic(fmt, r2, r3, r4, r5, r6, 6, 7, 8, 9);
    return 0;
}