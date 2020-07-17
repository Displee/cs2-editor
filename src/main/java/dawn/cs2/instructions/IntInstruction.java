package dawn.cs2.instructions;

import dawn.cs2.util.FunctionInfo;

public class IntInstruction extends AbstractInstruction {

    private int constant;

    public IntInstruction(int opcode, int constant) {
        super(opcode);
        this.constant = constant;
    }

    public int getConstant() {
        return constant;
    }

    @Override
    public String toString() {
        String name = Opcodes.getOpcodeName(this.getOpcode());
        if (name.equals("CALL_CS2")) {
//            if (Opcodes.scriptsDb != null) {
//                FunctionInfo f = Opcodes.scriptsDb.getInfo(constant);
//                if (f != null) {
//                    return String.format("%-16.16s %s", "CALL_" + constant, f.toString());
//                }
//            }
        }
        return String.format("%-16.16s %d", name, constant);
    }

}
