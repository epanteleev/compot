#include <runtime/runtime.h>

int main() {
    int size = sizeof(int);
    check(size, 4);

    check(sizeof(char), 1);
    check(sizeof(short), 2);
    check(sizeof(long), 8);
    check(sizeof(long long), 8);
    check(sizeof(float), 4);
    check(sizeof(double), 8);
    //check(sizeof(long double), 16); TODO: long double is not supported yet

    return 0;
}