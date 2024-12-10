
union FloatOrInt {
    int i32;
    float f32;
};

int main() {
    union FloatOrInt u = { 1 };
    return u.i32;
}