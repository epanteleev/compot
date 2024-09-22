#include "list.h"

int main() {
  Node* newNode = malloc(sizeof(Node));
  if (!newNode) {
    return NULL;
  }
  newNode->data = 6.7;
  newNode->next = NULL;

  printf("newNode->data: %f\n", newNode->data);
  return 0;
}