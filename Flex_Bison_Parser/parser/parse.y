%{
#include <stdio.h>
#include "attr.h"
#include "instrutil.h"
int yylex();
void yyerror(char * s);
#include "symtab.h"

FILE *outfile;
char *CommentBuffer;

char* vars[256];
int pos = 0;
char* arrVar[256];
int arrPos = 0;
int CurrLabel = -1;
%}

%union {tokentype token;
        regInfo targetReg;
       }

%token PROG PERIOD VAR 
%token INT BOOL PRINT THEN IF DO  
%token ARRAY OF 
%token BEG END ASG  
%token EQ NEQ LT LEQ GT GEQ AND OR TRUE FALSE
%token ELSE
%token WHILE 
%token <token> ID ICONST 

%type <targetReg> exp 
%type <targetReg> lhs 

%start program

%nonassoc EQ NEQ LT LEQ GT GEQ 
%left '+' '-' AND
%left '*' OR

%nonassoc THEN
%nonassoc ELSE

%%
program : {emitComment("Assign STATIC_AREA_ADDRESS to register \"r0\""); emit(NOLABEL, LOADI, STATIC_AREA_ADDRESS, 0, EMPTY);} 
           PROG ID ';' block PERIOD { }
	;

block	: variables cmpdstmt { }
	;

variables: /* empty */
	| VAR vardcls { }
	;

vardcls	: vardcls vardcl ';' { }
	| vardcl ';' { }
	| error ';' { yyerror("***Error: illegal variable declaration\n");}  
	;

vardcl	: idlist ':' type { }
	;

idlist	: idlist ',' ID { vars[pos] = $3.str; pos++; }
        | ID		{ vars[pos] = $1.str; pos++; } 
	;


type	: ARRAY '[' ICONST ']' OF stype { 
						arrVar[arrPos] = vars[0]; 
						arrPos++;
						int i;
						for(i = 0; i < pos; i++){ vars[i] = NULL; }
						pos = 0;
						NextOffset($3.num-1);
					}

        | stype { int i; for(i = 0; i < pos; i++){ vars[i] = NULL; } pos = 0; }
	;

stype	: INT { int i; for(i = 0; i < pos; i++){ int offset = NextOffset(1); insert(vars[i], TYPE_INT, offset); } }
        | BOOL { int i; for(i = 0; i < pos; i++){ int offset = NextOffset(1); insert(vars[i], TYPE_BOOL, offset); } }
	;

stmtlist : stmtlist ';' stmt { }
	| stmt { }
        | error { yyerror("***Error: ';' expected or illegal statement \n");}
	;

stmt    : ifstmt { emit(NextLabel(), NOP, 0, 0, 0); CurrLabel++;}
	| wstmt { }
	| astmt { }
	| writestmt { }
	| cmpdstmt { }
	;

cmpdstmt: BEG stmtlist END { }
	;

ifstmt :  ifhead 
          THEN stmt 
  	  ELSE { emit(NOLABEL, BR, CurrLabel+1, 0, 0); emit(NextLabel(), NOP, 0, 0, 0); CurrLabel++;}
          stmt 
	;

ifhead : IF condexp {  }
        ;

writestmt: PRINT '(' exp ')' { int printOffset = -4;
  	                         sprintf(CommentBuffer, "Code for \"PRINT\" from offset %d", printOffset);
	                         emitComment(CommentBuffer);
                                 emit(NOLABEL, STOREAI, $3.targetRegister, 0, printOffset);
                                 emit(NOLABEL, OUTPUTAI, 0, printOffset, EMPTY);
                               }
	;

wstmt	: WHILE  { emit(NextLabel(), NOP, 0, 0, 0); CurrLabel++;} 
          condexp {  } 
          DO stmt  { emit(NOLABEL, BR, CurrLabel-2, 0, 0); emit(NextLabel(), NOP, 0, 0, 0); CurrLabel++;} 
	;


