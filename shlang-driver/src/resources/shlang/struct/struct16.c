
typedef union {
    long intVal;
    double realVal;
} Const;

int copied(Const c) {
    if (c.intVal == 10) {
        return 0;
    }
    c.intVal = 30;
    return 1;
}

void getAndIntVal(Const *c, void* val) {
    if (copied(*c)) {
        return;
    }
    *(long*)val = c->intVal;
}


int main() {
    Const c = {.intVal = 10};
    long res;
    getAndIntVal(&c, &res);
    return res;
}