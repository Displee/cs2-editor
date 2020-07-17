package dawn.cs2.instructions;

public class AbstractInstruction {

	private int opcode;
	private int address;

	public AbstractInstruction(int opcode) {
		this.opcode = opcode;
		this.address = -1;
	}

	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}

	public int getOpcode() {
		return opcode;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public int getAddress() {
		return address;
	}

}
