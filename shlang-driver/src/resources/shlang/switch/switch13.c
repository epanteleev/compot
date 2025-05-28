

int doit(int value) {
retry:
    switch (value) {
        case 1:
            return 1;
        default:
            value--;
            goto retry;
    }
}

int main() {
    int result = doit(10);
    return result;
}