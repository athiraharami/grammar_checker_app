package com.example.demo;

import java.util.*;

public class RecursiveDescentParser {

    private Map<String, List<String>> grammarRules = new LinkedHashMap<>(); // Maintain insertion order
    private Set<String> nullableNonterminals = new HashSet<>();
    private List<String> nullableRules = new ArrayList<>();
    private List<String> bdwRelationships = new ArrayList<>();
    private List<String> bwRelationships = new ArrayList<>();
    private List<String> transitiveRelationships = new ArrayList<>();
    private List<String> reflexiveRelationships = new ArrayList<>();
    private Map<String, Set<String>> firstSets = new HashMap<>(); // Store First sets
    private List<String> fdbRelationships = new ArrayList<>();
    private List<String> deoRelationships = new ArrayList<>();
    private List<String> eoRelationships = new ArrayList<>();
    private List<String> transitiveEORelationships = new ArrayList<>();
    private List<String> reflexiveEORelationships = new ArrayList<>();
    private List<String> fbRelationships = new ArrayList<>();
    private Map<String, Set<String>> followSets = new HashMap<>(); // Declare followSets
    

    // Method to accept grammar input as a string (e.g., from a REST API)
    public boolean inputGrammarFromString(String grammarInput) {
        // Remove all `{...}` content entirely (Invisible content), but keep parts like "cat"
        grammarInput = grammarInput.replaceAll("\\{[^}]*\\}", "");
    
        // Split input by newline to process each rule
        String[] rules = grammarInput.split("\n");
    
        // Process each rule
        for (String rule : rules) {
            if (!parseGrammarRule(rule)) {
                System.out.println("Error parsing rule: " + rule);
                return false; // Parsing failed
            }
        }
    
        // Check if the grammar is LL(1)
        if (!isPotentiallyLL1(grammarRules)) {
            System.out.println("The grammar is not LL(1).");
            return false; // Grammar is not LL(1)
        }
    
        return true; // Grammar parsed and validated successfully
    }
    
    private boolean parseGrammarRule(String rule) {
        String[] parts = rule.split("->");
        if (parts.length != 2) {
            System.out.println("Invalid format. Use 'Nonterminal -> Production'.");
            return false; // Return false if the format is invalid
        }
    
        String left = parts[0].trim();
        String[] right = parts[1].split("\\|");
    
        grammarRules.putIfAbsent(left, new ArrayList<>());
        for (String production : right) {
            String trimmedProduction = production.trim();
            
            // Handle epsilon production
            if (trimmedProduction.isEmpty() || trimmedProduction.equals("{}")) {
                trimmedProduction = "ϵ"; // Replace empty production with epsilon
            }
    
            // Add the production to the grammar rules map
            grammarRules.get(left).add(trimmedProduction);
        }
    
        return true; // Return true if the rule was successfully parsed and added to the grammar
    }
    

    // Function to check if the grammar is potentially LL(1)
    public boolean isPotentiallyLL1(Map<String, List<String>> grammar) {
        for (String nonTerminal : grammar.keySet()) {
            List<String> productions = grammar.get(nonTerminal);

            // Check for direct left recursion
            for (String production : productions) {
                if (production.startsWith(nonTerminal)) {
                    System.out.println("Direct left recursion detected in rule: " + nonTerminal + " -> " + production);
                    return false; // Return false if direct left recursion is detected
                }
            }

            // Calculate First sets
            Set<String> currentFirstSet = new HashSet<>();
            for (String production : productions) {
                Set<String> productionFirstSet = getFirstSet(production);

                // Check for conflicts
                for (String symbol : productionFirstSet) {
                    // Skip epsilon in the conflict check
                    if ("ϵ".equals(symbol)) {
                        continue;
                    }
                    if (currentFirstSet.contains(symbol)) {
                        System.out.println("Conflict detected: Productions for " + nonTerminal + " have a common prefix: " + symbol);
                        return false; // Return false if there's a conflict between productions
                    }
                    currentFirstSet.add(symbol);
                }
            }
        }
        return true; // Grammar is LL(1)
    }

    // Helper function to get the First set of a production
    private Set<String> getFirstSet(String production) {
        Set<String> firstSet = new HashSet<>();
        String[] tokens = production.trim().split("\\s+");

        // Add the first symbol directly if it's a terminal or nullable (ϵ)
        String firstToken = tokens[0];
        if (Character.isLowerCase(firstToken.charAt(0)) || "ϵ".equals(firstToken)) {
            firstSet.add(firstToken);
        } else {
            // If the first symbol is a non-terminal, retrieve its First set
            if (grammarRules.containsKey(firstToken)) {
                List<String> firstProductions = grammarRules.get(firstToken);
                for (String subProduction : firstProductions) {
                    firstSet.addAll(getFirstSet(subProduction));
                }
            }
        }

        return firstSet; // Return the computed First set
    }

    // Getter for grammar rules
    public Map<String, List<String>> getGrammarRules() {
        return grammarRules;
    }

