package registerAllocator;

public class RegInfo {

	
	String regName;
	int entry;
	int last;
	int occ;
	int live;
	RegInfo next;
	boolean isMax;
	int offset;
	
	public RegInfo(String regName, int lineNum){
		this.regName = regName;
		this.entry = lineNum;
		this.last = lineNum;
		this.occ = 1;
		this.live = 0;
		this.next = null;
		this.isMax = false;
		this.offset = 0;
	}
	
	public String toString(){
		return this.regName + ": " + this.occ + "\t" + this.offset; 
	}
	
	
	public int getOcc(){
		return this.occ;
	}
	
	
}
