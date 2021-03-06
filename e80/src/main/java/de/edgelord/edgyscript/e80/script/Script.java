package de.edgelord.edgyscript.e80.script;

import de.edgelord.edgyscript.e80.interpreter.Function;
import de.edgelord.edgyscript.e80.interpreter.Interpreter;
import de.edgelord.edgyscript.e80.interpreter.Lexer;
import de.edgelord.edgyscript.e80.interpreter.token.Token;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Script {

    private File scriptFile;
    private List<ScriptLine> lines = new ArrayList<>();
    private Lexer lexer = new Lexer();

    private ScriptLine currentLine = null;
    private int lastIndentionLevel = 0;
    private int currIndentionLevel = 0;

    public Script(String path) {
        scriptFile = new File(path);
        ScriptException.SCRIPT_PATH = path;
    }

    public Script(File scriptFile) {
        this.scriptFile = scriptFile;
    }

    public void loadPreCompiled() {
        try {
            FileInputStream fileIn = new FileInputStream(scriptFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            lines = (ArrayList<ScriptLine>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * compiles the lines of the file into {@link ScriptLine}s.
     *
     * @throws FileNotFoundException when something went wrong with the file I/O
     */
    public void compile() throws FileNotFoundException {
        Scanner fileScanner = new Scanner(scriptFile);
        boolean insideMultipleLineComment = false;
        int currentLineNumber = 0;
        StringBuilder lineBuilder = new StringBuilder();

        while (fileScanner.hasNext()) {
            currentLineNumber++;
            ScriptException.LINE_NUMBER = currentLineNumber;
            String oLine = fileScanner.nextLine();
            String line = oLine.trim();
            int indention;
            int indentionLevel;

            if (insideMultipleLineComment) {
                if (line.endsWith("*/")) {
                    insideMultipleLineComment = false;
                }
                continue;
            } else {
                if (line.startsWith("#") || line.startsWith("//")) {
                    continue;
                } else if (line.startsWith("/*")) {
                    insideMultipleLineComment = true;
                    continue;
                }
            }

            indention = oLine.indexOf(line);
            indentionLevel = indention / 4;

            if (indention % 4 != 0) {
                throw new ScriptException("indention must be a multiple of 4");
            }

            if (line.length() > 1) {

                if (lineBuilder.length() == 0) {
                    currIndentionLevel = indentionLevel;
                }

                if (buildLine(lineBuilder, line)) {
                    int nextLastIndentionLevel = currIndentionLevel;
                    String finalLine = lineBuilder.toString();
                    List<Token> tokens = lexer.tokenize(finalLine.endsWith(";") || finalLine.endsWith(":") ? removeLastChar(finalLine) : finalLine);
                    ScriptLine scriptLine = new ScriptLine(tokens, currentLineNumber);
                    scriptLine.setIndentionLevel(currIndentionLevel);

                    if (currIndentionLevel < lastIndentionLevel) {
                        ScriptLine nextCurrLine = currentLine.getDirectParent();
                        int indentionLevelDiff = lastIndentionLevel - currIndentionLevel;

                        for (int i = 1; i < indentionLevelDiff; i++) {
                            nextCurrLine = nextCurrLine.getDirectParent();
                        }
                        currentLine = nextCurrLine;
                    }
                    if (finalLine.endsWith(":")) {

                        if (currIndentionLevel != 0) {
                            scriptLine.setDirectParent(currentLine);
                        }
                        addLine(scriptLine, currIndentionLevel);
                        currentLine = scriptLine;
                    } else {
                        addLine(scriptLine, currIndentionLevel);
                    }

                    lineBuilder.setLength(0);
                    lastIndentionLevel = nextLastIndentionLevel;
                }
            }
        }
    }

    private String removeLastChar(String s) {
        return s.substring(0, s.length() - 1);
    }

    private void addLine(ScriptLine line, int indentionLevel) {

        String function = line.getFunctionName().getValue();
        if (function.equalsIgnoreCase("def") || function.equalsIgnoreCase("define")
            || function.equalsIgnoreCase("func") || function.equalsIgnoreCase("function")) {

            if (line.getArgs().size() == 0) {
                throw new ScriptException("function name missing");
            } else if (line.getArgs().size() == 1) {
                Interpreter.FUNCTIONS.add(new Function(line.getArgs().get(0).getValue(), line.getSubLines()));
            } else {
                Interpreter.FUNCTIONS.add(new Function(line.getArgs().get(0).getValue(), line.getSubLines(), new ArgumentList(line.getArgs().subList(1, line.getArgs().size()), "").toStringList()));
            }
        } else if (function.equalsIgnoreCase("import") || function.equalsIgnoreCase("use")) {
            Interpreter.runFunction(line.getFunctionName().getValue(), new ArgumentList(line.getArgs(), line.getFunctionName().getValue()), line);
        } else if (indentionLevel == 0 || currentLine == null) {
            lines.add(line);
        } else {
            currentLine.addSubline(line);
        }
    }

    private boolean buildLine(StringBuilder lineBuilder, String line) {
        /*
        if (line.endsWith(";")) {
            lineBuilder.append(line, 0, line.length() - 1);
            return true;
        } else {
            lineBuilder.append(line);
            return false;
        }
        */
        if (line.endsWith(";")) {
            line = removeLastChar(line);
        }
        lineBuilder.append(line);
        return true;
    }

    public void saveCompiledScript(String path) {
        try {
            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(lines);
            out.close();
            fileOut.close();
            System.out.printf("Compiled script is saved in %s", path);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /**
     * If there is a main method without arguments, calls it.
     * Runs the script.
     */
    public void run() {
        for (ScriptLine line : lines) {
            ScriptException.LINE_NUMBER = line.getLineNumber();
            Interpreter.run(line);
        }
    }

    public File getFile() {
        return scriptFile;
    }
}
