extern void printf(const char *format, ...);

struct DATA {
    int a;
};

struct DATA data = { 2 };

int main() {
    printf("%d", data.a);
    return 0;
}