astmt : lhs ASG exp             { 
 				  if (! ((($1.type == TYPE_INT) && ($3.type == TYPE_INT)) || 
				         (($1.type == TYPE_BOOL) && ($3.type == TYPE_BOOL)))) {
				    printf("*** ERROR ***: Assignment types do not match.\n");
				  }

				  emit(NOLABEL, STORE,  $3.targetRegister, $1.targetRegister, EMPTY);
                                }
	;

lhs	: ID			{
                                  int newReg1 = NextRegister(); int newReg2 = NextRegister(); int offset;
                                  $$.targetRegister = newReg2;

				  if(lookup($1.str) != NULL){ SymTabEntry* temp = lookup($1.str); $$.type = temp->type; }

				  if(lookup($1.str) == NULL){ printf("\n*** ERROR ***: Variable %s not declared.\n", $1.str); }
				  else{ offset = lookup($1.str)->offset; }

				  int i;
				  for(i = 0; i < arrPos; i++){
				  	if(strcmp($1.str, arrVar[i]) == 0){
						printf("\n*** ERROR ***: Variable %s is not a scalar variable.\n", $1.str);
						break; 
					}
	  		  	  }
				   
				  emit(NOLABEL, LOADI, offset, newReg1, EMPTY);
				  emit(NOLABEL, ADD, 0, newReg1, newReg2);
                         	  }


                                |  ID '[' exp ']' {   
						  int newReg1 = NextRegister(); int newReg2 = NextRegister(); int newReg3 = NextRegister();
                                  	  	  int newReg4 = NextRegister(); int newReg5 = NextRegister(); int offset;
                                 		  $$.targetRegister = newReg1;

						  if($3.type == TYPE_BOOL){
								printf("\n*** ERROR ***: Array variable %s index type must be integer.\n", $1.str); 
						  }

						  if(lookup($1.str) != NULL){ SymTabEntry* temp = lookup($1.str); $$.type = temp->type; }

						  if(lookup($1.str) == NULL){ printf("\n*** ERROR ***: Variable %s not declared.\n", $1.str); }
						  else{ offset = lookup($1.str)->offset; }

						  int i;
						  int isArr = 0;
					  	  for(i = 0; i < arrPos; i++){
							if(strcmp($1.str, arrVar[i]) == 0){ isArr = 1; }
						  }
						  if(isArr == 0){ printf("\n*** ERROR ***: Variable %s is not an array variable.\n", $1.str); }

						  emit(NOLABEL, LOADI, 4, newReg2, 0);
						  emit(NOLABEL, MULT, newReg1-1, newReg2, newReg3);
						  emit(NOLABEL, LOADI, offset, newReg4, 0);
						  emit(NOLABEL, ADD, newReg4, newReg3, newReg5);
						  emit(NOLABEL, ADD, 0, newReg5, newReg1);
						}
                                ;


