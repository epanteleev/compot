extern int printf(const char *format, ...);

typedef struct Mat3_
{
   int red_X, red_Y, red_Z;
   int green_X, green_Y, green_Z;
   int blue_X, blue_Y, blue_Z;
} Mat;

typedef struct Holder
{
   Mat mat;
} Holder;

void copy(Holder *dst)
{
   static Mat mat = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
   dst->mat = mat;
}

int main()
{
   Holder h1;
   copy(&h1);
   printf("h1: %d %d %d %d %d %d %d %d %d\n", h1.mat.red_X, h1.mat.red_Y, h1.mat.red_Z,
          h1.mat.green_X, h1.mat.green_Y, h1.mat.green_Z,
          h1.mat.blue_X, h1.mat.blue_Y, h1.mat.blue_Z);
   return 0;
}