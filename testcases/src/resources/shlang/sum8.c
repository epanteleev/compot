#include "runtime/runtime.h"


int sum(int a, int b, int c, int d, int e, int f, int ff, int fff) {
    return a + b + c + d + e + f + ff + fff;
}

float sumFloat(float a, float b, float c, float d, float e, float f, float ff, float fff) {
    return a + b + c + d + e + f + ff + fff;
}

int main() {
    int sumRes = sum(1,1,1,1,1,1,1,1);
    check(sumRes, 8);

    float sumFloatRes = sumFloat(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
    printFloat(sumFloatRes);
    return 0;
}