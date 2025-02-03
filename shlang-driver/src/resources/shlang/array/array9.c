typedef struct
{
    long len, capacity;
} String;

char *getEmptyStr()
{
    String dims = {.len = 0, .capacity = 1};

    static char dimsAndData[sizeof(String) + 1];
    *(String *)dimsAndData = dims;

    char *data = dimsAndData + sizeof(String);
    data[0] = 0;

    return dimsAndData;
}

extern int printf(const char *fmt, ...);

int main()
{
    String* str = (String*)getEmptyStr();
    printf("len: %d, capacity: %d\n", str->len, str->capacity);
    return 0;
}