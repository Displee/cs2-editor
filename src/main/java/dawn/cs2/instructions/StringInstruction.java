package dawn.cs2.instructions;

import dawn.cs2.util.TextUtils;

public class StringInstruction extends AbstractInstruction {

    private String constant;

    public StringInstruction(int opcode, String constant) {
        super(opcode);
        this.constant = constant;
    }

    public String getConstant() {
        return constant;
    }

    @Override
    public String toString() {
        return String.format("%-16.16s %s", Opcodes.getOpcodeName(this.getOpcode()), TextUtils.quote(constant));
    }

}
