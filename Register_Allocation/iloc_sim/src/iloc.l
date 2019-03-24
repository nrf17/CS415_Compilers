WHITESPACE [ \t]
NUM [0-9]+

%{


  #include <stdlib.h>
  #include <stdio.h>
  #include <string.h>

  #include "instruction.h"
  #include "y.tab.h"

  int line_counter = 1;
  Opcode* current_opcode;

%}

%%

\[            {return OPEN_BRACKET;}
\]            {return CLOSE_BRACKET;}
\;            {return SEMICOLON;}
\,            {return COMMA;}
\=\>          {return ARROW;}

%{
/* 
r{NUM}        {return REGISTER;}
{NUM}         {return NUMBER;}
\-{NUM}	      {return NUMBER;}

[a-zA-Z0-9\_]+: {return TARGET;}

[a-zA-Z0-9\_]+  { current_opcode = get_opcode(yytext);
                if (current_opcode)
                    return OPCODE;
                else
                    return LABEL;
              }
 */
%}

r{NUM}        {
  yylval.operand_ptr = (Operand *) malloc (sizeof (Operand));
  yylval.operand_ptr->value = (int) strtol (yytext + 1, (char **) NULL, 10);
  return REGISTER;
}
{NUM}         {
  yylval.operand_ptr = (Operand *) malloc (sizeof (Operand));
  yylval.operand_ptr->value = (int) strtol (yytext, (char **) NULL, 10);
  return NUMBER;
}
\-{NUM}	      {
  yylval.operand_ptr = (Operand *) malloc (sizeof (Operand));
  yylval.operand_ptr->value = -((int) strtol (yytext + 1, (char **) NULL, 10));
  return NUMBER;
}

[a-zA-Z0-9\_]+: {
  yylval.label_ptr = (char *) malloc (yyleng);
  strcpy (yylval.label_ptr, yytext);
  return TARGET;
}

[a-zA-Z0-9\_]+  { current_opcode = get_opcode(yytext);
                if (current_opcode)
                    return OPCODE;
                else {
		  yylval.label_ptr = (char *) malloc (yyleng);
		  strcpy (yylval.label_ptr, yytext);
		  return LABEL;
		}
              }

\/\/[^\n]*  { /* Comment */}

[\n]          {line_counter++;}

{WHITESPACE}  {;}

%%

yywrap()
{
  return 1;
}

