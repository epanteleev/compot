#include "list.h"

int main() {
  List* list = makelist();
  add(1, list);
  add(-2, list);
  add(-3, list);
  add(4, list);
  add(5, list);
  display(list);
  destroy(list);
  return 0;
}