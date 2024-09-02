#include <assert.h>
#include <inttypes.h>
#include <stdio.h>

int main() {
    uint64_t hash = 5381; /* init value */
    printf("hash = %" PRIu64 "\n", hash & UINT64_MAX);
    return 0;
}