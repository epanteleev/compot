#include <stdlib.h>

typedef struct {
    long useless1;
    long useless2;
    long useless3;
    long data[];
} TestData;

void initTestData(TestData* data) {
    long* ptr = (long*)data;
    ptr[3] = 4;
    ptr[4] = 5;
    ptr[5] = 6;
}

int main() {
    TestData* data = (TestData*)malloc(sizeof(TestData) + 3 * sizeof(long));
    initTestData(data);
    return data->data[0] + data->data[1] + data->data[2];
}