#include <stdio.h>
#include <assert.h>

int print(const char* first, const char* second) {
    printf("%s %s", first, second);
    return 1;
}

int main() {
    assert(print("a=0; (function(x){a=x;})('hi'); a", "\"hi\""));
    return 0;
}