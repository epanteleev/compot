

static int gotoLoop(int p) {
  while (p != 1) {
    p--;
    p++;
    if (p == 0)
      goto remove;
    else if (p == 1) {
      goto remain;
    }
    else if (p == 2) {
      goto remain;
    }
    else {
      goto remove;
    }
    remove: p--; continue;
    remain: p--; continue;
  }
  return p;
}

int main() {
  int p = 2;
  return gotoLoop(p);
}