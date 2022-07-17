package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.DecompilerException;
import dawn.cs2.instructions.AbstractInstruction;
import dawn.cs2.instructions.IntInstruction;
import dawn.cs2.instructions.Opcodes;

public class GlobalVariable implements Variable {

    public static GlobalVariable find(String name, int idx, CS2Type type) {
        return new GlobalVariable(name, idx, type);
    }

    public static GlobalVariable VARP(int idx, CS2Type type) {
        return new GlobalVariable("VARP", idx, type);
    }

    public static GlobalVariable VARPBIT(int idx, CS2Type type) {
        return new GlobalVariable("VARPBIT", idx, type);
    }

    public static GlobalVariable VARC(int idx, CS2Type type) {
        return new GlobalVariable("VARC", idx, type);
    }

    public static GlobalVariable VARC_STRING(int idx) {
        return new GlobalVariable("STRING", idx, CS2Type.STRING);
    }

    public static GlobalVariable parse(String n) {
        int idx = Integer.parseInt(n.substring(n.indexOf('[') + 1, n.indexOf(']')));
        String prefix = n.substring(0, n.indexOf('['));
        switch (prefix) {
            case "VARP":
            case "VARPBIT":
            case "VARC":
            case "CLAN":
            case "CLANBIT":
            case "CLANDEF112":
            case "CLANDEF113":
                return find(prefix, idx, CS2Type.INT);
            case "STRING":
            case "CLANDEF_STRING":
            case "CLANDEF_STRING115":
                return find(prefix, idx, CS2Type.STRING);
            case "CLANDEF_LONG":
            case "CLANDEF_LONG114":
                return find(prefix, idx, CS2Type.LONG);
            default:
                throw new DecompilerException("I don't know how to parse this");
        }
    }

    private final String name;
    private final int idx;
    private CS2Type type;

    private GlobalVariable(String name, int idx, CS2Type type) {
        this.name = name;
        this.idx = idx;
        this.type = type;
    }

    @Override
    public String getName() {
        return name + "[" + idx + "]";
    }

    @Override
    public CS2Type getType() {
        return type;
    }

    @Override
    public AbstractInstruction generateStoreInstruction() {
        switch (name) {
            case "VARP":
                return new IntInstruction(Opcodes.STORE_VARP, idx);
            case "VARPBIT":
                return new IntInstruction(Opcodes.STORE_VARPBIT, idx);
            case "VARC":
                return new IntInstruction(Opcodes.STORE_VARC, idx);
            case "STRING":
                return new IntInstruction(Opcodes.STORE_VARCSTR, idx);
            default:
                throw new DecompilerException("This global is read-only");
        }
    }

    @Override
    public AbstractInstruction generateLoadInstruction() {
        int op = -1;
        switch (name) {
            case "VARP":
                op = Opcodes.LOAD_VARP;
                break;
            case "VARPBIT":
                op = Opcodes.LOAD_VARPBIT;
                break;
            case "VARC":
                op = Opcodes.LOAD_VARC;
                break;
            case "STRING":
                op = Opcodes.LOAD_VARCSTR_NEW;
                break;
            //These are READONLY, some are not even used
            case "CLANDEF_STRING115":
                op = 115;
                break;
            case "CLANDEF_LONG114":
                op = 114;
                break;
            case "CLANDEF113":
                op = 113;
                break;
            case "CLANDEF112":
                op = 112;
                break;
            case "CLANDEF_STRING":
                op = 108;
                break;
            case "CLANDEF_LONG":
                op = 109;
                break;
            case "CLANBIT":
                op = 107;
                break;
            case "CLAN":
                op = 106;
                break;
            default:
                throw new DecompilerException("I don't know how to load this");
        }
        return new IntInstruction(op, idx);

    }
}