    // Methods for grammar input and parsing
    public boolean parseRule(String rule) {
    String[] parts = rule.split("->");
    if (parts.length != 2) {
        return false; // Invalid rule format
    }

    String nonterminal = parts[0].trim();
    String[] productions = parts[1].split("\\|");

    grammarRules.putIfAbsent(nonterminal, new ArrayList<>());
    for (String production : productions) {
        grammarRules.get(nonterminal).add(production.trim());
    }

    return isNullable(nonterminal); // Check if the nonterminal is nullable
}
    
    //step 1
    public boolean isNullable(String nonterminal) {
        // Base case: If already determined nullable, return true
        if (nullableNonterminals.contains(nonterminal)) {
            return true;
        }
    
        List<String> productions = grammarRules.get(nonterminal);
        if (productions == null) return false; // No productions for this nonterminal
    
        for (String production : productions) {
            if (production.equals("ϵ")) {
                // Directly nullable
                nullableRules.add(nonterminal + " → ϵ");
                nullableNonterminals.add(nonterminal);
                return true;
            }
    
            // Recursive descent into the production
            boolean allNullable = true;
            for (char symbol : production.toCharArray()) {
                if (!Character.isUpperCase(symbol) || !isNullable(String.valueOf(symbol))) {
                    allNullable = false;
                    break;
                }
            }
    
            if (allNullable) {
                nullableRules.add(nonterminal + " → " + production);
                nullableNonterminals.add(nonterminal);
                return true;
            }
        }
    
        return false; // Not nullable
    }
    
        
    // Method to trigger nullable rule analysis for all nonterminals
    public void findNullableRules() {
        for (String nonterminal : grammarRules.keySet()) {
            isNullable(nonterminal);
        }
    }

    
        // Step 2: Identify BDW relationships for each nonterminal (Rule 2)
        public void findBDWRelationships() {
            for (String nonterminal : grammarRules.keySet()) {
                List<String> productions = grammarRules.get(nonterminal);
        
                for (String production : productions) {
                    // Skip productions that are nullable (ϵ)
                    if (production.equals("ϵ")) {
                        continue;
                    }
        
                    // Start checking from the leftmost symbol in the production
                    int i = 0;
                    while (i < production.length()) {
                        String symbol = String.valueOf(production.charAt(i));
        
                        // If it's a nonterminal
                        if (Character.isUpperCase(symbol.charAt(0))) {
                            // Add BDW relationship for the nonterminal symbol
                            bdwRelationships.add(nonterminal + " BDW " + symbol);
        
                            // If the symbol is nullable, check for the next symbol
                            if (nullableNonterminals.contains(symbol)) {
                                i++; // Skip to the next symbol to account for nullable nature
                            } else {
                                break; // Stop processing further if the current symbol is not nullable
                            }
                        } else {
                            // If it's a terminal symbol, just add the BDW and break
                            bdwRelationships.add(nonterminal + " BDW " + symbol);
                            break;
                        }
                    }
                }
            }
        }

        
        //step 3
        public void findBWRelationships() {
            // Step 1: Copy BDW relationships to BW
            for (String relationship : bdwRelationships) {
                bwRelationships.add(relationship.replace("BDW", "BW"));
            }
        
            // Step 2: Transitive closure (store transitive relationships separately)
            boolean updated = true;
            List<String> newTransitiveRelationships = new ArrayList<>(); // Store transitive relationships
        
            while (updated) {
                updated = false;
                List<String> newPairs = new ArrayList<>();
        
                // For each pair1, check if a transitive pair can be found
                for (String pair1 : bwRelationships) {
                    String[] parts1 = pair1.split(" BW ");
                    String left1 = parts1[0];
                    String right1 = parts1[1];
        
                    for (String pair2 : bwRelationships) {
                        String[] parts2 = pair2.split(" BW ");
                        String left2 = parts2[0];
                        String right2 = parts2[1];
        
                        // If right1 of pair1 matches left2 of pair2, we can create a transitive relationship
                        if (right1.equals(left2)) {
                            String transitivePair = left1 + " BW " + right2;
                            if (!bwRelationships.contains(transitivePair) && !newTransitiveRelationships.contains(transitivePair)) {
                                newPairs.add(transitivePair);
                                newTransitiveRelationships.add(transitivePair); // Track transitive relationships
                                updated = true;
                            }
                        }
                    }
                }
        
                // Add new transitive pairs to the BW relationships
                bwRelationships.addAll(newPairs);
            }
        
            // Step 3: Reflexive closure based on BDW relationships only
            Set<String> seenSymbols = new HashSet<>(); // Keep track of seen symbols to avoid duplicates
        
            // Add reflexive relationships for nonterminals from BDW relationships
            for (String relationship : bdwRelationships) {
                String[] parts = relationship.split(" BDW ");
                String nonterminal = parts[0];
        
                // Add reflexive relationship if not already added and is a nonterminal and not epsilon
                if (!seenSymbols.contains(nonterminal) && !nonterminal.equals("ϵ")) {
                    String reflexive = nonterminal + " BW " + nonterminal;
                    bwRelationships.add(reflexive);
                    reflexiveRelationships.add(reflexive);
                    seenSymbols.add(nonterminal);
                }
            }
        
            // Add reflexive relationships for terminals, excluding epsilon
            for (String terminal : getTerminals()) {
                if (!seenSymbols.contains(terminal) && !terminal.equals("ϵ")) {
                    String reflexive = terminal + " BW " + terminal;
                    bwRelationships.add(reflexive);
                    reflexiveRelationships.add(reflexive);
                    seenSymbols.add(terminal);
                }
            }
        
            // Store transitive relationships for later use or debugging
            this.transitiveRelationships = newTransitiveRelationships;
        }

