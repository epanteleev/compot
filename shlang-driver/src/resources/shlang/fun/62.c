
int fun() { return 1; }

int (*fun_ptr)() = fun;

int main() {
    if (fun_ptr == &fun) {
        return fun_ptr();
    }
    return 0;
}
