int get(int a) {
    return a;
}

int (*test1(int b))(int a) {
    return get;
}

int main() {
    return test1(2)(2);
}