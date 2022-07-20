package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.DecompilerException;
import dawn.cs2.instructions.AbstractInstruction;
import dawn.cs2.instructions.IntInstruction;
import dawn.cs2.instructions.Opcodes;

public class LocalVariable implements Variable {

    public static LocalVariable CHILD = new LocalVariable("CHILD", null, false);
    public static LocalVariable _CHILD = new LocalVariable("_CHILD", null, false);

    private String name;
    private CS2Type type;
    private int identifier = -1;
    private boolean isArgument;

    public LocalVariable(String name, CS2Type type) {
        this(name, type, false);
    }

    public LocalVariable(String name, CS2Type type, boolean isArgument) {
        this.name = name;
        this.type = type;
        this.isArgument = isArgument;
    }

    @Override
    public CS2Type getType() {
        return type;
    }

    public void changeType(CS2Type type) {
        if (!type.isCompatible(this.type)) {
            throw new DecompilerException("incompatible");
        }
        this.type = type;
        if (type != CS2Type.INT) {
            name = name.replace("arg", type.getName().toLowerCase()).replace("int", type.getName().toLowerCase());
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isArgument() {
        return isArgument;
    }

    @Override
    public String toString() {
        return type + " " + name;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public int getIdentifier() {
        return identifier;
    }

    public static int makeIdentifier(int index, int stackType) {
        return index | stackType << 16;
    }

    @Override
    public AbstractInstruction generateStoreInstruction() {
        int type = getIdentifier() >> 16;
        int id = getIdentifier() & 0xffff;
        if (type == 0) {
            return new IntInstruction(Opcodes.POP_INT_LOCAL, id);
        } else if (type == 1) {
            return new IntInstruction(Opcodes.POP_STRING_LOCAL, id);
        } else if (type == 2) {
            return new IntInstruction(Opcodes.STORE_LONG, id);
        }
        assert false : this;
        return null;
    }

    @Override
    public AbstractInstruction generateLoadInstruction() {
        int type = getIdentifier() >> 16;
        int id = getIdentifier() & 0xffff;
        if (type == 0) {
            return new IntInstruction(Opcodes.PUSH_INT_LOCAL, id);
        } else if (type == 1) {
            return new IntInstruction(Opcodes.PUSH_STRING_LOCAL, id);
        } else if (type == 2) {
            return new IntInstruction(Opcodes.LOAD_LONG, id);
        }
        //??
        throw new DecompilerException("unhandled var type" + this);
    }
}
