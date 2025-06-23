extern void printf(const char* fmt, ...);

typedef struct Vect3_ {
    int arr[3];
} Vect3;

void printVec(Vect3* rec) {
    printf("Vect3: %d %d %d\n", rec->arr[0], rec->arr[1], rec->arr[2]);
}

int main() {
    Vect3 p;
    p.arr[0] = 10;
    p.arr[1] = 20;
    p.arr[2] = 30;
    printVec(&p);
    return 0;
}