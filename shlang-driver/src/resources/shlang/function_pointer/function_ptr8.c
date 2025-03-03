

typedef int (*int_fun) (int *L);

typedef struct luaL_Reg {
  const char *name;
  int_fun func;
} Entry;

int fun1(int *v) {
  return *v + 1;
}

int fun2(int *v) {
  return *v + 2;
}

static const Entry funs[] = {
  {"fun1", fun1},
  {"fun2", fun2},
  {0, 0}
};

int main() {
  int v = 0;
  int_fun f = funs[1].func;
  return f(&v);
}