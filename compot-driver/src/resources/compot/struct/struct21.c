#include <stdlib.h>

typedef struct {
    long data[];
} TestData;

void initTestData(TestData* data) {
    long* ptr = (long*)data;
    ptr[0] = 4;
    ptr[0] = 5;
    ptr[0] = 6;
}

int main() {
    TestData* data = (TestData*)malloc(sizeof(TestData) + 3 * sizeof(long));
    initTestData(data);
    return data->data[0] + data->data[1] + data->data[2];
}