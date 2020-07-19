package dawn.cs2;

import com.displee.io.impl.InputBuffer;
import dawn.cs2.instructions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CS2Reader {

    public static CS2 readCS2ScriptNewFormat(File scriptFile, int scriptID, Map<Integer, Integer> unscramble) throws IOException {
        CS2Reader reader = new CS2Reader();
        if (!scriptFile.exists() || !scriptFile.isFile() || !scriptFile.canRead())
            return null;
//            throw new FileNotFoundException("Script file " + this.scriptFile + " does not exist.");
        FileInputStream stream = new FileInputStream(scriptFile);
        byte[] data = new byte[(int) scriptFile.length()];
        int readed = stream.read(data);
        stream.close();
        if (readed != data.length)
            throw new IOException("Reading failed.");
        return reader.readScript(scriptID, data, unscramble, false, false);
    }

    public static CS2 readCS2ScriptNewFormat(byte[] data, int scriptID, Map<Integer, Integer> unscramble, boolean disableSwitches, boolean disableLongs) throws IOException {
        CS2Reader reader = new CS2Reader();
        return reader.readScript(scriptID, data, unscramble, disableSwitches, disableLongs);
    }

    @SuppressWarnings("unchecked")
    private CS2 readScript(int scriptID, byte[] data, Map<Integer, Integer> unscramble, boolean disableSwitches, boolean disableLongs) throws IOException {
        InputBuffer buffer = new InputBuffer(data);

        boolean hasSwitches = !disableSwitches && unscramble.containsValue(Opcodes.SWITCH); //old OSRS doesnt have switches
        boolean hasLongs = !disableLongs && unscramble.containsValue(Opcodes.PUSH_LONG); //old revisions don't have longs

        if (hasSwitches) {
            buffer.setOffset(data.length - 2);
        }
        int switchBlocksSize = hasSwitches ? buffer.readUnsignedShort() : 0;

        int codeBlockEnd = data.length - switchBlocksSize - (hasLongs ? 16 : 12) - (hasSwitches ? 2 : 0);
        buffer.setOffset(codeBlockEnd);
        int codeSize = buffer.readInt();
        int intLocalsCount = buffer.readUnsignedShort();
        int stringLocalsCount = buffer.readUnsignedShort();
        int longLocalsCount = 0;
        if (hasLongs)
            longLocalsCount = buffer.readUnsignedShort();

        int intArgsCount = buffer.readUnsignedShort();
        int stringArgsCount = buffer.readUnsignedShort();
        int longArgsCount = 0;
        if (hasLongs)
            longArgsCount = buffer.readUnsignedShort();
        Map[] switches = null;
        if (hasSwitches) {
            int switchesCount = buffer.readUnsignedByte();
            switches = new HashMap[switchesCount];
            for (int i = 0; i < switchesCount; i++) {
                int numCases = buffer.readUnsignedShort();
                switches[i] = new HashMap<Integer, Integer>(numCases);
                while (numCases-- > 0) {
                    switches[i].put(buffer.readInt(), buffer.readInt());
                }
            }
        }
        buffer.setOffset(0);
        String scriptName = buffer.readStringNull();

        CS2Type[] args = new CS2Type[intArgsCount + stringArgsCount + longArgsCount];
        int write = 0;
        for (int i = 0; i < intArgsCount; i++)
            args[write++] = CS2Type.INT;
        for (int i = 0; i < stringArgsCount; i++)
            args[write++] = CS2Type.STRING;
        for (int i = 0; i < longArgsCount; i++)
            args[write++] = CS2Type.LONG;

        CS2 script = new CS2(scriptID, args, intLocalsCount, stringLocalsCount, longLocalsCount, intArgsCount, stringArgsCount, longArgsCount, codeSize);

        int writeOffset = 0;
        while (buffer.getOffset() < codeBlockEnd) {
            int opcode = buffer.readUnsignedShort();
            Integer n = unscramble.get(opcode);
            if (n == null) {
//                n = opcode;
                throw new DecompilerException("Unknown scrambling " + opcode);
            }
            opcode = n;
            if (opcode == Opcodes.PUSH_STRING) {
                script.getInstructions()[(writeOffset * 2) + 1] = new StringInstruction(opcode, buffer.readString());
            } else if (opcode == Opcodes.PUSH_LONG) {
                script.getInstructions()[(writeOffset * 2) + 1] = new LongInstruction(opcode, buffer.readLong());
            } else if (opcode == Opcodes.RETURN || opcode == Opcodes.POP_INT || opcode == Opcodes.POP_STRING) {
                //TODO: this might aswell be booleaninstructions, but decompiler kind of expects them to be intinstructions right now
                script.getInstructions()[(writeOffset * 2) + 1] = new IntInstruction(opcode, buffer.readUnsignedByte());
            } else if (opcode >= (hasLongs ? 150 : 100)) { // || opcode == 21 || opcode == 38 || opcode == 39)
                script.getInstructions()[(writeOffset * 2) + 1] = new BooleanInstruction(opcode, buffer.readUnsignedByte() == 1);
            } else if (opcode == Opcodes.SWITCH) { // switch
                Map block = switches[buffer.readInt()];
                List<Integer> cases = new ArrayList<>(block.size());
                List<Label> targets = new ArrayList<>(block.size());
                int w = 0;
                for (Object key : block.keySet()) {
                    cases.add((Integer) key);
                    Object addr = block.get(key);
                    int full = writeOffset + (Integer) addr + 1;
                    if (script.getInstructions()[full * 2] == null)
                        script.getInstructions()[full * 2] = new Label();

                    targets.add((Label) script.getInstructions()[full * 2]);
                }
                script.getInstructions()[(writeOffset * 2) + 1] = new SwitchInstruction(opcode, cases, targets);
                if (script.getInstructions()[writeOffset * 2 + 2] == null)
                    script.getInstructions()[(writeOffset * 2) + 2] = new Label(); //always insert label after switch
            } else if (opcode == Opcodes.GOTO || opcode == Opcodes.INT_NE || opcode == Opcodes.INT_EQ || opcode == Opcodes.INT_LT || opcode == Opcodes.INT_GT || opcode == Opcodes.INT_LE || opcode == Opcodes.INT_GE || opcode == Opcodes.LONG_NE || opcode == Opcodes.LONG_EQ || opcode == Opcodes.LONG_LT || opcode == Opcodes.LONG_GT || opcode == Opcodes.LONG_LE || opcode == Opcodes.LONG_GE || opcode == Opcodes.EQ1 || opcode == Opcodes.EQ0) {
                int fullAddr = writeOffset + buffer.readInt() + 1;
                if (script.getInstructions()[fullAddr * 2] == null)
                    script.getInstructions()[fullAddr * 2] = new Label();
                script.getInstructions()[(writeOffset * 2) + 1] = new JumpInstruction(opcode, (Label) script.getInstructions()[fullAddr * 2]);
                //always insert label after jumps
                if (script.getInstructions()[writeOffset * 2 + 2] == null)
                    script.getInstructions()[writeOffset * 2 + 2] = new Label();
            } else {
                script.getInstructions()[(writeOffset * 2) + 1] = new IntInstruction(opcode, buffer.readInt());
            }
            writeOffset++;
        }
        script.prepareInstructions();
        return script;
    }

}
