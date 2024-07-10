#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>

int main() {
    assert(sizeof(int8_t) == 1);
    assert(sizeof(uint8_t) == 1);
    assert(sizeof(int16_t) == 2);
    assert(sizeof(uint16_t) == 2);
    assert(sizeof(int32_t) == 4);
    assert(sizeof(uint32_t) == 4);
    assert(sizeof(int64_t) == 8);
    assert(sizeof(uint64_t) == 8);

    assert(sizeof(signed char) == 1);
    assert(sizeof(unsigned char) == 1);
    assert(sizeof(short) == 2);
    assert(sizeof(unsigned short) == 2);
    assert(sizeof(int) == 4);
    assert(sizeof(unsigned int) == 4);
    assert(sizeof(long) == 8);
    assert(sizeof(unsigned long) == 8);
    assert(sizeof(long long) == 8);
    assert(sizeof(unsigned long long) == 8);
    assert(sizeof(float) == 4);
    assert(sizeof(double) == 8);
    assert(sizeof(long double) == 8); //TODO 8 bytes

    return 0;
}