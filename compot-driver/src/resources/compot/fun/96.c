
int match(int value) {
    switch(value) {
        default:
            value = 1;
        ret:
            value = 3;
        case 2:
            return 2;
    }

    goto ret;
}

int main() {
    return match(3);
}