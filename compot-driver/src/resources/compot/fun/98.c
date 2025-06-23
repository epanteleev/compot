
int get() {
    return 1;
}

int main() {
    typedef int(*get_t)();
    get_t xLog = get;
    return xLog();
}