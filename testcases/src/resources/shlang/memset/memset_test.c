#include <stdlib.h>
#include <assert.h>

extern void memset(void *dest, char val, int n);


int test() {
    char dest[14];
    memset(dest, 0, 14);
    for (int i = 0; i < 14; i++) {
        assert(dest[i] == 0);
    }
    printf("Done");
    return 0;
}