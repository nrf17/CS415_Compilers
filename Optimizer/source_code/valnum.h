
#ifndef VALNUM_H
#define VALNUM_H

#include <string.h>

#include "attr.h"



typedef struct {
  char *name;
  int regNum;
  int regs [250];
} SymEntry;

extern
void InitTable();

extern
int enter(char *name, int field3);

extern
int find(int target);

extern
int hashFunc(char* str);

extern
void clean(int target);

extern
int specialIns(char *name, int field1, int op1);

#endif
