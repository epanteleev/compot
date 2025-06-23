
int match(int value) {
    switch(value) {
        if (value < 2) {
            case 2:
                return 2;
        }
        default:
            value = 1;
    }

    value = 3;
    return value;
}

int main() {
    return match(2);
}