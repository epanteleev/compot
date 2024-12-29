// Copyright (c) 2013-2022 Cesanta Software Limited
// All rights reserved
//
// This software is dual-licensed: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License version 3 as
// published by the Free Software Foundation. For the terms of this
// license, see http://www.fsf.org/licensing/licenses/agpl-3.0.html
//
// You are free to use this software under the terms of the GNU General
// Public License, but WITHOUT ANY WARRANTY; without even the implied
// warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the GNU General Public License for more details.
//
// Alternatively, you can license this software under a commercial
// license, please contact us at https://cesanta.com/contact.html

#if defined(__GNUC__) && !defined(JS_OPT) && !defined(ARDUINO_AVR_UNO) && \
    !defined(ARDUINO_AVR_NANO) && !defined(ARDUINO_AVR_PRO) &&            \
    !defined(__APPLE__)
#pragma GCC optimize("O3,inline")
#endif

#include <assert.h>
#include <math.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define JS_VERSION "3.0.0"

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

struct js;                 // JS engine (opaque)
typedef uint64_t jsval_t;  // JS value

struct js *js_create(void *buf, size_t len);         // Create JS instance
jsval_t js_eval(struct js *, const char *, size_t);  // Execute JS code
jsval_t js_glob(struct js *);                        // Return global object
const char *js_str(struct js *, jsval_t val);        // Stringify JS value
bool js_chkargs(jsval_t *, int, const char *);       // Check args validity
bool js_truthy(struct js *, jsval_t);                // Check if value is true
void js_setmaxcss(struct js *, size_t);              // Set max C stack size
void js_setgct(struct js *, size_t);                 // Set GC trigger threshold
void js_stats(struct js *, size_t *total, size_t *min, size_t *cstacksize);
void js_dump(struct js *);  // Print debug info. Requires -DJS_DUMP

// Create JS values from C values
jsval_t js_mkundef(void);  // Create undefined
jsval_t js_mknull(void);   // Create null, null, true, false
jsval_t js_mktrue(void);   // Create true
jsval_t js_mkfalse(void);  // Create false
jsval_t js_mkstr(struct js *, const void *, size_t);           // Create string
jsval_t js_mknum(double);                                      // Create number
jsval_t js_mkerr(struct js *js, const char *fmt, ...);         // Create error
jsval_t js_mkfun(jsval_t (*fn)(struct js *, jsval_t *, int));  // Create func
jsval_t js_mkobj(struct js *);                                 // Create object
void js_set(struct js *, jsval_t, const char *, jsval_t);      // Set obj attr

// Extract C values from JS values
enum { JS_UNDEF, JS_NULL, JS_TRUE, JS_FALSE, JS_STR, JS_NUM, JS_ERR, JS_PRIV };
int js_type(jsval_t val);       // Return JS value type
double js_getnum(jsval_t val);  // Get number
int js_getbool(jsval_t val);    // Get boolean, 0 or 1
char *js_getstr(struct js *js, jsval_t val, size_t *len);  // Get string

#ifdef __cplusplus
}
#endif


#ifndef JS_EXPR_MAX
#define JS_EXPR_MAX 20
#endif

#ifndef JS_GC_THRESHOLD
#define JS_GC_THRESHOLD 0.75
#endif

typedef uint32_t jsoff_t;

struct js {
  jsoff_t css;        // Max observed C stack size
  jsoff_t lwm;        // JS RAM low watermark: min free RAM observed
  const char *code;   // Currently parsed code snippet
  char errmsg[33];    // Error message placeholder
  uint8_t tok;        // Last parsed token value
  uint8_t consumed;   // Indicator that last parsed token was consumed
  uint8_t flags;      // Execution flags, see F_* constants below
#define F_NOEXEC 1U   // Parse code, but not execute
#define F_LOOP 2U     // We're inside the loop
#define F_CALL 4U     // We're inside a function call
#define F_BREAK 8U    // Exit the loop
#define F_RETURN 16U  // Return has been executed
  jsoff_t clen;       // Code snippet length
  jsoff_t pos;        // Current parsing position
  jsoff_t toff;       // Offset of the last parsed token
  jsoff_t tlen;       // Length of the last parsed token
  jsoff_t nogc;       // Entity offset to exclude from GC
  jsval_t tval;       // Holds last parsed numeric or string literal value
  jsval_t scope;      // Current scope
  uint8_t *mem;       // Available JS memory
  jsoff_t size;       // Memory size
  jsoff_t brk;        // Current mem usage boundary
  jsoff_t gct;        // GC threshold. If brk > gct, trigger GC
  jsoff_t maxcss;     // Maximum allowed C stack size usage
  void *cstk;         // C stack pointer at the beginning of js_eval()
};

// A JS memory stores diffenent entities: objects, properties, strings
// All entities are packed to the beginning of a buffer.
// The `brk` marks the end of the used memory:
//
//    | entity1 | entity2| .... |entityN|         unused memory        |
//    |---------|--------|------|-------|------------------------------|
//  js.mem                           js.brk                        js.size
//
//  Each entity is 4-byte aligned, therefore 2 LSB bits store entity type.
//  Object:   8 bytes: offset of the first property, offset of the upper obj
//  Property: 8 bytes + val: 4 byte next prop, 4 byte key offs, N byte value
//  String:   4xN bytes: 4 byte len << 2, 4byte-aligned 0-terminated data
//
// If C functions are imported, they use the upper part of memory as stack for
// passing params. Each argument is pushed to the top of the memory as jsval_t,
// and js.size is decreased by sizeof(jsval_t), i.e. 8 bytes. When function
// returns, js.size is restored back. So js.size is used as a stack pointer.

// clang-format off
enum {
  TOK_ERR, TOK_EOF, TOK_IDENTIFIER, TOK_NUMBER, TOK_STRING, TOK_SEMICOLON,
  TOK_LPAREN, TOK_RPAREN, TOK_LBRACE, TOK_RBRACE,
  // Keyword tokens
  TOK_BREAK = 50, TOK_CASE, TOK_CATCH, TOK_CLASS, TOK_CONST, TOK_CONTINUE,
  TOK_DEFAULT, TOK_DELETE, TOK_DO, TOK_ELSE, TOK_FINALLY, TOK_FOR, TOK_FUNC,
  TOK_IF, TOK_IN, TOK_INSTANCEOF, TOK_LET, TOK_NEW, TOK_RETURN, TOK_SWITCH,
  TOK_THIS, TOK_THROW, TOK_TRY, TOK_VAR, TOK_VOID, TOK_WHILE, TOK_WITH,
  TOK_YIELD, TOK_UNDEF, TOK_NULL, TOK_TRUE, TOK_FALSE,
  // JS Operator tokens
  TOK_DOT = 100, TOK_CALL, TOK_POSTINC, TOK_POSTDEC, TOK_NOT, TOK_TILDA,    // 100
  TOK_TYPEOF, TOK_UPLUS, TOK_UMINUS, TOK_EXP, TOK_MUL, TOK_DIV, TOK_REM,    // 106
  TOK_PLUS, TOK_MINUS, TOK_SHL, TOK_SHR, TOK_ZSHR, TOK_LT, TOK_LE, TOK_GT,  // 113
  TOK_GE, TOK_EQ, TOK_NE, TOK_AND, TOK_XOR, TOK_OR, TOK_LAND, TOK_LOR,      // 121
  TOK_COLON, TOK_Q,  TOK_ASSIGN, TOK_PLUS_ASSIGN, TOK_MINUS_ASSIGN,
  TOK_MUL_ASSIGN, TOK_DIV_ASSIGN, TOK_REM_ASSIGN, TOK_SHL_ASSIGN,
  TOK_SHR_ASSIGN, TOK_ZSHR_ASSIGN, TOK_AND_ASSIGN, TOK_XOR_ASSIGN,
  TOK_OR_ASSIGN, TOK_COMMA,
};

enum {
  // IMPORTANT: T_OBJ, T_PROP, T_STR must go first.  That is required by the
  // memory layout functions: memory entity types are encoded in the 2 bits,
  // thus type values must be 0,1,2,3
  T_OBJ, T_PROP, T_STR, T_UNDEF, T_NULL, T_NUM, T_BOOL, T_FUNC, T_CODEREF,
  T_CFUNC, T_ERR
};

static const char *typestr(uint8_t t) {
  const char *names[] = { "object", "prop", "string", "undefined", "null",
                          "number", "boolean", "function", "coderef",
                          "cfunc", "err", "nan" };
  return (t < sizeof(names) / sizeof(names[0])) ? names[t] : "??";
}

// Pack JS values into uin64_t, double nan, per IEEE 754
// 64bit "double": 1 bit sign, 11 bits exponent, 52 bits mantissa
//
// seeeeeee|eeeemmmm|mmmmmmmm|mmmmmmmm|mmmmmmmm|mmmmmmmm|mmmmmmmm|mmmmmmmm
// 11111111|11110000|00000000|00000000|00000000|00000000|00000000|00000000 inf
// 11111111|11111000|00000000|00000000|00000000|00000000|00000000|00000000 qnan
//
// 11111111|1111tttt|vvvvvvvv|vvvvvvvv|vvvvvvvv|vvvvvvvv|vvvvvvvv|vvvvvvvv
//  NaN marker |type|  48-bit placeholder for values: pointers, strings
//
// On 64-bit platforms, pointers are really 48 bit only, so they can fit,
// provided they are sign extended
static jsval_t tov(double d) { union { double d; jsval_t v; } u = {d}; return u.v; }
static double tod(jsval_t v) { union { jsval_t v; double d; } u = {v}; return u.d; }
static jsval_t mkval(uint8_t type, uint64_t data) { return ((jsval_t) 0x7ff0U << 48U) | ((jsval_t) (type) << 48) | (data & 0xffffffffffffUL); }
static bool is_nan(jsval_t v) { return (v >> 52U) == 0x7ffU; }
static uint8_t vtype(jsval_t v) { return is_nan(v) ? ((v >> 48U) & 15U) : (uint8_t) T_NUM; }
static size_t vdata(jsval_t v) { return (size_t) (v & ~((jsval_t) 0x7fffUL << 48U)); }
static jsval_t mkcoderef(jsval_t off, jsoff_t len) { return mkval(T_CODEREF, (off & 0xffffffU) | ((jsval_t)(len & 0xffffffU) << 24U)); }
static jsoff_t coderefoff(jsval_t v) { return v & 0xffffffU; }
static jsoff_t codereflen(jsval_t v) { return (v >> 24U) & 0xffffffU; }

