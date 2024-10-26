int num(int a) {
    _Bool is = a > 0;
    return a + is;
}

int main() {
    return num(2);
}