
union FloatOrInt {
    float f32;
    int i32;
};

int main() {
    union FloatOrInt u = { 1 };
    return u.i32;
}