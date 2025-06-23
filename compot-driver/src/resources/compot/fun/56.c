
union Value {
    int i;
    double d;
};

int main() {
    union Value a = {0};
    return a.i;
}