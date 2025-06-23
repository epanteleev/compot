#include <stdint.h>

extern int printf(const char *, ...);
extern void *memset(void *, int, unsigned long);


int get(int *array, uint8_t index) {
    return array[index + index * (uint8_t)16];
}

int main(void) {
    int array[1000];
    for (uint16_t i = 0; i < 1000; i++) {
        array[i] = i;
    }
    int idx = 4;
    printf("%d\n", *(array + (uint8_t)idx * (uint8_t)16));
    return 0;
}