static uint8_t unhex(uint8_t c) { return (c >= '0' && c <= '9') ? (uint8_t) (c - '0') : (c >= 'a' && c <= 'f') ? (uint8_t) (c - 'W') : (c >= 'A' && c <= 'F') ? (uint8_t) (c - '7') : 0; }
static bool is_space(int c) { return c == ' ' || c == '\r' || c == '\n' || c == '\t' || c == '\f' || c == '\v'; }
static bool is_digit(int c) { return c >= '0' && c <= '9'; }
static bool is_xdigit(int c) { return is_digit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'); }
static bool is_alpha(int c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
static bool is_ident_begin(int c) { return c == '_' || c == '$' || is_alpha(c); }
static bool is_ident_continue(int c) { return c == '_' || c == '$' || is_alpha(c) || is_digit(c); }
static bool is_err(jsval_t v) { return vtype(v) == T_ERR; }
static bool is_unary(uint8_t tok) { return tok >= TOK_POSTINC && tok <= TOK_UMINUS; }
static bool is_assign(uint8_t tok) { return (tok >= TOK_ASSIGN && tok <= TOK_OR_ASSIGN); }
static void saveoff(struct js *js, jsoff_t off, jsoff_t val) { memcpy(&js->mem[off], &val, sizeof(val)); }
static void saveval(struct js *js, jsoff_t off, jsval_t val) { memcpy(&js->mem[off], &val, sizeof(val)); }
static jsoff_t loadoff(struct js *js, jsoff_t off) { jsoff_t v = 0; assert(js->brk <= js->size); memcpy(&v, &js->mem[off], sizeof(v)); return v; }
static jsoff_t offtolen(jsoff_t off) { return (off >> 2) - 1; }
static jsoff_t vstrlen(struct js *js, jsval_t v) { return offtolen(loadoff(js, (jsoff_t) vdata(v))); }
static jsval_t loadval(struct js *js, jsoff_t off) { jsval_t v = 0; memcpy(&v, &js->mem[off], sizeof(v)); return v; }
static jsval_t upper(struct js *js, jsval_t scope) { return mkval(T_OBJ, loadoff(js, (jsoff_t) (vdata(scope) + sizeof(jsoff_t)))); }
static jsoff_t align32(jsoff_t v) { return ((v + 3) >> 2) << 2; }

#define CHECKV(_v) do { if (is_err(_v)) { res = (_v); goto done; } } while (0)
#define EXPECT(_tok, _e) do { if (next(js) != _tok) { _e; return js_mkerr(js, "parse error"); }; js->consumed = 1; } while (0)
// clang-format on

// Forward declarations of the private functions
static size_t tostr(struct js *js, jsval_t value, char *buf, size_t len);
static jsval_t js_expr(struct js *js);
static jsval_t js_stmt(struct js *js);
static jsval_t do_op(struct js *, uint8_t op, jsval_t l, jsval_t r);

static void setlwm(struct js *js) {
  jsoff_t n = 0, css = 0;
  if (js->brk < js->size) n = js->size - js->brk;
  if (js->lwm > n) js->lwm = n;
  if ((char *) js->cstk > (char *) &n)
    css = (jsoff_t) ((char *) js->cstk - (char *) &n);
  if (css > js->css) js->css = css;
}

// Copy src to dst, make no overflows, 0-terminate. Return bytes copied
static size_t cpy(char *dst, size_t dstlen, const char *src, size_t srclen) {
  size_t i = 0;
  for (i = 0; i < dstlen && i < srclen && src[i] != 0; i++) dst[i] = src[i];
  if (dstlen > 0) dst[i < dstlen ? i : dstlen - 1] = '\0';
  return i;
}

// Stringify JS object
static size_t strobj(struct js *js, jsval_t obj, char *buf, size_t len) {
  size_t n = cpy(buf, len, "{", 1);
  jsoff_t next = loadoff(js, (jsoff_t) vdata(obj)) & ~3U;  // First prop offset
  while (next < js->brk && next != 0) {                    // Iterate over props
    jsoff_t koff = loadoff(js, next + (jsoff_t) sizeof(next));
    jsval_t val = loadval(js, next + (jsoff_t) (sizeof(next) + sizeof(koff)));
    // printf("PROP %u, koff %u\n", next & ~3, koff);
    n += cpy(buf + n, len - n, ",", n == 1 ? 0 : 1);
    n += tostr(js, mkval(T_STR, koff), buf + n, len - n);
    n += cpy(buf + n, len - n, ":", 1);
    n += tostr(js, val, buf + n, len - n);
    next = loadoff(js, next) & ~3U;  // Load next prop offset
  }
  return n + cpy(buf + n, len - n, "}", 1);
}

// Stringify numeric JS value
static size_t strnum(jsval_t value, char *buf, size_t len) {
  double dv = tod(value), iv;
  const char *fmt = modf(dv, &iv) == 0.0 ? "%.17g" : "%g";
  return (size_t) snprintf(buf, len, fmt, dv);
}

// Return mem offset and length of the JS string
static jsoff_t vstr(struct js *js, jsval_t value, jsoff_t *len) {
  jsoff_t off = (jsoff_t) vdata(value);
  if (len) *len = offtolen(loadoff(js, off));
  return (jsoff_t) (off + sizeof(off));
}

// Stringify string JS value
static size_t strstring(struct js *js, jsval_t value, char *buf, size_t len) {
  jsoff_t slen, off = vstr(js, value, &slen);
  size_t n = 0;
  n += cpy(buf + n, len - n, "\"", 1);
  n += cpy(buf + n, len - n, (char *) &js->mem[off], slen);
  n += cpy(buf + n, len - n, "\"", 1);
  return n;
}

// Stringify JS function
static size_t strfunc(struct js *js, jsval_t value, char *buf, size_t len) {
  jsoff_t sn, off = vstr(js, value, &sn);
  size_t n = cpy(buf, len, "function", 8);
  return n + cpy(buf + n, len - n, (char *) &js->mem[off], sn);
}

jsval_t js_mkerr(struct js *js, const char *xx, ...) {
  va_list ap;
  size_t n = cpy(js->errmsg, sizeof(js->errmsg), "ERROR: ", 7);
  va_start(ap, xx);
  vsnprintf(js->errmsg + n, sizeof(js->errmsg) - n, xx, ap);
  va_end(ap);
  js->errmsg[sizeof(js->errmsg) - 1] = '\0';
  js->pos = js->clen, js->tok = TOK_EOF, js->consumed = 0;  // Jump to the end
  return mkval(T_ERR, 0);
}

// Stringify JS value into the given buffer
static size_t tostr(struct js *js, jsval_t value, char *buf, size_t len) {
  switch (vtype(value)) {  // clang-format off
    case T_UNDEF: return cpy(buf, len, "undefined", 9);
    case T_NULL:  return cpy(buf, len, "null", 4);
    case T_BOOL:  return cpy(buf, len, vdata(value) & 1 ? "true" : "false", vdata(value) & 1 ? 4 : 5);
    case T_OBJ:   return strobj(js, value, buf, len);
    case T_STR:   return strstring(js, value, buf, len);
    case T_NUM:   return strnum(value, buf, len);
    case T_FUNC:  return strfunc(js, value, buf, len);
    case T_CFUNC: return (size_t) snprintf(buf, len, "\"c_func_0x%lx\"", (unsigned long) vdata(value));
    case T_PROP:  return (size_t) snprintf(buf, len, "PROP@%lu", (unsigned long) vdata(value));
    default:      return (size_t) snprintf(buf, len, "VTYPE%d", vtype(value));
  }  // clang-format on
}

// Stringify JS value into a free JS memory block
const char *js_str(struct js *js, jsval_t value) {
  // Leave jsoff_t placeholder between js->brk and a stringify buffer,
  // in case if next step is convert it into a JS variable
  char *buf = (char *) &js->mem[js->brk + sizeof(jsoff_t)];
  size_t len, available = js->size - js->brk - sizeof(jsoff_t);
  if (is_err(value)) return js->errmsg;
  if (js->brk + sizeof(jsoff_t) >= js->size) return "";
  len = tostr(js, value, buf, available);
  js_mkstr(js, NULL, len);
  return buf;
}

bool js_truthy(struct js *js, jsval_t v) {
  uint8_t t = vtype(v);
  return (t == T_BOOL && vdata(v) != 0) || (t == T_NUM && tod(v) != 0.0) ||
         (t == T_OBJ || t == T_FUNC) || (t == T_STR && vstrlen(js, v) > 0);
}

static jsoff_t js_alloc(struct js *js, size_t size) {
  jsoff_t ofs = js->brk;
  size = align32((jsoff_t) size);  // 4-byte align, (n + k - 1) / k * k
  if (js->brk + size > js->size) return ~(jsoff_t) 0;
  js->brk += (jsoff_t) size;
  return ofs;
}

static jsval_t mkentity(struct js *js, jsoff_t b, const void *buf, size_t len) {
  jsoff_t ofs = js_alloc(js, len + sizeof(b));
  if (ofs == (jsoff_t) ~0) return js_mkerr(js, "oom");
  memcpy(&js->mem[ofs], &b, sizeof(b));
  // Using memmove - in case we're stringifying data from the free JS mem
  if (buf != NULL) memmove(&js->mem[ofs + sizeof(b)], buf, len);
  if ((b & 3) == T_STR) js->mem[ofs + sizeof(b) + len - 1] = 0;  // 0-terminate
  // printf("MKE: %u @ %u type %d\n", js->brk - ofs, ofs, b & 3);
  return mkval(b & 3, ofs);
}

jsval_t js_mkstr(struct js *js, const void *ptr, size_t len) {
  jsoff_t n = (jsoff_t) (len + 1);
  // printf("MKSTR %u %u\n", n, js->brk);
  return mkentity(js, (jsoff_t) ((n << 2) | T_STR), ptr, n);
}

static jsval_t mkobj(struct js *js, jsoff_t parent) {
  return mkentity(js, 0 | T_OBJ, &parent, sizeof(parent));
}

static jsval_t setprop(struct js *js, jsval_t obj, jsval_t k, jsval_t v) {
  jsoff_t koff = (jsoff_t) vdata(k);          // Key offset
  jsoff_t b, head = (jsoff_t) vdata(obj);     // Property list head
  char buf[sizeof(koff) + sizeof(v)];         // Property memory layout
  memcpy(&b, &js->mem[head], sizeof(b));      // Load current 1st prop offset
  memcpy(buf, &koff, sizeof(koff));           // Initialize prop data: copy key
  memcpy(buf + sizeof(koff), &v, sizeof(v));  // Copy value
  jsoff_t brk = js->brk | T_OBJ;              // New prop offset
  memcpy(&js->mem[head], &brk, sizeof(brk));  // Repoint head to the new prop
  // printf("PROP: %u -> %u\n", b, brk);
  return mkentity(js, (b & ~3U) | T_PROP, buf, sizeof(buf));  // Create new prop
}

// Return T_OBJ/T_PROP/T_STR entity size based on the first word in memory
static inline jsoff_t esize(jsoff_t w) {
  switch (w & 3U) {  // clang-format off
    case T_OBJ:   return (jsoff_t) (sizeof(jsoff_t) + sizeof(jsoff_t));
    case T_PROP:  return (jsoff_t) (sizeof(jsoff_t) + sizeof(jsoff_t) + sizeof(jsval_t));
    case T_STR:   return (jsoff_t) (sizeof(jsoff_t) + align32(w >> 2U));
    default:      return (jsoff_t) ~0U;
  }  // clang-format on
}

static bool is_mem_entity(uint8_t t) {
  return t == T_OBJ || t == T_PROP || t == T_STR || t == T_FUNC;
}

#define GCMASK ~(((jsoff_t) ~0) >> 1)  // Entity deletion marker
static void js_fixup_offsets(struct js *js, jsoff_t start, jsoff_t size) {
  for (jsoff_t n, v, off = 0; off < js->brk; off += n) {  // start from 0!
    v = loadoff(js, off);
    n = esize(v & ~GCMASK);
    if (v & GCMASK) continue;  // To be deleted, don't bother
    if ((v & 3) != T_OBJ && (v & 3) != T_PROP) continue;
    if (v > start) saveoff(js, off, v - size);
    if ((v & 3) == T_OBJ) {
      jsoff_t u = loadoff(js, (jsoff_t) (off + sizeof(jsoff_t)));
      if (u > start) saveoff(js, (jsoff_t) (off + sizeof(jsoff_t)), u - size);
    }
    if ((v & 3) == T_PROP) {
      jsoff_t koff = loadoff(js, (jsoff_t) (off + sizeof(off)));
      if (koff > start) saveoff(js, (jsoff_t) (off + sizeof(off)), koff - size);
      jsval_t val = loadval(js, (jsoff_t) (off + sizeof(off) + sizeof(off)));
      if (is_mem_entity(vtype(val)) && vdata(val) > start) {
        saveval(js, (jsoff_t) (off + sizeof(off) + sizeof(off)),
                mkval(vtype(val), (unsigned long) (vdata(val) - size)));
      }
    }
  }
  // Fixup js->scope
  jsoff_t off = (jsoff_t) vdata(js->scope);
  if (off > start) js->scope = mkval(T_OBJ, off - size);
  if (js->nogc >= start) js->nogc -= size;
  // Fixup code that we're executing now, if required
  if (js->code > (char *) js->mem && js->code - (char *) js->mem < js->size &&
      js->code - (char *) js->mem > start) {
    js->code -= size;
    // printf("GC-ing code under us!! %ld\n", js->code - (char *) js->mem);
  }
  // printf("FIXEDOFF %u %u\n", start, size);
}

static void js_delete_marked_entities(struct js *js) {
  for (jsoff_t n, v, off = 0; off < js->brk; off += n) {
    v = loadoff(js, off);
    n = esize(v & ~GCMASK);
    if (v & GCMASK) {  // This entity is marked for deletion, remove it
      // printf("DEL: %4u %d %x\n", off, v & 3, n);
      // assert(off + n <= js->brk);
      js_fixup_offsets(js, off, n);
      memmove(&js->mem[off], &js->mem[off + n], js->brk - off - n);
      js->brk -= n;  // Shrink brk boundary by the size of deleted entity
      n = 0;         // We shifted data, next iteration stay on this offset
    }
  }
}

static void js_mark_all_entities_for_deletion(struct js *js) {
  for (jsoff_t v, off = 0; off < js->brk; off += esize(v)) {
    v = loadoff(js, off);
    saveoff(js, off, v | GCMASK);
  }
}

static jsoff_t js_unmark_entity(struct js *js, jsoff_t off) {
  jsoff_t v = loadoff(js, off);
  if (v & GCMASK) {
    saveoff(js, off, v & ~GCMASK);
    // printf("UNMARK %5u %d\n", off, v & 3);
    if ((v & 3) == T_OBJ) js_unmark_entity(js, v & ~(GCMASK | 3));
    if ((v & 3) == T_PROP) {
      js_unmark_entity(js, v & ~(GCMASK | 3));  // Unmark next prop
      js_unmark_entity(js, loadoff(js, (jsoff_t) (off + sizeof(off))));  // key
      jsval_t val = loadval(js, (jsoff_t) (off + sizeof(off) + sizeof(off)));
      if (is_mem_entity(vtype(val))) js_unmark_entity(js, (jsoff_t) vdata(val));
    }
  }
  return v & ~(GCMASK | 3U);
}

static void js_unmark_used_entities(struct js *js) {
  jsval_t scope = js->scope;
  do {
    js_unmark_entity(js, (jsoff_t) vdata(scope));
    scope = upper(js, scope);
  } while (vdata(scope) != 0);  // When global scope is GC-ed, stop
  if (js->nogc) js_unmark_entity(js, js->nogc);
  // printf("UNMARK: nogc %u\n", js->nogc);
  // js_dump(js);
}

void js_gc(struct js *js) {
  // printf("================== GC %u\n", js->nogc);
  setlwm(js);
  if (js->nogc == (jsoff_t) ~0) return;  // ~0 is a special case: GC Is disabled
  js_mark_all_entities_for_deletion(js);
  js_unmark_used_entities(js);
  js_delete_marked_entities(js);
}

// Skip whitespaces and comments
static jsoff_t skiptonext(const char *code, jsoff_t len, jsoff_t n) {
  // printf("SKIP: [%.*s]\n", len - n, &code[n]);
  while (n < len) {
    if (is_space(code[n])) {
      n++;
    } else if (n + 1 < len && code[n] == '/' && code[n + 1] == '/') {
      for (n += 2; n < len && code[n] != '\n';) n++;
    } else if (n + 3 < len && code[n] == '/' && code[n + 1] == '*') {
      for (n += 4; n < len && (code[n - 2] != '*' || code[n - 1] != '/');) n++;
    } else {
      break;
    }
  }
  return n;
}

static bool streq(const char *buf, size_t len, const char *p, size_t n) {
  return n == len && memcmp(buf, p, len) == 0;
}

static uint8_t parsekeyword(const char *buf, size_t len) {
  switch (buf[0]) {  // clang-format off
    case 'b': if (streq("break", 5, buf, len)) return TOK_BREAK; break;
    case 'c': if (streq("class", 5, buf, len)) return TOK_CLASS; if (streq("case", 4, buf, len)) return TOK_CASE; if (streq("catch", 5, buf, len)) return TOK_CATCH; if (streq("const", 5, buf, len)) return TOK_CONST; if (streq("continue", 8, buf, len)) return TOK_CONTINUE; break;
    case 'd': if (streq("do", 2, buf, len)) return TOK_DO;  if (streq("default", 7, buf, len)) return TOK_DEFAULT; break; // if (streq("delete", 6, buf, len)) return TOK_DELETE; break;
    case 'e': if (streq("else", 4, buf, len)) return TOK_ELSE; break;
    case 'f': if (streq("for", 3, buf, len)) return TOK_FOR; if (streq("function", 8, buf, len)) return TOK_FUNC; if (streq("finally", 7, buf, len)) return TOK_FINALLY; if (streq("false", 5, buf, len)) return TOK_FALSE; break;
    case 'i': if (streq("if", 2, buf, len)) return TOK_IF; if (streq("in", 2, buf, len)) return TOK_IN; if (streq("instanceof", 10, buf, len)) return TOK_INSTANCEOF; break;
    case 'l': if (streq("let", 3, buf, len)) return TOK_LET; break;
    case 'n': if (streq("new", 3, buf, len)) return TOK_NEW; if (streq("null", 4, buf, len)) return TOK_NULL; break;
    case 'r': if (streq("return", 6, buf, len)) return TOK_RETURN; break;
    case 's': if (streq("switch", 6, buf, len)) return TOK_SWITCH; break;
    case 't': if (streq("try", 3, buf, len)) return TOK_TRY; if (streq("this", 4, buf, len)) return TOK_THIS; if (streq("throw", 5, buf, len)) return TOK_THROW; if (streq("true", 4, buf, len)) return TOK_TRUE; if (streq("typeof", 6, buf, len)) return TOK_TYPEOF; break;
    case 'u': if (streq("undefined", 9, buf, len)) return TOK_UNDEF; break;
    case 'v': if (streq("var", 3, buf, len)) return TOK_VAR; if (streq("void", 4, buf, len)) return TOK_VOID; break;
    case 'w': if (streq("while", 5, buf, len)) return TOK_WHILE; if (streq("with", 4, buf, len)) return TOK_WITH; break;
    case 'y': if (streq("yield", 5, buf, len)) return TOK_YIELD; break;
  }  // clang-format on
  return TOK_IDENTIFIER;
}

static uint8_t parseident(const char *buf, jsoff_t len, jsoff_t *tlen) {
  if (is_ident_begin(buf[0])) {
    while (*tlen < len && is_ident_continue(buf[*tlen])) (*tlen)++;
    return parsekeyword(buf, *tlen);
  }
  return TOK_ERR;
}

static uint8_t next(struct js *js) {
  if (js->consumed == 0) return js->tok;
  js->consumed = 0;
  js->tok = TOK_ERR;
  js->toff = js->pos = skiptonext(js->code, js->clen, js->pos);
  js->tlen = 0;
  const char *buf = js->code + js->toff;
  // clang-format off
  if (js->toff >= js->clen) { js->tok = TOK_EOF; return js->tok; }
#define TOK(T, LEN) { js->tok = T; js->tlen = (LEN); break; }
#define LOOK(OFS, CH) js->toff + OFS < js->clen && buf[OFS] == CH
  switch (buf[0]) {
    case '?': TOK(TOK_Q, 1);
    case ':': TOK(TOK_COLON, 1);
    case '(': TOK(TOK_LPAREN, 1);
    case ')': TOK(TOK_RPAREN, 1);
    case '{': TOK(TOK_LBRACE, 1);
    case '}': TOK(TOK_RBRACE, 1);
    case ';': TOK(TOK_SEMICOLON, 1);
    case ',': TOK(TOK_COMMA, 1);
    case '!': if (LOOK(1, '=') && LOOK(2, '=')) TOK(TOK_NE, 3); TOK(TOK_NOT, 1);
    case '.': TOK(TOK_DOT, 1);
    case '~': TOK(TOK_TILDA, 1);
    case '-': if (LOOK(1, '-')) TOK(TOK_POSTDEC, 2); if (LOOK(1, '=')) TOK(TOK_MINUS_ASSIGN, 2); TOK(TOK_MINUS, 1);
    case '+': if (LOOK(1, '+')) TOK(TOK_POSTINC, 2); if (LOOK(1, '=')) TOK(TOK_PLUS_ASSIGN, 2); TOK(TOK_PLUS, 1);
    case '*': if (LOOK(1, '*')) TOK(TOK_EXP, 2); if (LOOK(1, '=')) TOK(TOK_MUL_ASSIGN, 2); TOK(TOK_MUL, 1);
    case '/': if (LOOK(1, '=')) TOK(TOK_DIV_ASSIGN, 2); TOK(TOK_DIV, 1);
    case '%': if (LOOK(1, '=')) TOK(TOK_REM_ASSIGN, 2); TOK(TOK_REM, 1);
    case '&': if (LOOK(1, '&')) TOK(TOK_LAND, 2); if (LOOK(1, '=')) TOK(TOK_AND_ASSIGN, 2); TOK(TOK_AND, 1);
    case '|': if (LOOK(1, '|')) TOK(TOK_LOR, 2); if (LOOK(1, '=')) TOK(TOK_OR_ASSIGN, 2); TOK(TOK_OR, 1);
    case '=': if (LOOK(1, '=') && LOOK(2, '=')) TOK(TOK_EQ, 3); TOK(TOK_ASSIGN, 1);
    case '<': if (LOOK(1, '<') && LOOK(2, '=')) TOK(TOK_SHL_ASSIGN, 3); if (LOOK(1, '<')) TOK(TOK_SHL, 2); if (LOOK(1, '=')) TOK(TOK_LE, 2); TOK(TOK_LT, 1);
    case '>': if (LOOK(1, '>') && LOOK(2, '=')) TOK(TOK_SHR_ASSIGN, 3); if (LOOK(1, '>')) TOK(TOK_SHR, 2); if (LOOK(1, '=')) TOK(TOK_GE, 2); TOK(TOK_GT, 1);
    case '^': if (LOOK(1, '=')) TOK(TOK_XOR_ASSIGN, 2); TOK(TOK_XOR, 1);
    case '"': case '\'':
      js->tlen++;
      while (js->toff + js->tlen < js->clen && buf[js->tlen] != buf[0]) {
        uint8_t increment = 1;
        if (buf[js->tlen] == '\\') {
          if (js->toff + js->tlen + 2 > js->clen) break;
          increment = 2;
          if (buf[js->tlen + 1] == 'x') {
            if (js->toff + js->tlen + 4 > js->clen) break;
            increment = 4;
          }
        }
        js->tlen += increment;
      }
      if (buf[0] == buf[js->tlen]) js->tok = TOK_STRING, js->tlen++;
      break;
    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': {
      char *end;
      js->tval = tov(strtod(buf, &end)); // TODO(lsm): protect against OOB access
      TOK(TOK_NUMBER, (jsoff_t) (end - buf));
    }
    default: js->tok = parseident(buf, js->clen - js->toff, &js->tlen); break;
  }  // clang-format on
  js->pos = js->toff + js->tlen;
  // printf("NEXT: %d %d [%.*s]\n", js->tok, js->pos, (int) js->tlen, buf);
  return js->tok;
}

static inline uint8_t lookahead(struct js *js) {
  uint8_t old = js->tok, tok = 0;
  jsoff_t pos = js->pos;
  js->consumed = 1;
  tok = next(js);
  js->pos = pos, js->tok = old;
  return tok;
}

static void mkscope(struct js *js) {
  assert((js->flags & F_NOEXEC) == 0);
  jsoff_t prev = (jsoff_t) vdata(js->scope);
  js->scope = mkobj(js, prev);
  // printf("ENTER SCOPE %u, prev %u\n", (jsoff_t) vdata(js->scope), prev);
}

static void delscope(struct js *js) {
  js->scope = upper(js, js->scope);
  // printf("EXIT  SCOPE %u\n", (jsoff_t) vdata(js->scope));
}

static jsval_t js_block(struct js *js, bool create_scope) {
  jsval_t res = js_mkundef();
  if (create_scope) mkscope(js);  // Enter new scope
  js->consumed = 1;
  // jsoff_t pos = js->pos;
  while (next(js) != TOK_EOF && next(js) != TOK_RBRACE && !is_err(res)) {
    uint8_t t = js->tok;
    res = js_stmt(js);
    if (!is_err(res) && t != TOK_LBRACE && t != TOK_IF && t != TOK_WHILE &&
        js->tok != TOK_SEMICOLON) {
      res = js_mkerr(js, "; expected");
      break;
    }
  }
  // printf("BLOCKEND %s\n", js_str(js, res));
  if (create_scope) delscope(js);  // Exit scope
  return res;
}

// Seach for property in a single object
static jsoff_t lkp(struct js *js, jsval_t obj, const char *buf, size_t len) {
  jsoff_t off = loadoff(js, (jsoff_t) vdata(obj)) & ~3U;  // Load first prop off
  // printf("LKP: %lu %u [%.*s]\n", vdata(obj), off, (int) len, buf);
  while (off < js->brk && off != 0) {  // Iterate over props
    jsoff_t koff = loadoff(js, (jsoff_t) (off + sizeof(off)));
    jsoff_t klen = (loadoff(js, koff) >> 2) - 1;
    const char *p = (char *) &js->mem[koff + sizeof(koff)];
    // printf("  %u %u[%.*s]\n", off, (int) klen, (int) klen, p);
    if (streq(buf, len, p, klen)) return off;  // Found !
    off = loadoff(js, off) & ~3U;              // Load next prop offset
  }
  return 0;  // Not found
}

// Lookup variable in the scope chain
static jsval_t lookup(struct js *js, const char *buf, size_t len) {
  if (js->flags & F_NOEXEC) return 0;
  for (jsval_t scope = js->scope;;) {
    jsoff_t off = lkp(js, scope, buf, len);
    if (off != 0) return mkval(T_PROP, off);
    if (vdata(scope) == 0) break;
    scope =
        mkval(T_OBJ, loadoff(js, (jsoff_t) (vdata(scope) + sizeof(jsoff_t))));
  }
  return js_mkerr(js, "'%.*s' not found", (int) len, buf);
}

static jsval_t resolveprop(struct js *js, jsval_t v) {
  if (vtype(v) != T_PROP) return v;
  return resolveprop(js,
                     loadval(js, (jsoff_t) (vdata(v) + sizeof(jsoff_t) * 2)));
}

static jsval_t assign(struct js *js, jsval_t lhs, jsval_t val) {
  saveval(js, (jsoff_t) ((vdata(lhs) & ~3U) + sizeof(jsoff_t) * 2), val);
  return lhs;
}

static jsval_t do_assign_op(struct js *js, uint8_t op, jsval_t l, jsval_t r) {
  uint8_t m[] = {TOK_PLUS, TOK_MINUS, TOK_MUL, TOK_DIV, TOK_REM, TOK_SHL,
                 TOK_SHR,  TOK_ZSHR,  TOK_AND, TOK_XOR, TOK_OR};
  jsval_t res = do_op(js, m[op - TOK_PLUS_ASSIGN], resolveprop(js, l), r);
  return assign(js, l, res);
}

static jsval_t do_string_op(struct js *js, uint8_t op, jsval_t l, jsval_t r) {
  jsoff_t n1, off1 = vstr(js, l, &n1);
  jsoff_t n2, off2 = vstr(js, r, &n2);
  if (op == TOK_PLUS) {
    jsval_t res = js_mkstr(js, NULL, n1 + n2);
    // printf("STRPLUS %u %u %u %u [%.*s] [%.*s]\n", n1, off1, n2, off2, (int)
    // n1,
    //       &js->mem[off1], (int) n2, &js->mem[off2]);
    if (vtype(res) == T_STR) {
      jsoff_t n, off = vstr(js, res, &n);
      memmove(&js->mem[off], &js->mem[off1], n1);
      memmove(&js->mem[off + n1], &js->mem[off2], n2);
    }
    return res;
  } else if (op == TOK_EQ) {
    bool eq = n1 == n2 && memcmp(&js->mem[off1], &js->mem[off2], n1) == 0;
    return mkval(T_BOOL, eq ? 1 : 0);
  } else if (op == TOK_NE) {
    bool eq = n1 == n2 && memcmp(&js->mem[off1], &js->mem[off2], n1) == 0;
    return mkval(T_BOOL, eq ? 0 : 1);
  } else {
    return js_mkerr(js, "bad str op");
  }
}

static jsval_t do_dot_op(struct js *js, jsval_t l, jsval_t r) {
  const char *ptr = (char *) &js->code[coderefoff(r)];
  if (vtype(r) != T_CODEREF) return js_mkerr(js, "ident expected");
  // Handle stringvalue.length
  if (vtype(l) == T_STR && streq(ptr, codereflen(r), "length", 6)) {
    return tov(offtolen(loadoff(js, (jsoff_t) vdata(l))));
  }
  if (vtype(l) != T_OBJ) return js_mkerr(js, "lookup in non-obj");
  jsoff_t off = lkp(js, l, ptr, codereflen(r));
  return off == 0 ? js_mkundef() : mkval(T_PROP, off);
}

static jsval_t js_call_params(struct js *js) {
  jsoff_t pos = js->pos;
  uint8_t flags = js->flags;
  js->flags |= F_NOEXEC;
  js->consumed = 1;
  for (bool comma = false; next(js) != TOK_EOF; comma = true) {
    if (!comma && next(js) == TOK_RPAREN) break;
    js_expr(js);
    if (next(js) == TOK_RPAREN) break;
    EXPECT(TOK_COMMA, js->flags = flags);
  }
  EXPECT(TOK_RPAREN, js->flags = flags);
  js->flags = flags;
  return mkcoderef(pos, js->pos - pos - js->tlen);
}

static void reverse(jsval_t *args, int nargs) {
  for (int i = 0; i < nargs / 2; i++) {
    jsval_t tmp = args[i];
    args[i] = args[nargs - i - 1], args[nargs - i - 1] = tmp;
  }
}

// Call native C function
static jsval_t call_c(struct js *js,
                      jsval_t (*fn)(struct js *, jsval_t *, int)) {
  int argc = 0;
  while (js->pos < js->clen) {
    if (next(js) == TOK_RPAREN) break;
    jsval_t arg = resolveprop(js, js_expr(js));
    if (js->brk + sizeof(arg) > js->size) return js_mkerr(js, "call oom");
    js->size -= (jsoff_t) sizeof(arg);
    memcpy(&js->mem[js->size], &arg, sizeof(arg));
    argc++;
    // printf("  arg %d -> %s\n", argc, js_str(js, arg));
    if (next(js) == TOK_COMMA) js->consumed = 1;
  }
  reverse((jsval_t *) &js->mem[js->size], argc);
  jsval_t res = fn(js, (jsval_t *) &js->mem[js->size], argc);
  setlwm(js);
  js->size += (jsoff_t) sizeof(jsval_t) * (jsoff_t) argc;  // Restore stack
  return res;
}

// Call JS function. 'fn' looks like this: "(a,b) { return a + b; }"
static jsval_t call_js(struct js *js, const char *fn, jsoff_t fnlen) {
  jsoff_t fnpos = 1;
  // printf("JSCALL [%.*s] -> %.*s\n", (int) js->clen, js->code, (int) fnlen,
  // fn);
  // printf("JSCALL, nogc %u [%.*s]\n", js->nogc, (int) fnlen, fn);
  mkscope(js);  // Create function call scope
  // Loop over arguments list "(a, b)" and set scope variables
  while (fnpos < fnlen) {
    fnpos = skiptonext(fn, fnlen, fnpos);          // Skip to the identifier
    if (fnpos < fnlen && fn[fnpos] == ')') break;  // Closing paren? break!
    jsoff_t identlen = 0;                          // Identifier length
    uint8_t tok = parseident(&fn[fnpos], fnlen - fnpos, &identlen);
    if (tok != TOK_IDENTIFIER) break;
    // Here we have argument name. Calculate arg value
    // printf("  [%.*s] -> %u [%.*s] -> ", (int) identlen, &fn[fnpos], js->pos,
    //       (int) js->clen, js->code);
    js->pos = skiptonext(js->code, js->clen, js->pos);
    js->consumed = 1;
    jsval_t v = js->code[js->pos] == ')' ? js_mkundef() : js_expr(js);
    // Set argument in the function scope
    setprop(js, js->scope, js_mkstr(js, &fn[fnpos], identlen), v);
    js->pos = skiptonext(js->code, js->clen, js->pos);
    if (js->pos < js->clen && js->code[js->pos] == ',') js->pos++;
    fnpos = skiptonext(fn, fnlen, fnpos + identlen);  // Skip past identifier
    if (fnpos < fnlen && fn[fnpos] == ',') fnpos++;   // And skip comma
  }
  if (fnpos < fnlen && fn[fnpos] == ')') fnpos++;  // Skip to the function body
  fnpos = skiptonext(fn, fnlen, fnpos);            // Up to the opening brace
  if (fnpos < fnlen && fn[fnpos] == '{') fnpos++;  // And skip the brace
  size_t n = fnlen - fnpos - 1U;  // Function code with stripped braces
  // printf("flags: %d, body: %zu [%.*s]\n", js->flags, n, (int) n, &fn[fnpos]);
  js->flags = F_CALL;                        // Mark we're in the function call
  jsval_t res = js_eval(js, &fn[fnpos], n);  // Call function, no GC
  if (!is_err(res) && !(js->flags & F_RETURN)) res = js_mkundef();  // No return
  delscope(js);  // Delete call scope
  // printf("  -> %d [%s], tok %d\n", js->flags, js_str(js, res), js->tok);
  return res;
}

static jsval_t do_call_op(struct js *js, jsval_t func, jsval_t args) {
  if (vtype(args) != T_CODEREF) return js_mkerr(js, "bad call");
  if (vtype(func) != T_FUNC && vtype(func) != T_CFUNC)
    return js_mkerr(js, "calling non-function");
  const char *code = js->code;             // Save current parser state
  jsoff_t clen = js->clen, pos = js->pos;  // code, position and code length
  js->code = &js->code[coderefoff(args)];  // Point parser to args
  js->clen = codereflen(args);             // Set args length
  js->pos = skiptonext(js->code, js->clen, 0);  // Skip to 1st arg
  uint8_t tok = js->tok, flags = js->flags;     // Save flags
  jsoff_t nogc = js->nogc;
  jsval_t res = js_mkundef();
  if (vtype(func) == T_FUNC) {
    jsoff_t fnlen, fnoff = vstr(js, func, &fnlen);
    js->nogc = (jsoff_t) (fnoff - sizeof(jsoff_t));
    res = call_js(js, (const char *) (&js->mem[fnoff]), fnlen);
  } else {
    res = call_c(js, (jsval_t(*)(struct js *, jsval_t *, int)) vdata(func));
  }
  js->code = code, js->clen = clen, js->pos = pos;  // Restore parser
  js->flags = flags, js->tok = tok, js->nogc = nogc;
  js->consumed = 1;
  return res;
}

// clang-format off
static jsval_t do_op(struct js *js, uint8_t op, jsval_t lhs, jsval_t rhs) {
  if (js->flags & F_NOEXEC) return 0;
  jsval_t l = resolveprop(js, lhs), r = resolveprop(js, rhs);
  // printf("OP %d %d %d\n", op, vtype(lhs), vtype(r));
  setlwm(js);
  if (is_err(l)) return l;
  if (is_err(r)) return r;
  if (is_assign(op) && vtype(lhs) != T_PROP) return js_mkerr(js, "bad lhs");
  switch (op) {
    case TOK_TYPEOF:  return js_mkstr(js, typestr(vtype(r)), strlen(typestr(vtype(r))));
    case TOK_CALL:    return do_call_op(js, l, r);
    case TOK_ASSIGN:  return assign(js, lhs, r);
    case TOK_POSTINC: { do_assign_op(js, TOK_PLUS_ASSIGN, lhs, tov(1)); return l; }
    case TOK_POSTDEC: { do_assign_op(js, TOK_MINUS_ASSIGN, lhs, tov(1)); return l; }
    case TOK_NOT:     if (vtype(r) == T_BOOL) return mkval(T_BOOL, !vdata(r)); break;
  }
  if (is_assign(op))    return do_assign_op(js, op, lhs, r);
  if (vtype(l) == T_STR && vtype(r) == T_STR) return do_string_op(js, op, l, r);
  if (is_unary(op) && vtype(r) != T_NUM) return js_mkerr(js, "type mismatch");
  if (!is_unary(op) && op != TOK_DOT && (vtype(l) != T_NUM || vtype(r) != T_NUM)) return js_mkerr(js, "type mismatch");
  double a = tod(l), b = tod(r);
  switch (op) {
    //case TOK_EXP:     return tov(pow(a, b));
    case TOK_DIV:     return tod(r) == 0 ? js_mkerr(js, "div by zero") : tov(a / b);
    case TOK_REM:     return tov(a - b * ((double) (long) (a / b)));
    case TOK_MUL:     return tov(a * b);
    case TOK_PLUS:    return tov(a + b);
    case TOK_MINUS:   return tov(a - b);
    case TOK_XOR:     return tov((double)((long) a ^ (long) b));
    case TOK_AND:     return tov((double)((long) a & (long) b));
    case TOK_OR:      return tov((double)((long) a | (long) b));
    case TOK_UMINUS:  return tov(-b);
    case TOK_UPLUS:   return r;
    case TOK_TILDA:   return tov((double)(~(long) b));
    case TOK_NOT:     return mkval(T_BOOL, b == 0);
    case TOK_SHL:     return tov((double)((long) a << (long) b));
    case TOK_SHR:     return tov((double)((long) a >> (long) b));
    case TOK_DOT:     return do_dot_op(js, l, r);
    case TOK_EQ:      return mkval(T_BOOL, (long) a == (long) b);
    case TOK_NE:      return mkval(T_BOOL, (long) a != (long) b);
    case TOK_LT:      return mkval(T_BOOL, a < b);
    case TOK_LE:      return mkval(T_BOOL, a <= b);
    case TOK_GT:      return mkval(T_BOOL, a > b);
    case TOK_GE:      return mkval(T_BOOL, a >= b);
    default:          return js_mkerr(js, "unknown op %d", (int) op);  // LCOV_EXCL_LINE
  }
}  // clang-format on

static jsval_t js_str_literal(struct js *js) {
  uint8_t *in = (uint8_t *) &js->code[js->toff];
  uint8_t *out = &js->mem[js->brk + sizeof(jsoff_t)];
  size_t n1 = 0, n2 = 0;
  // printf("STR %u %lu %lu\n", js->brk, js->tlen, js->clen);
  if (js->brk + sizeof(jsoff_t) + js->tlen > js->size)
    return js_mkerr(js, "oom");
  while (n2++ + 2 < js->tlen) {
    if (in[n2] == '\\') {
      if (in[n2 + 1] == in[0]) {
        out[n1++] = in[0];
      } else if (in[n2 + 1] == 'n') {
        out[n1++] = '\n';
      } else if (in[n2 + 1] == 't') {
        out[n1++] = '\t';
      } else if (in[n2 + 1] == 'r') {
        out[n1++] = '\r';
      } else if (in[n2 + 1] == 'x' && is_xdigit(in[n2 + 2]) &&
                 is_xdigit(in[n2 + 3])) {
        out[n1++] = (uint8_t) ((unhex(in[n2 + 2]) << 4U) | unhex(in[n2 + 3]));
        n2 += 2;
      } else {
        return js_mkerr(js, "bad str literal");
      }
      n2++;
    } else {
      out[n1++] = ((uint8_t *) js->code)[js->toff + n2];
    }
  }
  return js_mkstr(js, NULL, n1);
}

static jsval_t js_obj_literal(struct js *js) {
  uint8_t exe = !(js->flags & F_NOEXEC);
  // printf("OLIT1\n");
  jsval_t obj = exe ? mkobj(js, 0) : js_mkundef();
  if (is_err(obj)) return obj;
  js->consumed = 1;
  while (next(js) != TOK_RBRACE) {
    jsval_t key = 0;
    if (js->tok == TOK_IDENTIFIER) {
      if (exe) key = js_mkstr(js, js->code + js->toff, js->tlen);
    } else if (js->tok == TOK_STRING) {
      if (exe) key = js_str_literal(js);
    } else {
      return js_mkerr(js, "parse error");
    }
    js->consumed = 1;
    EXPECT(TOK_COLON, );
    jsval_t val = js_expr(js);
    if (exe) {
      // printf("XXXX [%s] scope: %lu\n", js_str(js, val), vdata(js->scope));
      if (is_err(val)) return val;
      if (is_err(key)) return key;
      jsval_t res = setprop(js, obj, key, resolveprop(js, val));
      if (is_err(res)) return res;
    }
    if (next(js) == TOK_RBRACE) break;
    EXPECT(TOK_COMMA, );
  }
  EXPECT(TOK_RBRACE, );
  return obj;
}

static jsval_t js_func_literal(struct js *js) {
  uint8_t flags = js->flags;  // Save current flags
  js->consumed = 1;
  EXPECT(TOK_LPAREN, js->flags = flags);
  jsoff_t pos = js->pos - 1;
  for (bool comma = false; next(js) != TOK_EOF; comma = true) {
    if (!comma && next(js) == TOK_RPAREN) break;
    EXPECT(TOK_IDENTIFIER, js->flags = flags);
    if (next(js) == TOK_RPAREN) break;
    EXPECT(TOK_COMMA, js->flags = flags);
  }
  EXPECT(TOK_RPAREN, js->flags = flags);
  EXPECT(TOK_LBRACE, js->flags = flags);
  js->consumed = 0;
  js->flags |= F_NOEXEC;              // Set no-execution flag to parse the
  jsval_t res = js_block(js, false);  // Skip function body - no exec
  if (is_err(res)) {                  // But fail short on parse error
    js->flags = flags;
    return res;
  }
  js->flags = flags;  // Restore flags
  jsval_t str = js_mkstr(js, &js->code[pos], js->pos - pos);
  js->consumed = 1;
  // printf("FUNC: %u [%.*s]\n", pos, js->pos - pos, &js->code[pos]);
  return mkval(T_FUNC, (unsigned long) vdata(str));
}

#define RTL_BINOP(_f1, _f2, _cond)  \
  jsval_t res = _f1(js);            \
  while (!is_err(res) && (_cond)) { \
    uint8_t op = js->tok;           \
    js->consumed = 1;               \
    jsval_t rhs = _f2(js);          \
    if (is_err(rhs)) return rhs;    \
    res = do_op(js, op, res, rhs);  \
  }                                 \
  return res;

#define LTR_BINOP(_f, _cond)        \
  jsval_t res = _f(js);             \
  while (!is_err(res) && (_cond)) { \
    uint8_t op = js->tok;           \
    js->consumed = 1;               \
    jsval_t rhs = _f(js);           \
    if (is_err(rhs)) return rhs;    \
    res = do_op(js, op, res, rhs);  \
  }                                 \
  return res;

static jsval_t js_literal(struct js *js) {
  next(js);
  setlwm(js);
  // printf("css : %u\n", js->css);
  if (js->maxcss > 0 && js->css > js->maxcss) return js_mkerr(js, "C stack");
  js->consumed = 1;
  switch (js->tok) {  // clang-format off
    case TOK_ERR:         return js_mkerr(js, "parse error");
    case TOK_NUMBER:      return js->tval;
    case TOK_STRING:      return js_str_literal(js);
    case TOK_LBRACE:      return js_obj_literal(js);
    case TOK_FUNC:        return js_func_literal(js);
    case TOK_NULL:        return js_mknull();
    case TOK_UNDEF:       return js_mkundef();
    case TOK_TRUE:        return js_mktrue();
    case TOK_FALSE:       return js_mkfalse();
    case TOK_IDENTIFIER:  return mkcoderef((jsoff_t) js->toff, (jsoff_t) js->tlen);
    default:              return js_mkerr(js, "bad expr");
  }  // clang-format on
}

static jsval_t js_group(struct js *js) {
  if (next(js) == TOK_LPAREN) {
    js->consumed = 1;
    jsval_t v = js_expr(js);
    if (is_err(v)) return v;
    if (next(js) != TOK_RPAREN) return js_mkerr(js, ") expected");
    js->consumed = 1;
    return v;
  } else {
    return js_literal(js);
  }
}

static jsval_t js_call_dot(struct js *js) {
  jsval_t res = js_group(js);
  if (is_err(res)) return res;
  if (vtype(res) == T_CODEREF) {
    res = lookup(js, &js->code[coderefoff(res)], codereflen(res));
  }
  while (next(js) == TOK_LPAREN || next(js) == TOK_DOT) {
    if (js->tok == TOK_DOT) {
      js->consumed = 1;
      res = do_op(js, TOK_DOT, res, js_group(js));
    } else {
      jsval_t params = js_call_params(js);
      if (is_err(params)) return params;
      res = do_op(js, TOK_CALL, res, params);
    }
  }
  return res;
}

static jsval_t js_postfix(struct js *js) {
  jsval_t res = js_call_dot(js);
  if (is_err(res)) return res;
  next(js);
  if (js->tok == TOK_POSTINC || js->tok == TOK_POSTDEC) {
    js->consumed = 1;
    res = do_op(js, js->tok, res, 0);
  }
  return res;
}

static jsval_t js_unary(struct js *js) {
  if (next(js) == TOK_NOT || js->tok == TOK_TILDA || js->tok == TOK_TYPEOF ||
      js->tok == TOK_MINUS || js->tok == TOK_PLUS) {
    uint8_t t = js->tok;
    if (t == TOK_MINUS) t = TOK_UMINUS;
    if (t == TOK_PLUS) t = TOK_UPLUS;
    js->consumed = 1;
    return do_op(js, t, js_mkundef(), js_unary(js));
  } else {
    return js_postfix(js);
  }
}

static jsval_t js_mul_div_rem(struct js *js) {
  LTR_BINOP(js_unary,
            (next(js) == TOK_MUL || js->tok == TOK_DIV || js->tok == TOK_REM));
}

static jsval_t js_plus_minus(struct js *js) {
  LTR_BINOP(js_mul_div_rem, (next(js) == TOK_PLUS || js->tok == TOK_MINUS));
}

static jsval_t js_shifts(struct js *js) {
  LTR_BINOP(js_plus_minus, (next(js) == TOK_SHR || next(js) == TOK_SHL ||
                            next(js) == TOK_ZSHR));
}

static jsval_t js_comparison(struct js *js) {
  LTR_BINOP(js_shifts, (next(js) == TOK_LT || next(js) == TOK_LE ||
                        next(js) == TOK_GT || next(js) == TOK_GE));
}

static jsval_t js_equality(struct js *js) {
  LTR_BINOP(js_comparison, (next(js) == TOK_EQ || next(js) == TOK_NE));
}

static jsval_t js_bitwise_and(struct js *js) {
  LTR_BINOP(js_equality, (next(js) == TOK_AND));
}

static jsval_t js_bitwise_xor(struct js *js) {
  LTR_BINOP(js_bitwise_and, (next(js) == TOK_XOR));
}

static jsval_t js_bitwise_or(struct js *js) {
  LTR_BINOP(js_bitwise_xor, (next(js) == TOK_OR));
}

static jsval_t js_logical_and(struct js *js) {
  jsval_t res = js_bitwise_or(js);
  if (is_err(res)) return res;
  uint8_t flags = js->flags;
  while (next(js) == TOK_LAND) {
    js->consumed = 1;
    res = resolveprop(js, res);
    if (!js_truthy(js, res)) js->flags |= F_NOEXEC;  // false && ... shortcut
    if (js->flags & F_NOEXEC) {
      js_logical_and(js);
    } else {
      res = js_logical_and(js);
    }
  }
  js->flags = flags;
  return res;
}

static jsval_t js_logical_or(struct js *js) {
  jsval_t res = js_logical_and(js);
  if (is_err(res)) return res;
  uint8_t flags = js->flags;
  while (next(js) == TOK_LOR) {
    js->consumed = 1;
    res = resolveprop(js, res);
    if (js_truthy(js, res)) js->flags |= F_NOEXEC;  // true || ... shortcut
    if (js->flags & F_NOEXEC) {
      js_logical_or(js);
    } else {
      res = js_logical_or(js);
    }
  }
  js->flags = flags;
  return res;
}

static jsval_t js_ternary(struct js *js) {
  jsval_t res = js_logical_or(js);
  if (next(js) == TOK_Q) {
    uint8_t flags = js->flags;
    js->consumed = 1;
    if (js_truthy(js, resolveprop(js, res))) {
      res = js_ternary(js);
      js->flags |= F_NOEXEC;
      EXPECT(TOK_COLON, js->flags = flags);
      js_ternary(js);
      js->flags = flags;
    } else {
      js->flags |= F_NOEXEC;
      js_ternary(js);
      EXPECT(TOK_COLON, js->flags = flags);
      js->flags = flags;
      res = js_ternary(js);
    }
  }
  return res;
}

static jsval_t js_assignment(struct js *js) {
  RTL_BINOP(js_ternary, js_assignment,
            (next(js) == TOK_ASSIGN || js->tok == TOK_PLUS_ASSIGN ||
             js->tok == TOK_MINUS_ASSIGN || js->tok == TOK_MUL_ASSIGN ||
             js->tok == TOK_DIV_ASSIGN || js->tok == TOK_REM_ASSIGN ||
             js->tok == TOK_SHL_ASSIGN || js->tok == TOK_SHR_ASSIGN ||
             js->tok == TOK_ZSHR_ASSIGN || js->tok == TOK_AND_ASSIGN ||
             js->tok == TOK_XOR_ASSIGN || js->tok == TOK_OR_ASSIGN));
}

static jsval_t js_expr(struct js *js) {
  return js_assignment(js);
}

static jsval_t js_let(struct js *js) {
  uint8_t exe = !(js->flags & F_NOEXEC);
  js->consumed = 1;
  for (;;) {
    EXPECT(TOK_IDENTIFIER, );
    js->consumed = 0;
    jsoff_t noff = js->toff, nlen = js->tlen;
    char *name = (char *) &js->code[noff];
    jsval_t v = js_mkundef();
    js->consumed = 1;
    if (next(js) == TOK_ASSIGN) {
      js->consumed = 1;
      v = js_expr(js);
      if (is_err(v)) return v;  // Propagate error if any
    }
    if (exe) {
      if (lkp(js, js->scope, name, nlen) > 0)
        return js_mkerr(js, "'%.*s' already declared", (int) nlen, name);
      jsval_t x =
          setprop(js, js->scope, js_mkstr(js, name, nlen), resolveprop(js, v));
      if (is_err(x)) return x;
    }
    if (next(js) == TOK_SEMICOLON || next(js) == TOK_EOF) break;  // Stop
    EXPECT(TOK_COMMA, );
  }
  return js_mkundef();
}

static jsval_t js_block_or_stmt(struct js *js) {
  if (next(js) == TOK_LBRACE) return js_block(js, !(js->flags & F_NOEXEC));
  jsval_t res = resolveprop(js, js_stmt(js));
  js->consumed = 0;  //
  return res;
}

static jsval_t js_if(struct js *js) {
  js->consumed = 1;
  EXPECT(TOK_LPAREN, );
  jsval_t res = js_mkundef(), cond = resolveprop(js, js_expr(js));
  EXPECT(TOK_RPAREN, );
  bool cond_true = js_truthy(js, cond), exe = !(js->flags & F_NOEXEC);
  // printf("IF COND: %s, true? %d\n", js_str(js, cond), cond_true);
  if (!cond_true) js->flags |= F_NOEXEC;
  jsval_t blk = js_block_or_stmt(js);
  if (cond_true) res = blk;
  if (exe && !cond_true) js->flags &= (uint8_t) ~F_NOEXEC;
  if (lookahead(js) == TOK_ELSE) {
    js->consumed = 1;
    next(js);
    js->consumed = 1;
    if (cond_true) js->flags |= F_NOEXEC;
    blk = js_block_or_stmt(js);
    if (!cond_true) res = blk;
    if (cond_true && exe) js->flags &= (uint8_t) ~F_NOEXEC;
  }
  return res;
}

static inline bool expect(struct js *js, uint8_t tok, jsval_t *res) {
  if (next(js) != tok) {
    *res = js_mkerr(js, "parse error");
    return false;
  } else {
    js->consumed = 1;
    return true;
  }
}

static inline bool is_err2(jsval_t *v, jsval_t *res) {
  bool r = is_err(*v);
  if (r) *res = *v;
  return r;
}

static jsval_t js_for(struct js *js) {
  uint8_t flags = js->flags, exe = !(flags & F_NOEXEC);
  jsval_t v, res = js_mkundef();
  jsoff_t pos1 = 0, pos2 = 0, pos3 = 0, pos4 = 0;
  if (exe) mkscope(js);  // Enter new scope
  if (!expect(js, TOK_FOR, &res)) goto done;
  if (!expect(js, TOK_LPAREN, &res)) goto done;

  if (next(js) == TOK_SEMICOLON) {  // initialisation
  } else if (next(js) == TOK_LET) {
    v = js_let(js);
    if (is_err2(&v, &res)) goto done;
  } else {
    v = js_expr(js);
    if (is_err2(&v, &res)) goto done;
  }
  if (!expect(js, TOK_SEMICOLON, &res)) goto done;
  js->flags |= F_NOEXEC;
  pos1 = js->pos;  // condition
  if (next(js) != TOK_SEMICOLON) {
    v = js_expr(js);
    if (is_err2(&v, &res)) goto done;
  }
  if (!expect(js, TOK_SEMICOLON, &res)) goto done;
  pos2 = js->pos;  // final expr
  if (next(js) != TOK_RPAREN) {
    v = js_expr(js);
    if (is_err2(&v, &res)) goto done;
  }
  if (!expect(js, TOK_RPAREN, &res)) goto done;
  pos3 = js->pos;  // body
  v = js_block_or_stmt(js);
  if (is_err2(&v, &res)) goto done;
  pos4 = js->pos;  // end of body
  while (!(flags & F_NOEXEC)) {
    js->flags = flags, js->pos = pos1, js->consumed = 1;
    if (next(js) != TOK_SEMICOLON) {     // Is condition specified?
      v = resolveprop(js, js_expr(js));  // Yes. check condition
      if (is_err2(&v, &res)) goto done;  // Fail short on error
      if (!js_truthy(js, v)) break;      // Exit the loop if condition is false
    }
    js->pos = pos3, js->consumed = 1, js->flags |= F_LOOP;  // Execute the
    v = js_block_or_stmt(js);                               // loop body
    if (is_err2(&v, &res)) goto done;                       // Fail on error
    if (js->flags & F_BREAK) break;  // break was executed - exit the loop!
    js->flags = flags, js->pos = pos2, js->consumed = 1;  // Jump to final expr
    if (next(js) != TOK_RPAREN) {                         // Is it specified?
      v = js_expr(js);                                    // Yes. Execute it
      if (is_err2(&v, &res)) goto done;  // On error, fail short
    }
  }
  js->pos = pos4, js->tok = TOK_SEMICOLON, js->consumed = 0;
done:
  if (exe) delscope(js);  // Exit scope
  js->flags = flags;      // Restore flags
  return res;
}

static jsval_t js_break(struct js *js) {
  if (js->flags & F_NOEXEC) {
  } else {
    if (!(js->flags & F_LOOP)) return js_mkerr(js, "not in loop");
    js->flags |= F_BREAK | F_NOEXEC;
  }
  js->consumed = 1;
  return js_mkundef();
}

static jsval_t js_continue(struct js *js) {
  if (js->flags & F_NOEXEC) {
  } else {
    if (!(js->flags & F_LOOP)) return js_mkerr(js, "not in loop");
    js->flags |= F_NOEXEC;
  }
  js->consumed = 1;
  return js_mkundef();
}

static jsval_t js_return(struct js *js) {
  uint8_t exe = !(js->flags & F_NOEXEC);
  js->consumed = 1;
  if (exe && !(js->flags & F_CALL)) return js_mkerr(js, "not in func");
  if (next(js) == TOK_SEMICOLON) return js_mkundef();
  jsval_t res = resolveprop(js, js_expr(js));
  if (exe) {
    js->pos = js->clen;     // Shift to the end - exit the code snippet
    js->flags |= F_RETURN;  // Tell caller we've executed
  }
  return resolveprop(js, res);
}

static jsval_t js_stmt(struct js *js) {
  jsval_t res;
  // jsoff_t pos = js->pos - js->tlen;
  if (js->brk > js->gct) js_gc(js);
  switch (next(js)) {  // clang-format off
    case TOK_CASE: case TOK_CATCH: case TOK_CLASS: case TOK_CONST:
    case TOK_DEFAULT: case TOK_DELETE: case TOK_DO: case TOK_FINALLY:
    case TOK_IN: case TOK_INSTANCEOF: case TOK_NEW: case TOK_SWITCH:
    case TOK_THIS: case TOK_THROW: case TOK_TRY: case TOK_VAR: case TOK_VOID:
    case TOK_WITH: case TOK_WHILE: case TOK_YIELD:
      res = js_mkerr(js, "'%.*s' not implemented", (int) js->tlen, js->code + js->toff);
      break;
    case TOK_CONTINUE:  res = js_continue(js); break;
    case TOK_BREAK:     res = js_break(js); break;
    case TOK_LET:       res = js_let(js); break;
    case TOK_IF:        res = js_if(js); break;
    case TOK_LBRACE:    res = js_block(js, !(js->flags & F_NOEXEC)); break;
    case TOK_FOR:       res = js_for(js); break; // 25222 -> 27660
    case TOK_RETURN:    res = js_return(js); break;
    default:            res = resolveprop(js, js_expr(js)); break;
  }
  //printf("STMT [%.*s] -> %s, tok %d, flags %d\n", (int) (js->pos - pos), &js->code[pos], js_str(js, res), next(js), js->flags);
  if (next(js) != TOK_SEMICOLON && next(js) != TOK_EOF && next(js) != TOK_RBRACE) return js_mkerr(js, "; expected");
  js->consumed = 1;
  // clang-format on
  return res;
}

struct js *js_create(void *buf, size_t len) {
  struct js *js = NULL;
  if (len < sizeof(*js) + esize(T_OBJ)) return js;
  memset(buf, 0, len);                       // Important!
  js = (struct js *) buf;                    // struct js lives at the beginning
  js->mem = (uint8_t *) (js + 1);            // Then goes memory for JS data
  js->size = (jsoff_t) (len - sizeof(*js));  // JS memory size
  js->scope = mkobj(js, 0);                  // Create global scope
  js->size = js->size / 8U * 8U;             // Align js->size by 8 byte
  js->lwm = js->size;                        // Initial LWM: 100% free
  js->gct = js->size / 2;
  return js;
}

// clang-format off
void js_setgct(struct js *js, size_t gct) { js->gct = (jsoff_t) gct; }
void js_setmaxcss(struct js *js, size_t max) { js->maxcss = (jsoff_t) max; }
jsval_t js_mktrue(void) { return mkval(T_BOOL, 1); }
jsval_t js_mkfalse(void) { return mkval(T_BOOL, 0); }
jsval_t js_mkundef(void) { return mkval(T_UNDEF, 0); }
jsval_t js_mknull(void) { return mkval(T_NULL, 0); }
jsval_t js_mknum(double value) { return tov(value); }
jsval_t js_mkobj(struct js *js) { return mkobj(js, 0); }
jsval_t js_mkfun(jsval_t (*fn)(struct js *, jsval_t *, int)) { return mkval(T_CFUNC, (size_t) (void *) fn); }
double js_getnum(jsval_t value) { return tod(value); }
int js_getbool(jsval_t value) { return vdata(value) & 1 ? 1 : 0; }

jsval_t js_glob(struct js *js) { (void) js; return mkval(T_OBJ, 0); }

void js_set(struct js *js, jsval_t obj, const char *key, jsval_t val) {
  if (vtype(obj) == T_OBJ) setprop(js, obj, js_mkstr(js, key, strlen(key)), val);
}

char *js_getstr(struct js *js, jsval_t value, size_t *len) {
  if (vtype(value) != T_STR) return NULL;
  jsoff_t n, off = vstr(js, value, &n);
  if (len != NULL) *len = n;
  return (char *) &js->mem[off];
}

int js_type(jsval_t val) {
  switch (vtype(val)) {
    case T_UNDEF:   return JS_UNDEF;
    case T_NULL:    return JS_NULL;
    case T_BOOL:    return vdata(val) == 0 ? JS_FALSE: JS_TRUE;
    case T_STR:     return JS_STR;
    case T_NUM:     return JS_NUM;
    case T_ERR:     return JS_ERR;
    default:        return JS_PRIV;
  }
}
void js_stats(struct js *js, size_t *total, size_t *lwm, size_t *css) {
  if (total) *total = js->size;
  if (lwm) *lwm = js->lwm;
  if (css) *css = js->css;
}
// clang-format on

bool js_chkargs(jsval_t *args, int nargs, const char *spec) {
  int i = 0, ok = 1;
  for (; ok && i < nargs && spec[i]; i++) {
    uint8_t t = vtype(args[i]), c = (uint8_t) spec[i];
    ok = (c == 'b' && t == T_BOOL) || (c == 'd' && t == T_NUM) ||
         (c == 's' && t == T_STR) || (c == 'j');
  }
  if (spec[i] != '\0' || i != nargs) ok = 0;
  return ok;
}

jsval_t js_eval(struct js *js, const char *buf, size_t len) {
  // printf("EVAL: [%.*s]\n", (int) len, buf);
  jsval_t res = js_mkundef();
  if (len == (size_t) ~0U) len = strlen(buf);
  js->consumed = 1;
  js->tok = TOK_ERR;
  js->code = buf;
  js->clen = (jsoff_t) len;
  js->pos = 0;
  js->cstk = &res;
  while (next(js) != TOK_EOF && !is_err(res)) {
    res = js_stmt(js);
  }
  return res;
}

#ifdef JS_DUMP
void js_dump(struct js *js) {
  jsoff_t off = 0, v;
  printf("JS size %u, brk %u, lwm %u, css %u, nogc %u\n", js->size, js->brk,
         js->lwm, (unsigned) js->css, js->nogc);
  while (off < js->brk) {
    memcpy(&v, &js->mem[off], sizeof(v));
    printf(" %5u: ", off);
    if ((v & 3U) == T_OBJ) {
      printf("OBJ %u %u\n", v & ~3U,
             loadoff(js, (jsoff_t) (off + sizeof(off))));
    } else if ((v & 3U) == T_PROP) {
      jsoff_t koff = loadoff(js, (jsoff_t) (off + sizeof(v)));
      jsval_t val = loadval(js, (jsoff_t) (off + sizeof(v) + sizeof(v)));
      printf("PROP next %u, koff %u vtype %d vdata %lu\n", v & ~3U, koff,
             vtype(val), (unsigned long) vdata(val));
    } else if ((v & 3) == T_STR) {
      jsoff_t len = offtolen(v);
      printf("STR %u [%.*s]\n", len, (int) len, js->mem + off + sizeof(v));
    } else {
      printf("???\n");
      break;
    }
    off += esize(v);
  }
}
#endif

#include <time.h>
#define JS_DUMP

static bool ev(struct js *js, const char *expr, const char *expectation) {
  const char *result = js_str(js, js_eval(js, expr, strlen(expr)));
  bool correct = strcmp(result, expectation) == 0;
  if (!correct) printf("[%s] -> [%s] [%s]\n", expr, result, expectation);
  return correct;
}

static void test_arith(void) {
  char mem[200];
  struct js *js;
  assert((js = js_create(NULL, 0)) == NULL);
  assert((js = js_create(mem, 0)) == NULL);
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(ev(js, "", "undefined"));
  assert(ev(js, "1.23", "1.23"));
  assert(ev(js, "3 + 4", "7"));
  assert(ev(js, " + 1", "1"));
  assert(ev(js, "+ + 1", "1"));
  assert(ev(js, "+ + + 1", "1"));
  assert(ev(js, "1 + + + 1", "2"));
  assert(ev(js, "-1.23", "-1.23"));
  assert(ev(js, "1/2/4", "0.125"));
  assert(ev(js, "1.23 + 2.1 * 3.7 - 2.5", "6.5"));
  assert(ev(js, "2 * (3 + 4)", "14"));
  assert(ev(js, "2 * (3 + 4 * (2 +5))", "62"));
  assert(ev(js, "5.5 % 2", "1.5"));
  assert(ev(js, "5%2", "1"));
  assert(ev(js, "5 % - 2", "1"));
  assert(ev(js, "-5 % 2", "-1"));
  assert(ev(js, "- 5 % 2", "-1"));
  assert(ev(js, " - 5 % - 2", "-1"));
  assert(ev(js, "24 / 3 % 2", "0"));
  assert(ev(js, "4 / 5 % 3", "0.8"));
  assert(ev(js, "1 + 4 / 5 % 3", "1.8"));
  assert(ev(js, "7^9", "14"));
  assert(ev(js, "1+2*3+4*5+6", "33"));
  assert(ev(js, "1+2*3+4/5+6", "13.8"));
  assert(ev(js, "1+2*3+4/5%3+6", "13.8"));
  assert(ev(js, "1 - - - 2", "-1"));
  assert(ev(js, "1 + + + 2", "3"));
  assert(ev(js, "~5", "-6"));
  assert(ev(js, "6 / - - 2", "3"));
  assert(ev(js, "7+~5", "1"));
  assert(ev(js, "5/3", "1.66667"));
  assert(ev(js, "0x64", "100"));
#ifndef JS32
  assert(ev(js, "0x7fffffff", "2147483647"));
  assert(ev(js, "0xffffffff", "4294967295"));
#endif
  assert(ev(js, "100 << 3", "800"));
  assert(ev(js, "(0-14) >> 2", "-4"));
  assert(ev(js, "6 & 3", "2"));
  assert(ev(js, "6 | 3", "7"));
  assert(ev(js, "6 ^ 3", "5"));
  assert(ev(js, "0.1 + 0.2", "0.3"));
  assert(ev(js, "123.4 + 0.1", "123.5"));
  // assert(ev(js, "2**3", "8"));
  // assert(ev(js, "1.2**3.4", "1.85873"));
}

static void test_errors(void) {
  char mem[200];
  struct js *js;
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  js_setmaxcss(js, 5000);
  assert(ev(js, "1+(((((((((((((((((1)))))))))))))))))", "ERROR: C stack"));
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(ev(js, "+", "ERROR: bad expr"));
  assert(ev(js, "2+", "ERROR: bad expr"));
  assert(ev(js, "2 * * 2", "ERROR: bad expr"));
  assert(ev(js, "1 2", "ERROR: ; expected"));
  assert(ev(js, "1 2 + 3", "ERROR: ; expected"));
  assert(ev(js, "1 + 2 3", "ERROR: ; expected"));
  assert(ev(js, "1 2 + 3 4", "ERROR: ; expected"));
  assert(ev(js, "1 + 2 3 * 5", "ERROR: ; expected"));
  assert(ev(js, "1 + 2 3 * 5 + 6", "ERROR: ; expected"));

  assert(ev(js, "switch", "ERROR: 'switch' not implemented"));
  assert(ev(js, "with", "ERROR: 'with' not implemented"));
  assert(ev(js, "try", "ERROR: 'try' not implemented"));
  assert(ev(js, "class", "ERROR: 'class' not implemented"));
  assert(ev(js, "const x", "ERROR: 'const' not implemented"));
  assert(ev(js, "var x", "ERROR: 'var' not implemented"));

  assert(ev(js, "1 + yield", "ERROR: bad expr"));
  assert(ev(js, "yield", "ERROR: 'yield' not implemented"));
  assert(ev(js, "@", "ERROR: parse error"));
  assert(ev(js, "$", "ERROR: '$' not found"));
  assert(ev(js, "let a=0; a+=x;a", "ERROR: 'x' not found"));
}

static void test_basic(void) {
  struct js *js;
  char mem[sizeof(*js) + 350];
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(ev(js, "null", "null"));
  assert(ev(js, "null", "null"));
  assert(ev(js, "undefined", "undefined"));
  assert(ev(js, "true", "true"));
  assert(ev(js, "false", "false"));
  assert(ev(js, "({})", "{}"));
  assert(ev(js, "({a:1})", "{\"a\":1}"));
  assert(ev(js, "({a:1,b:true})", "{\"b\":true,\"a\":1}"));
  assert(ev(js, "({a:1,b:{c:2}})", "{\"b\":{\"c\":2},\"a\":1}"));
  js_gc(js);
  assert(js->brk < 100);

  assert(ev(js, "1;2", "2"));
  assert(ev(js, "1;2;", "2"));
  assert(ev(js, "let a ;", "undefined"));
  assert(ev(js, "{let a,}", "ERROR: parse error"));
  assert(ev(js, "let ;", "ERROR: parse error"));
  assert(ev(js, "{let a 2}", "ERROR: parse error"));
  assert(ev(js, "let a = 123;", "ERROR: 'a' already declared"));
  assert(ev(js, "let b = 123; 1; b", "123"));
  assert(ev(js, "let c = 2, d = 3; c", "2"));
  assert(ev(js, "1 = 7", "ERROR: bad lhs"));
  assert(ev(js, "a = 7", "7"));
  assert(ev(js, "a", "7"));
  assert(ev(js, "d = 1+2-3", "0"));
  assert(ev(js, "1 + d = 3", "ERROR: bad lhs"));
  assert(ev(js, "a = {b:2}", "{\"b\":2}"));
  assert(ev(js, "a", "{\"b\":2}"));
  assert(ev(js, "a.b", "2"));
  assert(ev(js, "({a:3}).a", "3"));
  assert(ev(js, "({\"a\":4})", "{\"a\":4}"));
  assert(ev(js, "a.b = {c:3}", "{\"c\":3}"));
  assert(ev(js, "a", "{\"b\":{\"c\":3}}"));
  assert(ev(js, "a.b.c", "3"));
  assert(ev(js, "a.b.c.", "ERROR: bad expr"));
  assert(ev(js, "a=1;1;", "1"));
  assert(ev(js, "a+=1;a;", "2"));
  assert(ev(js, "a-=3;a;", "-1"));
  assert(ev(js, "a*=8;a;", "-8"));
  assert(ev(js, "a/=2;a;", "-4"));
  assert(ev(js, "a%=3;a;", "-1"));
  assert(ev(js, "a^=5;a;", "-6"));
  assert(ev(js, "a>>=2;a;", "-2"));
  assert(ev(js, "a=3;a<<=2;a;", "12"));
  assert(ev(js, "a=b=7", "7"));
  assert(ev(js, "a", "7"));
  assert(ev(js, "a+", "ERROR: bad expr"));
  assert(ev(js, "a++", "7"));
  assert(ev(js, "a", "8"));
  assert(ev(js, "a--; a", "7"));
  assert(ev(js, "b", "7"));
  assert(ev(js, "~null", "ERROR: type mismatch"));
  assert(ev(js, "1 + ''", "ERROR: type mismatch"));
  assert(ev(js, "1 + true", "ERROR: type mismatch"));
  assert(ev(js, "1 === false", "ERROR: type mismatch"));
  assert(ev(js, "1 === 2", "false"));
  assert(ev(js, "13 + 4 === 17", "true"));
  assert(ev(js, "let o = {a: 1}; o.a += 1; o;", "{\"a\":2}"));

  assert(ev(js, "a= 0; 2 * (3 + a++)", "6"));
  assert(ev(js, "a", "1"));
  assert(ev(js, "a = 0; a++", "0"));
  assert(ev(js, "a = 0; a++ - a++", "-1"));
  assert(ev(js, ",", "ERROR: bad expr"));
  assert(ev(js, "a = 0; 1 + a++ + 2", "3"));
  assert(ev(js, "a", "1"));
  assert(ev(js, "a = 0; 3 * (1 + a++ + (2 + a++))", "12"));

  assert(ev(js, "1+2;", "3"));
  assert(ev(js, "1+2; ", "3"));
  assert(ev(js, "1+2;//9", "3"));
  assert(ev(js, "1+2;//", "3"));
  assert(ev(js, "1/**/+2;//9", "3"));
  assert(ev(js, "1/**/+2;/**///9", "3"));
  assert(ev(js, "1/**/+ /* some comment*/2;/**///9", "3"));
  assert(ev(js, "1/**/+ /* */2;/**///9", "3"));
  assert(ev(js, "1/**/+ /* \n*/2;/**///9", "3"));
  assert(ev(js, "1 + /* * */ 2;", "3"));
  assert(ev(js, "1 + /* **/ 2;", "3"));
  assert(ev(js, "1 + /* ///**/ 2;", "3"));
  assert(ev(js, "1 + /*\n//*/ 2;", "3"));
  assert(ev(js, "1 + /*\n//\n*/ 2;", "3"));

  assert(ev(js, "b = 2; a = {x:1,y:b};", "{\"y\":2,\"x\":1}"));

  assert(ev(js, "a=5;a;", "5"));
  assert(ev(js, "a&=3;a;", "1"));
  assert(ev(js, "a|=3;a;", "3"));
  assert(ev(js, "(((((((1)))))))", "1"));
  assert(ev(js, "1+(((((((1)))))))", "2"));

  size_t a = 0, b = 0, c = 0;
  js_stats(js, &a, &b, &c);
  assert(a > 0 && b > 0 && c > 0);
}

static void test_memory(void) {
  char mem[sizeof(struct js) + 8];  // Not enough memory to create an object
  struct js *js;
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(ev(js, "({a:1})", "ERROR: oom"));  // OOM
}

static void test_strings(void) {
  struct js *js;
  char mem[sizeof(*js) + 200];
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(ev(js, "''", "\"\""));
  assert(ev(js, "\"\"", "\"\""));
  assert(ev(js, "'foo'", "\"foo\""));
  assert(ev(js, "'foo\\'", "ERROR: parse error"));
  assert(ev(js, "'foo\\q", "ERROR: parse error"));
  assert(ev(js, "'f\\x", "ERROR: parse error"));
  assert(ev(js, "'f\\xx", "ERROR: parse error"));
  assert(ev(js, "'f\\xxx", "ERROR: parse error"));
  assert(ev(js, "'foo\\q'", "ERROR: bad str literal"));
  assert(ev(js, "'f\\xrr'", "ERROR: bad str literal"));
  assert(ev(js, "'f\\x61'", "\"fa\""));
  assert(ev(js, "'x\\x61\\t\\r\\n\\''", "\"xa\t\r\n'\""));
  assert(ev(js, "'a'+'b'", "\"ab\""));
  assert(ev(js, "'hi'+' ' + 'there'", "\"hi there\""));
  assert(ev(js, "'a' == 'b'", "ERROR: bad expr"));
  assert(ev(js, "'a' === 'b'", "false"));
  assert(ev(js, "'a' !== 'b'", "true"));
  assert(ev(js, "let a = 'b'; a === 'b'", "true"));
  assert(ev(js, "let b = 'c'; b === 'c'", "true"));
  assert(ev(js, "a === b", "false"));
  assert(ev(js, "a = b = 'hi'", "\"hi\""));
  assert(ev(js, "a", "\"hi\""));
  assert(ev(js, "b", "\"hi\""));
  assert(ev(js, "a = b = 1", "1"));
  assert(ev(js, "'x' * 'y'", "ERROR: bad str op"));
  assert(ev(js, "'aa'.foo", "ERROR: lookup in non-obj"));
  assert(ev(js, "'aa'.length", "2"));
  assert(ev(js, "'Київ'.length", "8"));
  assert(ev(js, "({a:'ї'}).a.length", "2"));
  assert(ev(js, "a=true;a", "true"));
  assert(ev(js, "a=!a;a", "false"));
  assert(ev(js, "!123", "false"));
  assert(ev(js, "!0", "true"));
  assert(ev(js, "let r=''; r+='x'; r+='y'; r", "\"xy\""));
  assert(ev(js, "let i=0;r=''; for(;i<2;) { r+='x'; i++; } r", "\"xx\""));
}

static void test_flow(void) {
  struct js *js;
  char mem[sizeof(*js) + 300];
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(ev(js, "let a = 1; a", "1"));
  assert(ev(js, "if (true) a++; a", "2"));
  assert(ev(js, "if (a) a++; a", "3"));
  assert(ev(js, "if (!a) a++; a", "3"));
  assert(ev(js, "a=0;if (a) a++; a", "0"));
  assert(ev(js, "if ('') a--; a", "0"));
  assert(ev(js, "a=2; for(;a;)a--; a", "0"));
  assert(ev(js, "a=2", "2"));
  assert(ev(js, "if (1) 2;", "2"));
  assert(ev(js, "if (0) 2;", "undefined"));
  assert(ev(js, "if (0) { a = 7; a++; }", "undefined"));
  assert(ev(js, "a", "2"));
  assert(ev(js, "if (1) {}", "undefined"));
  assert(ev(js, "if ('boo') { a = 7; a++; }", "7"));
  assert(ev(js, "a", "8"));
  assert(ev(js, "while (false)break;", "ERROR: 'while' not implemented"));
  assert(ev(js, "break;", "ERROR: not in loop"));
  assert(ev(js, "continue;", "ERROR: not in loop"));
  assert(ev(js, "let b = 0; for (;b < 10;) {b++; a--;} a;", "-2"));
  assert(ev(js, "b = 0; for (;b++ < 10;) a += 3;  a;", "28"));
  assert(ev(js, "b = 0; for (;;) break; ", "undefined"));
  assert(ev(js, "b = 0; for (;true;) break; ", "undefined"));
  assert(ev(js, "b = 0; for (;;) break; b", "0"));
  assert(ev(js, "a", "28"));
  assert(ev(js, "b = 0; for (;;) if (a-- < 10) break;", "undefined"));
  assert(ev(js, "a", "8"));
  assert(ev(js, "b = 0; for (;;) if (b++ > 10) break; b;", "12"));
  assert(ev(js, "a = b = 0; for (;b++ < 10;) for (;a < b;) a++; a", "10"));
  assert(ev(js, "a = 0; for (;;) { if (a++ < 10) continue; break;} a", "11"));
  assert(ev(js, "a=b=0; for (;b++<10;) {true;a++;} a", "10"));
  assert(ev(js, "a=b=0; if (false) b++; else b--; b", "-1"));
  assert(ev(js, "a=b=0; if (false) {b++;} else {b--;} b", "-1"));
  assert(ev(js, "a=b=0; if (false) {2;b++;} else {2;b--;} b", "-1"));
  assert(ev(js, "a=b=0; if (true) b++; else b--; b", "1"));
  assert(ev(js, "a=b=0; if (true) {2;b++;} else {2;b--;} b", "1"));
  assert(ev(js, "a=0; if (1) a=1; else if (0) a=2; a;", "1"));
  assert(ev(js, "a=0; if (0) a=1; else if (1) a=2; a;", "2"));
  assert(ev(js, "a=0; if (0){7;a=1;}else if (1){7;a=2;} a;", "2"));
  assert(ev(js, "a=0; if(0){7;a=1;}else if(0){5;a=2;}else{3;a=3;} a;", "3"));
  assert(ev(js, "a=0; for (let i=0;i<5;i++) a++; a", "5"));
  assert(ev(js, "a=0; for (let i=0;x;i++) a++; a", "ERROR: 'x' not found"));
  assert(ev(js, "a=0; for (x;1;i++) a++; a", "ERROR: 'x' not found"));
  assert(ev(js, "a=0; for (1;1;x) a++; a", "ERROR: 'x' not found"));
  assert(ev(js, "a=0; for (1 1;1;1) a++; a", "ERROR: parse error"));
  assert(ev(js, "a=0; for (1;1 1;1) a++; a", "ERROR: parse error"));
  assert(ev(js, "a=0; for (1;1;1 1) a++; a", "ERROR: parse error"));
  assert(ev(js, "a=0; for (;;) break; a", "0"));
  assert(ev(js, "a=0; for (;;) {a++; break;} a", "1"));
  assert(ev(js, "a=0; for (;a<100;) {a++; continue;} a", "100"));
  assert(ev(js, "a=0; for (let i=0;i<10;i++) { continue; a++;} a", "0"));
  assert(ev(js, "a=0; for (let i=0;i++<2;i) a+=x; b", "ERROR: 'x' not found"));
}

static void test_scopes(void) {
  struct js *js;
  char mem[sizeof(*js) + 250];
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(ev(js, "let a = 5; { a = 6; let x = 2; } a", "6"));
  assert(ev(js, "let b = 5; { let b = 6; } b", "5"));
  js_gc(js);
  jsoff_t brk = js->brk;
  assert(ev(js, "{}", "undefined"));
  assert(ev(js, "{1}", "ERROR: ; expected"));
  assert(ev(js, "{1;}", "1"));
  assert(ev(js, "{1;2}", "ERROR: ; expected"));
  assert(ev(js, "{1;2;}", "2"));
  assert(ev(js, "{1}2", "ERROR: ; expected"));
  assert(ev(js, "{1;}2", "2"));
  assert(ev(js, "{1;}2;3", "3"));
  assert(ev(js, "{1;}2;3;", "3"));
  assert(ev(js, "{{}}", "undefined"));
  assert(ev(js, "{let a=0; for(;a<5;){a++;}}", "undefined"));
  js_gc(js);
  assert(js->brk == brk);
  assert(ev(js, "{ let b = 6; } 1", "1"));
  js_gc(js);
  assert(js->brk == brk);
  // js_dump(js);
  assert(ev(js, "{ let a = 'hello'; { let a = 'world'; } }", "undefined"));
  js_gc(js);
  assert(js->brk == brk);
}

static void test_funcs(void) {
  struct js *js;
  char mem[sizeof(*js) + 500];
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(ev(js, "function(){};1;", "1"));
  assert(ev(js, "let f=function(){};1;", "1"));
  assert(ev(js, "f;", "function(){}"));
  assert(ev(js, "function(){1}", "ERROR: ; expected"));
  assert(ev(js, "function(){1;}", "function(){1;}"));
  assert(ev(js, "function(){1;};", "function(){1;}"));
  assert(js->flags == 0);
  assert(ev(js, "typeof 1", "\"number\""));
  assert(ev(js, "typeof(1)", "\"number\""));
  assert(ev(js, "typeof('hello')", "\"string\""));
  assert(ev(js, "typeof {}", "\"object\""));
  assert(ev(js, "typeof f", "\"function\""));
  assert(ev(js, "function(,){};", "ERROR: parse error"));
  assert(ev(js, "function(a,){};", "ERROR: parse error"));
  assert(ev(js, "function(a b){};", "ERROR: parse error"));
  assert(ev(js, "function(a,b){};", "function(a,b){}"));
  assert(ev(js, "1 + f", "ERROR: type mismatch"));
  assert(ev(js, "f = function(a){return 17;}; 1", "1"));
  assert(ev(js, "1()", "ERROR: calling non-function"));
  assert(ev(js, "f(,)", "ERROR: parse error"));
  assert(ev(js, "f(1,)", "ERROR: parse error"));
  assert(ev(js, "f(,2)", "ERROR: parse error"));
  assert(ev(js, "return", "ERROR: not in func"));
  assert(ev(js, "return 2;", "ERROR: not in func"));
  assert(ev(js, "{ return } ", "ERROR: not in func"));
  assert(ev(js, "f(3,4)", "17"));
  assert(ev(js, "return", "ERROR: not in func"));
  assert(ev(js, "(function(){})()", "undefined"));
  assert(ev(js, "(function(){})(1,2,3)", "undefined"));
  assert(ev(js, "(function(){1;})(1,2,3)", "undefined"));
  assert(js->flags == 0);
  assert(ev(js, "(function(x){res+=x;})(1)", "ERROR: 'res' not found"));
  assert(ev(js, "(function(){return 1;})()", "1"));
  assert(ev(js, "(function(){1;2;return 1;})()", "1"));
  assert(ev(js, "(function(){return 1;})(1)", "1"));
  assert(ev(js, "(function(){return 1;})(1,2,3)", "1"));
  assert(ev(js, "(function(){return 1;})(1)", "1"));
  assert(ev(js, "(function(){return 1;})(1,)", "ERROR: parse error"));
  assert(ev(js, "(function(){return 1;2;})()", "1"));
  assert(ev(js, "(function(){return 1;2;return 3;})()", "1"));
  assert(ev(js, "(function(a){return b;})(1)", "ERROR: 'b' not found"));
  assert(ev(js, "(function(a,b){return a + b;})()", "ERROR: type mismatch"));
  assert(ev(js, "(function(a,b){return a + b;})(1)", "ERROR: type mismatch"));
  assert(ev(js, "(function(a,b){return a + b;})(1,2)", "3"));
  assert(ev(js, "(function(a,b){return a + b;})('foo','bar')", "\"foobar\""));
  assert(ev(js, "(function(a,b){return a + b;})(1,2,3,4)", "3"));
  assert(ev(js, "f = function(a,b){return a + b;}; 1", "1"));
  js_gc(js);
  jsoff_t brk = js->brk;
  assert(ev(js, "f(3, 4 )", "7"));
  assert(ev(js, "f(3,4)", "7"));
  assert(ev(js, "f(1+2,4)", "7"));
  assert(ev(js, "f(1+2,f(2,3))", "8"));
  js_gc(js);
  assert(js->brk == brk);
  assert(ev(js, "f('a','b')", "\"ab\""));

  assert(ev(js, "let i,a=0; (function(){a++;})(); a", "1"));
  assert(ev(js, "a=0; (function(){ a++; })(); a", "1"));

  assert(ev(js, "a=0; (function(x){a=x;})(2); a", "2"));
  assert(ev(js, "a=0; (function(x){a=x;})('hi'); a", "\"hi\""));
  assert(ev(js, "a=0;(function(x){let z=x;a=typeof z;})('x');a", "\"string\""));
  assert(ev(js, "(function(x){return x;})(1);", "1"));
  assert(ev(js, "(function(x){return {a:x};null;})(1).a;", "1"));
  assert(ev(js, "(function(x){let m= {a:7}; return m;})(1).a;", "7"));
  assert(ev(js, "(function(x){let m=7;return m;})(1);", "7"));
  assert(ev(js, "(function(x){let m='hi';return m;})(1);", "\"hi\""));
  assert(ev(js, "(function(x){let m={a:2};return m;})(1).a;", "2"));
  assert(ev(js, "(function(x){let m={a:x};return m;})(3).a;", "3"));

  assert(ev(js, "i=a=0;f=function(x,y){return x*y;};1;", "1"));
  js_gc(js);
  brk = js->brk;
  assert(ev(js, "i=a=0; for (;i++<99;) a=i;a", "99"));
  js_gc(js);
  assert(js->brk == brk);
  assert(ev(js, "i=a=0; for (;i++ < 9999;) a += i*i; a", "333283335000"));
  js_gc(js);
  assert(js->brk == brk);
  assert(ev(js, "i=a=0; for (;i++ < 9999;) a += f(i,i); a", "333283335000"));
  js_gc(js);
  assert(js->brk == brk);

  js_eval(js, "f=function(){return 1;};", ~0UL);
  assert(ev(js, "f();", "1"));
  assert(ev(js, "f() + 2;", "3"));

  assert(ev(js, "f=function (x){return x+1;}; f(1);", "2"));

  assert(ev(js, "f = function(x){return x;};", "function(x){return x;}"));
  assert(ev(js, "f(2)", "2"));
  assert(ev(js, "f({})", "{}"));
  assert(ev(js, "f({a:5,b:3}).b", "3"));
  assert(ev(js, "f({\"a\":5,\"b\":3}).b", "3"));
}

static void test_bool(void) {
  struct js *js;
  char mem[sizeof(*js) + 200];
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(js->size == 200);  // Check 8-byte size align
  assert((js = js_create(mem, sizeof(mem) - 1U)) != NULL);
  assert(js->size == 192);  // Check 8-byte size align
  assert(ev(js, "1 && 2", "2"));
  assert(ev(js, "1 && 'x'", "\"x\""));
  assert(ev(js, "1 && ''", "\"\""));
  assert(ev(js, "1 && false", "false"));
  assert(ev(js, "1 && false || true", "true"));
  assert(ev(js, "1 && false && true", "false"));
  assert(ev(js, "1 === 2", "false"));
  assert(ev(js, "1 !== 2", "true"));
  assert(ev(js, "1 === true", "ERROR: type mismatch"));
  assert(ev(js, "1 <= 2", "true"));
  assert(ev(js, "1 < 2", "true"));
  assert(ev(js, "2 >= 2", "true"));
  assert(ev(js, "let a=true;a", "true"));
  assert(ev(js, "a=!a;a", "false"));
  assert(ev(js, "!123", "false"));
  assert(ev(js, "!0", "true"));
  assert(ev(js, "a=1; 0 || a++; a", "2"));
  assert(ev(js, "a=1; 1 || a++; a", "1"));
  assert(ev(js, "a=1; 0 && a++; a", "1"));
  assert(ev(js, "a=1; 1 && a++; a", "2"));
  assert(ev(js, "a=1; 1 && 2 && a++; a", "2"));
}

void prnt(const char *s) {
  (void) s;
  // printf("%s", s);
}

static void test_gc(void) {
  struct js *js;
  char mem[sizeof(*js) + 1500];
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  jsval_t obj = js_mkobj(js);
  js_set(js, js_glob(js), "os", obj);
  js_set(js, obj, "a", mkval(T_BOOL, 0));
  js_set(js, obj, "b", mkval(T_BOOL, 1));
#if 0
  jsoff_t brk = js->brk;
  js_gc(js);
  assert(js->brk == brk);
  js_set(js, js_glob(js), "prnt", js_import(js, (uintptr_t) prnt, "vs"));
  js_set(js, js_glob(js), "str", js_import(js, (uintptr_t) js_str, "smj"));
  assert(ev(js,
            "let f=function(){let n=0; while (n++ < "
            "100){prnt(str(0,n)+'\\n');} return n;}; f()",
            "101"));
#endif

  assert(ev(js, "let a='';", "undefined"));
  assert(ev(js, "(function(x){for(let i=0;i<x;i++)a+='x';})(2);a", "\"xx\""));
  assert(
      ev(js, "(function(x){for(let i=0;i<x;){a+='y';i++;}})(1);a", "\"xxy\""));
}

// Postponed callback invocation. C code stores a callback, then calls later
static void (*s_timer_fn)(int, void *);
static void *s_timer_fn_data;
static void set_timer(void (*fp)(int, void *), void *userdata) {
  s_timer_fn = fp;
  s_timer_fn_data = userdata;
}

struct js_timer_data {
  struct js *js;
  char name[10];
};

static void js_timer_fn(int n, void *userdata) {
  struct js_timer_data *d = (struct js_timer_data *) userdata;
  char buf[20];
  int len = snprintf(buf, sizeof(buf), "%s(%d)", d->name, n);
  // printf("EVEEEEE [%s]\n", buf);
  if (d->js) js_eval(d->js, buf, len > 0 ? (size_t) len : 0);
}

static jsval_t js_set_timer(struct js *js, jsval_t *args, int nargs) {
  static struct js_timer_data d;
  if (nargs != 1) return js_mkerr(js, "1 cb expected");
  d.js = js;
  size_t i, len = 0;
  char *name = js_getstr(js, args[0], &len);
  for (i = 0; i < len && i + 1 < sizeof(d.name); i++) d.name[i] = name[i];
  d.name[i] = '\0';
  set_timer(js_timer_fn, &d);
  return js_mkundef();
}

static jsval_t js_gt(struct js *js, jsval_t *args, int nargs) {
  if (!js_chkargs(args, nargs, "dd")) return js_mkerr(js, "doh");
  return js_getnum(args[0]) > js_getnum(args[1]) ? js_mktrue() : js_mkfalse();
}

static void test_c_funcs(void) {
  struct js *js;
  char mem[sizeof(*js) + 1800];

  assert((js = js_create(mem, sizeof(mem))) != NULL);
  js_set(js, js_glob(js), "gt", js_mkfun(js_gt));
  assert(ev(js, "gt()", "ERROR: doh"));
  assert(ev(js, "gt(1,null)", "ERROR: doh"));
  assert(ev(js, "gt(null, 1)", "ERROR: doh"));
  assert(ev(js, "gt(1,2)", "false"));
  assert(ev(js, "gt(1,1)", "false"));
  assert(ev(js, "gt(2,1)", "true"));
  assert(ev(js, "gt(0.78,-12.5)", "true"));
  // assert(ev(js, "gt(2,2)", "true"));

  js_set(js, js_glob(js), "set_timer", js_mkfun(js_set_timer));
  js_eval(js, "let v = 0, f = function(x) { v+=x; };", ~0UL);
  js_eval(js, "set_timer('f');", ~0UL);
  if (s_timer_fn) s_timer_fn(7, s_timer_fn_data);  // C code calls timer
  assert(ev(js, "v", "7"));
  if (s_timer_fn) s_timer_fn(1, s_timer_fn_data);  // C code calls timer
  assert(ev(js, "v", "8"));
  // printf("--> [%s]\n", js_str(js, js_glob(js)));

  jsval_t args[] = {0, js_mktrue(), js_mkstr(js, "a", 1), js_mknull()};
  assert(js_chkargs(args, 4, "dbsj") == true);
  assert(js_chkargs(args, 4, "dbsjb") == false);
  assert(js_chkargs(args, 4, "bbsj") == false);
  assert(js_chkargs(args, 4, "ddsj") == false);
  assert(js_chkargs(args, 4, "dbdj") == false);
  assert(js_chkargs(args, 4, "dbss") == false);
  assert(js_chkargs(args, 4, "d") == false);
}

static void test_ternary(void) {
  struct js *js;
  char mem[sizeof(*js) + 4500];
  assert((js = js_create(mem, sizeof(mem))) != NULL);
  assert(ev(js, "'aa'; 'cc'; 'bb';", "\"bb\""));
  assert(ev(js, "'aa'; 'cc'; '12345'; 'bb';", "\"bb\""));
  assert(ev(js, "1?2:3", "2"));
  assert(ev(js, "0?2:3", "3"));
  assert(ev(js, "true ? 1 + 2 : 'doh'", "3"));
  assert(ev(js, "false ? 1 + 2 : 'doh'", "\"doh\""));
  assert(ev(js, "1?2:3", "2"));
  assert(ev(js, "0?2:3", "3"));
  assert(ev(js, "0?1+1:1+2", "3"));
  assert(ev(js, "let a,b=0?1+1:1+2; b", "3"));
  assert(ev(js, "a=b=0; a=b=0?1+1:1+2", "3"));
  assert(ev(js, "a=0; a=a?1:2;a", "2"));
  assert(ev(js, "a=0; 0?a++:7; a", "0"));
  assert(ev(js, "a=1?2:0?3:4;a", "2"));
  assert(ev(js, "a=0?2:false?3:4;a", "4"));
  assert(ev(js, "a=0?2:true?3:4;a", "3"));
  assert(ev(js, "a=1;a=a?0:1;a", "0"));
  assert(ev(js, "a=0;a=a?0:1;a", "1"));
  // Calculate factorial using ternary op
  assert(ev(js, "let f=function(n){return n<2?1:n*f(n-1);}; 0", "0"));
  assert(ev(js, "f(0)", "1"));
  assert(ev(js, "f(3)", "6"));
  assert(ev(js, "f(4)", "24"));
  assert(ev(js, "f(5)", "120"));
  assert(ev(js, "f(10)", "3628800"));
}

int main(void) {
  clock_t a = clock();
  test_basic();
  test_bool();
  test_scopes();
  test_arith();
  test_errors();
  test_memory();
  test_strings();
  test_flow();
  test_funcs();
  test_c_funcs();
  test_ternary();
  test_gc();
  double ms = (double) (clock() - a) * 1000 / CLOCKS_PER_SEC;
  printf("SUCCESS. All tests passed\n");
  return EXIT_SUCCESS;
}
