#include "runtime/runtime.h"

extern void printf(const char* message);

void test1() {
    int x = 0;
    for (int i = 0; i < x; i++) {
        x = x + 1;
    }
    check(x, 0);
}

void test2() {
    int x = 10;
    int i = 0;
    for (;i < x;) {
        i++;
    }
    check(x, 10);
    check(i, 10);
}

void test3() {
    int i = 0;
    for (; i < 1000; i++) {}

    check(i, 1000);
}

void test4() {
    int i = 0;
    for (; i < 1000;) {
        i++;
    }

    check(i, 1000);
}

int main() {
	test1();
	test2();
	test3();
	test4();
	printf("Done");
	return 0;
}