#include <string.h>
#include <assert.h>

int test() {
    const char* a = "test";
    int b = 0;
    if (b == 0 && strcmp(a, "test") == 0) {
        return 1;
    }
    return 0;
}

int test1() {
    const char* a = "test";
    int b = 0;
    if (b == 0 || strcmp(a, "tewerst") == 0) {
        return 1;
    }
    return 0;
}

int main() {
    assert(test() == 1);
    assert(test1() == 1);
    return 0;
}