

int main() {
    int x = 50;
    switch (x) {
        case 50:
            x = x - 1;
            break;
        case 49:
            x = x - 2;
            break;
        default:
            x = 0;
    }
    return x;
}