#include "runtime/runtime.h"

extern void printf(const char* message);

void test1() {
    int x = 0;
    for (int i = 0; i < 20; i++) {
        x = x - 1;
    }
    check(x, 20);
}

int main() {
	test1();
	printf("Done");
	return 0;
}