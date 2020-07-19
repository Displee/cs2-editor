package dawn.cs2;

import com.displee.io.impl.OutputBuffer;
import dawn.cs2.ast.*;
import dawn.cs2.instructions.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CS2Compiler {

    public FunctionNode function;
    public List<AbstractInstruction> instructions;

    private Map<Integer, Integer> scramble;
    private boolean supportEq01 = true;
    private boolean supportSwitch = true;
    private boolean supportLongs = true;

    public CS2Compiler(FunctionNode function, Map<Integer, Integer> scramble, boolean disableSwitches, boolean disableLongs) {
        this.function = function;
        this.scramble = scramble;
        this.supportEq01 = scramble.containsKey(Opcodes.EQ0);
        this.supportSwitch = !disableSwitches && scramble.containsKey(Opcodes.SWITCH);
        this.supportLongs = !disableLongs && scramble.containsKey(Opcodes.PUSH_LONG);
    }

    public byte[] compile(PrintWriter assembly) throws IOException {


        instructions = new LinkedList<>();
        compileNode(function.getMainScope());
        if (instructions.get(instructions.size() - 1).getOpcode() != Opcodes.RETURN) {
            instructions.add(new BooleanInstruction(Opcodes.RETURN, false));
        }

        optimize();

        //Finalize GOTO addresses by removing all dummy label instructions
        int addrC = 0;
        for (Iterator<AbstractInstruction> iterator = instructions.iterator(); iterator.hasNext(); ) {
            AbstractInstruction instr = iterator.next();
            if (instr instanceof Label) {
                iterator.remove();
                instr.setAddress(addrC);
                ((Label) instr).setLabelID(addrC);
            } else {
                instr.setAddress(addrC++);
            }
        }
        if (assembly != null) {
            int maxl = String.valueOf(instructions.size()).length();
            String format = "[%0" + maxl + "d]: %s" + System.lineSeparator();
            for (int i = 0; i < instructions.size(); i++) {
                assembly.write(String.format(format, i, instructions.get(i)));
            }
        }
//        System.err.println("-----------------------------");
        OutputBuffer output = new OutputBuffer(500000);
//        if (function.getName() != null)
//            output.writeString(function.getName());
//          else
        output.writeByte(0); //name terminator
        int switchCount = 0;
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInstruction instr = instructions.get(i);
            int opcode = instr.getOpcode();
            Integer n = scramble.get(opcode);
            if (n == null) {
                throw new DecompilerException("I don't know how to scramble this op " + opcode);
            }
            opcode = n;
            output.writeShort(opcode);
            if (instr instanceof SwitchInstruction) {
                output.writeInt(switchCount++);
            } else if (instr instanceof LongInstruction) {
                output.writeLong(((LongInstruction) instr).getConstant());
            } else if (instr instanceof StringInstruction) {
                output.writeString(((StringInstruction) instr).getConstant());
            } else if (instr instanceof JumpInstruction) {
                output.writeInt(((JumpInstruction) instr).getTarget().getAddress() - i - 1);
            } else if (instr instanceof IntInstruction) {
                output.writeInt(((IntInstruction) instr).getConstant());
            } else if (instr instanceof BooleanInstruction) {
                output.writeByte(((BooleanInstruction) instr).getConstant() ? 1 : 0);
            } else {
                throw new DecompilerException("");
            }

        }
        output.writeInt(instructions.size());
        int locI = 0;
        int locS = 0;
        int locL = 0;
        int argI = 0;
        int argS = 0;
        int argL = 0;
        for (LocalVariable var : function.getMainScope().copyDeclaredVariables()) {
            if (var.isArgument()) {
                if (var.getType().isCompatible(CS2Type.INT)) {
                    argI++;
                } else if (var.getType().isCompatible(CS2Type.STRING)) {
                    argS++;
                } else {
                    argL++;
                }
            } else {
                if (var.getType().isCompatible(CS2Type.INT)) {
                    locI++;
                } else if (var.getType().isCompatible(CS2Type.STRING)) {
                    locS++;
                } else {
                    locL++;
                }
            }
        }
        output.writeShort(locI + argI);
        output.writeShort(locS + argS);
        if (supportLongs)
            output.writeShort(locL + argL);
        output.writeShort(argI);
        output.writeShort(argS);
        if (supportLongs)
            output.writeShort(argL);
        if (supportSwitch) {
            long markSwitch = output.getOffset();
            output.writeByte(switchCount);
            for (int addr = 0; addr < instructions.size(); addr++) {
                AbstractInstruction instr = instructions.get(addr);
                if (instr instanceof SwitchInstruction) {
                    SwitchInstruction sw = (SwitchInstruction) instr;
                    output.writeShort(sw.cases.size());
                    for (int c = 0; c < sw.cases.size(); c++) {
                        output.writeInt(sw.cases.get(c));
                        output.writeInt(sw.targets.get(c).getAddress() - addr - 1);
                    }
                }
            }
            output.writeShort((short) (output.getOffset() - markSwitch));
        }
        return output.array();

    }

    private void compileNode(AbstractCodeNode node) {
        if (node instanceof PopableNode) {
            if (((PopableNode) node).getExpression() != null)
                compileExpression(((PopableNode) node).getExpression(), true);
        } else if (node instanceof ReturnNode) {
            if (((ReturnNode) node).getExpression() != null)
                compileExpression(((ReturnNode) node).getExpression());
            instructions.add(new BooleanInstruction(Opcodes.RETURN, false));
        } else if (node instanceof ScopeNode) {
            for (AbstractCodeNode child : ((ScopeNode) node).listChilds()) {
                compileNode(child);
            }
        } else if (node instanceof IfElseNode) {
            IfElseNode i = (IfElseNode) node;
            Label ifLabel = new Label();// labels++;

            Label afterIf = new Label();
            Label elseLabel = new Label();
            if (!compileConditionalJmp(i.condition, ifLabel, i.hasElseScope() ? elseLabel : afterIf)) {
                //boolean can be null, -1 = falsy
                if (supportEq01) {
                    instructions.add(new JumpInstruction(Opcodes.EQ1, ifLabel));
                } else {
                    instructions.add(new IntInstruction(Opcodes.PUSH_INT, 1));
                    instructions.add(new JumpInstruction(Opcodes.INT_EQ, ifLabel));
                }
                instructions.add(new JumpInstruction(Opcodes.GOTO, i.hasElseScope() ? elseLabel : afterIf));
            }

            //For IF and ELSE scopes
            //Original compiler emits EQ1 IF, GOTO ELSE,  L_IF: ...     GOTO AFTERIF, L_ELSE: ... , L_AFTERIF
            //We do (after optimizer) EQ1 IF, L_ELSE: ... GOTO AFTERIF, L_IF: ...,    L_AFTERIF
            //FOR IF WITHOUT ELSE SCOPES
            //Original compiler emits EQ1 IF, GOTO AFTERIF, L_IF: ... L_AFTERIF
            //We do the same. Following would be shorter but not valid due to null booleans:
            //                        EQ0 AFTERIF, L_IF: ... L_AFTERIF

            if (i.hasElseScope()) {
                instructions.add(elseLabel);
                //ELSE BODY
                compileNode(i.elseScope);
                instructions.add(new JumpInstruction(Opcodes.GOTO, afterIf));
            }

            instructions.add(ifLabel);
            //IF BODY
            compileNode(i.ifScope);
            instructions.add(afterIf);
        } else if (node instanceof LoopNode) {
            LoopNode loop = (LoopNode) node;
            assert loop.getType() == LoopNode.LOOPTYPE_WHILE : "Other loops not implemented";
            Label conditionLabel = new Label();
            Label bodyLabel = new Label();
            Label afterWhile = new Label();
            instructions.add(conditionLabel);
            if (!compileConditionalJmp(loop.getExpression(), bodyLabel, afterWhile)) {
//                instructions.add(new JumpInstruction(Opcodes.EQ0, afterWhile));
//                instructions.add(new JumpInstruction(Opcodes.GOTO, bodyLabel));
                if (supportEq01) {
                    instructions.add(new JumpInstruction(Opcodes.EQ1, bodyLabel));
                } else {
                    instructions.add(new IntInstruction(Opcodes.PUSH_INT, 1));
                    instructions.add(new JumpInstruction(Opcodes.INT_EQ, bodyLabel));
                }
                instructions.add(new JumpInstruction(Opcodes.GOTO, afterWhile));
            }
            instructions.add(bodyLabel);
            compileNode(loop.getScope());
            instructions.add(new JumpInstruction(Opcodes.GOTO, conditionLabel));
            instructions.add(afterWhile);
        } else if (node instanceof SwitchNode) {
            if (!supportSwitch) {
                throw new DecompilerException("Switch blocks are not supported in this revision!");
            }
            SwitchNode sw = (SwitchNode) node;
            compileExpression(sw.getExpression());
            sw.getScope().setCodeAddress(0);
            AbstractCodeNode c;
            Label caseEntry = new Label();
            Label afterSwitch = new Label();
            Label defCase = new Label();
            List<Label> labels = new LinkedList<>();
            List<Integer> cases = new LinkedList<>();

            SwitchInstruction instr = new SwitchInstruction(Opcodes.SWITCH, cases, labels);
            instructions.add(instr);
            instructions.add(new JumpInstruction(Opcodes.GOTO, defCase));

            while ((c = sw.getScope().read()) != null) {
                if (c instanceof CaseAnnotation) {
                    if (caseEntry == null) {
                        caseEntry = new Label();
                    }
                    if (!((CaseAnnotation) c).isDefault()) {
                        cases.add(((CaseAnnotation) c).getCaseNumber());
                        labels.add(caseEntry);
                    } else {
                        //Default case.
                        instructions.add(defCase);
                        defCase = caseEntry = null;
                    }
                } else {
                    if (caseEntry != null) {
                        instructions.add(caseEntry);
                        caseEntry = null;
                    }
                    if (c instanceof BreakNode) {
                        instructions.add(new JumpInstruction(Opcodes.GOTO, afterSwitch));
                    } else {
                        compileNode(c);
                    }
                }
            }
            instr.sort();
            instructions.add(new JumpInstruction(Opcodes.GOTO, afterSwitch)); //incase last case didnt have a break. always jump to afterswitch TODO: probably remove this?? doesnt seem correct (fallthrough to default case??)
            if (defCase != null) {
                //If there was no default case, insert the label anyway (we already made the jump), it will be merged with afterswitch label in optimizer
                instructions.add(defCase);
                instructions.add(new JumpInstruction(Opcodes.GOTO, afterSwitch));
            }
            instructions.add(afterSwitch);
        } else {
            System.out.println("UNHANDLED NODE " + node.getClass());
            throw new DecompilerException("unhandled node " + node);
        }
    }

    private boolean compileConditionalJmp(ExpressionNode expr, Label ifJump, Label elseJump) {
        if (expr instanceof ConditionalExpressionNode) {
            ConditionalExpressionNode c = (ConditionalExpressionNode) expr;

            ExpressionNode l = c.getLeft();

            // LOGIC OR AND
            if (c.conditional == Operator.OR || c.conditional == Operator.AND) {
                Label secondaryCondition = new Label();
                //6 = OR: if true enter IF, else check secondary condition
                //7 = AND: if true enter secondary condition, else enter ELSE
                Label ifTrue = c.conditional == Operator.OR ? ifJump : secondaryCondition;
                Label ifFalse = c.conditional == Operator.OR ? secondaryCondition : elseJump;
                if (!compileConditionalJmp(l, ifTrue, ifFalse)) {
                    if (supportEq01) {
                        instructions.add(new JumpInstruction(Opcodes.EQ1, ifTrue));
                    } else {
                        instructions.add(new IntInstruction(Opcodes.PUSH_INT, 1));
                        instructions.add(new JumpInstruction(Opcodes.INT_EQ, ifTrue));
                    }
                    instructions.add(new JumpInstruction(Opcodes.GOTO, ifFalse));
                }
                instructions.add(secondaryCondition);
                if (!compileConditionalJmp(c.getRight(), ifJump, elseJump)) {
                    if (supportEq01) {
                        instructions.add(new JumpInstruction(Opcodes.EQ1, ifJump));
                    } else {
                        instructions.add(new IntInstruction(Opcodes.PUSH_INT, 1));
                        instructions.add(new JumpInstruction(Opcodes.INT_EQ, ifJump));
                    }
                    instructions.add(new JumpInstruction(Opcodes.GOTO, elseJump));
                }
                return true;
            }

            boolean longInstr = l.getType() == CS2Type.LONG;
            assert longInstr || l.getType().isCompatible(CS2Type.INT);

            // LOGIC COMPARISONS
            compileExpression(l, false);
            ExpressionNode r = c.getRight();
            assert r.getType().isCompatible(longInstr ? CS2Type.LONG : CS2Type.INT);

            assert (l.getType() == CS2Type.BOOLEAN) == (r.getType() == CS2Type.BOOLEAN) : "Cannot compare boolean and non boolean expression";

            compileExpression(r, false);

            int op = -1;
            switch (c.conditional) {
                case NEQ:
                    op = longInstr ? Opcodes.LONG_NE : Opcodes.INT_NE;
                    break;
                case EQ:
                    op = longInstr ? Opcodes.LONG_EQ : Opcodes.INT_EQ;
                    break;
                case GT:
                    op = longInstr ? Opcodes.LONG_GT : Opcodes.INT_GT;
                    break;
                case LT:
                    op = longInstr ? Opcodes.LONG_LT : Opcodes.INT_LT;
                    break;
                case GE:
                    op = longInstr ? Opcodes.LONG_GE : Opcodes.INT_GE;
                    break;
                case LE:
                    op = longInstr ? Opcodes.LONG_LE : Opcodes.INT_LE;
                    break;
                default:
                    throw new DecompilerException("unknown conditional op " + c.conditional);
            }
            instructions.add(new JumpInstruction(op, ifJump));
            instructions.add(new JumpInstruction(Opcodes.GOTO, elseJump));
            return true;
        } else {
            if (expr instanceof BooleanConditionalExpressionNode) {
                BooleanConditionalExpressionNode bool = (BooleanConditionalExpressionNode) expr;
                if (bool.invert) { //invert ? swap jumps.
                    if (!compileConditionalJmp(bool.getCondition(), elseJump, ifJump)) {
                        assert bool.getCondition().getType().isCompatible(CS2Type.INT); //TODO: if long we need to PUSH 1L, LONG_EQ, but
                        //inner expression could either be a conditional eg !(1 > x) that would have made the jumps already OR an expression that only pushed 0 or 1 eg !(getBoolean()), so insert jumps ourselves in this case
//                        instructions.add(new JumpInstruction(Opcodes.EQ0, ifJump));
//                        instructions.add(new JumpInstruction(Opcodes.GOTO, elseJump));
                        if (supportEq01) {
                            instructions.add(new JumpInstruction(Opcodes.EQ1, elseJump));
                        } else {
                            instructions.add(new IntInstruction(Opcodes.PUSH_INT, 1));
                            instructions.add(new JumpInstruction(Opcodes.INT_EQ, elseJump));
                        }
                        instructions.add(new JumpInstruction(Opcodes.GOTO, ifJump));
                    }
                } else {
                    if (!compileConditionalJmp(bool.getCondition(), ifJump, elseJump)) {
                        assert bool.getCondition().getType().isCompatible(CS2Type.INT);
                        if (supportEq01) {
                            instructions.add(new JumpInstruction(Opcodes.EQ1, ifJump));
                        } else {
                            instructions.add(new IntInstruction(Opcodes.PUSH_INT, 1));
                            instructions.add(new JumpInstruction(Opcodes.INT_EQ, ifJump));
                        }
                        instructions.add(new JumpInstruction(Opcodes.GOTO, elseJump));
                    }
                }
                return true;
            }
            if (expr.getType() != CS2Type.BOOLEAN) {
                throw new DecompilerException("Condition requires boolean expression: " + expr);
            }

            //Part of a comparison (not logic)
            compileExpression(expr);
            return false;
        }
    }

    private void compileExpression(ExpressionNode expression) {
        compileExpression(expression, false);
    }

    private void compileExpression(ExpressionNode expression, boolean pop) {
        //Most expressions ignore the popable flag, and just assert what is expected. for example this makes it so int1 = int2 = 5 is not supported yet
        if (expression instanceof CallExpressionNode) {
            CallExpressionNode call = (CallExpressionNode) expression;
            boolean constant = false;

            int op = call.info.id;
            for (ExpressionNode arg : call.arguments) {
                if (arg instanceof VariableLoadNode) {
                    //Special argument. this is not an actual argument
                    VariableLoadNode loadArg = (VariableLoadNode) arg;
                    if (loadArg.getVariable() == LocalVariable._CHILD) {
                        constant = true;
                        continue;
                    }
                    if (loadArg.getVariable() == LocalVariable.CHILD) {
                        continue;
                    }
                }
                //Arg null means it is fulfilled by another arg which returns multiple values
                if (arg != null) {
                    if (arg instanceof CallbackExpressionNode && ((CallbackExpressionNode) arg).call.info == null) {
                        continue;
                    }
                    compileExpression(arg);
                }
            }
            if (call.info.isScript) {
                instructions.add(new IntInstruction(Opcodes.CALL_CS2, op));
            } else {
                instructions.add(new BooleanInstruction(op, constant));
            }
            if (pop) {
                List<CS2Type> composite = call.getType().composite;
                for (int i = composite.size() - 1; i >= 0; i--) {
                    CS2Type ret = composite.get(i);
                    if (ret.isCompatible(CS2Type.INT)) {
                        instructions.add(new BooleanInstruction(Opcodes.POP_INT, false));
                    } else if (ret.isCompatible(CS2Type.STRING)) {
                        instructions.add(new BooleanInstruction(Opcodes.POP_STRING, false));
                    } else if (ret.isCompatible(CS2Type.LONG)) {
                        instructions.add(new IntInstruction(Opcodes.POP_LONG, 0));
                    }
                }
            }
        } else if (expression instanceof CallbackExpressionNode) {
            assert !pop : "Not a statement " + expression;
            CallbackExpressionNode callback = (CallbackExpressionNode) expression;
            StringBuilder types = new StringBuilder();
            if (callback.call == null) {
                instructions.add(new IntInstruction(Opcodes.PUSH_INT, -1));
            } else {
                instructions.add(new IntInstruction(Opcodes.PUSH_INT, callback.call.info.id));
                for (ExpressionNode arg : callback.call.arguments) {
                    compileExpression(arg);
                    //Multiple return value call might be used as an argument
                    for (CS2Type carg : arg.getType().composite) {
                        types.append(carg.charDesc);
                    }
                }
            }
            if (callback.trigger != null) {
                compileExpression(callback.trigger);
                instructions.add(new IntInstruction(Opcodes.PUSH_INT, callback.trigger.arguments.size()));
                types.append("Y");
            }

            instructions.add(new StringInstruction(Opcodes.PUSH_STRING, types.toString()));
        } else if (expression instanceof ExpressionList) {
//            assert !pop : "Not a statement " + expression;
            for (ExpressionNode sub : ((ExpressionList) expression).arguments) {
                compileExpression(sub, pop);
            }
        } else if (expression instanceof MathExpressionNode) {
            assert !pop : "Not a statement " + expression;
            MathExpressionNode math = (MathExpressionNode) expression;
            if (math.operator == Operator.DIV && math.getLeft() instanceof MathExpressionNode && ((MathExpressionNode) math.getLeft()).operator == Operator.MUL) {
                //Optimization for MULDIV opcode 4018 ('scale')
                MathExpressionNode left = (MathExpressionNode) math.getLeft();
                compileExpression(left.getLeft());
                compileExpression(math.getRight());
                compileExpression(left.getRight());
                instructions.add(new BooleanInstruction(4018, false));
            } else {
                //Normal math
                compileExpression(math.getLeft());
                compileExpression(math.getRight());
                int op = math.operator == Operator.REM ? 4011 : 4000 + math.operator.ordinal();
                instructions.add(new BooleanInstruction(op, false));
            }
        } else if (expression instanceof PlaceholderValueNode) {
            assert !pop : "Not a statement " + expression;
            instructions.add(new IntInstruction(Opcodes.PUSH_INT, ((PlaceholderValueNode) expression).magic));
        } else if (expression instanceof NewWidgetPointerNode) {
            assert !pop : "Not a statement " + expression;
            NewWidgetPointerNode w = (NewWidgetPointerNode) expression;
            compileExpression(w.getExpression());
        } else if (expression instanceof NewLocationNode) {
            assert !pop : "Not a statement " + expression;
            NewLocationNode l = (NewLocationNode) expression;
            compileExpression(l.getExpression());
        } else if (expression instanceof NewColorNode) {
            assert !pop : "Not a statement " + expression;
            NewColorNode c = (NewColorNode) expression;
            compileExpression(c.getExpression());
        } else if (expression instanceof CharExpressionNode) {
            assert !pop : "Not a statement " + expression;
            CharExpressionNode c = (CharExpressionNode) expression;
            instructions.add(new IntInstruction(Opcodes.PUSH_INT, c.getData()));
        } else if (expression instanceof CastNode) {
            assert !pop : "Not a statement " + expression;
            CastNode c = (CastNode) expression;
            assert c.getType().isCompatible(c.getExpression().getType()) : "Incompatible types????";
            compileExpression(c.getExpression());
        } else if (expression instanceof IntExpressionNode) {
            assert !pop : "Not a statement " + expression;
            instructions.add(new IntInstruction(Opcodes.PUSH_INT, ((IntExpressionNode) expression).getData()));
        } else if (expression instanceof LongExpressionNode) {
            assert !pop : "Not a statement " + expression;
            instructions.add(new LongInstruction(Opcodes.PUSH_LONG, ((LongExpressionNode) expression).getData()));
        } else if (expression instanceof NullableIntExpressionNode) {
            assert !pop : "Not a statement " + expression;
            instructions.add(new IntInstruction(Opcodes.PUSH_INT, ((NullableIntExpressionNode) expression).getData()));
        } else if (expression instanceof BooleanExpressionNode) {
            assert !pop : "Not a statement " + expression;
            instructions.add(new IntInstruction(Opcodes.PUSH_INT, ((BooleanExpressionNode) expression).getData() ? 1 : 0));
        } else if (expression instanceof StringExpressionNode) {
            assert !pop : "Not a statement " + expression;
            instructions.add(new StringInstruction(Opcodes.PUSH_STRING, ((StringExpressionNode) expression).getData()));
        } else if (expression instanceof BuildStringNode) {
            assert !pop : "Not a statement " + expression;
            //TODO: If multiple literals, we can just merge them at compile time this happens a lot because all tags (<br> <col>) appear in seperate strings (they have some special syntax for it i guess)
            BuildStringNode build = (BuildStringNode) expression;
            for (ExpressionNode n : build.arguments) {
                compileExpression(n);
            }
            instructions.add(new IntInstruction(Opcodes.MERGE_STRINGS, build.arguments.size()));
        } else if (expression instanceof VariableLoadNode) {
            assert !pop : "Not a statement " + expression;
            instructions.add(((VariableLoadNode) expression).getVariable().generateLoadInstruction());
        } else if (expression instanceof VariableAssignationNode) {
            assert pop;
            VariableAssignationNode store = (VariableAssignationNode) expression;
            if (store.getExpression() == null) {
                return;
            }
            compileExpression(store.getExpression());
            CS2Type storeType = store.getExpression().getType();
            assert store.variables.size() == storeType.composite.size(); //TODO: Allow popping right side as if there were underscores?
            for (int i = store.variables.size() - 1; i >= 0; i--) {
                Variable var = store.variables.get(i);
                instructions.add(var.generateStoreInstruction());
            }
        } else if (expression instanceof NewArrayNode) {
            assert pop;
            NewArrayNode init = (NewArrayNode) expression;
            compileExpression(init.getExpression());
            instructions.add(new IntInstruction(Opcodes.ARRAY_NEW, init.arrayId << 16 | init.getType().charDesc));
        } else if (expression instanceof ArrayStoreNode) {
            assert pop;
            ArrayStoreNode store = (ArrayStoreNode) expression;
            compileExpression(store.getIndex());
            compileExpression(store.getValue());
            instructions.add(new IntInstruction(Opcodes.ARRAY_STORE, store.arrayId));
        } else if (expression instanceof ArrayLoadNode) {
            assert !pop : "Not a statement " + expression;
            ArrayLoadNode load = (ArrayLoadNode) expression;
            compileExpression(load.getIndex());
            instructions.add(new IntInstruction(Opcodes.ARRAY_LOAD, load.arrayId));
        } else if (expression instanceof ConditionalExpressionNode) {
            assert !pop : "Not a statement " + expression;
            //Compile a conditional expression. But leave the result on the stack rather than making a jump based on the result
            Label push1 = new Label();
            Label push0 = new Label();
            Label merge = new Label();
            //Based on expression type conditional might have not needed to make jumps. this means results were left on the stack
            if (compileConditionalJmp(expression, push1, push0)) {
                //It did have to jump. push results back onto the stack
                instructions.add(push1);
                instructions.add(new IntInstruction(Opcodes.PUSH_INT, 1));
                instructions.add(new JumpInstruction(Opcodes.GOTO, merge));
                instructions.add(push0);
                instructions.add(new IntInstruction(Opcodes.PUSH_INT, 0));
                //(flows to merge)
                instructions.add(merge);
            }
        } else {
            System.out.println("UNHANDLED EXPR " + expression.getClass());
            throw new DecompilerException("unhandled expr " + expression);
        }
    }

    private void optimize() {
        int size = 0;
        while (size != (size = instructions.size())) {
            for (int i = 0; i < instructions.size() - 2; i++) { //instructions size might change and we look 2 values ahead sometimes
                AbstractInstruction instr = instructions.get(i);
                if (instr instanceof Label) {
                    //Merge Label if next instruction is also Label (and rewrite jumps to those labels)
                    AbstractInstruction next = instructions.get(i + 1);
                    if (next instanceof Label) {
                        rewriteJumps((Label) next, (Label) instr);
                        instructions.remove(next);
                        continue;
                    }
                    //Remove label if instruction after label is GOTO. Rewrite all jumps to this label to the GOTO target
                    if ((next instanceof JumpInstruction) && next.getOpcode() == Opcodes.GOTO) {
                        rewriteJumps((Label) instr, ((JumpInstruction) next).getTarget());
                        instructions.remove(instr);
                        continue;
                    }
                }
                //Remove GOTO if next instruction is that label
                if (instr instanceof JumpInstruction && instr.getOpcode() == Opcodes.GOTO) {
                    AbstractInstruction next = instructions.get(i + 1);
                    if (next instanceof Label && ((JumpInstruction) instr).getTarget() == next) {
                        instructions.remove(instr);
                        continue;
                    }
                }

                //Switch CONDJMP X GOTO Y L_X: ... TO INVERSE_CONDJMP Y L_X: ... (where a inverse conditional jump instr exists).
                // This optimization causes a lot of issues for the decompiler loop detection though
                if (false) {
                    int opposite = Opcodes.oppositeJump(instr.getOpcode());
                    if (instr instanceof JumpInstruction && opposite != -1) {
                        AbstractInstruction next = instructions.get(i + 1);
                        if (next instanceof JumpInstruction && next.getOpcode() == Opcodes.GOTO) {
                            AbstractInstruction nn = instructions.get(i + 2);
                            if (nn instanceof Label && ((JumpInstruction) instr).getTarget() == nn) {
                                instr.setOpcode(opposite);
                                ((JumpInstruction) instr).setTarget(((JumpInstruction) next).getTarget());
                                instructions.remove(next);
                                continue;
                            }
                        }
                    }
                }
                if (supportEq01) {
                    //change push int 0/1 INT_EQ to EQ0/1 without push
                    if (instr instanceof IntInstruction && instr.getOpcode() == Opcodes.PUSH_INT) {
                        AbstractInstruction next = instructions.get(i + 1);
                        if (next instanceof JumpInstruction && next.getOpcode() == Opcodes.INT_EQ) {
                            if (((IntInstruction) instr).getConstant() == 0) {
                                instructions.remove(instr);
                                next.setOpcode(Opcodes.EQ0);
                                continue;
                            }
                            if (((IntInstruction) instr).getConstant() == 1) {
                                instructions.remove(instr);
                                next.setOpcode(Opcodes.EQ1);
                                continue;
                            }
                        }
                    }
                }

            }

        }
    }

    private void rewriteJumps(Label oldJump, Label newJump) {
        for (AbstractInstruction instr : instructions) {
            if (instr instanceof JumpInstruction) {
                if (((JumpInstruction) instr).getTarget() == oldJump) {
                    ((JumpInstruction) instr).setTarget(newJump);
                }
            } else if (instr instanceof SwitchInstruction) {
                List<Label> targets = ((SwitchInstruction) instr).targets;
                for (int i1 = 0; i1 < targets.size(); i1++) {
                    Label caseLabel = targets.get(i1);
                    if (caseLabel == oldJump) {
                        targets.set(i1, newJump);
                    }
                }
            }
        }
    }

}
