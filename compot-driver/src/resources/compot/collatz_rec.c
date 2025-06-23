#include <runtime/runtime.h>

int collatz(int x) {
  if (x == 1) {
    return 1;
  }
  if (x % 2 == 0) {
    return collatz(x / 2);
  }
  if (x % 2 == 1) {
    return collatz(3 * x + 1);
  }
  return 0;
}

int main() {
  check(collatz(123), 1);
  return 0;
}