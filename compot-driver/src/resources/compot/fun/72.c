

int main() {
    unsigned endian;
    *(unsigned char *)&endian = 1;
    if (endian) {
        return 1;
    }

    return 0;
}