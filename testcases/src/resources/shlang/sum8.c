#include "runtime/runtime.h"


int sum(int a, int b, int c, int d, int e, int f, int ff, int fff) {
    return a + b + c + d + e + f + ff + fff;
}

short sumShort(short a, short b, short c, short d, short e, short f, short ff, short fff) {
    return a + b + c + d + e + f + ff + fff;
}

long sumLong(long a, long b, long c, long d, long e, long f, long ff, long fff) {
    return a + b + c + d + e + f + ff + fff;
}

float sumFloat(float a, float b, float c, float d, float e, float f, float ff, float fff) {
    return a + b + c + d + e + f + ff + fff;
}

float sumIntFloat(int a, float b, int c, int d, int e, int f, int ff, int fff, float g) {
    return a + b + c + d + e + f + ff + fff + g;
}

int main() {
    int sumRes = sum(1,1,1,1,1,1,1,1);
    check(sumRes, 8);

    short sumShortRes = sumShort(1,1,1,1,1,1,1,1);
    check(sumShortRes, 8);

    long sumLongRes = sumLong(1,1,1,1,1,1,1,1);
    check(sumLongRes, 8);

    float sumFloatRes = sumFloat(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
    printFloat(sumFloatRes);

    float sumIntFloatRes = sumIntFloat(1, 1.0, 1, 1, 1, 1, 1, 1, 1.0);
    printFloat(sumIntFloatRes);
    return 0;
}