
int main() {
    char buffer[4] = {0, 0, 0, 8};
    int value = ((int)*buffer << 24) | ((int)*(buffer + 1) << 16) | ((int)*(buffer + 2) << 8) | *(buffer + 3);

    return value;
}