
int set() {
    static char prefix[100] = "";
    prefix[0] = 'a';
    return prefix[0];
}

int main() {
    set();
    set();
    int k = set();
    return k;
}