        private Set<String> getTerminals() {
            Set<String> terminals = new HashSet<>();
            for (List<String> productions : grammarRules.values()) {
                for (String production : productions) {
                    for (char symbol : production.toCharArray()) {
                        if (Character.isLowerCase(symbol)) {
                            terminals.add(String.valueOf(symbol));
                        }
                    }
                }
            }
            return terminals;
        }
        
        // Step 4: Method to compute First sets based on the productions
        public void computeFirstSets() {
            for (String nonterminal : grammarRules.keySet()) {
                firstSets.put(nonterminal, computeFirst(nonterminal));
            }
            
            // New rule: Compute First sets based on BW relationships
            computeFirstFromBW();
        }
        
        // Method to compute First for a single nonterminal
        public Set<String> computeFirst(String nonterminal) {
            Set<String> firstSet = new HashSet<>();
        
            // Get the list of productions for this nonterminal
            List<String> productions = grammarRules.get(nonterminal);
        
            for (String production : productions) {
                // Case 1: If the production is epsilon, add epsilon to the first set
                if (production.equals("ϵ")) {
                    firstSet.add("ϵ");
                    continue;
                }
        
                // Case 2: If the production starts with a terminal, add it to the first set
                char firstChar = production.charAt(0);
                if (Character.isLowerCase(firstChar)) {
                    firstSet.add(String.valueOf(firstChar));
                }
                // Case 3: If the production starts with a nonterminal, recursively compute its First set
                else if (Character.isUpperCase(firstChar)) {
                    Set<String> subFirstSet = computeFirst(String.valueOf(firstChar));
                    firstSet.addAll(subFirstSet);
                    // If the nonterminal can derive epsilon, we need to check the next symbols
                    if (subFirstSet.contains("ϵ")) {
                        // Check the rest of the production for First sets
                        for (int i = 1; i < production.length(); i++) {
                            char nextChar = production.charAt(i);
                            if (Character.isLowerCase(nextChar)) {
                                firstSet.add(String.valueOf(nextChar));
                                break;
                            } else if (Character.isUpperCase(nextChar)) {
                                Set<String> nextFirstSet = computeFirst(String.valueOf(nextChar));
                                firstSet.addAll(nextFirstSet);
                                if (!nextFirstSet.contains("ϵ")) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        
            return firstSet;
        }
        
        // New method to compute First sets based on BW relationships
        private void computeFirstFromBW() {
            for (String relationship : bwRelationships) {
                String[] parts = relationship.split(" BW ");
                String left = parts[0];
                String right = parts[1];
        
                if (Character.isLowerCase(right.charAt(0))) { // Check if 'right' is a terminal
                    firstSets.putIfAbsent(left, new HashSet<>());
                    // Add only if it's not epsilon
                    if (!right.equals("ϵ")) {
                        firstSets.get(left).add(right);
                    }
                }
            }
        }
        
        // Step 5: Compute First of right side of each production
        public void computeFirstOfRightSide(StringBuilder output) {
            output.append("\nStep 5: Compute First of Right Side\n");
        
            for (Map.Entry<String, List<String>> entry : grammarRules.entrySet()) {
                for (String production : entry.getValue()) {
                    Set<String> firstOfRight = new HashSet<>();
                    boolean allNullable = true; // Track if all symbols in production are nullable
        
                    output.append("First(").append(production).append(") = {");
        
                    for (int i = 0; i < production.length(); i++) {
                        String symbol = String.valueOf(production.charAt(i));
        
                        // Retrieve First set for the current symbol
                        Set<String> symbolFirst = firstSets.getOrDefault(symbol, new HashSet<>());
        
                        // Add all elements from First(symbol) except 'ϵ'
                        firstOfRight.addAll(symbolFirst);
                        firstOfRight.remove("ϵ"); // Avoid adding 'ϵ' initially for correct union behavior
        
                        // If the current symbol is not nullable, stop processing further symbols
                        if (!nullableNonterminals.contains(symbol)) {
                            allNullable = false;
                            break;
                        }
                    }
        
                    // If all symbols in the production are nullable, add 'ϵ' to the First set
                    if (allNullable) {
                        firstOfRight.add("ϵ");
                    }
        
                    // Format and append the First set with curly brackets
                    output.append(String.join(", ", firstOfRight)).append("}\n");
                }
            }
        }

        // Step 6: Compute the relation Is Followed Directly By (FDB)
        public void computeFDBRelationships() {
            fdbRelationships.clear();  // Clear any existing FDB relationships before computing new ones
            Set<String> seenFDB = new HashSet<>();  // To avoid duplicate entries
        
            // Process all grammar rules
            for (Map.Entry<String, List<String>> entry : grammarRules.entrySet()) {
                List<String> productions = entry.getValue();
        
                for (String production : productions) {
                    // Start from the leftmost symbol in the production
                    processProduction(production, seenFDB);
                }
            }
        }
        
        private void processProduction(String production, Set<String> seenFDB) {
            for (int i = 0; i < production.length(); i++) {
                String symbol = String.valueOf(production.charAt(i));
        
                // Process nonterminals and nullable nonterminals
                if (Character.isUpperCase(symbol.charAt(0))) {
                    // If the symbol is a nonterminal, check for direct FDB relationships
                    if (i + 1 < production.length()) {
                        String nextSymbol = String.valueOf(production.charAt(i + 1));
        
                        // If next symbol is nonterminal or terminal, add FDB relationship
                        if (!seenFDB.contains(symbol + " FDB " + nextSymbol)) {
                            fdbRelationships.add(symbol + " FDB " + nextSymbol);
                            seenFDB.add(symbol + " FDB " + nextSymbol);
                            System.out.println("Adding FDB: " + symbol + " FDB " + nextSymbol);
                        }
                    }
        
                    // If the symbol is a nullable nonterminal, process it with its surrounding symbols
                    if (nullableNonterminals.contains(symbol)) {
                        // Check if nullable nonterminal is between two symbols
                        if (i - 1 >= 0 && i + 1 < production.length()) {
                            String prevSymbol = String.valueOf(production.charAt(i - 1));
                            String nextSymbol = String.valueOf(production.charAt(i + 1));
        
                            // If the previous symbol is a nonterminal and the next symbol is a terminal or nonterminal
                            if (Character.isUpperCase(prevSymbol.charAt(0)) && !seenFDB.contains(prevSymbol + " FDB " + nextSymbol)) {
                                fdbRelationships.add(prevSymbol + " FDB " + nextSymbol);
                                seenFDB.add(prevSymbol + " FDB " + nextSymbol);
                                System.out.println("Nullable nonterminal handling, Adding FDB: " + prevSymbol + " FDB " + nextSymbol);
                            }
                        }
                    }
                }
            }
        }
                        
        // Step 7: Compute Direct End Of (DEO) Relationships
        public void computeDEORelationships() {
            deoRelationships.clear(); // Clear any existing relationships
        
            List<String> allDEORelationships = new ArrayList<>(); // List to store relationships in order of rules
        
            // Step 1: Iterate through the grammar rules in the order they appear
            for (Map.Entry<String, List<String>> entry : grammarRules.entrySet()) {
                String nonterminal = entry.getKey();
                List<String> productions = entry.getValue();
        
                List<String> ruleDEORelationships = new ArrayList<>(); // List to store relationships for this rule
        
                // Step 2: Process each production for the current nonterminal
                for (String production : productions) {
                    // Skip epsilon productions (A → ϵ)
                    if (production.equals("ϵ")) continue;
        
                    // Step 3: Process the production from right to left
                    String[] symbols = production.split("");
        
                    // Flag to track if a terminal or nonterminal DEO relation is added
                    boolean foundDEO = false;
        
                    // Step 4: Iterate through the symbols in reverse order (right to left)
                    for (int i = symbols.length - 1; i >= 0; i--) {
                        String symbol = symbols[i];
        
                        // Case 1: If the symbol is a terminal, form the DEO relationship with the nonterminal on the left
                        if (!Character.isUpperCase(symbol.charAt(0))) {
                            ruleDEORelationships.add(symbol + " DEO " + nonterminal);
                            foundDEO = true; // DEO relationship found
                            break; // Stop after finding the terminal
                        }
        
                        // Case 2: If the symbol is a nonterminal and it is not nullable, form DEO relationship
                        if (Character.isUpperCase(symbol.charAt(0)) && !nullableNonterminals.contains(symbol)) {
                            ruleDEORelationships.add(symbol + " DEO " + nonterminal);
                            foundDEO = true; // DEO relationship found
                            break; // Stop after finding the non-nullable nonterminal
                        }
                    }
        
                    // Optionally, if the nonterminal is nullable, propagate its DEO to itself
                    if (nullableNonterminals.contains(nonterminal)) {
                        ruleDEORelationships.add(nonterminal + " DEO " + nonterminal);
                    }
        
                    // If a nullable nonterminal is found, we need to propagate to the terminal/nonterminal after it
                    if (foundDEO) {
                        // Now, check for the case where nullable nonterminal is in the middle
                        for (int i = 0; i < symbols.length; i++) {
                            String symbol = symbols[i];
        
                            // If a terminal follows a nullable nonterminal, we create a DEO relationship
                            if (nullableNonterminals.contains(symbol) && i + 1 < symbols.length) {
                                String nextSymbol = symbols[i + 1];
                                if (!Character.isUpperCase(nextSymbol.charAt(0))) { // Terminal follows nullable nonterminal
                                    ruleDEORelationships.add(symbol + " DEO " + nextSymbol);
                                }
                            }
                        }
                    }
                }
        
                // After processing all productions for this rule, add the rule's DEO relationships to the overall list
                allDEORelationships.addAll(ruleDEORelationships);
            }
        
            // Remove duplicates and sort relationships
            deoRelationships = new ArrayList<>(new HashSet<>(allDEORelationships));
            deoRelationships.sort(String::compareTo); // Optional: You can sort if required
        }
        
        //step 8
        public void computeEORelationships() {
            // Step 1: Copy DEO relationships to EO
            for (String deo : deoRelationships) {
                eoRelationships.add(deo.replace("DEO", "EO"));
            }
        
            // Step 2: Transitive closure for EO
            boolean updated = true;
            List<String> newTransitiveEORelationships = new ArrayList<>(); // Track new transitive relationships
        
            while (updated) {
                updated = false;
                List<String> newPairs = new ArrayList<>();
        
                // For each pair1, check if a transitive pair can be found
                for (String pair1 : eoRelationships) {
                    String[] parts1 = pair1.split(" EO ");
                    if (parts1.length != 2) continue; // Skip invalid pairs
                    String left1 = parts1[0];
                    String right1 = parts1[1];
        
                    for (String pair2 : eoRelationships) {
                        String[] parts2 = pair2.split(" EO ");
                        if (parts2.length != 2) continue; // Skip invalid pairs
                        String left2 = parts2[0];
                        String right2 = parts2[1];
        
                        // Create transitive relationship
                        if (right1.equals(left2)) {
                            String transitivePair = left1 + " EO " + right2;
                            if (!eoRelationships.contains(transitivePair) && !newTransitiveEORelationships.contains(transitivePair)) {
                                newPairs.add(transitivePair);
                                newTransitiveEORelationships.add(transitivePair); // Track transitive relationships
                                updated = true;
                            }
                        }
                    }
                }
        
                // Add new transitive pairs to the EO relationships
                eoRelationships.addAll(newPairs);
            }
        
            // Step 3: Reflexive closure for all nonterminals and terminals (excluding epsilon)
            Set<String> seenSymbols = new HashSet<>();
            
            // Reflexive for nonterminals
            for (String nonterminal : grammarRules.keySet()) {
                String reflexive = nonterminal + " EO " + nonterminal;
                if (!seenSymbols.contains(nonterminal) && !eoRelationships.contains(reflexive)) {
                    eoRelationships.add(reflexive);
                    reflexiveEORelationships.add(reflexive);
                    seenSymbols.add(nonterminal);
                }
            }
        
            // Reflexive for terminals (excluding epsilon)
            for (String terminal : getTerminals()) {
                String reflexive = terminal + " EO " + terminal;
                if (!seenSymbols.contains(terminal) && !terminal.equals("ϵ") && !eoRelationships.contains(reflexive)) {
                    eoRelationships.add(reflexive);
                    reflexiveEORelationships.add(reflexive);
                    seenSymbols.add(terminal);
                }
            }
        
            // Store transitive relationships for later use or debugging
            this.transitiveEORelationships = newTransitiveEORelationships;
        }
        
        //step 9
        public List<String> computeFBRelationships() {
            List<String> fbRelationships = new ArrayList<>();
        
            // Iterate through each "X EO A" in the From EO list
            for (String eo : eoRelationships) {
                String[] eoParts = eo.split(" ");  // Split to get "X EO A" format
                if (eoParts.length == 3) {
                    String X = eoParts[0];  // "X" from "X EO A"
                    String A = eoParts[2];  // "A" from "X EO A"
        
                    // Look for "A FDB Y" in the FDB list
                    for (String fdb : fdbRelationships) {
                        String[] fdbParts = fdb.split(" ");  // Split to get "A FDB Y" format
                        if (fdbParts.length == 3 && fdbParts[0].equals(A)) {
                            String Y = fdbParts[2];  // "Y" from "A FDB Y"
        
                            // Now look for "Y BW Z" in the From BW list
                            for (String bw : bwRelationships) {
                                String[] bwParts = bw.split(" ");  // Split to get "Y BW Z" format
                                if (bwParts.length == 3 && bwParts[0].equals(Y)) {
                                    String Z = bwParts[2];  // "Z" from "Y BW Z"
        
                                    // Add the final relationship "X FB Z"
                                    fbRelationships.add(X + " FB " + Z);
                                }
                            }
                        }
                    }
                }
            }
        
            // Return the list of FB relationships
            return fbRelationships;
        }
        
        

        //step 10
        // Step 10
        // Step 10: Compute FB Relationships with End Mark
        public void computeFBRelationshipsWithEndMark(StringBuilder output) {
            // List to store the final FB relationships (including FB ←)
            fbRelationships.clear();  // Ensure we're working with a fresh list
        
            // Iterate through each "X EO A" in the From EO list
            for (String eo : eoRelationships) {
                String[] eoParts = eo.split(" ");  // Split to get "X EO A" format
                if (eoParts.length == 3) {
                    String X = eoParts[0];  // "X" from "X EO A"
                    String A = eoParts[2];  // "A" from "X EO A"
        
                    // Look for "A FDB Y" in the FDB list
                    for (String fdb : fdbRelationships) {
                        String[] fdbParts = fdb.split(" ");  // Split to get "A FDB Y" format
                        if (fdbParts.length == 3 && fdbParts[0].equals(A)) {
                            String Y = fdbParts[2];  // "Y" from "A FDB Y"
        
                            // Now look for "Y BW Z" in the From BW list
                            for (String bw : bwRelationships) {
                                String[] bwParts = bw.split(" ");  // Split to get "Y BW Z" format
                                if (bwParts.length == 3 && bwParts[0].equals(Y)) {
                                    String Z = bwParts[2];  // "Z" from "Y BW Z"
        
                                    // Add the final relationship "X FB Z"
                                    fbRelationships.add(X + " FB " + Z);
                                }
                            }
                        }
                    }
                }
            }
        
            // Step 10: Extend FB relationships by adding FB ← if A EO S exists
            for (String eo : eoRelationships) {
                String[] eoParts = eo.split(" ");
                if (eoParts.length == 3 && eoParts[2].equals("S")) {
                    // If "A EO S" exists, add "A FB ←" only if A is a nonterminal
                    String A = eoParts[0];
                    if (isNonTerminal(A)) {  // Check if A is a nonterminal
                        fbRelationships.add(A + " FB ←");
                    }
                }
            }
        
            // Append Step 10 header and output
            output.append("\nStep 10: Final FB Relationships with End Mark\n");
            output.append("-------------------------------------------------------\n");
        
            if (fbRelationships.isEmpty()) {
                output.append("No FB relationships found.\n");
            } else {
                // Display the final FB relationships (including FB ←)
                for (String fb : fbRelationships) {
                    output.append(fb).append("\n");
                }
            }
        }
        
        
        
        // Helper method to check if a symbol is a nonterminal
        private boolean isNonTerminal(String symbol) {
            // Check if the symbol exists as a key in the grammarRules map
            return grammarRules.containsKey(symbol);
        }
        
        // Method to display the final FB relationships (including FB ←)
        public void displayFBResults(List<String> fbRelationships) {
            System.out.println("\nStep 10: Final FB Relationships with End Mark");
            System.out.println("--------------------------------------------------------------");
            for (String fb : fbRelationships) {
                System.out.println(fb);
            }
        }
        
        //step 11
        public void computeFollowSetForNullable(StringBuilder output) {
            followSets.clear(); // Clear any previous data
            output.append("\nStep 11: Compute Follow Set for Nullable Nonterminals\n");
        
            for (String nullable : nullableNonterminals) {
                followSets.put(nullable, new HashSet<>()); // Initialize empty Follow set
            }
        
            for (String fb : fbRelationships) { // Assuming fbRelationships is populated
                String[] fbParts = fb.split(" ");
                if (fbParts.length == 3) {
                    String leftSymbol = fbParts[0];
                    String rightSymbol = fbParts[2];
        
                    if (nullableNonterminals.contains(leftSymbol) && !isNonTerminal(rightSymbol)) {
                        followSets.get(leftSymbol).add(rightSymbol); // Add terminal to Follow set
                    }
                    if (nullableNonterminals.contains(leftSymbol) && "←".equals(rightSymbol)) {
                        followSets.get(leftSymbol).add("←"); // Add end marker
                    }
                }
            }
        
            for (Map.Entry<String, Set<String>> entry : followSets.entrySet()) {
                output.append("Fol(").append(entry.getKey()).append(") = {")
                      .append(String.join(", ", entry.getValue())).append("}\n");
            }
        }

        //step 12
        public void computeSelectSet(StringBuilder output) {
            output.append("\nStep 12: Compute Select Set for Each Production\n");
        
            int productionNumber = 1;
            for (Map.Entry<String, List<String>> entry : grammarRules.entrySet()) {
                String nonTerminal = entry.getKey();
        
                for (String production : entry.getValue()) {
                    Set<String> selectSet = new HashSet<>();
        
                    if (production.equals("ϵ")) { // Nullable rule
                        selectSet.addAll(followSets.getOrDefault(nonTerminal, new HashSet<>()));
                        output.append("Sel(").append(productionNumber).append(") = First(ϵ) U Fol(")
                              .append(nonTerminal).append(") = {} U {")
                              .append(String.join(", ", followSets.getOrDefault(nonTerminal, new HashSet<>())))
                              .append("}\n");
                    } else { // Non-nullable rule
                        Set<String> firstOfProduction = computeFirstForProduction(production);
                        selectSet.addAll(firstOfProduction);
        
                        output.append("Sel(").append(productionNumber).append(") = First(")
                              .append(production).append(") = {")
                              .append(String.join(", ", firstOfProduction)).append("}\n");
                    }
        
                    productionNumber++;
                }
            }
        }
        
        // Helper Method: Compute First of a production
        private Set<String> computeFirstForProduction(String production) {
            Set<String> firstSet = new HashSet<>();
            boolean allNullable = true;
        
            for (int i = 0; i < production.length(); i++) {
                String symbol = String.valueOf(production.charAt(i));
                Set<String> symbolFirst = firstSets.getOrDefault(symbol, new HashSet<>());
        
                firstSet.addAll(symbolFirst);
                firstSet.remove("ϵ"); // Exclude ϵ temporarily
        
                if (!nullableNonterminals.contains(symbol)) {
                    allNullable = false;
                    break;
                }
            }
        
            if (allNullable) {
                firstSet.add("ϵ");
            }
        
            return firstSet;
        }
        
        // Method to display the nullable rules and nonterminals
        public void displayNullableResults(StringBuilder output) {
            output.append("Nullable Rules:\n");
            for (String rule : nullableRules) {
                output.append(rule).append("\n");
            }
        
            output.append("\nNullable Nonterminals:\n");
            for (String nonterminal : nullableNonterminals) {
                output.append(nonterminal).append("\n");
            }
        }
    
        // Method to display the BDW relationships in the order of input rules
        public void displayBDWResults(StringBuilder output) {
            for (String relationship : bdwRelationships) {
                output.append(relationship).append("\n");
            }
        }
    
        // Method to display the BW relationships with categories
        public void displayBWResults(StringBuilder output) {
            output.append("From BDW Relationships:\n");
            for (String relationship : bwRelationships) {
                if (bdwRelationships.contains(relationship.replace("BW", "BDW"))) {
                    output.append(relationship).append("\n");
                }
            }
        
            output.append("\nTransitive Relationships:\n");
            for (String relationship : transitiveRelationships) {
                output.append(relationship).append("\n");
            }
        
            output.append("\nReflexive Relationships:\n");
            for (String relationship : reflexiveRelationships) {
                output.append(relationship).append("\n");
            }
        }

        // Method to display the First sets, excluding epsilon
        public void displayFirstSets(StringBuilder output) {
            
            for (Map.Entry<String, Set<String>> entry : firstSets.entrySet()) {
                Set<String> filteredFirstSet = new HashSet<>(entry.getValue());
                filteredFirstSet.remove("ϵ"); // Exclude epsilon
        
                // Append results to the output in a readable format
                output.append("First(").append(entry.getKey()).append(") = {");
                output.append(String.join(", ", filteredFirstSet));
                output.append("}\n");
            }
        }
        
        //method to display fdb
        public void displayFDBResults(StringBuilder output) {
            
            for (String fdb : fdbRelationships) {
                output.append(fdb).append("\n");
            }
        }
        
        // Display DEO Relationships
        public void displayDEORelationships(StringBuilder output) {
            
            for (String relationship : deoRelationships) {
                output.append(relationship).append("\n");
            }
        }
        
        // Method to display the EO relationships with categories (for Step 8)
        public String displayEORelationships() {
            StringBuilder output = new StringBuilder();
            
            // Display relationships from DEO (converted to EO)
            output.append("\nFrom DEO Relationships:\n");
            if (deoRelationships.isEmpty()) {
                output.append("No DEO relationships found.\n");
            } else {
                for (String relationship : deoRelationships) {
                    output.append(relationship.replace("DEO", "EO")).append("\n");
                }
            }
        
            // Display transitive relationships
            output.append("\nTransitive Relationships:\n");
            if (transitiveEORelationships.isEmpty()) {
                output.append("No transitive relationships found.\n");
            } else {
                for (String transitive : transitiveEORelationships) {
                    output.append(transitive).append("\n");
                }
            }
        
            // Display reflexive relationships
            output.append("\nReflexive Relationships:\n");
            if (reflexiveEORelationships.isEmpty()) {
                output.append("No reflexive relationships found.\n");
            } else {
                for (String reflexive : reflexiveEORelationships) {
                    output.append(reflexive).append("\n");
                }
            }
        
            return output.toString();
        }
        
        
        public void displayFBResults(StringBuilder output) {
            // Display EO Relationships
            output.append("\nFrom EO Relationships:\n");
            if (eoRelationships.isEmpty()) {
                output.append("No EO relationships found.\n");
            } else {
                for (String relationship : eoRelationships) {
                    output.append(relationship).append("\n");
                }
            }
        
            // Display FDB Relationships
            output.append("\nFrom FDB Relationships:\n");
            if (fdbRelationships.isEmpty()) {
                output.append("No FDB relationships found.\n");
            } else {
                for (String relationship : fdbRelationships) {
                    output.append(relationship).append("\n");
                }
            }
        
            // Display BW Relationships
            output.append("\nFrom BW Relationships:\n");
            if (bwRelationships.isEmpty()) {
                output.append("No BW relationships found.\n");
            } else {
                for (String relationship : bwRelationships) {
                    output.append(relationship).append("\n");
                }
            }
        }
                
        
        
        public void displayFBResultsPart2(StringBuilder output) {
            // Filter EO and BW relationships based on FDB relationships
            List<String> filteredEO = new ArrayList<>();
            List<String> filteredBW = new ArrayList<>();
        
            for (String eo : eoRelationships) {
                String[] eoParts = eo.split(" ");
                if (eoParts.length == 3) {
                    String nonterminal = eoParts[2];
                    for (String fdb : fdbRelationships) {
                        String[] fdbParts = fdb.split(" ");
                        if (fdbParts.length == 3 && fdbParts[0].equals(nonterminal)) {
                            filteredEO.add(eo);
                            break;
                        }
                    }
                }
            }
        
            for (String bw : bwRelationships) {
                String[] bwParts = bw.split(" ");
                if (bwParts.length == 3) {
                    String nonterminal = bwParts[0];
                    for (String fdb : fdbRelationships) {
                        String[] fdbParts = fdb.split(" ");
                        if (fdbParts.length == 3 && fdbParts[2].equals(nonterminal)) {
                            filteredBW.add(bw);
                            break;
                        }
                    }
                }
            }
        
            // Fixed column widths
            int columnWidth = 15;
        
            // Print header
            output.append(String.format("%-" + columnWidth + "s%-" + columnWidth + "s%-" + columnWidth + "s\n", "From EO", "From FDB", "From BW"));
            output.append("-".repeat(columnWidth * 3)).append("\n");
        
            // Determine the maximum number of rows
            int maxSize = Math.max(filteredEO.size(), Math.max(fdbRelationships.size(), filteredBW.size()));
        
            // Display filtered relationships
            for (int i = 0; i < maxSize; i++) {
                String eo = i < filteredEO.size() ? filteredEO.get(i) : "";
                String fdb = i < fdbRelationships.size() ? fdbRelationships.get(i) : "";
                String bw = i < filteredBW.size() ? filteredBW.get(i) : "";
        
                output.append(String.format("%-" + columnWidth + "s%-" + columnWidth + "s%-" + columnWidth + "s\n", eo, fdb, bw));
            }
        }
        
        
                
        
        public String runParser(List<String> grammarInput) {
            StringBuilder output = new StringBuilder();  // Collect all results in a StringBuilder
        
            // Step 1: Process each grammar rule
            for (String rule : grammarInput) {
                boolean success = inputGrammarFromString(rule);
                if (!success) {
                    return "There was an error processing the grammar.";
                }
            }
        
            // Step 1: Find and display Nullable Rules and Nonterminals
            findNullableRules();
            output.append("\nStep 1: Nullable Rules and Nonterminals\n");
            displayNullableResults(output);
        
            // Step 2: BDW Relationships
            findBDWRelationships();
            output.append("\nStep 2: BDW Relationships\n");
            displayBDWResults(output);
        
            // Step 3: BW Relationships
            findBWRelationships();
            output.append("\nStep 3: BW Relationships\n");
            displayBWResults(output);
        
            // Step 4: First sets
            computeFirstSets();
            output.append("\nStep 4: First Sets\n");
            displayFirstSets(output);
        
            // Step 5: First of Right Side
            computeFirstOfRightSide(output);
        
            // Step 6: FDB Relationships
            computeFDBRelationships();
            output.append("\nStep 6: Followed Directly By (FDB) Relationships\n");
            displayFDBResults(output);
        
            // Step 7: DEO Relationships
            computeDEORelationships();
            output.append("\nStep 7: Direct End Of (DEO) Relationships\n");
            displayDEORelationships(output);
        
            // Step 8: EO Relationships
            computeEORelationships();
            output.append("\nStep 8: EO Relationships");
            output.append(displayEORelationships()); // Append the EO relationships
        
            // Step 9: Display Relationships
            output.append("\nStep 9: First Part - Display Relationships\n");
            displayFBResults(output);
        
            // Step 9: Second Part - Display Filtered Relationships
            output.append("\nStep 9: Second Part - Display Filtered Relationships\n");
            displayFBResultsPart2(output);
        
            // Step 9: Final FB Relationships
            output.append("\nStep 9: Final FB Relationships\n");
            List<String> fbRelationships = computeFBRelationships();  // Compute the final FB relationships
            for (String fb : fbRelationships) {
                output.append(fb).append("\n");
            }

            // Step 10: Compute and display the final FB relationships (including FB ←)
            computeFBRelationshipsWithEndMark(output);

            // Step 11: Compute and display the Follow sets for nullable nonterminals
            computeFollowSetForNullable(output);

            //step 12:  
            computeSelectSet(output);
        
            return output.toString();  // Return the accumulated results
        }
        
        
        
    
}