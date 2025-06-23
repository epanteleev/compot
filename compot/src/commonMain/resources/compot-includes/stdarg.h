#ifndef _STDARG_H
#define _STDARG_H

// 7.16 Variable arguments <stdarg.h>
// https://port70.net/~nsz/c/c11/n1570.html#7.16

typedef __builtin_va_list va_list;
#define va_start __builtin_va_start
#define va_arg __builtin_va_arg
#define va_copy __builtin_va_copy
#define va_end __builtin_va_end

typedef __builtin_va_list __gnuc_va_list;
#endif /* not _STDARG_H */