package registerAllocator;

public class SimpleTopDown {

	public static String getMax(RegInfo regs){
		
		RegInfo max1 = regs;
		RegInfo curr = regs;
		curr = curr.next;
		
		while(max1.isMax == true && max1 != null){
			max1 = max1.next;
			if(max1.next != null){
				curr = max1.next;
			}
			else{
				break;
			}
		}
		
		while(curr != null){
			
			if(curr.isMax == true){
				curr = curr.next;
			}
			
			else{
				int currOcc = curr.occ;
				int max1occ = max1.occ;
				if(currOcc > max1occ){
					max1 = curr;
				}
				else if (curr.occ == max1.occ){
					if(curr.live > max1.live){
						max1 = curr;
					}
				} 	
				
				curr = curr.next;
			}
		}
		String maxR1 = max1.regName;
		max1.isMax = true;
		return maxR1;	
	}
		
	public static void setOffset(RegInfo regs){
		RegInfo itr = regs;
		int mem = 0;
		while(itr != null){
			if(itr.isMax == false){
				mem = mem - 4;
				itr.offset = mem;
			}
			itr = itr.next;
		}
		return;
	}
	
	public static int getOffset(String reg, RegInfo regs){
		RegInfo itr = regs;
		int mem = 0;
		while(itr != null){
			if(itr.regName.equals(reg)){ mem = itr.offset; }
			itr = itr.next;
		}
		return mem;
	}
	
