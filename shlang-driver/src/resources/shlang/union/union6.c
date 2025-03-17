

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

struct Holder {
  Node* n;
};

int main() {
  struct Holder h;
  h.n = &dummy;
  printf("u.key_tt: %c, u.next: %d, u.key_val: %lf, i_val: %lf\n", h.n->u.key_tt, h.n->u.next, h.n->u.key_val, h.n->i_val);

  return 0;
}