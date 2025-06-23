#include <math.h>

int is_inf(double x) {
    if (x == HUGE_VAL) {
        return 1;
    }

    return 0;
}

int is_neg_inf(double x) {
    if (x == -HUGE_VAL) {
        return 1;
    }

    return 0;
}

int main() {
    double acc = HUGE_VAL;
    if (!is_inf(acc)) {
        return 1;
    }

    if (is_inf(-acc)) {
        return 1;
    }

    if (!is_neg_inf(-acc)) {
        return 1;
    }

    if (is_neg_inf(acc)) {
        return 1;
    }

    return 0;
}