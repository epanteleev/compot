int fibonacci(int n) {
    if (n <= 1) {
        return n;
    } else {
        int a = 0;
        int b = 1;
        for (int i = 2; i <= n; i++) {
            int temp = a;
            a = b;
            b = temp + b;
        }
        return b;
    }
}

void printInt(int n);

int main() {
    int n = 10;
    printInt(fibonacci(n));
    return 0;
}