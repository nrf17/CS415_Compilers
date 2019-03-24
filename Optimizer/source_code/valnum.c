
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "instrutil.h"
#include "valnum.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "symtab.h"

#define HASH_TABLE_SIZE 467



int regNumber = 0;
int init = 0;
/*  --- Static VARIABLES AND FUNCTIONS --- */
static 
SymEntry **HashT;



void
InitTable() {
  int i;
  int dummy;

  HashT = (SymEntry **) malloc (sizeof(SymEntry *) * HASH_TABLE_SIZE);
  for (i=0; i < HASH_TABLE_SIZE; i++)
    HashT[i] = NULL;



}



int hashFunc(char* str){

	int len = 0;
	int i = 0;
	while(str[i] != '\0'){
		len++;
		i++;
	}

	int hash = 0;
	for(i = 0; i <= len; i++) {
		char c = str[i];
		int a = c - '0';
		hash = (hash * 10) + a;     
	} 


	return hash;
}


//return ints, return reg number for RHS
int 
enter(char *name, int field3) {
  
  if(init == 0){ InitTable(); init = 1; }

  int hasEntry = 0;
  int currentIndex;
  int visitedSlots = 0;

  currentIndex = hashFunc(name);
  currentIndex = currentIndex % HASH_TABLE_SIZE;

  //if hash spot is not empty
  while (HashT[currentIndex] != NULL && visitedSlots < HASH_TABLE_SIZE) {
	
	//found duplicate
	if(strcmp(HashT[currentIndex]->name, name) == 0){ hasEntry = 1; break; }
	
	//colision, keep moving down to find an open spot
	else{ currentIndex = (currentIndex + 1) % HASH_TABLE_SIZE; visitedSlots++; }

  }
  

  if (visitedSlots == HASH_TABLE_SIZE) { printf("*** ERROR *** in function \"insert\": No more space for entry %s\n", name); return -1; }
  


  if(hasEntry == 0){ //new entry

  	HashT[currentIndex] = (SymEntry *) malloc (sizeof(SymEntry));
  	HashT[currentIndex]->name = (char *) malloc (strlen(name)+1);
  	strcpy(HashT[currentIndex]->name, name);
  	HashT[currentIndex]->regNum = regNumber;
  	regNumber++;
	int i;
	for(i = 0; i < 250; i++){ HashT[currentIndex]->regs[i] = -1; }
	HashT[currentIndex]->regs[0] = field3;
	return HashT[currentIndex]->regNum;
  }
  
  else{ //duplicate, add reg to its list, add RHS reg
	int i = 0;
	while(HashT[currentIndex]->regs[i] != -1 && i < 250){ i++; }
	HashT[currentIndex]->regs[i] = field3;
	return -1;
  }
}






int find(int target) {
	int i;
	int j;
	for(i = 0; i < 467; i++){
		if(HashT[i] != NULL){
			j = 0;
			while(j < 250){
				if(HashT[i]->regs[j] == target){ return HashT[i]->regNum; }
				j++;
			}
		}
	}
return -1;
}



void 
clean(int target) {
	int i;
	int j;
	for(i = 0; i < 467; i++){
		if(HashT[i] != NULL){
			j = 0;
			while(j < 250){
				if(HashT[i]->regs[j] == target){ HashT[i]->regs[j] = -1; }
				j++;
			}
		}
	}
return;
}












int specialIns(char *name, int field1, int op1){


  int hasEntry = 0;
  int currentIndex;
  int visitedSlots = 0;

  currentIndex = hashFunc(name);
  currentIndex = currentIndex % HASH_TABLE_SIZE;

  //if hash spot is not empty
  while (HashT[currentIndex] != NULL && visitedSlots < HASH_TABLE_SIZE) {
	
	//found duplicate
	if(strcmp(HashT[currentIndex]->name, name) == 0){ hasEntry = 1; break; }
	
	//colision, keep moving down to find an open spot
	else{ currentIndex = (currentIndex + 1) % HASH_TABLE_SIZE; visitedSlots++; }

  }
  

  if (visitedSlots == HASH_TABLE_SIZE) { printf("*** ERROR *** in function \"insert\": No more space for entry %s\n", name); return -1; }
  


  if(hasEntry == 0){ //new entry

  	HashT[currentIndex] = (SymEntry *) malloc (sizeof(SymEntry));
  	HashT[currentIndex]->name = (char *) malloc (strlen(name)+1);
  	strcpy(HashT[currentIndex]->name, name);
  	HashT[currentIndex]->regNum = op1;
	int i;
	for(i = 0; i < 250; i++){ HashT[currentIndex]->regs[i] = -1; }
	HashT[currentIndex]->regs[0] = field1;
	return HashT[currentIndex]->regNum;
  }
  
  else{ //duplicate, add reg to its list, add RHS reg
	int i = 0;
	while(HashT[currentIndex]->regs[i] != -1 && i < 250){ i++; }
	HashT[currentIndex]->regs[i] = field1;
	HashT[currentIndex]->regNum = op1;
	return HashT[currentIndex]->regNum;
  }


}






