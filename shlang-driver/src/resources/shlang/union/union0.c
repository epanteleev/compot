#include <runtime/runtime.h>

union Value {
    float x;
    int y;
};

int main() {
    union Value p;
    p.x = 1.0;
    printFloat(p.x);
    return 0;
}