#include <limits.h>

#define LUA_MAXUNSIGNED    (~0UL)

void* ptr(long a) {
    void* p = (a == 8);
    return p;
}

int main() {
    return (long)ptr(8);
}