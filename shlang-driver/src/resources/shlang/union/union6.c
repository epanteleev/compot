

typedef union Node {
  struct NodeKey {
    char key_tt;
    int next;
    double key_val;
  } u;
  double i_val;
} Node;


static Node dummy = {.u = {.key_tt = 'a', .next = 2, .key_val = 0.3}};

extern int printf(const char* fmt, ...);

int main() {
  printf("u.key_tt: %c, u.next: %d, u.key_val: %lf, i_val: %lf\n", dummy.u.key_tt, dummy.u.next, dummy.u.key_val, dummy.i_val);
  return 0;
}