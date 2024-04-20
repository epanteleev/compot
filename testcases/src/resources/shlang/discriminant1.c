
void printDouble(double x);

double calculateDiscriminant(double a, double b, double c) {
    double discriminant = b * b - 4 * a * c;
    return discriminant;
}

int main() {
    double a = 1.0, b = -3.0, c = 2.0;
    double discriminant = calculateDiscriminant(a, b, c);
    printDouble(discriminant);
    return 0;
}