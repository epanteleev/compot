char ptr0[] = "b" ; // NULL
char ptr[] = "a" ; // NULL
static char *table[][2] = {
    { ptr0, ptr }
};

int main() {
    return (long)*table[0][0];
}