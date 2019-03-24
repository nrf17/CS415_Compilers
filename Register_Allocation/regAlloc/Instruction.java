package registerAllocator;

import java.util.ArrayList;

public class Instruction {

	String opCode;
	String field1;
	String field2;
	String field3;
	Instruction next;
	int lineNum;
	ArrayList<RegInfo> maxLive;
	ArrayList<String> strMax;
	
	public Instruction(String opCode, String field1, String field2, String field3, int lineNum){
		this.opCode = opCode;
		this.field1 = field1;
		this.field2 = field2;
		this.field3 = field3;
		this.next = null;
		this.lineNum = lineNum;
		this.maxLive = new ArrayList<RegInfo>();
		this.strMax = new ArrayList<String>();
	}
	
	
	public String toString(){
	
		if(this.opCode.equals("output")){
			return "Line #" + this.lineNum + ": " + this.opCode + " " + this.field1;
		}
		
		else if( this.opCode.equals("loadI") || this.opCode.equals("load") || this.opCode.equals("store") ){
			return "Line #" + this.lineNum + ": " + this.opCode + " " + this.field1 + " => " + this.field2;
		}
		
		else if( this.opCode.equals("add") || this.opCode.equals("sub") || this.opCode.equals("mult") || this.opCode.equals("lshift") || this.opCode.equals("rshift") ){
			return "Line #" + this.lineNum + ": " + this.opCode + " " + this.field1 +  ", " + this.field2 + " => " + this.field3;
		}
		
		else{ return "error"; }
		
	}
	
	
}
