package dawn.cs2.instructions;

import java.util.ArrayList;
import java.util.List;

public class SwitchInstruction extends AbstractInstruction {

    public List<Integer> cases;
    public List<Label> targets;

    public SwitchInstruction(int opcode, List<Integer> cases, List<Label> targets) {
        super(opcode);
        this.cases = cases;
        this.targets = targets;

//        for (int i = 0; i < targets.size(); i++)
//            targets.get(i).getJumpers().add(this);

//        this.sort();
    }

    public void sort() {
        List<Integer> sCases = new ArrayList<>(cases.size());
        List<Label> sTargets = new ArrayList<>(targets.size());
        boolean[] usage = new boolean[cases.size()];
        for (int sWrite = 0; sWrite < cases.size(); sWrite++) {
            int lowestAddr = Integer.MAX_VALUE;
            int lowestIndex = -1;
            for (int i = 0; i < cases.size(); i++)
                if (!usage[i] && targets.get(i).getAddress() < lowestAddr)
                    lowestAddr = targets.get(lowestIndex = i).getAddress();
            usage[lowestIndex] = true;
            sCases.add(cases.get(lowestIndex));
            sTargets.add(targets.get(lowestIndex));
        }
        cases = sCases;
        targets = sTargets;
    }


    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append("switch { \r\n");
        for (int i = 0; i < cases.size(); i++) {
            bld.append("\tcase " + cases.get(i) + ": ");
            bld.append("\t").append(Opcodes.getOpcodeName(Opcodes.GOTO)).append("\t").append(targets.get(i).toString()).append(" \r\n");
        }
        bld.append("}");
        return bld.toString();
    }

}
