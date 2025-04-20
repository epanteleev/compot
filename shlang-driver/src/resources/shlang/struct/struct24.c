extern int printf(const char *format, ...);

typedef struct Mat3_
{
   int red_X, red_Y, red_Z;
   int green_X, green_Y, green_Z;
   int blue_X, blue_Y, blue_Z;
} Mat;

int main()
{
   Mat m = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
   static Mat mat;
   mat = m;
   printf("h1: %d %d %d %d %d %d %d %d %d\n", mat.red_X, mat.red_Y, mat.red_Z,
          mat.green_X, mat.green_Y, mat.green_Z,
          mat.blue_X, mat.blue_Y, mat.blue_Z);
   return 0;
}