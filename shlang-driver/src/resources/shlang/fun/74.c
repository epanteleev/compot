

void set(unsigned* endian, unsigned char value) {
    *(unsigned char *)endian = value;
}

int main() {
    unsigned endian;
    set(&endian, 1);
    if (endian) {
        return 1;
    }

    return 0;
}