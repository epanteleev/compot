
long ptrdiff(char *a, char *b) {
    long d = 0;
    d += a - b;
    return d;
}

int main() {
    int arr[5];
    char *p = (char *)arr;
    char *q = (char *)arr + 1;
    long d = ptrdiff(q, p);
    return d;
}