#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

extern void memset_m(void *dest, char val, int n);


int test() {
    char dest[14];
    memset_m(dest, 0, 14);
    for (int i = 0; i < 14; i++) {
        assert(dest[i] == 0);
    }
    printf("Done");
    return 0;
}