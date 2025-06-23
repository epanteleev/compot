
#define dynArray(T) struct \
{ \
    void *internal; \
    int itemSize; \
    T *data; \
}

int main() {
    dynArray(long) a;
    a.itemSize = 90;
    return a.itemSize;
}
