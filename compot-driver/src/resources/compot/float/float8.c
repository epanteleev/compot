#include <float.h>

#define NAN1 (0.0 / 0.0)

int main() {
    double nan = NAN1;
    return nan != 0;
}