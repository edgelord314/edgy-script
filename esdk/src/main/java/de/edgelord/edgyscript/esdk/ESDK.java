package de.edgelord.edgyscript.esdk;

import de.edgelord.edgyscript.e80.interpreter.DirectValue;
import de.edgelord.edgyscript.e80.interpreter.NativeProvider;
import de.edgelord.edgyscript.e80.interpreter.Value;
import de.edgelord.edgyscript.e80.script.ArgumentList;

import java.util.*;

public class ESDK implements NativeProvider {

    private Map<String, NativeProvider> usedNativeProviders = new HashMap<>();

    @Override
    public Value function(String function, ArgumentList args) {

        // use a dependency e.g. use stdio
        if (function.equalsIgnoreCase("use")) {
            String value = args.get(0).getValue();
            String usedName = value;

            if (args.size() >= 3) {
                if (args.get(1).getValue().equalsIgnoreCase("as")) {
                    usedName = args.get(2).getValue();
                }
            }
            try {
                usedNativeProviders.put(usedName, getDependency(value));
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
            return new DirectValue(value);
        } else {
            Value returnVal = null;

            if (function.contains(".")) {
                String[] parts = function.split("\\.", 2);
                return usedNativeProviders.get(parts[0]).function(parts[1], args);
            }

            for (Map.Entry<String, NativeProvider> stringNativeProviderEntry : usedNativeProviders.entrySet()) {
                NativeProvider np = (NativeProvider) ((Map.Entry) stringNativeProviderEntry).getValue();
                returnVal = np.function(function, args);

                if (returnVal != null) {
                    break;
                }
            }
            return returnVal;
        }
    }

    private NativeProvider getDependency(String name) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        switch (name.toLowerCase()) {
            case "stdio":
                return Class.forName("de.edgelord.edgyscript.esdk.StdIO").asSubclass(NativeProvider.class).newInstance();

            case "strings":
            case "string":
            case "stringutils":
                return Class.forName("de.edgelord.edgyscript.esdk.Strings").asSubclass(NativeProvider.class).newInstance();

            case "fileio":
                return Class.forName("de.edgelord.edgyscript.esdk.FileIO").asSubclass(NativeProvider.class).newInstance();

            case "files":
            case "file":
                return Class.forName("de.edgelord.edgyscript.esdk.Files").asSubclass(NativeProvider.class).newInstance();

            case "structures":
            case "controls":
                return Class.forName("de.edgelord.edgyscript.esdk.Structures").asSubclass(NativeProvider.class).newInstance();

            default:
                return Class.forName(name).asSubclass(NativeProvider.class).newInstance();
        }
    }
}
