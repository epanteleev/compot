extern int printf(const char* fmt, ...);

void set_gammas(double** gammas, int n) {
   static double
      init_gammas[]={2.2, 1.0, 2.2/1.45, 1.8, 1.5, 2.4, 2.5, 2.62, 2.9};

   *gammas = init_gammas;
}

int main() {
   double *gammas;
   set_gammas(&gammas, 9);
   printf("gammas: %f %f %f %f %f %f %f %f %f\n", gammas[0], gammas[1], gammas[2],
          gammas[3], gammas[4], gammas[5], gammas[6], gammas[7], gammas[8]);
   return 0;
}