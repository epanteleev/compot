extern void printf(const char *format, ...);

int variable = 100;

int main() {
    variable = 89;
    printf("%d", variable);
    return 0;
}