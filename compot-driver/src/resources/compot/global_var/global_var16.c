extern void printf(const char *format, ...);

struct DATA {
    int a;
};

struct DATA data = { 2 };

void print(struct DATA* escaped) {
    printf("%d", escaped->a);
}

int main() {
    print(&data);
    return 0;
}