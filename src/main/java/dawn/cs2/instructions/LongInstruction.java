package dawn.cs2.instructions;

public class LongInstruction extends AbstractInstruction {

    private long constant;

    public LongInstruction(int opcode, long constant) {
        super(opcode);
        this.constant = constant;
    }

    public long getConstant() {
        return constant;
    }

    @Override
    public String toString() {
        return String.format("%-16.16s %d", Opcodes.getOpcodeName(this.getOpcode()), constant);
    }

}
