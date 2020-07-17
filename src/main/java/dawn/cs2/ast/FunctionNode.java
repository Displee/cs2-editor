package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

import java.util.List;

public class FunctionNode extends AbstractCodeNode {

    private String name;
    private CS2Type[] arguments;
    private LocalVariable[] argumentLocals;
    private CS2Type returnType;
    private ScopeNode mainScope;


    public FunctionNode(String name, CS2Type[] args, CS2Type returnType, ScopeNode scope) {
        this.name = name;
        this.arguments = args;
        this.returnType = returnType;
        this.argumentLocals = new LocalVariable[args.length];
        this.mainScope = scope;
    }

    public FunctionNode(String name, CS2Type[] args, CS2Type returnType, ScopeNode scope, List<LocalVariable> locals) {
        this.name = name;
        this.arguments = args;
        this.returnType = returnType;
        this.argumentLocals = locals.toArray(new LocalVariable[locals.size()]);
        this.mainScope = scope;
    }

    public String getName() {
        return name;
    }

    public CS2Type[] getArguments() {
        return arguments;
    }

    public LocalVariable[] getArgumentLocals() {
        return argumentLocals;
    }

    public void setReturnType(CS2Type returnType) {
        this.returnType = returnType;
    }

    public CS2Type getReturnType() {
        return returnType;
    }


    public ScopeNode getMainScope() {
        return mainScope;
    }


    @Override
    public void print(CodePrinter printer) {
        printer.print(this.returnType.toString());
        printer.print(' ');
        printer.print(this.name);
        printer.print('(');
        for (int i = 0; i < argumentLocals.length; i++) {
            printer.print(argumentLocals[i].toString());
            if ((i + 1) < arguments.length)
                printer.print(", ");
        }
        printer.print(')');
        printer.print(' ');
        this.mainScope.print(printer);
    }


}
