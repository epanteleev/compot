#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

bool as_bool(int* p) {
    return p;
}

int main(void) {
    int *p2 = aligned_alloc(256, 1024*sizeof *p2);
    if (as_bool(p2)) {
        printf("Done\n");
    }
    free(p2);
    return 0;
}