extern void printf(char* fmt, ...);

void printBitmask(char bitmask) {
    for (int i = 7; i >= 0; i--) {
        printf("%d", (bitmask >> i) & 1);
    }
}

int main() {
    char* str = "Здрав\n";
    printBitmask(str[0]);
    printf(" ");
    printBitmask(str[1]);
    return 0;
}