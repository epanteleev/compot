#ifndef UMKA_EXPR_H_INCLUDED
#define UMKA_EXPR_H_INCLUDED

#include "umka_compiler.h"


void doPushConst                    (Compiler *comp, Type *type, Const *constant);
void doPushVarPtr                   (Compiler *comp, Ident *ident);
void doCopyResultToTempVar          (Compiler *comp, Type *type);
bool doTryRemoveCopyResultToTempVar (Compiler *comp);
void doImplicitTypeConv             (Compiler *comp, Type *dest, Type **src, Const *constant);
void doAssertImplicitTypeConv       (Compiler *comp, Type *dest, Type **src, Const *constant);
void doExplicitTypeConv             (Compiler *comp, Type *dest, Type **src, Const *constant);
void doApplyOperator                (Compiler *comp, Type **type, Type **rightType, Const *constant, Const *rightConstant, TokenKind op, bool apply, bool convertLhs);

Ident *parseQualIdent               (Compiler *comp);
void parseDesignatorList            (Compiler *comp, Type **type, Const *constant, bool *isVar, bool *isCall, bool *isCompLit);
void parseExpr                      (Compiler *comp, Type **type, Const *constant);
void parseExprList                  (Compiler *comp, Type **type, Const *constant);

void doPassParam(Compiler *comp, Type *formalParamType);
void doTryImplicitDeref(Compiler *comp, Type **type);
void doEscapeToHeap(Compiler *comp, Type *ptrType);
void doOrdinalToOrdinalOrRealToRealConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doIntToRealConv(Compiler *comp, Type *dest, Type **src, Const *constant, bool lhs);
void doCharToStrConv(Compiler *comp, Type *dest, Type **src, Const *constant, bool lhs);
void doDynArrayToStrConv(Compiler *comp, Type *dest, Type **src, Const *constant, bool lhs);
void doStrToDynArrayConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doDynArrayToArrayConv(Compiler *comp, Type *dest, Type **src, Const *constant, bool lhs);
void doArrayToDynArrayConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doDynArrayToDynArrayConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doPtrToInterfaceConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doInterfaceToInterfaceConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doValueToInterfaceConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doInterfaceToPtrConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doInterfaceToValueConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doPtrToWeakPtrConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void doWeakPtrToPtrConv(Compiler *comp, Type *dest, Type **src, Const *constant, bool lhs);
void doFnToClosureConv(Compiler *comp, Type *dest, Type **src, Const *constant);
void parseTerm(Compiler *comp, Type **type, Const *constant);
void parseFactor(Compiler *comp, Type **type, Const *constant);

#endif // UMKA_EXPR_H_INCLUDED
