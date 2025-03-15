#define LUA_MAXUNSIGNED    (~0UL)

int main() {
#if (LUA_MAXUNSIGNED >> 31) >= 3
    int x = 0;
#else
    int x = 1;
#endif
    return x;
}