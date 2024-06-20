#include <runtime/runtime.h>

struct Value {
};

int main() {
    int size = sizeof(struct Value);
    printInt(size);
    return size;
}