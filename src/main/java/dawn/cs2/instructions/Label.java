package dawn.cs2.instructions;

public class Label extends AbstractInstruction {

//	private List<AbstractInstruction> jumpers = new ArrayList<AbstractInstruction>();
	private int labelID;
	
	public Label() {
		super(-1);
	}
	
	@Override
	public int hashCode() {
		return labelID;
	}

//	public List<AbstractInstruction> getJumpers() {
//		return jumpers;
//	}
//
//	public AbstractInstruction getLastJumper() {
//		int addr = -1;
//		AbstractInstruction jmp = null;
//		for (AbstractInstruction instr : jumpers) {
//			if (instr.getAddress() > addr) {
//				addr = instr.getAddress();
//				jmp = instr;
//			}
//		}
//		return jmp;
//	}

	@Override
	public String toString() {
		return "label_" + labelID + ":";// (" +super.hashCode()+")";
	}

	public void setLabelID(int labelID) {
		this.labelID = labelID;
	}

	public int getLabelID() {
		return labelID;
	}
}
