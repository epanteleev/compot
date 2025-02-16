

int main() {
    unsigned endian;
    endian = 1;
    if (*(unsigned char *)&endian) {
        return 1;
    }

    return 0;
}