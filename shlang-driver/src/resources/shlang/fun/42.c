
const char ptr[] = "a" ; // NULL
static char *table[][1] = {
    { ptr }
};

int main() {
    return (long)table[0][0];
}