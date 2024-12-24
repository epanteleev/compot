
union Value {
    int i;
    double d;
};

int main() {
    union Value a[2] = {0};
    return a[1].i;
}