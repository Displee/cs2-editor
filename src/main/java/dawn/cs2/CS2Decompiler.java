package dawn.cs2;

import dawn.cs2.ast.FunctionNode;
import dawn.cs2.ast.LocalVariable;
import dawn.cs2.ast.ScopeNode;
import dawn.cs2.util.FunctionDatabase;
import dawn.cs2.util.FunctionInfo;

import java.util.stream.IntStream;

public class CS2Decompiler {

    private CS2 cs2;
    private FunctionNode function;
    private FunctionDatabase opcodesDatabase;
    private FunctionDatabase scriptsDatabase;

//    public CS2Decompiler(CS2 cs2) {
//        this(cs2, new FunctionDatabase(new File("rs2_new.ini"), false), new FunctionDatabase(new File("scripts_db.ini"), true));
//    }


    public CS2Decompiler(CS2 cs2, FunctionDatabase opcodesDatabase, FunctionDatabase scriptsDatabase) {
        this.opcodesDatabase = opcodesDatabase;
        this.scriptsDatabase = scriptsDatabase;
        this.cs2 = cs2;
        FunctionInfo info = scriptsDatabase.getInfo(cs2.getScriptID());
//        if (info == null) throw new DecompilerException("no script def in scripts db " + cs2.getScriptID());
        if (info == null) {
            info = new FunctionInfo("script_" + cs2.scriptID, cs2.getScriptID(), cs2.getArguments(), CS2Type.UNKNOWN, IntStream.range(0, cs2.getArguments().length).mapToObj(i -> "arg" + i).toArray(String[]::new), true);
            scriptsDatabase.putInfo(cs2.getScriptID(), info);
        }
        this.function = new FunctionNode(info.getName(), info.getArgumentTypes(), info.getReturnType(), new ScopeNode());
    }


    public void decompile() throws DecompilerException {
        this.declareAllVariables();
        FlowBlocksGenerator generator = new FlowBlocksGenerator(this);
        generator.generate();

        ControlFlowSolver main = new ControlFlowSolver(this, function.getMainScope(), generator.getBlocks());
        main.solve();
    }

    public void optimize() {

    }

    private void declareAllVariables() {

        int i = 0, ic = 0, sc = 0, lc = 0;
        for (; i < function.getArguments().length; i++) {
            CS2Type argument = function.getArguments()[i];
            if (argument.isCompatible(CS2Type.INT)) {
                LocalVariable var = new LocalVariable("arg" + i, argument, true);
                var.setIdentifier(LocalVariable.makeIdentifier(ic++, 0));
                this.function.getMainScope().declare(this.function.getArgumentLocals()[i] = var);
            } else if (argument.isCompatible(CS2Type.STRING)) {
                LocalVariable var = new LocalVariable("arg" + i, argument, true);
                var.setIdentifier(LocalVariable.makeIdentifier(sc++, 1));
                this.function.getMainScope().declare(this.function.getArgumentLocals()[i] = var);
            } else if (argument.isCompatible(CS2Type.LONG)) {
                LocalVariable var = new LocalVariable("arg" + i, argument, true);
                var.setIdentifier(LocalVariable.makeIdentifier(lc++, 2));
                this.function.getMainScope().declare(this.function.getArgumentLocals()[i] = var);
            } else {
                throw new RuntimeException("structs in args?");
            }
        }
        if (ic != cs2.getIntArgumentsCount() || sc != cs2.getStringArgumentsCount() || lc != cs2.getLongArgumentsCount())
            throw new RuntimeException("Expected signature " + function.toString()+" binary args i"+cs2.getIntArgumentsCount()+" s"+cs2.getStringArgumentsCount()+" l"+cs2.getLongArgumentsCount());

        for (int j = cs2.getIntArgumentsCount(); j < cs2.getIntLocalsSize(); j++) {
            LocalVariable var = new LocalVariable("int" + i++, CS2Type.INT);
            var.setIdentifier(LocalVariable.makeIdentifier(j, 0));
            this.function.getMainScope().declare(var);
        }
        for (int j = cs2.getStringArgumentsCount(); j < cs2.getStringLocalsSize(); j++) {
            LocalVariable var = new LocalVariable("str" + i++, CS2Type.STRING);
            var.setIdentifier(LocalVariable.makeIdentifier(j, 1));
            this.function.getMainScope().declare(var);
        }
        for (int j = cs2.getLongArgumentsCount(); j < cs2.getLongLocalsSize(); j++) {
            LocalVariable var = new LocalVariable("long" + i++, CS2Type.LONG);
            var.setIdentifier(LocalVariable.makeIdentifier(j, 2));
            this.function.getMainScope().declare(var);
        }
    }


    public CS2 getCs2() {
        return cs2;
    }

    public FunctionNode getFunction() {
        return function;
    }

    public FunctionDatabase getOpcodesDatabase() {
        return opcodesDatabase;
    }

    public FunctionDatabase getScriptsDatabase() {
        return scriptsDatabase;
    }


}
