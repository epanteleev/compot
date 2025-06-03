int main() {
    int a = 9;
    if (a == 0) {
        goto L;
    } else {
        goto B;
    }
    goto L;
L:
    a = 90;
B:
    return 90;
}