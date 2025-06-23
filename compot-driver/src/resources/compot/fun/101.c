int get(int a, int c) {
    return a;
}

int (*test1(int b))(int a, int c) {
    return get;
}

int main() {
    return test1(2)(2, 4);
}