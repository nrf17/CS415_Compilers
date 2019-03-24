package registerAllocator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Scanner;

public class Alloc {		
	
	public static void update(RegInfo list, String reg, int lineNum){	
		RegInfo curr = list;
		if(reg.equals("r0")){ return; }
		else if(list.regName.equals("") && list.entry == 0){
			list.regName = reg;
			list.entry = lineNum;
			list.last = lineNum;
			list.occ = 1;
			list.live = 0;
			list.next = null;
			return;
		}
		
		else{
			while(curr.next != null) {
				if(curr.regName.equals(reg)){ curr.occ++; curr.last = lineNum; return; }
				curr = curr.next;
			}
			
			if(curr.regName.equals(reg)){ curr.occ++; curr.last = lineNum; return; }
			
			RegInfo temp = new RegInfo(reg, lineNum);
			curr.next = temp;
			return;
			
		}
		
	}
	
	
	public static String getOut(String line){
		String[] outty = line.split("output");
		String end = outty[1];
		String result = end.trim();
		return result;
	}
	
	
	public static String[] getMem(String op, String line){
		
		switch(op){
		case "loadI":
			String[] tokens1 = line.split("loadI");
			String[] sep1 = tokens1[1].split("=>");
			String a = sep1[0].trim();
			String b = sep1[1].trim();
			String[] temp1 = {a, b};
			return temp1;
			
			
		case "load":
			String[] tokens2 = line.split("load");
			String[] sep2 = tokens2[1].split("=>");
			String c = sep2[0].trim();
			String d = sep2[1].trim();
			String[] temp2 = {c, d};
			return temp2;
			
			
		case "store":
			String[] tokens3 = line.split("store");
			String[] sep3 = tokens3[1].split("=>");
			String e = sep3[0].trim();
			String f = sep3[1].trim();
			String[] temp3 = {e, f};
			return temp3;
		
		}
		
		String [] aa = null;
		return aa;
	}
	

	public static String[] getOps(String op, String line){
		
		switch(op){
		
		case "add":
			String[] tokens1 = line.split("add");
			String[] sep1 = tokens1[1].split("=>");
			String last1 = sep1[1].trim();
			String[] fields1 = sep1[0].split(",");
			String a = fields1[0].trim();
			String b = fields1[1].trim();
			String[] temp1 = {a, b, last1};
			return temp1;
			
		case "sub":
			String[] tokens2 = line.split("sub");
			String[] sep2 = tokens2[1].split("=>");
			String last2 = sep2[1].trim();
			String[] fields2 = sep2[0].split(",");
			String c = fields2[0].trim();
			String d = fields2[1].trim();
			String[] temp2 = {c, d, last2};
			return temp2;
			
		case "mult":
			String[] tokens3 = line.split("mult");
			String[] sep3 = tokens3[1].split("=>");
			String last3 = sep3[1].trim();
			String[] fields3 = sep3[0].split(",");
			String e = fields3[0].trim();
			String f = fields3[1].trim();
			String[] temp3 = {e, f, last3};
			return temp3;
			
		case "lshift":
			String[] tokens4 = line.split("lshift");
			String[] sep4 = tokens4[1].split("=>");
			String last4 = sep4[1].trim();
			String[] fields4 = sep4[0].split(",");
			String g = fields4[0].trim();
			String h = fields4[1].trim();
			String[] temp4 = {g, h, last4};
			return temp4;
			
		case "rshift":
			String[] tokens5 = line.split("rshift");
			String[] sep5 = tokens5[1].split("=>");
			String last5 = sep5[1].trim();
			String[] fields5 = sep5[0].split(",");
			String i = fields5[0].trim();
			String j = fields5[1].trim();
			String[] temp5 = {i, j, last5};
			return temp5;
		
		}
		
		String[] aa = null;
		return aa;
	}
	
	
	
	
	public static void main(String[] args){
		int lineNum = 0;
		Instruction start = null;
		Instruction curr = null;

		if(args.length != 3){ System.out.println("Error, wrong number of inputs"); return; }

		int k = Integer.parseInt(args[0]);
		if ( k < 2) { System.out.println("Error, k is too small, k must be <= 2"); return; }
		k = k - 2;

		String allocator = args[1];
		if(!allocator.equals("s") && !allocator.equals("t") && !allocator.equals("b") && !allocator.equals("o") ) {
			System.out.println("Error, incorrect allocator flag, must use s, t, b, or o");
			return;
		}
		if (allocator.equals("o")) { System.out.println("Did not implement 4th optional allocator"); return; }
		if (allocator.equals("b")) { System.out.println("Did not complete the 3rd allocator"); return; }

		String fileName = args[2];




		try{
			BufferedReader buff = new BufferedReader(new FileReader(fileName));
			String line = null;
			while( (line = buff.readLine()) != null){
				if(line.length() < 1 || line.equals("\t") || line.equals("") ){ continue; }
				else if(line.charAt(0) == '/'){ continue; }
				
				else{
					lineNum++;
					String[] tokens = line.split(" ");
					String[] oper = tokens[0].split("\t");

					String op = null;
					if(oper[0].length() < 1){ op = oper[1]; } //tab before iloc instruction
					else{ op = oper[0]; } //no tab before iloc instruction
					
					switch(op){
						case "output":
							String outField = getOut(line);
							Instruction temp1 = new Instruction(op, outField, null, null, lineNum);
							if(lineNum == 1){ start = temp1; curr = start; }
							else{ curr.next = temp1; curr= curr.next; }
							break;
									
						case "loadI":
						case "load":
						case "store":
							String[] reg = getMem(op, line);
							Instruction temp2 = new Instruction(op, reg[0], reg[1], null, lineNum);
							if(lineNum == 1){ start = temp2; curr = start; }
							else{ curr.next = temp2; curr= curr.next; }
							break;
						
						case "add":
						case "sub":
						case "mult":
						case "lshift":
						case "rshift":
							String[] fields = getOps(op, line);
							Instruction temp3 = new Instruction(op, fields[0], fields[1], fields[2], lineNum);
							if(lineNum == 1){ start = temp3; curr = start; }
							else{ curr.next = temp3; curr= curr.next; }
							break;
							
					} 
					
				} 
			} 
			
			curr = start;
			RegInfo list = new RegInfo("", 0);
			while(curr != null){
				if(curr.opCode.equals("loadI")){ update(list, curr.field2, curr.lineNum); }
				
				else if(curr.opCode.equals("load") || curr.opCode.equals("store")){
					update(list, curr.field1, curr.lineNum);
					update(list, curr.field2, curr.lineNum);
				}
				
				else if(curr.opCode.equals("add") || curr.opCode.equals("sub") || curr.opCode.equals("mult") || curr.opCode.equals("lshift") || curr.opCode.equals("rshift") ){
					update(list, curr.field1, curr.lineNum);
					update(list, curr.field2, curr.lineNum);
					update(list, curr.field3, curr.lineNum);
				}
				
				curr = curr.next;
			} 
						
			RegInfo itr = list;
			while(itr != null){ itr.live = itr.last - itr.entry; itr = itr.next; }
			
			if(allocator.equals("s")){ SimpleTopDown.simpleTopDown(start, list, k); }

			if(allocator.equals("t")){ TopDown.topDown(start, list, k); }
			
			
			
		} 
		
		catch (Exception e){
			System.out.println(fileName + " does not exist.");
			e.printStackTrace();
		}
	} 
} 