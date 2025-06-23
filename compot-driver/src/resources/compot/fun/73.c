

int fun(unsigned* endian) {
    if (*(unsigned char *)endian) {
        return 1;
    }

    return 0;
}

int main() {
    unsigned endian;
    endian = 1;
    return fun(&endian);
}