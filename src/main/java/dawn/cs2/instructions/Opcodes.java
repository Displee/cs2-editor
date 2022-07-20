package dawn.cs2.instructions;

import dawn.cs2.util.FunctionDatabase;

import java.lang.reflect.Field;

public class Opcodes {

    public static final int PUSH_CONSTANT_INT = 0;
    public static final int PUSH_VAR = 1;
    public static final int POP_VAR = 2;
    public static final int PUSH_CONSTANT_STRING = 3;
    public static final int BRANCH = 6;
    public static final int BRANCH_NOT = 7;
    public static final int BRANCH_EQUALS = 8;
    public static final int BRANCH_LESS_THAN = 9;
    public static final int BRANCH_GREATER_THAN = 10;
    public static final int RETURN = 21;
    public static final int PUSH_VARBIT = 25;
    public static final int POP_VARBIT = 27;
    public static final int BRANCH_LESS_THAN_OR_EQUALS = 31;
    public static final int BRANCH_GREATER_THAN_OR_EQUALS = 32;
    public static final int PUSH_INT_LOCAL = 33;
    public static final int POP_INT_LOCAL = 34;
    public static final int PUSH_STRING_LOCAL = 35;
    public static final int POP_STRING_LOCAL = 36;
    public static final int JOIN_STRING = 37;
    public static final int POP_INT_DISCARD = 38;
    public static final int POP_STRING_DISCARD = 39;
    public static final int GOSUB_WITH_PARAMS = 40;
    public static final int PUSH_VARC_INT = 42;
    public static final int POP_VARC_INT = 43;
    public static final int DEFINE_ARRAY = 44;
    public static final int PUSH_ARRAY_INT = 45;
    public static final int POP_ARRAY_INT = 46;
    public static final int LOAD_VARCSTR = 47;
    public static final int STORE_VARCSTR = 48;
    public static final int LOAD_VARCSTR_NEW = 49;
    public static final int SWITCH = 51;
    public static final int PUSH_LONG = 54;
    public static final int POP_LONG = 55;
    public static final int LOAD_LONG = 66;
    public static final int STORE_LONG = 67;
    public static final int LONG_EQ = 69;
    public static final int LONG_NE = 68;
    public static final int LONG_LT = 70;
    public static final int LONG_GT = 71;
    public static final int LONG_LE = 72;
    public static final int LONG_GE = 73;
    public static final int EQ1 = 86;
    public static final int EQ0 = 87;

    public static FunctionDatabase opcodesDb = null;
//    public static FunctionDatabase scriptsDb = null;


    public static String getOpcodeName(int opcode) {
        try {
            Field[] flds = Opcodes.class.getFields();
            for (Field f : flds) {
                if (f.getType() != int.class || f.getInt(null) != opcode) {
                    continue;
                }
                return (f.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OP_" + opcode;
    }

    public static int oppositeJump(int op) {
        switch (op) {
            case BRANCH_EQUALS:
                return BRANCH_NOT;
            case BRANCH_NOT:
                return BRANCH_EQUALS;
            case BRANCH_GREATER_THAN_OR_EQUALS:
                return BRANCH_LESS_THAN;
            case BRANCH_LESS_THAN:
                return BRANCH_GREATER_THAN_OR_EQUALS;
            case BRANCH_GREATER_THAN:
                return BRANCH_LESS_THAN_OR_EQUALS;
            case BRANCH_LESS_THAN_OR_EQUALS:
                return BRANCH_GREATER_THAN;
        }
        return -1;
    }

    public static boolean isAssign(int op) {
        switch (op) {
            case POP_INT_DISCARD:
            case POP_STRING_DISCARD:
            case POP_LONG:
            case POP_INT_LOCAL:
            case POP_STRING_LOCAL:
            case STORE_LONG:
            case POP_VAR:
            case POP_VARBIT:
            case POP_VARC_INT:
            case STORE_VARCSTR:
//            case ARRAY_STORE:
                return true;
        }
        return false;
    }

}
