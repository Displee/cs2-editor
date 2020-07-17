package dawn.cs2.instructions;

import dawn.cs2.util.FunctionInfo;

public class BooleanInstruction extends AbstractInstruction {

    private boolean constant;

    public BooleanInstruction(int opcode, boolean constant) {
        super(opcode);
        this.constant = constant;
    }

    public boolean getConstant() {
        return constant;
    }

    @Override
    public String toString() {
        String name = Opcodes.getOpcodeName(this.getOpcode());

        if (name.startsWith("OP_") && Opcodes.opcodesDb != null) {
            FunctionInfo f = Opcodes.opcodesDb.getInfo(this.getOpcode());
            return String.format("%-16.16s %s", name, f);
        }
        return String.format("%-16.16s %b", name, constant);

    }

}
