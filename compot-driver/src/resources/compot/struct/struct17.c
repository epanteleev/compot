
extern int printf(const char*, ...);

typedef struct
{
    long data1;
    long data2[255];
    union
    {
        char small;
        double big;
    };
} Large;

Large init() {
    Large l;
    l.small = 'a';
    return l;
}

int main() {
    Large l = init();
    printf("%c\n", l.small);
    return 0;
}