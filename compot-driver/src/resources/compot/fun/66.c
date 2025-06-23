
typedef struct {
    long x;
    long y;
    long z;
} Vec3;

int main() {
    Vec3 v0 = {1, 2, 3};
    Vec3 v1 = {4, 5, 6};
    Vec3 v3 = 1 ? v0 : v1;
    return v3.x + v3.y + v3.z;
}