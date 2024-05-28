#include <runtime/runtime.h>

int main() {
    int size = 0;
    check(sizeof(size), 4);

    int array[10];
    check(sizeof(array), 40);
    return 0;
}