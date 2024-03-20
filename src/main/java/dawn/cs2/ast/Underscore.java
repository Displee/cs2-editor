package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.DecompilerException;
import dawn.cs2.instructions.AbstractInstruction;
import dawn.cs2.instructions.BooleanInstruction;
import dawn.cs2.instructions.IntInstruction;
import dawn.cs2.instructions.Opcodes;

public class Underscore implements Variable {

    public static Underscore UNDERSCORE_I = new Underscore(CS2Type.INT);
    public static Underscore UNDERSCORE_S = new Underscore(CS2Type.STRING);
    public static Underscore UNDERSCORE_L = new Underscore(CS2Type.LONG);
    public static Underscore UNDERSCORE = new Underscore(CS2Type.UNKNOWN);
    private CS2Type type;

    private Underscore(CS2Type type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return "_";
    }

    @Override
    public CS2Type getType() {
        return type;
    }

    public static Underscore forType(CS2Type other) {
        if (other.isCompatible(CS2Type.INT)) {
            return UNDERSCORE_I;
        } else if (other.isCompatible(CS2Type.STRING)) {
            return UNDERSCORE_S;
        } else if (other.isCompatible(CS2Type.LONG)) {
            return UNDERSCORE_L;
        }
        return UNDERSCORE;
    }

    @Override
    public AbstractInstruction generateStoreInstruction() {
        if (this == UNDERSCORE_I) {
            return new BooleanInstruction(Opcodes.POP_INT_DISCARD, false);
        } else if (this == UNDERSCORE_S) {
            return new BooleanInstruction(Opcodes.POP_STRING_DISCARD, false);
        } else if (this == UNDERSCORE_L) {
            return new IntInstruction(Opcodes.POP_LONG, 0);
        } else {
            throw new RuntimeException("Cannot generate instruction for unknown type");
        }
    }

    @Override
    public AbstractInstruction generateLoadInstruction() {
        throw new DecompilerException("Can't load the void");
    }
}
