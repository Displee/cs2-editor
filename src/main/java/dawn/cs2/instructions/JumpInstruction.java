package dawn.cs2.instructions;

public class JumpInstruction extends AbstractInstruction {

	private Label target;

	public JumpInstruction(int opcode, Label target) {
		super(opcode);
		this.target = target;
//		target.getJumpers().add(this);
	}

	public Label getTarget() {
		return target;
	}

	public void setTarget(Label target) {
		this.target = target;
	}

	@Override
	public String toString() {
		return String.format("%-16.16s %s", Opcodes.getOpcodeName(this.getOpcode()), target.toString());
	}

}
