#include <assert.h>
#include <stdio.h>

#ifndef VALUE_TYPE
#define VALUE_TYPE int
#define VALUE_FMT "%d"
#endif

int f(VALUE_TYPE a0, VALUE_TYPE a1, VALUE_TYPE a2, VALUE_TYPE a3, VALUE_TYPE a4,VALUE_TYPE a5, VALUE_TYPE a6, VALUE_TYPE a7, VALUE_TYPE a8, VALUE_TYPE a9, VALUE_TYPE a10, VALUE_TYPE a11, VALUE_TYPE a12) {
  return a0+a1+a2+a3+a4+a5+a6+a7+a8+a9+a10+a11+a12;
}

int main() {
  VALUE_TYPE res = f(1,1,1,1,1,1,1,1,1,1,1,1,1);
  printf(VALUE_FMT, res);
  return 0;
}