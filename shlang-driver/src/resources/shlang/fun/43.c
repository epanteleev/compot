
int static_var1() {
    static int variable = 1;
    return variable;
}

int static_var2() {
    static int variable = 2;
    return variable;
}

int main() {
    int a = static_var1();
    int b = static_var2();
    return a + b;
}
