int main() {
    int i;
    int bi;
    for (i = 0, bi = 0; i < 10; ++i, ++bi) {
        if (i == 5) {
            break;
        }
    }
    return bi;
}