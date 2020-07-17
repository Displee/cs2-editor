package dawn.cs2.util;

import dawn.cs2.ast.Operator;

public class OpcodeUtils {

	
	public static int getTwoConditionsJumpStackType(int opcode) {
		if ((opcode >= 7 && opcode <= 10) || opcode == 31 || opcode == 32)
			return 0;
		else if (opcode >= 68 && opcode <= 73)
			return 2;
		else
			return -1;
	}
	
	public static Operator getTwoConditionsJumpConditional(int opcode) {
		switch (opcode) {
			case 7:
			case 68:
				return Operator.NEQ; // !=
			case 8:
			case 69:
				return Operator.EQ; // ==
			case 9:
			case 70:
				return Operator.LT; // <
			case 10:
			case 71:
				return Operator.GT; // >
			case 31:
			case 72:
				return Operator.LE; // <=
			case 32:
			case 73:
				return Operator.GE; // >=
			default:
				return null;
		}
	}
	
}
