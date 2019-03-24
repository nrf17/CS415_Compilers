
#include <stdio.h>
#include <stdlib.h>
#include "instrutil.h"
#include "valnum.h"

static next_register = 1; /* register 0 is reserved */
static next_label = 0;
static next_offset = 0;

int NextRegister() 
{
  if (next_register < MAX_VIRTUAL_REGISTERS)
    return next_register++;
  else {
    printf("*** ERROR *** Reached limit of virtual registers: %d\n", next_register);
    exit(-1);
  }
}

int NextLabel() 
{
  return next_label++;
}

int NextOffset(int units) 
{ 
  int current_offset = next_offset;
  next_offset = next_offset + 4*units;
  return current_offset;
}

void
emitComment(char *comment)
{
  fprintf(outfile, "\t// %s\n", comment);  
}


/*
 * emit implements CSE
 */

int
emit(int label_index,
     Opcode_Name opcode, 
     int field1, 
     int field2, 
     int field3) 
{
  char *label = " ";
  
  if (label_index < NOLABEL) {
    printf("ERROR: \"%d\" is an illegal label index.\n", label_index);
    return -1;
  }
    
  if (label_index > NOLABEL) {
    label = (char *) malloc(100);
    sprintf(label, "L%d:", label_index);
  };


  if (!cse_optimization_flag) {

    switch (opcode) { /* ---------------------- NON OPTIMIZED ------------------------------- */
    case NOP: 
      fprintf(outfile, "%s\t nop \n", label);
      return -1;
      break;
    case ADD:
      fprintf(outfile, "%s\t add r%d, r%d \t=> r%d \n", label, field1, field2, field3);
      return field3;
      break;
    case SUB: 
      fprintf(outfile, "%s\t sub r%d, r%d \t=> r%d \n", label, field1, field2, field3);
      return field3;
      break;
    case MULT: 
      fprintf(outfile, "%s\t mult r%d, r%d \t=> r%d \n", label, field1, field2, field3);
      return field3;
      break;
    case LOADI: 
      /* Example: loadI 1024 => r1 */
      fprintf(outfile, "%s\t loadI %d \t=> r%d \n", label, field1, field2);
      return field2;
      break;
    case LOADAI: 
      /* Example: loadAI r1, 16 => r3 */
      fprintf(outfile, "%s\t loadAI r%d, %d \t=> r%d \n", label, field1, field2, field3);
      return field3;
      break;
    case STOREAI: 
      /* Example: storeAI r1 => r2, 16 */
      fprintf(outfile, "%s\t storeAI r%d \t=> r%d, %d \n", label, field1, field2, field3);
      break;
    case OUTPUTAI: 
      /* Example: outputAI r0, 16  */
      fprintf(outfile, "%s\t outputAI r%d, %d\n", label, field1, field2);
      break;
    default:
      fprintf(stderr, "Illegal instruction in \"emit\" \n");
    }
    return -1;
  }

  else { 
	
	int i;
	char* op = "\0";
	char f1[9];
	for(i = 0; i < 9; i++){ f1[i] = '\0'; }
	char f2[9];
	for(i = 0; i < 9; i++){ f2[i] = '\0'; }
	char key[250];
	for(i = 0; i < 250; i++){ key[i] = '\0'; }
	int op1 = -1;
	int op2 = -1;
	int rhs = -1;	


    switch (opcode) { /* ---------------------- CSE OPTIMIZED ------------------------------- */


    case NOP: 
      fprintf(outfile, "%s\t nop \n", label);
      return -1;
      break;
    


    case ADD: //search for field1 and gets reg#, then do the same for field2, then create key and hash, if found get reg#, else insert into table

	op = "add";
	op1 = find(field1);
	op2 = find(field2);
	sprintf(f1, "%d", op1);
	sprintf(f2, "%d", op2);
	strcpy(key, op);
	strcat(key, f1);
	strcat(key, f2);
	rhs = enter(key, field3);
	if(rhs > -1){ fprintf(outfile, "%s\t add r%d, r%d \t=> r%d \n", label, op1, op2, rhs); }
      	return field3;
      break;
    



    case SUB: //search for field1 and gets reg#, then do the same for field2, then create key and hash, if found get reg#, else insert into table
	op = "sub";
	op1 = find(field1);
	op2 = find(field2);
	sprintf(f1, "%d", op1);
	sprintf(f2, "%d", op2);
	strcpy(key, op);
	strcat(key, f1);
	strcat(key, f2);
	rhs = enter(key, field3);
	if(rhs > -1){ fprintf(outfile, "%s\t sub r%d, r%d \t=> r%d \n", label, op1, op2, rhs); }
      	return field3;      
      break;
    
   
   
   
    case MULT: //search for field1 and gets reg#, then do the same for field2, then create key and hash, if found get reg#, else insert into table
	op = "mult";
	op1 = find(field1);
	op2 = find(field2);
	sprintf(f1, "%d", op1);
	sprintf(f2, "%d", op1);
	strcpy(key, op);
	strcat(key, f1);
	strcat(key, f2);
	rhs = enter(key, field3);
	if(rhs > -1){ fprintf(outfile, "%s\t mult r%d, r%d \t=> r%d \n", label, op1, op2, rhs); }
      	return field3;
      break;
    



    case LOADI: //create key and hash, if found get reg#, else insert into table
      /* Example: loadI 1024 => r1 */
	op = "li";
	sprintf(f1, "%d", field1);
	strcpy(key, op);
	strcat(key, f1);
	rhs = enter(key, field2);
	if(rhs > -1){ fprintf(outfile, "%s\t loadI %d \t=> r%d \n", label, field1, rhs); }
      	return field2;
      break;
    



    case LOADAI: //search for field1 and gets reg#, then create key and hash, if found get reg#, else insert into table
      /* Example: loadAI r1, 16 => r3 */
	op = "lai";
	sprintf(f2, "%d", field2);
	strcpy(key, op);
	strcat(key, "0");
	strcat(key, f2);
	rhs = enter(key, field3);
	if(rhs > -1){ fprintf(outfile, "%s\t loadAI r%d, %d \t=> r%d \n", label, 0, field2, rhs); }
      	return field3;
      break;
    



    case STOREAI: //search for field1 and gets reg#, 
      /* Example: storeAI r1 => r2, 16 */
	op = "lai";
	op1 = find(field1);
	clean(field1);
	sprintf(f1, "%d", field3);
	strcpy(key, op);
	strcat(key, "0");
	strcat(key, f1);
	rhs = specialIns(key, field1, op1);
	fprintf(outfile, "%s\t storeAI r%d \t=> r%d, %d \n", label, rhs, field2, field3);
      break;
    

 
    case OUTPUTAI: 
      /* Example: outputAI r0, 16  */
      fprintf(outfile, "%s\t outputAI r%d, %d\n", label, field1, field2);
      break;
    



    default:
      fprintf(stderr, "Illegal instruction in \"emit\" \n");
    }
  return -1;
  }
}




