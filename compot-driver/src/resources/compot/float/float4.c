#include <math.h>
#include <stdio.h>

#define NAN -(0.0 / 0.0)

int main() {
    double nan = NAN;
    for (int j = 0; j < sizeof(double);j++) {
        printf("%x", ((unsigned char *)&nan)[j]);
    }

    printf("\n");
    return 0;
}