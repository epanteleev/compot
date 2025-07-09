_Bool return_status() {
    return 1;
}

int main() {
    _Bool status = 0;
    status |= return_status();
    return status;
}
