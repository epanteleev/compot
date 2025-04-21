static const char *invalidName = "\1";

int main() {
    char *p = (char *)invalidName;
    return p[0];
}