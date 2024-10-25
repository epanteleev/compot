#include <stdio.h>
#include <assert.h>

void test1() {
    int i = 0;
    for (i = 0; ; i++) {
        if (i == 1000) {
            break;
        }
        i++;
    }

    assert(i == 1000);
}

int main() {
	test1();
	printf("Done");
	return 0;
}