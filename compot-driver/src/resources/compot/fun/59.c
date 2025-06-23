
int fun(int a) {
    if (a == 0) goto done;
    a = 1;
    while (a < 10) {
        a++;
    }
done:
    a = 2;
    if (a == 0) {
        goto done;
    }
    a = 2;
    return a;
}

int main() {
    return fun(2);
}