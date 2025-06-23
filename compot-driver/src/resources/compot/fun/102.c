int get(int a, int c) {
    return a;
}

int test1(int(fn)(int a, int c)) {
    return fn(2, 3);
}

int main() {
    return test1(get);
}