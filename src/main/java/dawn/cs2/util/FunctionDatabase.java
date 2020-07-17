package dawn.cs2.util;

import dawn.cs2.CS2Type;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class FunctionDatabase {

    private FunctionInfo[] info;
    private Map<String, List<FunctionInfo>> lookup = new HashMap<>();

    public FunctionDatabase(InputStream stream, boolean isScript, Map<Integer, Integer> scramble) {
        this.info = new FunctionInfo[40000];
        this.readDatabase(new InputStreamReader(stream), isScript, scramble);
    }

    public FunctionDatabase(String s, boolean isScript, Map<Integer, Integer> scramble) {
        this.info = new FunctionInfo[40000];
        this.readDatabase(new StringReader(s), isScript, scramble);
    }

    public FunctionDatabase() {
        this.info = new FunctionInfo[40000];
    }

    public FunctionDatabase copy() {
        FunctionDatabase cpy = new FunctionDatabase();
        System.arraycopy(info, 0, cpy.info, 0, info.length);
        for (Entry<String, List<FunctionInfo>> entry : lookup.entrySet()) {
            cpy.lookup.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return cpy;
    }

    private void readDatabase(Reader r, boolean isScript, Map<Integer, Integer> scramble) {
        try {
            BufferedReader reader = new BufferedReader(r);
            int linesCount = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine(), linesCount++) {
                if (line.length() <= 0 || line.startsWith(" ") || line.startsWith("//") || line.startsWith("#"))
                    continue;
                try {
                    String[] split = line.split(" ");
                    int opcode = Integer.parseInt(split[0]);
                    if(scramble != null && !scramble.containsKey(opcode)) {
                        continue;
                    }
                    String name = split[1];
                    CS2Type[] returnTypes = new CS2Type[0];
                    if (split[2].contains("|")) {
                        String[] multiReturn = split[2].split("\\|");
                        returnTypes = new CS2Type[multiReturn.length];
                        for(int i = 0; i < returnTypes.length; i++) {
                            returnTypes[i] = CS2Type.forDesc(multiReturn[i]);
                        }
                    } else {
                        returnTypes = new CS2Type[]{ CS2Type.forDesc(split[2]) };
                    }
                    CS2Type[] argTypes = new CS2Type[(split.length - 2) / 2];
                    String[] argNames = new String[(split.length - 2) / 2];
                    int write = 0;
                    for (int i = 3; i < split.length; i += 2) {
                        argTypes[write] = CS2Type.forDesc(split[i]);
                        argNames[write++] = split[i + 1];
                    }
                    putInfo(opcode, new FunctionInfo(name, opcode, argTypes, returnTypes[0], returnTypes, argNames, isScript));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException("Error parsing function database file on line:" + (linesCount + 1));
                }
            }
            reader.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    public FunctionInfo getInfo(int opcode) {
        if (opcode < 0 || opcode >= info.length)
            return null;
        return info[opcode];
    }


    public void putInfo(int opcode, FunctionInfo f) {
        this.info[opcode] = f;
        lookup.computeIfAbsent(f.getName(), n -> new ArrayList<>());
        lookup.get(f.getName()).add(this.info[opcode]);
    }

    public List<FunctionInfo> getByName(String symbol) {
        return lookup.getOrDefault(symbol, Collections.emptyList());
    }

}
