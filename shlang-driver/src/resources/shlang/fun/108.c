struct map
{
    int i;
    const char *s;
};

int main()
{
    struct map m[] = {
        {1, "a"},
    };

    return m[0].i;
}