#include "runtime/runtime.h"

extern void printf(const char* message);

void test5() {
    int x = -1;
    for (int i = 0; i < x; i++) {
        x = x + 1;
    }
    check(x, -1);
}

int main() {
	test5();
	printf("Done");
	return 0;
}