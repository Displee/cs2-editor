package dawn.cs2.instructions;

import dawn.cs2.util.FunctionDatabase;

import java.lang.reflect.Field;

public class Opcodes {

    public static final int PUSH_INT = 0;
    public static final int PUSH_STRING = 3;
    public static final int PUSH_LONG = 54;
    public static final int POP_INT = 38;
    public static final int POP_STRING = 39;
    public static final int POP_LONG = 55;
    public static final int RETURN = 21;
    public static final int LOAD_INT = 33;
    public static final int STORE_INT = 34;
    public static final int LOAD_STRING = 35;
    public static final int STORE_STRING = 36;
    public static final int MERGE_STRINGS = 37;
    public static final int LOAD_LONG = 66;
    public static final int STORE_LONG = 67;
    public static final int STORE_VARPBIT = 27;
    public static final int LOAD_VARP = 1;
    public static final int STORE_VARP = 2;
    public static final int LOAD_VARC = 42;
    public static final int LOAD_VARCSTR = 47;
    public static final int LOAD_VARCSTR_NEW = 49;
    public static final int STORE_VARC = 43;
    public static final int LOAD_VARPBIT = 25;
    public static final int SWITCH = 51;

    public static final int ARRAY_NEW = 44;
    public static final int ARRAY_LOAD = 45;
    public static final int ARRAY_STORE = 46;


    public static final int STORE_VARCSTR = 48;
    public static final int CALL_CS2 = 40;
    public static final int GOTO = 6;
    public static final int INT_EQ = 8;
    public static final int INT_NE = 7;
    public static final int INT_LT = 9;
    public static final int INT_GT = 10;
    public static final int INT_LE = 31;
    public static final int INT_GE = 32;
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
            case INT_EQ:
                return INT_NE;
            case INT_NE:
                return INT_EQ;
            case INT_GE:
                return INT_LT;
            case INT_LT:
                return INT_GE;
            case INT_GT:
                return INT_LE;
            case INT_LE:
                return INT_GT;
        }
        return -1;
    }

    public static boolean isAssign(int op) {
        switch (op) {
            case POP_INT:
            case POP_STRING:
            case POP_LONG:
            case STORE_INT:
            case STORE_STRING:
            case STORE_LONG:
            case STORE_VARP:
            case STORE_VARPBIT:
            case STORE_VARC:
            case STORE_VARCSTR:
//            case ARRAY_STORE:
                return true;
        }
        return false;
    }

}
