#include <stddef.h>
#include <assert.h>

struct Point {
    int a;
    int b;
};

int main(void) {
	struct Point arr = {1, 2};
	char *ptr = (char *) arr;
    assert(offsetof(struct Point, a) == 0);
    assert(offsetof(struct Point, b) == 4);
	return (int) *ptr;
}
