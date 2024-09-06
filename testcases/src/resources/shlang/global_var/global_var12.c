#include <inttypes.h>
#include <stdio.h>

/* length of test inputs */
#define LEN ((size_t)8)

/* input pcm for test */
int16_t pcm[LEN] = {1000, -1000, 1234, 3200, -1314, 0, 32767, -32768};

void test(int16_t *pcm, size_t len) {
    for (size_t i = 0; i < len; i++) {
        printf("%d, ", pcm[i]);
    }
}

int main() {
    test(pcm, LEN);
}