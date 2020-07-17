package dawn.cs2;

import java.util.ArrayList;
import java.util.List;

import dawn.cs2.instructions.AbstractInstruction;
import dawn.cs2.instructions.Label;
import dawn.cs2.instructions.SwitchInstruction;

public class CS2 {

//	public static boolean OSRS = false;

	public final int scriptID;
	private String name;
	private CS2Type[] arguments;
	private CS2Type returnType = CS2Type.UNKNOWN;
	private int intLocalsSize;
	private int stringLocalsSize;
	private int longLocalsSize;
	private int intArgumentsCount;
	private int stringArgumentsCount;
	private int longArgumentsCount;
	public AbstractInstruction[] instructions;

	public CS2(int scriptID, CS2Type[] args, int intls, int stringls, int longls, int intac, int stringac, int longac, int codeSize) {
		this.scriptID = scriptID;
		this.arguments = args;
		this.intLocalsSize = intls;
		this.stringLocalsSize = stringls;
		this.longLocalsSize = longls;
		this.intArgumentsCount = intac;
		this.stringArgumentsCount = stringac;
		this.longArgumentsCount = longac;
		this.instructions = new AbstractInstruction[codeSize * 2 +1];
	}

	public int getScriptID() {
		return scriptID;
	}

	public String getName() {
		return name;
	}

	public void setReturnType(CS2Type returnType) {
		this.returnType = returnType;
	}

	public CS2Type getReturnType() {
		return returnType;
	}

	public CS2Type[] getArguments() {
		return arguments;
	}

	public void setIntLocalsSize(int intLocalsSize) {
		this.intLocalsSize = intLocalsSize;
	}

	public int getIntLocalsSize() {
		return intLocalsSize;
	}

	public void setStringLocalsSize(int stringLocalsSize) {
		this.stringLocalsSize = stringLocalsSize;
	}

	public int getStringLocalsSize() {
		return stringLocalsSize;
	}

	public void setLongLocalsSize(int longLocalsSize) {
		this.longLocalsSize = longLocalsSize;
	}

	public int getLongLocalsSize() {
		return longLocalsSize;
	}

	public int getIntArgumentsCount() {
		return intArgumentsCount;
	}
	
	public int getStringArgumentsCount() {
		return stringArgumentsCount;
	}

	public int getLongArgumentsCount() {
		return longArgumentsCount;
	}

	public AbstractInstruction[] getInstructions() {
		return instructions;
	}
	
	@Deprecated
	public int addressOf(AbstractInstruction instr) {
		for (int i = 0; i < this.instructions.length; i++)
			if (instructions[i] == instr)
				return i;
		return -1;
	}


	public void prepareInstructions() {
		List<AbstractInstruction> buffer = new ArrayList<AbstractInstruction>();
		for (int i = 0; i < this.instructions.length; i++)
			if (this.instructions[i] != null)
				buffer.add(this.instructions[i]);
		this.instructions = new AbstractInstruction[buffer.size()];
		int write = 0;
		for (AbstractInstruction instr : buffer)
			this.instructions[write++] = instr;
		for (int i = 0; i < instructions.length; i++)
			instructions[i].setAddress(i);
		for (int i = 0,labelsFound = 0; i < instructions.length; i++)
			if (instructions[i] instanceof Label)
				((Label)instructions[i]).setLabelID(labelsFound++);
		for (int i = 0; i < instructions.length; i++)
			if (instructions[i] instanceof SwitchInstruction)
				((SwitchInstruction)instructions[i]).sort();
	}

	
	public int countOf(Class<? extends AbstractInstruction> type) { 
		int total = 0;
		for (int i = 0; i < instructions.length; i++)
			if (instructions[i].getClass() == type)
				total++;
		return total;
	}

}
