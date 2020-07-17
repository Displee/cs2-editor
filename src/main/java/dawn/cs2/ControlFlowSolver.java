package dawn.cs2;

import dawn.cs2.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ControlFlowSolver {

    private CS2Decompiler decompiler;
    private ScopeNode scope;
    private List<FlowBlock> blocks;

    public ControlFlowSolver(CS2Decompiler decompiler, ScopeNode scope, List<FlowBlock> blocks) {
        this.decompiler = decompiler;
        this.scope = scope;
        this.blocks = blocks;
    }


    public void solve() throws DecompilerException {

        int i = 0;
        if (!CS2ConstantsKt.GENERATE_SCRIPTS_DB)
            while (true) {
                i++;
//                System.out.println("-----------");
//                for (FlowBlock block : blocks) {
//                    System.out.println(block);
//                }
//                    assertSanity();
                if (findConnect() > 0) continue;
                if (mergeConditionals() > 0) continue;
                if (findWhile() > 0) continue;
                if (findIfElse() > 0) continue;
                if (findSwitch() > 0) continue;
                if (findStupidShit() > 0) continue;
                if (invertElseReturn() > 0) continue; //this must come last, may also be loops that use similar structure

                break;
            }

        List<FlowBlock> blocks = listBlocks();
        for (FlowBlock block : blocks) {
            if (block.children.size() <= 0 && block.parents.size() <= 0) {
                List<AbstractCodeNode> childs = block.listChilds();
                for (AbstractCodeNode node : childs)
                    this.scope.write(node);
            } else {
                this.scope.write(block);
            }
        }
    }

    private void assertSanity() {
        for (FlowBlock b : blocks) {
            if (b != null) {
                if (!b.children.stream().allMatch(c -> c.parents.contains(b))) {
                    throw new DecompilerException("sanity lostp " + b.getBlockID());
                }
                if (!b.parents.stream().allMatch(p -> p.children.contains(b))) {
                    throw new DecompilerException("sanity lostc " + b.getBlockID());
                }
            }
        }
    }

    private int findConnect() {
        int solved = 0;
        for (FlowBlock block : blocks)
            if (solveConnect(block)) {
                solved++;
                break;
            }
//        if (solved > 0)
//            System.err.println("Found connect - " + solved);
        return solved;
    }

    private boolean solveConnect(FlowBlock b) {
        if (b.children.size() == 1 && b.children.get(0).parents.size() == 1) {
            FlowBlock c = b.children.get(0);
            if (!(b.read(b.size() - 1) instanceof UnconditionalFlowBlockJump)) {
                return false;
            }
            assert b.read(b.size() - 1) instanceof UnconditionalFlowBlockJump;
            b.setCodeAddress(b.size() - 1);
            b.delete();
            for (int i = 0; i < c.size(); i++) {
                b.write(c.read(i));
            }
            b.children = c.children;
            for (FlowBlock child : b.children) {
                child.parents.remove(c);
                child.parents.add(b);
            }
            delete(c);
            return true;
        }
        if (b.parents.size() == 1 && b.size() == 1) { //single parent, single GOTO instruction. Replace the jumps
            FlowBlock parent = b.parents.get(0);
            if (!(b.read(0) instanceof UnconditionalFlowBlockJump)) {
                return false;
            }
            parent.setCodeAddress(0);
            AbstractCodeNode n;
            while ((n = parent.read()) != null) {
                boolean done = false;
                if (n instanceof ConditionalFlowBlockJump) {
                    if (((ConditionalFlowBlockJump) n).getTarget() == b) {
                        ((ConditionalFlowBlockJump) n).setTarget(b.children.get(0));
                        done = true;
                    }
                }
                if (n instanceof UnconditionalFlowBlockJump) {
                    if (((UnconditionalFlowBlockJump) n).getTarget() == b) {
                        ((UnconditionalFlowBlockJump) n).setTarget(b.children.get(0));
                        done = true;
                    }
                }
                if (done) {
                    parent.children.remove(b);
                    parent.children.add(b.children.get(0));
                    b.children.get(0).parents.remove(b);
                    b.children.get(0).parents.add(parent);
                    delete(b);
                    return true;
                }
            }
            //goto is somewhere in a switch target
            return false;
        }
        return false;
    }


    private int findIfElse() {
        int solved = 0;
        for (FlowBlock block : blocks) {
            if (solveIf(block) || solveIfElse(block) || solveIfReturn(block)) {
                solved++;
                break;
            }
        }
//        if (solved > 0)
//            System.err.println("Found ifs - " + solved);
        return solved;
    }

    private boolean solveIfReturn(FlowBlock b) {
        b.setCodeAddress(0);
        AbstractCodeNode n;
        while ((n = b.read()) != null) {
            if (n instanceof ConditionalFlowBlockJump) {
                ConditionalFlowBlockJump j = (ConditionalFlowBlockJump) n;
                if (j.getTarget().parents.size() == 1 && j.getTarget().children.size() == 0) {
                    //inline if block if it always returns
                    b.setCodeAddress(b.getCodeAddress() - 1);
                    b.delete();
                    FlowBlock t = j.getTarget();
                    ScopeNode ifs = new ScopeNode();
                    for (int i = 0; i < t.size(); i++) {
                        ifs.write(t.read(i));
                    }
                    b.children.remove(t);
                    delete(t);
                    b.write(new IfElseNode(j.getExpression(), ifs, new ScopeNode()));
                    return true;
                }
            }
        }

        return false;
    }

    private boolean solveIf(FlowBlock b) {
        if (b.children.size() == 2) {
            AbstractCodeNode n2 = b.read(b.size() - 2);
            AbstractCodeNode n1 = b.read(b.size() - 1);

            if (!(n2 instanceof ConditionalFlowBlockJump) || !(n1 instanceof UnconditionalFlowBlockJump)) {
                //switches etc
                return false;
            }

            ConditionalFlowBlockJump jmp = (ConditionalFlowBlockJump) n2;
            UnconditionalFlowBlockJump merge = (UnconditionalFlowBlockJump) n1;
            FlowBlock ifblock = jmp.getTarget();
            FlowBlock elseblock = merge.getTarget();
            boolean invert = false;
            if (elseblock.children.size() == 1 && elseblock.children.get(0) == ifblock && elseblock.parents.size() == 1 && elseblock.read(elseblock.size() - 1) instanceof UnconditionalFlowBlockJump) {
                FlowBlock t = ifblock;
                ifblock = elseblock;
                elseblock = t;
                invert = true;
            }

            if (ifblock.children.size() == 1 && ifblock.children.get(0) == elseblock && ifblock.parents.size() == 1 && ifblock.read(ifblock.size() - 1) instanceof UnconditionalFlowBlockJump) {
                ScopeNode ifs = new ScopeNode();
                for (int i = 0; i < ifblock.size() - 1; i++) { //skip last goto E
                    ifs.write(ifblock.read(i));
                }
                b.setCodeAddress(b.size() - 2);
                b.delete(); //IF GOTO
                if (!b.children.remove(ifblock)) {
                    assert false;
                    System.out.println("err");
                }
                if (!elseblock.parents.remove(ifblock)) {
                    assert false;
                    System.out.println("err");
                }
                delete(ifblock);
                ExpressionNode expr = jmp.getExpression();
                if (invert) {
                    if (expr instanceof BooleanConditionalExpressionNode) {
                        ((BooleanConditionalExpressionNode) expr).invert = !((BooleanConditionalExpressionNode) expr).invert;
                    } else {
                        expr = new BooleanConditionalExpressionNode(true, jmp.getExpression());
                    }
                }
                b.write(new IfElseNode(expr, ifs, new ScopeNode()));
                return true;
            }

        }
        return false;
    }

    private boolean solveIfElse(FlowBlock b) {
        if (b.children.size() == 2) {
            FlowBlock l = b.children.get(0);
            FlowBlock r = b.children.get(1);
            if (l.parents.size() != 1 || r.parents.size() != 1) {
                return false;
            }
            boolean isEnd = false;
            if (l.children.size() == 0 && r.children.size() == 0) {
                isEnd = true;
            } else if (l.children.size() != 1 || r.children.size() != 1 || l.children.get(0) != r.children.get(0)) {
                return false;
            }
            AbstractCodeNode n1 = b.read(b.size() - 1);
            AbstractCodeNode n2 = b.read(b.size() - 2);
            if (!(n2 instanceof ConditionalFlowBlockJump) || !(n1 instanceof UnconditionalFlowBlockJump)) {
                //switches etc
//                throw new DecompilerException("break me");
                return false;
            }
            UnconditionalFlowBlockJump j1 = (UnconditionalFlowBlockJump) n1;
            ConditionalFlowBlockJump j2 = (ConditionalFlowBlockJump) n2;
            if (j2.getTarget() != l) {
                FlowBlock t = l;
                l = r;
                r = t;
            }
            assert j2.getTarget() == l;
            assert j1.getTarget() == r;
            b.setCodeAddress(b.size() - 2);
            b.delete();
            b.delete();
            ScopeNode ifs = new ScopeNode();
            ScopeNode elses = new ScopeNode();
            b.write(new IfElseNode(j2.getExpression(), ifs, elses));

            if (l.children.size() > 0) {
                //LAST NODE OF L/R MUST BE GOTO E
                assert l.read(l.size() - 1) instanceof UnconditionalFlowBlockJump;
                assert r.read(r.size() - 1) instanceof UnconditionalFlowBlockJump;
                for (int i = 0; i < l.size() - 1; i++) {
                    ifs.write(l.read(i));
                }
                for (int i = 0; i < r.size() - 1; i++) {
                    elses.write(r.read(i));
                }

            } else {
                //IF ... RETURN ...
                for (int i = 0; i < l.size(); i++) {
                    ifs.write(l.read(i));
                }
                for (int i = 0; i < r.size(); i++) {
                    b.write(r.read(i));
                }
            }
            b.children.clear();
            if (!isEnd) {
                FlowBlock e = l.children.get(0);
                b.write(new UnconditionalFlowBlockJump(e));
                b.children.add(e);
                e.parents.remove(l);
                e.parents.remove(r);
                e.parents.add(b);
            }
            delete(l);
            delete(r);
            return true;
        }
        return false;
    }


    private int invertElseReturn() {
        int solved = 0;
        for (FlowBlock block : blocks)
            if (solveElseReturn(block)) {
                solved++;
                break;
            }

//        if (solved > 0)
//            System.err.println("Found else return - " + solved);
        return solved;
    }


    private boolean solveElseReturn(FlowBlock b) {
        b.setCodeAddress(0);
        AbstractCodeNode n;
        while ((n = b.read()) != null) {
            if (n instanceof ConditionalFlowBlockJump) {
                AbstractCodeNode n2 = b.read();
                if (n2 instanceof UnconditionalFlowBlockJump) {
                    ConditionalFlowBlockJump j = (ConditionalFlowBlockJump) n;
                    UnconditionalFlowBlockJump jnot = (UnconditionalFlowBlockJump) n2;
                    if (j.getTarget().children.size() > 1) {
                        //not required but less aggressive
                        return false;
                    }
                    if (jnot.getTarget().parents.size() == 1 && jnot.getTarget().children.size() == 0) {
                        FlowBlock t = jnot.getTarget();
                        //change if ... then x else return
                        //to if !... return else then x
                        jnot.setTarget(j.getTarget());
                        j.setTarget(t);
                        j.setExpression(new BooleanConditionalExpressionNode(true, j.getExpression()));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int findStupidShit() {
        int solved = 0;
        for (FlowBlock block : blocks)
            if (solveStupidShit(block)) {
                solved++;
                break;
            }

//        if (solved > 0)
//            System.err.println("Found stupid shit - " + solved);
        return solved;
    }

    private boolean solveStupidShit(FlowBlock b) {
        if (b.children.size() == 2) {
            FlowBlock l = b.children.get(0);
            FlowBlock r = b.children.get(1);
            if (l != r) {
                return false;
            }
            AbstractCodeNode n1 = b.read(b.size() - 1);
            AbstractCodeNode n2 = b.read(b.size() - 2);
            if (!(n2 instanceof ConditionalFlowBlockJump) || !(n1 instanceof UnconditionalFlowBlockJump)) {
                return false;
            }
            //change
            //IF X GOTO Y
            //GOTO Y
            //to
            //if X {
            // do nothing
            //}
            //GOTO Y
            UnconditionalFlowBlockJump j1 = (UnconditionalFlowBlockJump) n1;
            ConditionalFlowBlockJump j2 = (ConditionalFlowBlockJump) n2;
            assert j1.getTarget() == j2.getTarget();
            b.setCodeAddress(b.size() - 2);
            b.delete();
            b.write(new IfElseNode(j2.getExpression(), new ScopeNode(), new ScopeNode()));
            b.children.remove(j1.getTarget());
            j1.getTarget().parents.remove(b);
            return true;
        }

        //Empty switch
        if (b.children.size() == 0) {
            b.setCodeAddress(0);
            AbstractCodeNode n;
            while ((n = b.read()) != null) {
                if (n instanceof SwitchFlowBlockJump) {
                    b.setCodeAddress(b.getCodeAddress() - 1);
                    b.delete();
                    b.write(new SwitchNode(new ScopeNode(), ((SwitchFlowBlockJump) n).getExpression()));
                    return true;
                }
            }
        }
        return false;
    }

    private int findSwitch() {
        int solved = 0;
        for (FlowBlock block : blocks)
            if (solveSwitches(block)) {
                solved++;
                break;
            }

//        if (solved > 0)
//            System.err.println("Found switches - " + solved);
        return solved;
    }

    private boolean solveSwitches(FlowBlock b) {
        b.setCodeAddress(0);
        AbstractCodeNode n;
        while ((n = b.read()) != null) {
            if (n instanceof SwitchFlowBlockJump) {
                AbstractCodeNode aftern = b.read();
                if (!(aftern instanceof UnconditionalFlowBlockJump)) {
                    return false;
                }
                assert b.getCodeAddress() == b.size();
                UnconditionalFlowBlockJump j = (UnconditionalFlowBlockJump) aftern;
                FlowBlock defaultCaseBlock = j.getTarget();
                if (defaultCaseBlock.children.size() > 1) {
                    return false;
                }
                SwitchFlowBlockJump sw = (SwitchFlowBlockJump) n;
                List<FlowBlock> targets = sw.getTargets();

                //Check if all blocks can be inlined
                if (!targets.stream().allMatch(t -> t.children.size() <= 1)) {
                    return false;
                }
                if (!targets.stream().collect(Collectors.groupingBy(t -> t, Collectors.counting())).entrySet().stream().allMatch(g -> g.getKey().parents.size() == g.getValue())) {
                    //Compiler support this, decompiler doesn't. Not used in original scripts so cba adding proper support for it here
                    System.err.println("Switch has fall-through?");
                    return false;
                }


                FlowBlock mustBreakTo = null;
                FlowBlock prevBlock = null;

                for (FlowBlock target : targets) {
                    if (target.children.size() > 0) {
                        FlowBlock breakTo = target.children.get(0);
                        if (mustBreakTo == null) {
                            mustBreakTo = breakTo;
                        }
                        if (mustBreakTo != breakTo) {
                            return false;
                        }
                    }
                }

                SwitchNode sn = new SwitchNode(new ScopeNode(), sw.getExpression());

                for (int i = 0; i < targets.size(); i++) {
                    FlowBlock target = targets.get(i);
                    if (target != prevBlock) {
                        if (prevBlock != null) {
                            prevBlock.setCodeAddress(0);
                            if (prevBlock.children.size() > 0)
                                assert prevBlock.read(prevBlock.size() - 1) instanceof UnconditionalFlowBlockJump;
                            for (int a = 0; a < prevBlock.size() - prevBlock.children.size(); a++) { //skip final GOTO
                                sn.getScope().write(prevBlock.read(a));
                            }
                            if (prevBlock.children.size() > 0) {
                                sn.getScope().write(new BreakNode());
                            }
                            FlowBlock finalPrevBlock1 = prevBlock;
                            prevBlock.children.forEach(c -> c.parents.remove(finalPrevBlock1));
                            delete(prevBlock);

                        }
                        prevBlock = target;

                    }
                    //
                    b.children.remove(target);
                    sn.getScope().write(new CaseAnnotation(sw.getCases().get(i), sw.getExpression()));
                }
                //write last block too
                if (prevBlock != null) { //if still null = empty switch
                    prevBlock.setCodeAddress(0);
                    if (prevBlock.children.size() > 0)
                        assert prevBlock.read(prevBlock.size() - 1) instanceof UnconditionalFlowBlockJump;
                    for (int a = 0; a < prevBlock.size() - prevBlock.children.size(); a++) { //skip final GOTO
                        sn.getScope().write(prevBlock.read(a));
                    }
                    if (prevBlock.children.size() > 0) {

                        sn.getScope().write(new BreakNode());
                    }
                    FlowBlock finalPrevBlock = prevBlock;
                    prevBlock.children.forEach(c -> c.parents.remove(finalPrevBlock));
                    delete(prevBlock);
                }
                boolean diddef = false;
                //if mustbreakto = null, we can treat the default case as code after the switch. (all cases return, rather than jumping to after switch)
                if (mustBreakTo != null && defaultCaseBlock != mustBreakTo) {
                    //default case
                    diddef = true;
                    if (defaultCaseBlock.size() == 1 && defaultCaseBlock.children.size() == 1) {
                        assert defaultCaseBlock.children.get(0) == mustBreakTo;
                        //empty default block, just delete it
                        defaultCaseBlock.children.get(0).parents.remove(defaultCaseBlock);
                    } else {
                        sn.getScope().write(new CaseAnnotation());
                        for (int a = 0; a < defaultCaseBlock.size() - defaultCaseBlock.children.size(); a++) {
                            sn.getScope().write(defaultCaseBlock.read(a));
                        }
                        if (defaultCaseBlock.children.size() > 0) {
                            assert defaultCaseBlock.children.size() == 1;
                            assert defaultCaseBlock.children.get(0) == mustBreakTo;
                            defaultCaseBlock.children.get(0).parents.remove(defaultCaseBlock);
                            sn.getScope().write(new BreakNode());
                        }
                    }
                    delete(defaultCaseBlock);

                }
                b.setCodeAddress(b.getCodeAddress() - 2);
                b.delete();
                b.children.forEach(c -> c.parents.remove(b));
                b.children.clear();
                if (diddef) {
                    b.delete();
                } else {
                    b.children.add(defaultCaseBlock);
                    defaultCaseBlock.parents.add(b);
                }
                b.write(sn);
                if (mustBreakTo != null) { //everything RETURNS
                    if (b.read(b.size() - 1) instanceof UnconditionalFlowBlockJump) {
                        assert ((UnconditionalFlowBlockJump) b.read(b.size() - 1)).getTarget() == mustBreakTo;
                        return true;
                    }
                    b.write(new UnconditionalFlowBlockJump(mustBreakTo));
//                    mustBreakTo.parents.clear();
                    mustBreakTo.parents.add(b);
                    b.children.add(mustBreakTo);
                }
//                assertSanity();
                return true;
            }
        }
        return false;
    }


    private int findWhile() {
        int solved = 0;
        for (FlowBlock block : blocks)
            if (solveWhile(block)) {
                solved++;
                break;
            }
//        if (solved > 0)
//            System.err.println("Found while loops - " + solved);
        return solved;
    }

    private boolean solveWhile(FlowBlock b) {
        if (b.children.size() == 2) {
            AbstractCodeNode n1 = b.read(b.size() - 2);
            AbstractCodeNode n2 = b.read(b.size() - 1);
            if (!(n1 instanceof ConditionalFlowBlockJump) || !(n2 instanceof UnconditionalFlowBlockJump)) {
                return false;
            }
            ConditionalFlowBlockJump j = (ConditionalFlowBlockJump) n1;
            UnconditionalFlowBlockJump e = (UnconditionalFlowBlockJump) n2;
            if (j.getTarget().children.size() == 1 && j.getTarget().parents.size() == 1 && j.getTarget().children.contains(b)) {
                LoopNode loop = new LoopNode(LoopNode.LOOPTYPE_WHILE, new ScopeNode(), j.getExpression());
                //Skip last GOTO
                for (int i = 0; i < j.getTarget().size() - 1; i++) {
                    loop.getScope().write(j.getTarget().read(i));
                }
                b.children.remove(j.getTarget());
                b.parents.remove(j.getTarget());
                b.setCodeAddress(b.size() - 2);
                b.delete();
                b.write(loop);
                delete(j.getTarget());
                return true;
            }
        }
        return false;
    }

    private void delete(FlowBlock l) {
        blocks.remove(l);
    }

    private int mergeConditionals() {
        int merged = 0;
        for (FlowBlock block : blocks)
            if (doIfANDConditionsMerge(block) || doIfORConditionsMerge1(block) || doIfORConditionsMerge2(block)) {
                merged++;
                break;
            }
//        if (merged > 0)
//            System.err.println("Merged if conditions:" + merged);
        return merged;
    }

    private boolean doIfORConditionsMerge1(FlowBlock block) {
        if (block.size() < 2)
            return false;
        block.setCodeAddress(0);
        for (AbstractCodeNode node = block.read(); node != null; node = block.read()) {
            if (node instanceof ConditionalFlowBlockJump) {
                ConditionalFlowBlockJump jmp = (ConditionalFlowBlockJump) node;
                int nextAddr = block.addressOf(jmp) + 1;
                if (nextAddr >= block.size())
                    return false;
                if (!(block.read(nextAddr) instanceof ConditionalFlowBlockJump))
                    return false;
                ConditionalFlowBlockJump jmp2 = (ConditionalFlowBlockJump) block.read(nextAddr);
                if (jmp.getTarget() == jmp2.getTarget()) {
                    block.children.remove(jmp.getTarget());
                    jmp.getTarget().parents.remove(block);
                    block.setCodeAddress(nextAddr - 1);
                    block.delete();
                    block.delete();
                    block.write(new ConditionalFlowBlockJump(new ConditionalExpressionNode(jmp.getExpression(), jmp2.getExpression(), Operator.OR), jmp.getTarget()));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doIfORConditionsMerge2(FlowBlock block) {
        if (block.size() < 2)
            return false;

        AbstractCodeNode v0 = block.read(block.size() - 2);
        AbstractCodeNode v1 = block.read(block.size() - 1);
        if (!(v0 instanceof ConditionalFlowBlockJump) || !(v1 instanceof UnconditionalFlowBlockJump))
            return false;

        ConditionalFlowBlockJump condition1 = (ConditionalFlowBlockJump) v0;
        UnconditionalFlowBlockJump jmp = (UnconditionalFlowBlockJump) v1;

        FlowBlock target = jmp.getTarget();
        if (/*target.getBlockID() <= block.getBlockID() ||*/ target.parents.size() != 1 || target.size() < 2)
            return false;

        AbstractCodeNode v3 = target.read(0);
        AbstractCodeNode v4 = target.read(1);
        if (!(v3 instanceof ConditionalFlowBlockJump) || !(v4 instanceof UnconditionalFlowBlockJump))
            return false;

        ConditionalFlowBlockJump condition2 = (ConditionalFlowBlockJump) v3;
        UnconditionalFlowBlockJump jmp2 = (UnconditionalFlowBlockJump) v4;

        FlowBlock realTarget = jmp2.getTarget();

        if (condition1.getTarget() != condition2.getTarget())
            return false;

        block.setCodeAddress(block.size() - 2);
        block.delete();
        block.delete();

        block.write(new ConditionalFlowBlockJump(new ConditionalExpressionNode(condition1.getExpression(), condition2.getExpression(), Operator.OR), condition1.getTarget()));
        block.write(new UnconditionalFlowBlockJump(realTarget));

        block.children.remove(target);
        target.parents.remove(block);

        block.children.add(realTarget);
        realTarget.parents.add(block);


        target.setCodeAddress(0);
        target.delete();
        target.delete();

        target.children.remove(condition2.getTarget());
        condition2.getTarget().parents.remove(target);
        target.children.remove(realTarget);
        realTarget.parents.remove(target);

        return true;
    }

    private boolean doIfANDConditionsMerge(FlowBlock block) {
        if (block.size() < 2)
            return false;
        AbstractCodeNode v0 = block.read(block.size() - 2);
        AbstractCodeNode v1 = block.read(block.size() - 1);
        if (!(v0 instanceof ConditionalFlowBlockJump) || !(v1 instanceof UnconditionalFlowBlockJump))
            return false;
        ConditionalFlowBlockJump conditionPart = (ConditionalFlowBlockJump) v0;
        UnconditionalFlowBlockJump jumpOut = (UnconditionalFlowBlockJump) v1;
        FlowBlock condition = conditionPart.getTarget();
        FlowBlock out = jumpOut.getTarget();
//        if (condition.getBlockID() <= block.getBlockID() || out.getBlockID() <= condition.getBlockID())
//            return false;
        if (condition.parents.size() != 1 || condition.size() < 1 || condition.size() > 2)
            return false;

        //TODO: can probably remove this?? size should always be 2?
        if (condition.size() == 1) {
            if (!condition.children.contains(out)) {
                return false;
            }

            AbstractCodeNode v3 = condition.read(condition.size() - 1);
            if (!(v3 instanceof ConditionalFlowBlockJump))
                return false;

            if (1 == 1)
                throw new DecompilerException("disabled");

            ConditionalFlowBlockJump realjmp = (ConditionalFlowBlockJump) v3;
            FlowBlock target = realjmp.getTarget();

            block.setCodeAddress(block.size() - 2);
            block.delete();
            block.write(new ConditionalFlowBlockJump(new ConditionalExpressionNode(conditionPart.getExpression(), realjmp.getExpression(), Operator.AND), target));
            block.children.remove(condition);
            block.children.add(target);
            condition.parents.remove(block);
            target.parents.add(block);

            condition.setCodeAddress(condition.size() - 1);
            condition.delete();
            condition.children.remove(target);
            target.parents.remove(condition);
            return true;

        } else {

            AbstractCodeNode v3 = condition.read(condition.size() - 2);
            AbstractCodeNode v4 = condition.read(condition.size() - 1);
            if (!(v3 instanceof ConditionalFlowBlockJump) || !(v4 instanceof UnconditionalFlowBlockJump))
                return false;
            ConditionalFlowBlockJump realjmp = (ConditionalFlowBlockJump) v3;
            UnconditionalFlowBlockJump jumpOut2 = (UnconditionalFlowBlockJump) v4;

            if (jumpOut2.getTarget() != out)
                return false;

            if (condition.size() > 2) {
                return false;
            }

            FlowBlock target = realjmp.getTarget();

            block.setCodeAddress(block.size() - 2);
            block.delete();
            block.write(new ConditionalFlowBlockJump(new ConditionalExpressionNode(conditionPart.getExpression(), realjmp.getExpression(), Operator.AND), target));
            block.children.remove(condition);
            block.children.add(target);
            condition.parents.remove(block);
            target.parents.add(block);

            condition.setCodeAddress(condition.size() - 2);
            for (int i = 0; i < 2; i++)
                condition.delete();
            condition.children.remove(target);
            condition.children.remove(out);
            target.parents.remove(condition);
            out.parents.remove(condition);
            return true;
        }
    }

    public List<FlowBlock> listBlocks() {
        return new ArrayList<FlowBlock>(this.blocks);
    }


    public ScopeNode getScope() {
        return scope;
    }


}
