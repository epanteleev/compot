#ifndef _LIST_H
#define _LIST_H

#ifndef DATATYPE
#define DATATYPE int
#define FMT "%d\n"
#endif

extern void printf(char *fmt, ...);
extern void *malloc(int size);
extern void free(void *ptr);
#define NULL 0


typedef struct node {
  DATATYPE data;
  struct node* next;
} Node;

typedef struct list {
  Node * head;
} List;

Node * createnode(DATATYPE data) {
  Node * newNode = malloc(sizeof(Node));
  if (!newNode) {
    return NULL;
  }
  newNode->data = data;
  newNode->next = NULL;
  return newNode;
}

List * makelist() {
  List * list = malloc(sizeof(List));
  if (!list) {
    return NULL;
  }
  list->head = NULL;
  return list;
}

void display(List * list) {
  Node * current = list->head;
  if(list->head == NULL) {
    return;
  }

  for(; current != NULL; current = current->next) {
    printf(FMT, current->data);
  }
}

void add(DATATYPE data, List * list) {
  Node * current = NULL;
  if(list->head == NULL){
    list->head = createnode(data);
  }
  else {
    current = list->head;
    while (current->next != NULL){
      current = current->next;
    }
    current->next = createnode(data);
  }
}

void delete(DATATYPE data, List * list) {
  Node * current = list->head;
  Node * previous = current;
  while(current != NULL){
    if(current->data == data){
      previous->next = current->next;
      if(current == list->head)
        list->head = current->next;
      free(current);
      return;
    }
    previous = current;
    current = current->next;
  }
}

void reverse(List * list) {
  Node * reversed = NULL;
  Node * current = list->head;
  Node * temp = NULL;
  while(current != NULL){
    temp = current;
    current = current->next;
    temp->next = reversed;
    reversed = temp;
  }
  list->head = reversed;
}

void destroy(List * list) {
  Node * current = list->head;
  Node * next = current;
  while(current != NULL){
    next = current->next;
    free(current);
    current = next;
  }
  free(list);
}

#endif