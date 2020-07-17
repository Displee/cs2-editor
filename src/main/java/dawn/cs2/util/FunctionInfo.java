package dawn.cs2.util;

import dawn.cs2.CS2Type;
import dawn.cs2.ast.FunctionNode;

public class FunctionInfo {

    public String name;
    public int id;
    public CS2Type returnType;
    public CS2Type[] returnTypes;
    public CS2Type[] argumentTypes;
    public String[] argumentNames;
    public boolean isScript;

    public FunctionInfo(String name, int id, CS2Type[] argTypes, CS2Type returnType, String[] argNames, boolean isScript) {
        this.name = name;
        this.id = id;
        this.argumentTypes = argTypes;
        this.returnType = returnType;
        this.argumentNames = argNames;
        this.isScript = isScript;
    }

    public FunctionInfo(String name, int id, CS2Type[] argTypes, CS2Type returnType, CS2Type[] returnTypes, String[] argNames, boolean isScript) {
        this.name = name;
        this.id = id;
        this.argumentTypes = argTypes;
        this.returnType = returnType;
        this.returnTypes = returnTypes;
        this.argumentNames = argNames;
        this.isScript = isScript;
    }

    public FunctionInfo(int id, FunctionNode sig) {
        this.name = sig.getName();
        this.id = id;
        this.argumentTypes = sig.getArguments();
        this.returnType = sig.getReturnType();
        this.argumentNames = new String[argumentTypes.length];
        this.isScript = true;
    }


    public String getName() {
        return name;
    }

    public CS2Type[] getArgumentTypes() {
        return argumentTypes;
    }

    public CS2Type getReturnType() {
        return returnType;
    }

    public CS2Type[] getReturnTypes() {
        return returnTypes;
    }

    public String[] getArgumentNames() {
        return argumentNames;
    }

    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append(returnType);
        bld.append(' ');
        bld.append(name);
        bld.append('(');
        for (int i = 0; i < argumentTypes.length; i++) {
            bld.append(argumentTypes[i].toString() /*+ " " + argumentNames[i]*/);
            if ((i + 1) < argumentTypes.length)
                bld.append(", ");
        }
        bld.append(')');
//		bld.append(';');
        return bld.toString();
    }
}
