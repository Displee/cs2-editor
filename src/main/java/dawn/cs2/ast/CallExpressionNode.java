package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;
import dawn.cs2.util.FunctionInfo;

public class CallExpressionNode extends ExpressionNode {

    public FunctionInfo info;
    public ExpressionNode[] arguments;
    public boolean invokeOnLastArg;


    public CallExpressionNode(FunctionInfo info, ExpressionNode[] arguments, boolean invokeOnLastArg) {
        this.info = info;
        this.arguments = arguments;
        this.invokeOnLastArg = invokeOnLastArg;
    }

    @Override
    public void solveCS2Type(ExpressionNode node, int index) {
        CS2Type[] types = getCS2Types();
        if (types.length == 0 || index >= types.length) {
            info.returnType = types[0];
            return;
        }
        info.returnType = types[index];
    }

    @Override
    public CS2Type getType() {
        return info.getReturnType();
    }

    @Override
    public CS2Type[] getCS2Types() {
        return info.getReturnTypes();
    }

    @Override
    public CallExpressionNode copy() {
        ExpressionNode[] argsCopy = new ExpressionNode[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if(arguments[i] == null) continue; //Possible because when doing something like script1(script2()) where script1 has 2 args and script2 returns 2 values, only 1 item will be on the stack!
            argsCopy[i] = arguments[i].copy();
        }
        return new CallExpressionNode(this.info, argsCopy, invokeOnLastArg);
    }

    @Override
    public void print(CodePrinter printer) {
        if (invokeOnLastArg) {
            arguments[arguments.length - 1].print(printer);
            printer.print(".");
        }
        printer.print(info.getName());
        printer.print('(');
        int max = arguments.length - (invokeOnLastArg ? 1 : 0);
        for (int i = 0; i < max; i++) {
            if (arguments[i] != null) { //this argument is fulfilled by another argument that is actually multiple return values
                arguments[i].print(printer);
            } else {
//                printer.print("/* <-- multiple values */ ");
            }
            if ((i + 1) < max && arguments[i + 1] != null)
                printer.print(", ");
        }
        printer.print(')');
    }

}
