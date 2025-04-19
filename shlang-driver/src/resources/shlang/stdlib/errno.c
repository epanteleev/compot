extern int errno;
extern int printf(const char *format, ...);

#define	EPERM		 1	/* Operation not permitted */
#define	ENOENT		 2	/* No such file or directory */

static struct
{
	int errno_value;
	const char *errno_str;
} errno_list[] = {

#define STRINGIFY(x) #x
#define ENTRY(x) {x, &STRINGIFY(undef_ ## x)[6]}
	ENTRY(EPERM),
	ENTRY(ENOENT)
};

int main() {
    printf("errno: %s\n", errno_list[0].errno_str);
    return 0;
}