exp	: exp '+' exp		{ 
				  int newReg = NextRegister();
                                  if (!(($1.type == TYPE_INT) && ($3.type == TYPE_INT))) { printf("*** ERROR ***: Operator types must be integer\n");}
                                  $$.type = $1.type;
                                  $$.targetRegister = newReg;					
                                  emit(NOLABEL, ADD, $1.targetRegister, $3.targetRegister, newReg);
                                }

        | exp '-' exp		{  
				int newReg = NextRegister();
                                if (! (($1.type == TYPE_INT) && ($3.type == TYPE_INT))) { printf("*** ERROR ***: Operator types must be integer.\n");}
                                $$.type = $1.type;
                                $$.targetRegister = newReg;					
				emit(NOLABEL, SUB, $1.targetRegister, $3.targetRegister, newReg);
				}

        | exp '*' exp		{ 
				int newReg = NextRegister();
                                if (! (($1.type == TYPE_INT) && ($3.type == TYPE_INT))) { printf("*** ERROR ***: Operator types must be integer.\n");}
                                $$.type = $1.type;
                                $$.targetRegister = newReg;					
                                emit(NOLABEL, MULT, $1.targetRegister, $3.targetRegister, newReg);
				}

        | exp AND exp		{ 
				int newReg = NextRegister();
                                if (! (($1.type == TYPE_BOOL) && ($3.type == TYPE_BOOL))) { printf("*** ERROR ***: Operator types must be boolea\n");}
                                $$.type = TYPE_BOOL;
                                $$.targetRegister = newReg;					
                                emit(NOLABEL, AND_INSTR, $1.targetRegister, $3.targetRegister, newReg);
				} 


        | exp OR exp       	{  
				int newReg = NextRegister();
                                if (!(($1.type == TYPE_BOOL) && ($3.type == TYPE_BOOL))){ printf("*** ERROR ***: Operator types must be boolean.\n");}
                                $$.type = TYPE_BOOL;
                                $$.targetRegister = newReg;
                                emit(NOLABEL, OR_INSTR, $1.targetRegister, $3.targetRegister, newReg);
				}


        | ID			{ 
	                        int newReg = NextRegister(); int offset;
	                        $$.targetRegister = newReg;

				if(lookup($1.str) != NULL){ SymTabEntry* temp = lookup($1.str); $$.type = temp->type; }
				
				if(lookup($1.str) == NULL){ printf("\n*** ERROR ***: Variable %s not declared.\n", $1.str); }
				else{ offset = lookup($1.str)->offset; }

				int i;
				for(i = 0; i < arrPos; i++){
					if(strcmp($1.str, arrVar[i]) == 0){
						printf("\n*** ERROR ***: Variable %s is not a scalar variable.\n", $1.str);
						break;
					}
				}
				
				emit(NOLABEL, LOADAI, 0, offset, newReg); 
	                        }


        | ID '[' exp ']'	{   
				  int newReg1 = NextRegister(); int newReg2 = NextRegister(); int newReg3 = NextRegister();
                                  int newReg4 = NextRegister(); int newReg5 = NextRegister(); int offset;
                                  $$.targetRegister = newReg1;

				  if($3.type == TYPE_BOOL){ printf("\n*** ERROR ***: Array variable %s index type must be integer.\n", $1.str); }
						
				  if(lookup($1.str) != NULL){ SymTabEntry* temp = lookup($1.str); $$.type = temp->type; }

				  if(lookup($1.str) == NULL){ printf("\n*** ERROR ***: Variable %s not declared.\n", $1.str); }
				  else{ offset = lookup($1.str)->offset; }

				  int i;
				  int isArr = 0;
				  for(i = 0; i < arrPos; i++){
					if(strcmp($1.str, arrVar[i]) == 0){ isArr = 1; }
				  }
				  if(isArr == 0){ printf("\n*** ERROR ***: Variable %s is not an array variable.\n", $1.str); }

				  emit(NOLABEL, LOADI, 4, newReg2, 0);
				  emit(NOLABEL, MULT, newReg1-1, newReg2, newReg3);
				  emit(NOLABEL, LOADI, offset, newReg4, 0);
				  emit(NOLABEL, ADD, newReg4, newReg3, newReg5);
				  emit(NOLABEL, LOADAO, 0, newReg5, newReg1);
				}
 


	| ICONST                 { int newReg = NextRegister();
	                           $$.targetRegister = newReg;
				   $$.type = TYPE_INT;
				   emit(NOLABEL, LOADI, $1.num, newReg, EMPTY); }

        | TRUE                   { int newReg = NextRegister(); /* TRUE is encoded as value '1' */
	                           $$.targetRegister = newReg;
				   $$.type = TYPE_BOOL;
				   emit(NOLABEL, LOADI, 1, newReg, EMPTY); }

        | FALSE                   { int newReg = NextRegister(); /* FALSE is encoded as value '0' */
	                           $$.targetRegister = newReg;
				   $$.type = TYPE_BOOL;
				   emit(NOLABEL, LOADI, 0, newReg, EMPTY); }

	| error { yyerror("***Error: illegal expression\n");}  
	;



