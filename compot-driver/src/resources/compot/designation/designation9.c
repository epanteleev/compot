extern void printf(char format[], ...);

typedef struct {
    int x;
    int y;
    int z;
} Vector;

int main() {
    Vector p = {.y = 1};
    printf("x: %d, y: %d, z: %d\n", p.x, p.y, p.z);
    return 0;
}
