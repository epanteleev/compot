#include <assert.h>

enum color {
    RED = 56,
    GREEN,
    BLUE = RED + 2
};

int main() {
    enum color c = RED;
    assert(c == 56);
    assert(GREEN == 57);
    assert(BLUE == 58);
    return 0;
}