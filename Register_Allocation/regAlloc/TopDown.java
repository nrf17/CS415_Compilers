package registerAllocator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class TopDown {

	public static void popMax(RegInfo curr, Instruction ins){	
		String r = curr.regName;
		int enter = curr.entry;
		int last = curr.last;
		int occur = curr.occ;
		int liveRange = curr.live;
		RegInfo temp = new RegInfo(r, 0);
		temp.entry = enter; temp.last = last; temp.occ = occur; temp.live = liveRange;
		Instruction itr = ins;
		while(itr.lineNum != enter){ itr = itr.next; }
		if(enter == last){ itr.maxLive.add(temp); return; }
		while(itr.lineNum != last){ itr.maxLive.add(temp); itr = itr.next; }
		return;
	}
	
	public static void removeSpill(Instruction ins, RegInfo spillReg){
		Instruction itr = ins;
		while(itr != null){
			if(itr.maxLive.contains(spillReg)){ int remove = itr.maxLive.indexOf(spillReg); itr.maxLive.remove(remove); }	
			itr = itr.next;
		}
	}
	
	public static ArrayList<String> setSpill(Instruction ins, int k){		
		
		ArrayList<String> spillRegs =  new ArrayList<String>();
		
		Instruction itr = ins;
		itr = itr.next;
		
		while(itr != null){
			
			if(itr.maxLive.size() > k){
				
				while(itr.maxLive.size() > k){
					int min = 999;
					int LiveRange = 0;
					int minIndx = 0;
					for(int i = 0; i < itr.maxLive.size(); i++){
						RegInfo temp = itr.maxLive.get(i);
						if(temp.occ < min){ min = temp.occ; LiveRange = temp.live; minIndx = i; }
						else if(temp.occ == min){
							if(temp.live > LiveRange){ min = temp.occ; LiveRange = temp.live; minIndx = i; }
						}
					} 
					RegInfo spill = itr.maxLive.get(minIndx);
					spillRegs.add(spill.regName);
					removeSpill(ins, spill);	
				} 
			
			} 
			
			itr = itr.next;
		} 
		
		
		return spillRegs;
	}
	
	public static void popStrMax(Instruction ins){
		Instruction itr = ins;
		itr = itr.next;
		while(itr != null){
			for(int i = 0; i < itr.maxLive.size(); i++){ RegInfo temp1 = itr.maxLive.get(i); String temp2 = temp1.regName; itr.strMax.add(temp2); }
			itr = itr.next;
		}
	}
	
	public static void setOffset(RegInfo regs, ArrayList<String> spillRegs){
		int mem = 0;
		for(int i = 0; i < spillRegs.size(); i++){
			String reg = spillRegs.get(i);
			RegInfo itr = regs;
			while(itr != null){
				if(itr.regName.equals(reg)){ mem = mem - 4; itr.offset = mem; break; }
				itr = itr.next;
			}	
		} 
	}
	
	public static int getOffset(String reg, RegInfo regs){
		RegInfo itr = regs;
		int mem = 0;
		while(itr != null){
			if(itr.regName.equals(reg)){ mem = itr.offset; break; }
			itr = itr.next;
		}
		return mem;
	}
	
	//case 1 deals with LOADI, dealing with 1 register
	public static void caseOne(Instruction itr, RegInfo regs, ArrayList<String> spillRegs, String[] pRegs, String feas1, String feas2, int k){
		
		String r1 = itr.field2;
		boolean isSpill = false;
		if(spillRegs.contains(r1)){ isSpill = true; }
		
		//dealing with a spill reg
		if(isSpill == true){
			int mem = getOffset(r1, regs);
			System.out.println(itr.opCode + " " + itr.field1 + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem);
			return;
		}
		
		
		//reg is already in the physical reg
		for(int i = 0; i < pRegs.length; i++){
			if(pRegs[i].equals(r1)){ int pNum = i + 3; System.out.println(itr.opCode + " " + itr.field1 + " => r" + pNum); return; }
		}
		
		
		//open physical reg to use
		for(int i = 0; i < pRegs.length; i++){
			if(pRegs[i].equals("") ){ pRegs[i] = r1; int pNum = i + 3; System.out.println(itr.opCode + " " + itr.field1 + " => r" + pNum); return; }
		}
		
		
		//clear dead regs from physical regs
		for(int i = 0; i < pRegs.length; i++){
			if(!itr.strMax.contains(pRegs[i]) ){ pRegs[i] = ""; }
		}
		
		//add reg to and open physical reg
		for(int i = 0; i < pRegs.length; i++){
			if(pRegs[i].equals("") ){ pRegs[i] = r1; int pNum = i + 3; System.out.println(itr.opCode + " " + itr.field1 + " => r" + pNum); return; }
		}
		
	}
	
	//case 2 deals with LOAD & STORE, dealing with 2 registers
	public static void caseTwo(Instruction itr, RegInfo regs, ArrayList<String> spillRegs, String[] pRegs, String feas1, String feas2, int k){
		String r1 = itr.field1; // left side reg of => (r# =>)
		String r2 = itr.field2; //right side reg of => (=> r#)
		boolean isSpill1 = false; // if r1 is a spill reg
		boolean isSpill2 = false; // if r2 is a spill reg
		boolean hasR1 = false; // if r1 is currently in physical reg
		boolean hasR2 = false; // if r2 is currently in physical reg
		int pNum1 = -1;
		int pNum2 = -1;
		String hasR1b4 = null; // physical register # of r1 before it possibly cleared out
		String hasR2b4 = null; // physical register # of r2 before it possibly cleared out
		if(spillRegs.contains(r1)){ isSpill1 = true; }
		if(spillRegs.contains(r2)){ isSpill2 = true; }
		for(int i = 0; i < pRegs.length; i++){
			if(pRegs[i].equals(r1)){ hasR1 = true; pNum1 = i+3; hasR1b4 = "r" + Integer.toString(pNum1); }
			if(pRegs[i].equals(r2)){ hasR2 = true; pNum2 = i+3; hasR2b4 = "r" + Integer.toString(pNum2);}
		}
		
		//both regs are spill regs
		if(isSpill1 == true && isSpill2 == true){ 
			int mem1 = getOffset(r1, regs);
			int mem2 = getOffset(r2, regs);
			System.out.println("loadAI r0, " + mem1 + " => " + feas1);
			System.out.println("loadAI r0, " + mem2 + " => " + feas2);
			System.out.println(itr.opCode + " " + feas1 + " => " + feas2);
			System.out.println("storeAI " + feas2 + " => r0, " + mem2);
			return;
		}
		
		
		//both regs are already in physical registers
		if(hasR1 == true && hasR2 == true){ System.out.println(itr.opCode + " r" + pNum1 + " => r" + pNum2); return; }
		
		// r1: spill, r2: in physical reg
		if(isSpill1 == true && hasR2 == true){
			int mem = getOffset(r1, regs);
			System.out.println("loadAI r0, " + mem + " => " + feas1);
			System.out.println(itr.opCode + " " + feas1 + " => r" + pNum2);
			return;
		}
		
		// r1: in physical reg, r2: spill
		if(hasR1 == true && isSpill2 == true){
			int mem = getOffset(r2, regs);
			System.out.println(itr.opCode + " r" + pNum1 + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem);
			return;
		}
		
		
		
		//clear dead registers from physical ones
		for(int i = 0; i < pRegs.length; i++){
			if(!itr.strMax.contains(pRegs[i]) ){ pRegs[i] = ""; }
		}
		
		
		//____________________________________________________________________________________________________________________________________________________
		
		//r1: in physical reg, r2: needs to be stored into an open physical reg
		if(hasR1 == true && hasR2 == false){
			int pNumB = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumB = i + 3; break;}
			}
			
			//check to make sure we never do: store r3 => r3
			String rB = "r" + Integer.toString(pNumB);
			if(hasR1b4.equals(rB) ){
				pRegs[pNumB-3] = "";
				for(int i = (pNumB-2); i < pRegs.length; i++){
					if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumB = i + 3; break;}
				}
			}
			
			for(int i = 0; i < pRegs.length; i++){ if(pRegs[i].equals("r0")){ pRegs[i] = ""; } }
			
			rB = "r" + Integer.toString(pNumB);
			if(r2.equals("r0")){ rB = "r0"; }
			
			System.out.println(itr.opCode + " " + hasR1b4 + " => " + rB);
			return;
		} 
		
		
		
		// r1: needs to be put in physical reg, r2: already in physical reg
		if(hasR1 == false && hasR2 == true){
			int pNumA = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r1; pNumA = i + 3; break;}
			}
			
			//check to make sure we never do: store r3 => r3
			String rA = "r" + Integer.toString(pNumA);
			if(hasR2b4.equals(rA) ){
				pRegs[pNumA-3] = "";
				for(int i = (pNumA-2); i < pRegs.length; i++){
					if(pRegs[i].equals("") ){ pRegs[i] = r1; pNumA = i + 3; break;}
				}
			}
			
			for(int i = 0; i < pRegs.length; i++){ if(pRegs[i].equals("r0")){ pRegs[i] = ""; } }
			
			rA = "r" + Integer.toString(pNumA);
			if(r1.equals("r0")){ rA = "r0"; }
			
			
			System.out.println(itr.opCode + " " + rA + " => " + hasR2b4);
			return;
		}
		
		
		
		// r1: spill, r2: needs to be put in open physical reg
		if(isSpill1 == true && hasR2 == false){
			int pNumB = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumB = i + 3; break;}
			}

			for(int i = 0; i < pRegs.length; i++){ if(pRegs[i].equals("r0")){ pRegs[i] = ""; } }
			
			int mem = getOffset(r1, regs);
			
			String rB = "r" + Integer.toString(pNumB);
			if(r2.equals("r0")){ rB = "r0"; }
			
			System.out.println("loadAI r0, " + mem + " => " + feas1);
			System.out.println(itr.opCode + " " + feas1 + " => " + rB);
			return;
		} 
		
		
		// r1: needs to be put in open physical reg, r2: spill
		if(hasR1 == false && isSpill2 == true){
			int pNumA = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r1; pNumA = i + 3; break;}
			}
			
			int mem = getOffset(r2, regs);
			
			for(int i = 0; i < pRegs.length; i++){ if(pRegs[i].equals("r0")){ pRegs[i] = ""; } }
			
			String rA = "r" + Integer.toString(pNumA);
			if(r1.equals("r0")){ rA = "r0"; }
			
			
			System.out.println(itr.opCode + " " + rA + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem);
			return;
		}
		//________________________________________________________________________________________________________________________________________________
		
		
		// r1:  needs to be put in open physical reg, r2:  needs to be put in open physical reg
		if(hasR1 == false && hasR2 == false){
			
			int pNumA = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r1; pNumA = i + 3; break;}
			}
			
			for(int i = 0; i < pRegs.length; i++){ if(pRegs[i].equals("r0")){ pRegs[i] = ""; } }
						
			int pNumB = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumB = i + 3; break;}
			}
			
			for(int i = 0; i < pRegs.length; i++){ if(pRegs[i].equals("r0")){ pRegs[i] = ""; } }
			
			String rA = "r" + Integer.toString(pNumA);
			if(r1.equals("r0")){ rA = "r0"; }
			String rB = "r" + Integer.toString(pNumB);
			if(r2.equals("r0")){ rB = "r0"; }
			
			
			System.out.println(itr.opCode + " " + rA + " => " + rB);
			return;
		} 
	}
	
	//case 3 deals with ADD, SUB, MULT, LSHIFT, & RSHIFT, dealing with 3 registers
	public static void caseThree(Instruction itr, RegInfo regs, ArrayList<String> spillRegs, String[] pRegs, String feas1, String feas2, int k){
		
		String r1 = itr.field1; // left side reg of => (r# =>)
		String r2 = itr.field2; // left side reg of => (r# =>)
		String r3 = itr.field3; //right side reg of => (=> r#)
		boolean isSpill1 = false; // if r1 is a spill reg
		boolean isSpill2 = false; // if r2 is a spill reg
		boolean isSpill3 = false; // if r3 is a spill reg
		boolean hasR1 = false; // if r1 is currently in physical reg
		boolean hasR2 = false; // if r2 is currently in physical reg
		boolean hasR3 = false; // if r3 is currently in physical reg
		int pNum1 = -1;
		int pNum2 = -1;
		int pNum3 = -1;
		String r1b4 = null; // physical register # of r1 before it possibly cleared out
		String r2b4 = null; // physical register # of r2 before it possibly cleared out
		String r3b4 = null; // physical register # of r3 before it possibly cleared out
		if(spillRegs.contains(r1)){ isSpill1 = true; }
		if(spillRegs.contains(r2)){ isSpill2 = true; }
		if(spillRegs.contains(r3)){ isSpill3 = true; }
		for(int i = 0; i < pRegs.length; i++){
			if(pRegs[i].equals(r1)){ hasR1 = true; pNum1 = i+3; r1b4 = "r" + Integer.toString(pNum1); }
			if(pRegs[i].equals(r2)){ hasR2 = true; pNum2 = i+3; r2b4 = "r" + Integer.toString(pNum2);}
			if(pRegs[i].equals(r2)){ hasR2 = true; pNum3 = i+3; r2b4 = "r" + Integer.toString(pNum3);}
		}
		
		//	S, S, S
		if(isSpill1 == true && isSpill2 == true && isSpill3 == true){
			int mem1 = getOffset(r1, regs);
			int mem2 = getOffset(r2, regs);
			int mem3 = getOffset(r3, regs);
			System.out.println("loadAI r0, " + mem1 + " => " + feas1);
			System.out.println("loadAI r0, " + mem2 + " => " + feas2);
			System.out.println(itr.opCode + " " + feas1 + ", " + feas2 + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem3);
			return;
		}
		
		
		//	S, S, P
		if(isSpill1 == true && isSpill2 == true && hasR3 == true){
			int mem1 = getOffset(r1, regs);
			int mem2 = getOffset(r2, regs);
			System.out.println("loadAI r0, " + mem1 + " => " + feas1);
			System.out.println("loadAI r0, " + mem2 + " => " + feas2);
			System.out.println(itr.opCode + " " + feas1 + ", " + feas2 + " => r" + pNum3);
			return;
		}
		
		
		
		//	S, P, S
		if(isSpill1 == true && hasR2 == true && isSpill3 == true){
			int mem1 = getOffset(r1, regs);
			int mem3 = getOffset(r3, regs);
			System.out.println("loadAI r0, " + mem1 + " => " + feas1);
			System.out.println(itr.opCode + " " + feas1 + ", r" + pNum2 + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem3);
			return;
		}
		
		
		// S, P, P
		if(isSpill1 == true && hasR2 == true && hasR3 == true){
			int mem1 = getOffset(r1, regs);
			System.out.println("loadAI r0, " + mem1 + " => " + feas1);
			System.out.println(itr.opCode + " " + feas1 + ", r" + pNum2 + " => r" + pNum3);
			return;
		}
		
		
		// P, S, S
		if(hasR1 == true && isSpill2 == true && isSpill3 == true){
			int mem2 = getOffset(r2, regs);
			int mem3 = getOffset(r3, regs);
			System.out.println("loadAI r0, " + mem2 + " => " + feas1);
			System.out.println(itr.opCode + " r" + pNum1 + ", " + feas1 + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem3);
			return;
		}
		
		
		//	P, S. P
		if(hasR1 == true && isSpill2 == true && hasR3 == true){
			int mem2 = getOffset(r2, regs);
			System.out.println("loadAI r0, " + mem2 + " => " + feas1);
			System.out.println(itr.opCode + " r" + pNum1 + ", " + feas1 + " => r" + pNum3);
			return;
		}
		
		
		
		// P, P, S
		if(hasR1 == true && hasR2 == true && isSpill3 == true){
			int mem3 = getOffset(r3, regs);
			System.out.println(itr.opCode + " r" + pNum1 + ", r" + pNum2 + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem3);
			return;
		}
		
		
		// P, P, P
		if(hasR1 == true && hasR2 == true && hasR3 == true){ System.out.println(itr.opCode + " r" + pNum1 + ", r" + pNum2 + " => r" + pNum3); return; }
		
		//	^before any clearing of physical registers
		//_________________________________________________________________________________________________________________________________________________
		//	after clearing of physical registers, need to use the String b4's for the fields
		
		//clear dead registers from physical ones
		for(int i = 0; i < pRegs.length; i++){
			if(!itr.strMax.contains(pRegs[i]) ){ pRegs[i] = ""; }
		}
		
		
		
		// P S NP
		if(hasR1 == true && isSpill2 == true && hasR3 == false){
			int mem2 = getOffset(r2, regs);
			int pNumC = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumC = i + 3; break;}
			}
			System.out.println("loadAI r0, " + mem2 + " => " + feas1);
			System.out.println(itr.opCode + " " + r1b4 + ", " + feas1 + " => r" + pNumC);
			return;
		}
		
		
		
		
		
		
		//	S, S, NP
		if(isSpill1 == true && isSpill2 == true && hasR3 == false){
			int mem1 = getOffset(r1, regs);
			int mem2 = getOffset(r2, regs);
			int pNumC = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumC = i + 3; break;}
			}
			System.out.println("loadAI r0, " + mem1 + " => " + feas1);
			System.out.println("loadAI r0, " + mem2 + " => " + feas2);
			System.out.println(itr.opCode + " " + feas1 + ", " + feas2 + " => r" + pNumC);
			return;
		}
		
		
		//	 S, P, NP
		if(isSpill1 == true && hasR2 == true && hasR3 == false){
			int mem1 = getOffset(r1, regs);
			int pNumC = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumC = i + 3; break;}
			}
			System.out.println("loadAI r0, " + mem1 + " => " + feas1);
			System.out.println(itr.opCode + " " + feas1 + ", " + r2b4 + " => r" + pNumC);
			return;
		}
		
		
			//		NP, P, S
			if(hasR1 == false && hasR2 == true && isSpill3 == true){
				int mem3 = getOffset(r3, regs);
				int pNumA = -1;
				for(int i = 0; i < pRegs.length; i++){
					if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumA = i + 3; break;}
				}
				//check to avoid r3, r? => f1 -> !r3,r3 => f1
				String rA = "r" + Integer.toString(pNumA);
				if(r2b4.equals(rA) ){
					pRegs[pNumA-3] = "";
					for(int i = (pNumA-2); i < pRegs.length; i++){
						if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumA = i + 3; break;}
					}
				}
				
				System.out.println(itr.opCode + " r" + pNumA + ", " + r2b4 + " => " + feas1);
				System.out.println("storeAI " + feas1 + " => r0, " + mem3);
				return;
			}
		
		
		// S, NP, NP
		if(isSpill1 == true && hasR2 == false && hasR3 == false){
			int pNumB = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumB = i + 3; break;}
			}
			int pNumC = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumC = i + 3; break;}
			}
			
			int mem1 = getOffset(r1, regs);
			System.out.println("loadAI r0, " + mem1 + " => " + feas1);
			System.out.println(itr.opCode + " " + feas1 + ", r" + pNumB + " => r" + pNumC);
			return;
		}
		
		
		//	S, NP, S
		if(isSpill1 == true && hasR2 == false && isSpill3 == true){
			int mem1 = getOffset(r1, regs);
			int mem3 = getOffset(r3, regs);
			int pNumB = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumB = i + 3; break;}
			}
			System.out.println("loadAI r0, " + mem1 + " => " + feas1);
			System.out.println(itr.opCode + " " + feas1 + ", r" + pNumB + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem3);
			return;
		}
		
		
		
		//	NP, S, S
		if(hasR1 == false && isSpill2 == true && isSpill3 == true){
			int mem2 = getOffset(r2, regs);
			int mem3 = getOffset(r3, regs);
			int pNumA = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumA = i + 3; break;}
			}
			System.out.println("loadAI r0, " + mem2 + " => " + feas1);
			System.out.println(itr.opCode + " r" + pNumA + ", " + feas1 + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem3);
			return;
		}
		
		
		
		//	 NP, S, NP
		if(hasR1 == false && isSpill2 == true && hasR3 == false){
			int mem2 = getOffset(r2, regs);
			int pNumA = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumA = i + 3; break;}
			}
			int pNumC = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumC = i + 3; break;}
			}
			System.out.println("loadAI r0, " + mem2 + " => " + feas1);
			System.out.println(itr.opCode + " r" + pNumA + ", " + feas1 + " => r" + pNumC);
			return;
		}
		
		
		//	NP, NP, S
		if(hasR1 == false && hasR2 == false && isSpill3 == true){
			int mem3 = getOffset(r3, regs);
			int pNumA = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r1; pNumA = i + 3; break;}
			}
			int pNumB = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumB = i + 3; break;}
			}
			
			
			String rA = "r" + Integer.toString(pNumA);
			String rB = "r" + Integer.toString(pNumB);
			if(r2.equals(r1)){ rB = rA; }
			
			System.out.println(itr.opCode + " " + rA + ", " + rB + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem3);
			return;
		}
		
		
		
		// NP, NP, NP
		if(hasR1 == false && hasR2 == false && hasR3 == false){
			int pNumA = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumA = i + 3; break;}
			}
			int pNumB = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumB = i + 3; break;}
			}
			int pNumC = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumC = i + 3; break;}
			}
			
			String rA = "r" + Integer.toString(pNumA);
			String rB = "r" + Integer.toString(pNumB);
			if(r2.equals(r1)){ rB = rA; }
			
			System.out.println(itr.opCode + " " + rA + ", " + rB + " => r" + pNumC);
			return;
		}
		
		
		
		
		// P, NP, S
		if(hasR1 == true && hasR2 == false && isSpill3 == true){
			int mem3 = getOffset(r3, regs);
			int pNumB = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumB = i + 3; break;}
			}
			
			//check to avoid r3, r? => f1 -> !r3,r3 => f1
			String rB = "r" + Integer.toString(pNumB);
			if(r1b4.equals(rB) ){
				pRegs[pNumB-3] = "";
				for(int i = (pNumB-2); i < pRegs.length; i++){
					if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumB = i + 3; break;}
				}
			}
			
			
			System.out.println(itr.opCode + " " + r1b4 + ", r" + pNumB + " => " + feas1);
			System.out.println("storeAI " + feas1 + " => r0, " + mem3);
			return;
		}

		
		
		
		//	P, NP, NP
		if(hasR1 == true && hasR2 == false && hasR3 == false){
			int pNumB = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumB = i + 3; break;}
			}
			//check to avoid r3, r? => f1 -> !r3,r3 => f1
			String rB = "r" + Integer.toString(pNumB);
			if(r1b4.equals(rB) ){
				pRegs[pNumB-3] = "";
				for(int i = (pNumB-2); i < pRegs.length; i++){
					if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumB = i + 3; break;}
				}
			}
			
			int pNumC = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumC = i + 3; break;}
			}
			
			System.out.println(itr.opCode + " " + r1b4 + ", r" + pNumB + " => r" + pNumC);
			return;
		}
		
		
		
		
		//	NP, P, NP
		if(hasR1 == false && hasR2 == true && hasR3 == false){
			int pNumA = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumA = i + 3; break;}
			}
			//check to avoid r3, r? => f1 -> !r3,r3 => f1
			String rA = "r" + Integer.toString(pNumA);
			if(r2b4.equals(rA) ){
				pRegs[pNumA-3] = "";
				for(int i = (pNumA-2); i < pRegs.length; i++){
					if(pRegs[i].equals("") ){ pRegs[i] = r2; pNumA = i + 3; break;}
				}
			}
			
			int pNumC = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumC = i + 3; break;}
			}
			
			System.out.println(itr.opCode + " r" + pNumA + ", " + r2b4 + " => r" + pNumC);
			return;
		}
		
		
		// P, P, NP
		if(hasR1 == true && hasR2 == true && hasR3 == false){
			int pNumC = -1;
			for(int i = 0; i < pRegs.length; i++){
				if(pRegs[i].equals("") ){ pRegs[i] = r3; pNumC = i + 3; break;}
			}
			System.out.println(itr.opCode + " " + r1b4 + ", " + r2b4 + " => r" + pNumC);
			return;
		}
		
		
		
	}
	
	
	
	public static void topDown(Instruction ins, RegInfo regs, int k){
		
		RegInfo curr = regs;
		while(curr != null){ popMax(curr, ins); curr = curr.next; }
		ArrayList<String> spillRegs =  new ArrayList<String>();
		spillRegs = setSpill(ins, k);
		popStrMax(ins);
		setOffset(regs, spillRegs);
		String[] pRegs =  new String[k];
		for(int i = 0; i < k; i++){ pRegs[i] = ""; }
		String feas1 = "r1";
		String feas2 = "r2";
		
		Instruction itr = ins;
		itr = itr.next;
		System.out.println("loadI 1024 => r0");
		
		while(itr != null){
			String op = itr.opCode;
			
			if(op.equals("loadI")){ caseOne(itr, regs, spillRegs, pRegs, feas1, feas2, k); }
			
			else if(op.equals("load") || op.equals("store")){ caseTwo(itr, regs, spillRegs, pRegs, feas1, feas2, k); }
			
			else if( op.equals("add") || op.equals("sub") || op.equals("mult") || op.equals("lshift") || op.equals("rshift") ){
				caseThree(itr, regs, spillRegs, pRegs, feas1, feas2, k);
			}
			
			else if(op.equals("output")){ System.out.println(itr.opCode + " " + itr.field1); }
			
			itr = itr.next;
		} 
		
		
		return;
	}
	
	
}
