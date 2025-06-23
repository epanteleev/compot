
extern void printf(const char *format, ...);

int variable = 100;

void print(int* escaped) {
    printf("%d", *escaped);
}

int main() {
    print(&variable);
    return 0;
}