//Assembler.java
//Devon Burton 10/22/24
//Coverts an hack assembly language program into binary
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Assembler {
    private static final int VARIABLE_START_ADDRESS = 16;
    private static final int BINARY_LENGTH = 15;

    public static void main(String[] args) throws FileNotFoundException {
        // Create important variables
        String fileName = "SumN.asm";
        File outputFile = new File(fileName.substring(0, fileName.indexOf(".")) + ".hack");
        ArrayList<String> wholeProgram = new ArrayList<String>();
        HashMap<String, String> symbolMap = new HashMap<>();
        HashMap<String, String> compMap = new HashMap<>();
        HashMap<String, String> destMap = new HashMap<>();
        HashMap<String, String> jmpMap = new HashMap<>();

        // load every line of the assembly into an ArrayList doesn't add empty lines
        loadArrayList(new File(fileName), wholeProgram);

        // remove whitespace and comments
        cleanCode(wholeProgram);

        // load labels into symbol map
        extractLabelsIntoSymbolMap(wholeProgram, symbolMap);

        // generic method so that two methods are not needed for slymbols and other look
        // up tables
        // load lookup table symbols into symbol map
        loadMapFromFile(new File("symbols.txt"), symbolMap);

        // load lookup table comp, dest, jmp into their own maps
        loadMapFromFile(new File("comp.txt"), compMap);
        loadMapFromFile(new File("dest.txt"), destMap);
        loadMapFromFile(new File("jmp.txt"), jmpMap);

        // go through A instructions and replace with symbols
        // if symbol isn't in symbol table then add to variable table and replace it
        createVariablesAndReplaceSymbols(wholeProgram, symbolMap);

        // After cleaning the code we can directly convert each instruction to binary
        convertToBinary(wholeProgram, outputFile, compMap, destMap, jmpMap);

        // Now with a binary instruction list we print this to the output file
        writeToFile(outputFile, wholeProgram);
    }

    // loadArrayList(Scanner scanner, ArrayList<String> wholeProgram)
    // Takes an input file and array of string and loads the array with
    // line of file skipping any empty lines
    public static void loadArrayList(File inputFile, ArrayList<String> wholeProgram) throws FileNotFoundException {
        Scanner scanner = new Scanner(inputFile);
        while (scanner.hasNext()) {
            String nextLine = scanner.nextLine();
            if (nextLine == "")
                continue;
            wholeProgram.add(nextLine);
        }
        scanner.close();
    }

    // cleanCode(ArrayList<String> wholeProgram)
    // Takes an array of strings as the input and removes all whitespace
    // also deletes comments at end of lines and removes comment lines
    public static void cleanCode(ArrayList<String> wholeProgram) {
        for (int i = 0; i < wholeProgram.size(); i++) {
            wholeProgram.get(i);
            wholeProgram.set(i, wholeProgram.get(i).replaceAll("\\s", ""));
            int indexOfPotentialComment = wholeProgram.get(i).indexOf("//");
            // 0 means line of comment, -1 means no comment, default means comment in middle
            // of line
            switch (indexOfPotentialComment) {
                case 0:
                    wholeProgram.remove(i);
                    i--;
                    break;

                case -1:
                    break;

                default:
                    wholeProgram.set(i, wholeProgram.get(i).substring(0, indexOfPotentialComment));
                    break;
            }
        }
    }

    // extractLabelsIntoSymbolMap(ArrayList<String> wholeProgram, HashMap<String,
    // Integer> symbolMap)
    // goes through each line and if it starts with ( it is a label
    // we then add to a map it's name and it's line number then remove it
    public static void extractLabelsIntoSymbolMap(ArrayList<String> wholeProgram, HashMap<String, String> symbolMap) {
        for (int i = 0; i < wholeProgram.size(); i++) {
            if (wholeProgram.get(i).charAt(0) == '(') {
                String labelContents = wholeProgram.get(i).substring(1, wholeProgram.get(i).length() - 1);
                symbolMap.put(labelContents, i + "");
                wholeProgram.remove(i);
                i--;
            }
        }
    }

    // loadMapFromFile(File file, HashMap<String, String> map)
    // takes a file and hashmap and scans the file line by line
    // filling up the table by separating values at a _
    public static void loadMapFromFile(File file, HashMap<String, String> map) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        while (scanner.hasNext()) {
            String[] symbol = scanner.nextLine().split("_");
            map.put(symbol[0], symbol[1]);
        }
        scanner.close();

    }

    // createVariablesAndReplaceSymbols(ArrayList<String> wholeProgram,
    // HashMap<String, String> symbolMap)
    // loops through each instrucion and checks if it starts with @
    // if it does then either replace the rest with a symbol or if
    // no symbol exists for it then make it a variable
    public static void createVariablesAndReplaceSymbols(ArrayList<String> wholeProgram,
            HashMap<String, String> symbolMap) {
        HashMap<String, Integer> varMap = new HashMap<>();
        for (int i = 0; i < wholeProgram.size(); i++) {
            if (wholeProgram.get(i).charAt(0) == '@') {
                if (symbolMap.containsKey(wholeProgram.get(i).substring(1))) {
                    wholeProgram.set(i, "@" + symbolMap.get(wholeProgram.get(i).substring(1)));
                } else {
                    if (!varMap.containsKey(wholeProgram.get(i).substring(1))) {
                        varMap.put(wholeProgram.get(i).substring(1), VARIABLE_START_ADDRESS + varMap.size());
                    }
                    wholeProgram.set(i, "@" + varMap.get(wholeProgram.get(i).substring(1)));
                }
            }
        }
    }

    //computeAInstructionBinary(String AInstruction)
    //Convert number in A instruction to binary number and add 0 to begginging 
    public static String computeAInstructionBinary(String AInstruction) {
        return "0" + decimalToBinary(Integer.parseInt(AInstruction.substring(1)));
    }

    //computeCInstructionBinary(String CInstruction, HashMap<String, String> compMap,
    //HashMap<String, String> destMap, HashMap<String, String> jmpMap)
    //Takes a C instruction and converts it to binary 
    //Works left to right on the bits and adding to the final binary
    public static String computeCInstructionBinary(String CInstruction, HashMap<String, String> compMap,
            HashMap<String, String> destMap, HashMap<String, String> jmpMap) {
        String binary = "";
        int equalsIndex = CInstruction.indexOf('=');
        int semiCollenIndex = CInstruction.indexOf(';');
        String compString = "";
        if (equalsIndex != -1 && semiCollenIndex != -1) {
            compString = CInstruction.substring(equalsIndex + 1, semiCollenIndex);
        } else if (equalsIndex == -1 && semiCollenIndex != -1) {
            compString = CInstruction.substring(0, semiCollenIndex);
        } else
            compString = CInstruction.substring(equalsIndex + 1);
        String compBinary = compMap.get(compString);
        binary += "111";
        binary += compBinary.charAt(0);
        binary += compBinary.substring(1);

        if (equalsIndex != -1) {
            binary += destMap.get(CInstruction.substring(0, equalsIndex));
        } else
            binary += "000";

        if (semiCollenIndex != -1) {
            binary += jmpMap.get(CInstruction.substring(semiCollenIndex + 1));
        } else
            binary += "000";
        return binary;
    }

    //convertToBinary(ArrayList<String> wholeProgram, File outputFile, HashMap<String, String> compMap,
    //HashMap<String, String> destMap, HashMap<String, String> jmpMap)
    //Determines if a function is an A ins or C ins then calls the method to convert them to binary
    public static void convertToBinary(ArrayList<String> wholeProgram, File outputFile, HashMap<String, String> compMap,
            HashMap<String, String> destMap, HashMap<String, String> jmpMap) throws FileNotFoundException {

        for (int i = 0; i < wholeProgram.size(); i++) {
            // If A instruction
            if (wholeProgram.get(i).charAt(0) == '@') {
                wholeProgram.set(i, computeAInstructionBinary(wholeProgram.get(i)));
            }
            // else C instruction
            else {
                wholeProgram.set(i, computeCInstructionBinary(wholeProgram.get(i), compMap, destMap, jmpMap));
            }
        }
    }

    //writeToFile(File outputFile, ArrayList<String> wholeProgram)
    //Uses printwriter to print each line of binary to output file 
    public static void writeToFile(File outputFile, ArrayList<String> wholeProgram) throws FileNotFoundException {
        PrintWriter printWriter = new PrintWriter(outputFile);
        for (int i = 0; i < wholeProgram.size(); i++) {
            printWriter.println(wholeProgram.get(i));
        }
        printWriter.close();
    }

    //decimalToBinary(int num)
    //Converts a deacimal number to binary and adds empty 0's to the left so it is 15 bits
    public static String decimalToBinary(int num) {
        String binaryNum = "";
        while (num != 0) {
            binaryNum += num % 2;
            num /= 2;
        }
        String reverseString = new String(new StringBuilder(binaryNum).reverse());
        while (reverseString.length() < BINARY_LENGTH) {
            reverseString = "0" + reverseString;
        }
        return reverseString;
    }
}