	public static void simpleTopDown(Instruction ins, RegInfo regs, int k){
		
		String[] max = new String[k];
		for(int i = 0; i < k; i++){
			String maxR = getMax(regs);
			max[i] = maxR;
		}
		
		Instruction itr = ins;
		itr = itr.next;
		String feas1 = "r1";
		String feas2 = "r2";
		setOffset(regs);
		
		
		
		System.out.println("loadI 1024 => r0");
		
		while(itr != null){
			String op = itr.opCode;
			
			//------------------------------------------------------------------------------------------------------------------------------------------
			
			if(op.equals("loadI")){
				String r1 = itr.field2; //register being dealt with
				String phys = "";
				int pNum = 0;
				
				for(int i = 0; i < k; i++){
					if(max[i].equals(r1)){ pNum = i+1+2; phys = "r" + Integer.toString(pNum);}
				} //ends for loop
				
				if(pNum > 0){ System.out.println(itr.opCode + " " + itr.field1 + " => " + phys); }
				
				else{
					int mem = getOffset(r1, regs);
					System.out.println(itr.opCode + " " + itr.field1 + " => " + feas1);
					System.out.println("storeAI " + feas1 + " => r0, " + mem);
				}
			
			} //ends LOADI
			
			//------------------------------------------------------------------------------------------------------------------------------
			
			else if(op.equals("load") || op.equals("store")){
				
				String r1 = itr.field1; //register being dealt with
				String phys1 = "";
				int pNum1 = 0;
				String r2 = itr.field2; //register being dealt with
				String phys2 = "";
				int pNum2 = 0;
				
				for(int i = 0; i < k; i++){
					if(max[i].equals(r1)){ pNum1 = i+1+2; phys1 = "r" + Integer.toString(pNum1); }
				} 
				
				for(int i = 0; i < k; i++){
					if(max[i].equals(r2)){ pNum2 = i+1+2; phys2 = "r" + Integer.toString(pNum2); }
				} 
				
				if( !phys1.equals("") && !phys2.equals("") ){ System.out.println(itr.opCode + " " + phys1 + " => " + phys2); }
				
				else if( phys1.equals("") && !phys2.equals("") ){
					int mem = getOffset(r1, regs);
					System.out.println("loadAI r0, " + mem + " => " + feas1);
					System.out.println(itr.opCode + " " + feas1 + " => " + phys2);
				}
				
				else if( !phys1.equals("") && phys2.equals("") ){
					int mem = getOffset(r2, regs);
					System.out.println(itr.opCode + " " + phys1 + " => " + feas1);
					System.out.println("storeAI " + feas1 + " => r0, " + mem);
				}
				
				else{ 
					
				
					
					
					int mem1 = getOffset(r1, regs);
					int mem2 = getOffset(r2, regs);
					System.out.println("loadAI r0, " + mem1 + " => " + feas1);
					System.out.println("loadAI r0, " + mem2 + " => " + feas2);
					System.out.println(itr.opCode + " " + feas1 + " => " + feas2);
					System.out.println("storeAI " + feas2 + " => r0, " + mem2);
				}
				
				
			} //ends LOAD / STORE
			
			//--------------------------------------------------------------------------------------------------------------------------------
			
			
			else if( op.equals("add") || op.equals("sub") || op.equals("mult") || op.equals("lshift") || op.equals("rshift") ){
				
				String r1 = itr.field1; //register being dealt with
				String phys1 = "";
				int pNum1 = 0;
				String r2 = itr.field2; //register being dealt with
				String phys2 = "";
				int pNum2 = 0;
				String r3 = itr.field3; //register being dealt with
				String phys3 = "";
				int pNum3 = 0;
				
				for(int i = 0; i < k; i++){
					if(max[i].equals(r1)){ pNum1 = i+1+2; phys1 = "r" + Integer.toString(pNum1); }
				} 
				
				for(int i = 0; i < k; i++){
					if(max[i].equals(r2)){ pNum2 = i+1+2; phys2 = "r" + Integer.toString(pNum2); }
				} 
				
				for(int i = 0; i < k; i++){
					if(max[i].equals(r3)){ pNum3 = i+1+2; phys3 = "r" + Integer.toString(pNum3); }
				} 
				
				
				if(pNum1 > 0 && pNum2 > 0 && pNum3 > 0){ System.out.println(itr.opCode + " " + phys1 + ", " + phys2 + " => " + phys3); }
				
				else if(pNum1 <= 0 && pNum2 > 0 && pNum3 > 0){
					int mem1 = getOffset(r1, regs);
					System.out.println("loadAI r0, " + mem1 + " => " + feas1);
					System.out.println(itr.opCode + " " + feas1 + ", " + phys2 + " => " + phys3);
				}
				
				else if(pNum1 > 0 && pNum2 <= 0 && pNum3 > 0){
					int mem2 = getOffset(r2, regs);
					System.out.println("loadAI r0, " + mem2 + " => " + feas1);
					System.out.println(itr.opCode + " " + phys1 + ", " + feas1 + " => " + phys3);
				}
				
				else if(pNum1 > 0 && pNum2 > 0 && pNum3 <= 0){
					int mem3 = getOffset(r3, regs);
					System.out.println(itr.opCode + " " + phys1 + ", " + phys2 + " => " + feas1);
					System.out.println("storeAI " + feas1 + " => r0, " + mem3);
				}
				
				else if(pNum1 <= 0 && pNum2 <= 0 && pNum3 > 0){
					int mem1 = getOffset(r1, regs);
					int mem2 = getOffset(r2, regs);
					System.out.println("loadAI r0, " + mem1 + " => " + feas1);
					System.out.println("loadAI r0, " + mem2 + " => " + feas2);
					System.out.println(itr.opCode + " " + feas1 + ", " + feas2 + " => " + phys3);
				}
				
				else if(pNum1 <= 0 && pNum2 > 0 && pNum3 <= 0){
					int mem1 = getOffset(r1, regs);
					int mem3 = getOffset(r3, regs);
					System.out.println("loadAI r0, " + mem1 + " => " + feas1);
					System.out.println(itr.opCode + " " + feas1 + ", " + phys2 + " => " + feas1);
					System.out.println("storeAI " + feas1 + " => r0, " + mem3);
				}
				
				else if(pNum1 > 0 && pNum2 <= 0 && pNum3 <= 0){
					int mem2 = getOffset(r2, regs);
					int mem3 = getOffset(r3, regs);
					System.out.println("loadAI r0, " + mem2 + " => " + feas1);
					System.out.println(itr.opCode + " " + phys1 + ", " + feas1 + " => " + feas1);
					System.out.println("storeAI " + feas1 + " => r0, " + mem3);
				}
				
				else{
					int mem1= getOffset(r1, regs);
					int mem2 = getOffset(r2, regs);
					int mem3 = getOffset(r3, regs);
					System.out.println("loadAI r0, " + mem1 + " => " + feas1);
					System.out.println("loadAI r0, " + mem2 + " => " + feas2);
					System.out.println(itr.opCode + " " + feas1 + ", " + feas2 + " => " + feas1);
					System.out.println("storeAI " + feas1 + " => r0, " + mem3);
				}
				
			} // ends ADD / SUB / MULT / LSHIFT / RSHIFT
			
			//-----------------------------------------------------------------------------------------------------------------------------------------
			
			else if(op.equals("output")){
				System.out.println(itr.opCode + " " + itr.field1);
			}
			

			//--------------------------------------------------------------------------------------------------------------------------------------
			
			itr = itr.next;
		} 
		
		
		return;
	}
}
