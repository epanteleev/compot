#include <stdbool.h>

bool val;

void set_val(bool v) {
    val = v;
}


int main() {
    set_val(true);
    if (val) {
        return 0;
    }
    return 1;
}