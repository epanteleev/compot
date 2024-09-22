extern void printf(char format[], ...);


typedef struct {
    int point[3];
} Vect3;

int main() {
    Vect3 a[] = {{{1, 2, 3}}, {{4, 5, 6}}, {{7, 8, 9}}, {{10, 11, 12}}};
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 3; j++)
            printf("%d ", a[i].point[j]);
        printf("\n");
    }
    return 0;
}