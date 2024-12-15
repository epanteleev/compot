
int ret2() {
    return 2;
}

int ret1() {
    return 1;
}

int main() {
    int t = 0;
    int(*ptr)() = t != 0 ? ret1 : ret2;
    return ptr();
}
