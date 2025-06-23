

struct Array {
    int (*coef_bits)[64];
};

int main() {
    struct Array a;
    int coef_bits[64] = {0, 1};
    a.coef_bits = &coef_bits;
    a.coef_bits[0][0] = 2;
    return a.coef_bits[0][1] + a.coef_bits[0][0];
}