condexp	: exp NEQ exp		{	
				int newReg = NextRegister();
                                if (! ((($1.type == TYPE_INT) && ($3.type == TYPE_INT)) || 
				         (($1.type == TYPE_BOOL) && ($3.type == TYPE_BOOL)))) {
				    printf("\n*** ERROR ***: == or != operator with different types.\n"); 
				}
				emit(NOLABEL, CMPNE, $1.targetRegister, $3.targetRegister, newReg);
				emit(NOLABEL, CBR, $3.targetRegister+1, CurrLabel, CurrLabel+1);
				emit(NextLabel(), NOP, 0, 0, 0);
				CurrLabel++;
				} 

        | exp EQ exp		{
				int newReg = NextRegister();
                                if (! ((($1.type == TYPE_INT) && ($3.type == TYPE_INT)) || 
				         (($1.type == TYPE_BOOL) && ($3.type == TYPE_BOOL)))) {
				    printf("\n*** ERROR ***: == or != operator with different types.\n");
				}
				emit(NOLABEL, CMPEQ, $1.targetRegister, $3.targetRegister, newReg);
				emit(NOLABEL, CBR, $3.targetRegister+1, CurrLabel, CurrLabel+1);
				emit(NextLabel(), NOP, 0, 0, 0);
				CurrLabel++;
				} 

        | exp LT exp		{
				int newReg = NextRegister();
				if($1.type != TYPE_INT || $3.type != TYPE_INT){ printf("\n*** ERROR ***: Relational operator with illegal type.\n"); }
				emit(NOLABEL, CMPLT, $1.targetRegister, $3.targetRegister, newReg);
				emit(NOLABEL, CBR, $3.targetRegister+1, CurrLabel, CurrLabel+1);
				emit(NextLabel(), NOP, 0, 0, 0);
				CurrLabel++;
				}

        | exp LEQ exp		{
				int newReg = NextRegister();
                                if($1.type != TYPE_INT || $3.type != TYPE_INT){ printf("\n*** ERROR ***: Relational operator with illegal type.\n"); }
				emit(NOLABEL, CMPLE, $1.targetRegister, $3.targetRegister, newReg);
				emit(NOLABEL, CBR, $3.targetRegister+1, CurrLabel, CurrLabel+1);
				emit(NextLabel(), NOP, 0, 0, 0);
				CurrLabel++;
				}

	| exp GT exp		{
				int newReg = NextRegister();
                                if($1.type != TYPE_INT || $3.type != TYPE_INT){ printf("\n*** ERROR ***: Relational operator with illegal type.\n"); }
				emit(NOLABEL, CMPGT, $1.targetRegister, $3.targetRegister, newReg);
				emit(NOLABEL, CBR, $3.targetRegister+1, CurrLabel, CurrLabel+1);
				emit(NextLabel(), NOP, 0, 0, 0);
				CurrLabel++;
				}

	| exp GEQ exp		{
				int newReg = NextRegister();
                                if($1.type != TYPE_INT || $3.type != TYPE_INT){ printf("\n*** ERROR ***: Relational operator with illegal type.\n"); }
				emit(NOLABEL, CMPGE, $1.targetRegister, $3.targetRegister, newReg);
				emit(NOLABEL, CBR, $3.targetRegister+1, CurrLabel, CurrLabel+1);
				emit(NextLabel(), NOP, 0, 0, 0);
				CurrLabel++;
				}

	| error { yyerror("***Error: illegal conditional expression\n");}  
        ;

%%

void yyerror(char* s) {
        fprintf(stderr,"%s\n",s);
        }


int
main(int argc, char* argv[]) {

  printf("\n     CS415 Spring 2018 Compiler\n\n");

  outfile = fopen("iloc.out", "w");
  if (outfile == NULL) { 
    printf("ERROR: cannot open output file \"iloc.out\".\n");
    return -1;
  }

  CommentBuffer = (char *) malloc(650);  
  InitSymbolTable();

  printf("1\t");
  yyparse();
  printf("\n");

  PrintSymbolTable();
  
  fclose(outfile);
  
  return 1;
}




