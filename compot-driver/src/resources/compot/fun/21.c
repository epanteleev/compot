
void assign(void *var) {
    *(int*)var = 10;
}

int main() {
    int var;
    assign(&var);
    return var;
}