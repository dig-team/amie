package amie.mining.miniAmie;

import amie.data.KB;
import amie.mining.assistant.MiningAssistant;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.*;
import org.eclipse.rdf4j.sparqlbuilder.constraint.In;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class utils {

    public static MiningAssistant miningAssistant;

    public static final int ATOM_SIZE = 3;

    public static final int SUBJECT_POSITION = 0;
    public static final int RELATION_POSITION = 1;
    public static final int OBJECT_POSITION = 2;

    public static final int NO_OVERLAP_VALUE = -1;

    /**
     * ExplorationResult is instantiated to return the result of an exploration.
     * - sumExploredRules is the approximate number of rules to be explored in the subtree
     * - finalRules is the list of rules to be kept as a result of the mining process
     */
    public static class ExplorationResult {
        int sumExploredRules;
        List<Rule> finalRules;

        public ExplorationResult(int sumExploredRules, List<Rule> finalRules) {
            this.sumExploredRules = sumExploredRules;
            this.finalRules = finalRules;
        }

    }

    protected static List<Integer> SelectRelations() {
        List<Integer> relations = new ArrayList<>();

        for (int relation : miniAMIE.kb.getRelations()) {

            if (miniAMIE.kb.relationSize(relation) >= miniAMIE.MinSup)
                relations.add(relation);
        }
        return relations;
    }

    private static boolean RuleDoesNotContainsRelation(Rule rule, int relation) {

        if (relation == rule.getHead()[RELATION_POSITION])
            return false;

        // Seeking unwanted duplicates
        for (int[] atom : rule.getBody()) {
            if (relation == atom[RELATION_POSITION])
                return false;
        }
        return true;
    }

    private static List<Integer> PromisingRelations(Rule rule) {
        List<Integer> relations = new ArrayList<>();

        for (int relation : miniAMIE.SelectedRelations) {
            if (RuleDoesNotContainsRelation(rule, relation))
                relations.add(relation);
        }
        return relations;
    }

    /**
     * AddClosureToEmptyBody adds a closure atom to an empty body rule, with respect to perfect path pattern
     * (i.e. Head subject is closure subject, Head object is closure object)
     *
     * @param rule to be closed
     * @return A set of possible closed rules.
     */
    public static ArrayList<Rule> AddClosureToEmptyBody(final Rule rule) {
        ArrayList<Rule> closedRules = new ArrayList<>();
        int[] headAtom = rule.getHead();

        List<Integer> relations = PromisingRelations(rule);

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            Rule closedRule = new Rule(rule, -1, miniAMIE.kb);

            int[] newAtom = new int[ATOM_SIZE];
            closedRule.getBody().add(newAtom);

            newAtom[SUBJECT_POSITION] = headAtom[SUBJECT_POSITION];
            newAtom[RELATION_POSITION] = relation;
            newAtom[OBJECT_POSITION] = headAtom[OBJECT_POSITION];
            closedRules.add(closedRule);
        }

        return closedRules;
    }

    /**
     * AddClosure adds a closure atom to a non-empty body rule, with respect to perfect path pattern
     * (i.e. Head subject is closure subject, dangling's subject is closure object)
     *
     * @param rule to be closed
     * @return A set of possible closed rules.
     */
    public static ArrayList<Rule> AddClosure(final Rule rule) {
        ArrayList<Rule> closedRules = new ArrayList<>();
        int[] headAtom = rule.getHead();
        int[] lastBodyAtom = rule.getLastTriplePattern();

        List<Integer> relations = PromisingRelations(rule);

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            Rule closedRule = new Rule(rule, -1, miniAMIE.kb);

            int[] newAtom = new int[ATOM_SIZE];
            closedRule.getBody().add(newAtom);

            newAtom[SUBJECT_POSITION] = headAtom[SUBJECT_POSITION];
            newAtom[RELATION_POSITION] = relation;
            newAtom[OBJECT_POSITION] = lastBodyAtom[SUBJECT_POSITION];
            closedRules.add(closedRule);
        }

        return closedRules;
    }


    /**
     * AddDanglingToEmptyBody adds a dangling atom to an empty body rule, with respect to the perfect path pattern
     * (i.e. Only open variable is dangling's subject, Head object is dangling's object )
     *
     * @param rule parent
     * @return A set of possible open rules
     */
    public static ArrayList<Rule> AddDanglingToEmptyBody(Rule rule) {
        ArrayList<Rule> openRules = new ArrayList<>();
        int[] headAtom = rule.getHead();

        List<Integer> relations = PromisingRelations(rule);

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            Rule closedRule = new Rule(rule, -1, miniAMIE.kb);

            int[] newAtom = new int[ATOM_SIZE];
            closedRule.getBody().add(newAtom);

            newAtom[SUBJECT_POSITION] = closedRule.fullyUnboundTriplePattern()[SUBJECT_POSITION];
            newAtom[RELATION_POSITION] = relation;
            newAtom[OBJECT_POSITION] = headAtom[OBJECT_POSITION];
            openRules.add(closedRule);
        }

        return openRules;
    }

    /**
     * AddDangling adds a dangling atom to an empty body rule, with respect to the perfect path pattern
     * (i.e. Only open variable is dangling's subject, Head object is dangling's object )
     *
     * @param rule parent
     * @return A set of possible open rules
     */
    public static ArrayList<Rule> AddDangling(Rule rule) {
        ArrayList<Rule> openRules = new ArrayList<>();
        int[] lastBodyAtom = rule.getLastTriplePattern();

        List<Integer> closureRelations = PromisingRelations(rule);

        if (closureRelations.isEmpty()) {
            return null;
        }

        for (int relation : closureRelations) {
            Rule closedRule = new Rule(rule, -1, miniAMIE.kb);

            int[] newAtom = new int[ATOM_SIZE];
            closedRule.getBody().add(newAtom);

            newAtom[SUBJECT_POSITION] = closedRule.fullyUnboundTriplePattern()[SUBJECT_POSITION];
            newAtom[RELATION_POSITION] = relation;
            newAtom[OBJECT_POSITION] = lastBodyAtom[SUBJECT_POSITION];
            openRules.add(closedRule);
        }

        return openRules;
    }


    private static int range(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> factSet = kb.relation2object2subject.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    private static int domain(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> factSet = kb.relation2subject2object.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    private static int overlap(int r1, int r2,
                               Int2ObjectMap<Int2IntMap> overlapTable,
                               Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1,
                               Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2) {
        Int2IntMap factSet = overlapTable.get(r1);
        int overlap;
        if (factSet == null) {
            IntSet r1_set = triplesKeySet1.get(r1).keySet();
            IntSet r2_set = triplesKeySet2.get(r2).keySet();
            overlap = KB.computeOverlap(r1_set, r2_set);
//            List<Integer> overlapValues = KB.computeOverlapValues(r1_set, r2_set);
//            System.out.print("Overlap: ") ;
//            for (int value : overlapValues) {
//                System.out.print(" " + miniAMIE.kb.unmap(value));
//            }
//            System.out.println(" ") ;
//
//            System.out.println("Adding " + miniAMIE.kb.unmap(r1) + " " + miniAMIE.kb.unmap(r2) + " overlap "+overlap);
            Int2IntMap overlaps1 = new Int2IntOpenHashMap();
            overlaps1.put(r2, overlap);
            overlapTable.put(r1, overlaps1);

            Int2IntMap overlaps2 = new Int2IntOpenHashMap();
            overlaps2.put(r1, overlap);
            overlapTable.put(r2, overlaps2);

        } else {
            overlap = factSet.getOrDefault(r2, NO_OVERLAP_VALUE);
//            System.out.println("factSet " + factSet + " r1 " + r1 + " overlap " + overlap) ;
            if (overlap == NO_OVERLAP_VALUE) {
                IntSet r1_set = triplesKeySet1.get(r1).keySet();
                IntSet r2_set = triplesKeySet2.get(r2).keySet();
//                System.out.println(" r1_sets " + r1_set + " r2_sets " + r2_set);
                overlap = KB.computeOverlap(r1_set, r2_set);

//                List<Integer> overlapValues = KB.computeOverlapValues(r1_set, r2_set);
//                System.out.print("Overlap: ") ;
//                for (int value : overlapValues) {
//                    System.out.print(" " + miniAMIE.kb.unmap(value));
//                }
//                System.out.println(" ") ;

//                System.out.println("Adding "+ r1 + " = "+ miniAMIE.kb.unmap(r1) +
//                        " " + r2 + " = "+ miniAMIE.kb.unmap(r2) + " overlap "+ overlap);
                factSet.put(r1, overlap);
                factSet.put(r2, overlap);
//            } else {
//                System.out.println("Found " + r1 + " = "+ miniAMIE.kb.unmap(r1) +
//                        " " + r2 + " = "+ miniAMIE.kb.unmap(r2) + " overlap "+ overlap);
            }
        }
//        IntSet r1_set = triplesKeySet1.get(r1).keySet();
//        IntSet r2_set = triplesKeySet2.get(r2).keySet();
//        int real_overlap = KB.computeOverlap(r1_set, r2_set);
//        System.out.println(overlap + " real " + real_overlap);
        return overlap;
    }

    private static int objectToObjectOverlap(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.object2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2object2subject;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlap(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    private static int subjectToObjectOverlap(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlap(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }


    private static int subjectToSubjectOverlap(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2subjectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2subject2object;
        int overlap = overlap(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    private static List<int[]> sortPerfectPathBody(Rule rule) {
        int bodySize = rule.getBody().size();
        rule.setBodySize(rule.getBody().size());
        if(bodySize < 2 )
            return rule.getBody() ;

        List<int[]> body = rule.getBody();
        List<int[]> sortedBody = new ArrayList<>();
        int[] head = rule.getHead();
        int var = head[SUBJECT_POSITION];
        for (int i = 0; i < bodySize + 1; i++) {
//            System.err.println("Looking for atom with object " + miniAMIE.kb.unmap(var)) ;
            for (int[] atom : body)
                if (atom[SUBJECT_POSITION] == var) {
//                    System.err.println("Found " + miniAMIE.kb.unmap(atom[RELATION_POSITION])) ;
                    sortedBody.add(atom);
                    var = atom[OBJECT_POSITION];
                    break;
                }
        }
        return sortedBody;
    }

    private static void printBodyAsPerfectPath(List<int[]> body) {
        for (int[] triple : body) {
            System.out.print(
                    miniAMIE.kb.unmap(triple[SUBJECT_POSITION]) + " " +
                            miniAMIE.kb.unmap(triple[RELATION_POSITION]) + " " +
                            miniAMIE.kb.unmap(triple[OBJECT_POSITION]) + " "
            );
        }
    }

    protected static void printRuleAsPerfectPath(Rule rule) {
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = sortPerfectPathBody(rule);
        printBodyAsPerfectPath(body);


        int[] head = rule.getHead();
        System.out.print("=> ");
        System.out.print(
                miniAMIE.kb.unmap(head[SUBJECT_POSITION]) + " " +
                        miniAMIE.kb.unmap(head[RELATION_POSITION]) + " " +
                        miniAMIE.kb.unmap(head[OBJECT_POSITION])
        );
//        System.out.print(new Rule(head, body, -1, miniAMIE.kb));
    }



    /**
     * bodyEstimate computes the total product operation for estimating support of a rule
     *
     * @param rule
     * @return total product operation iterating over the provided rule's body
     */
    private static double bodyEstimate(Rule rule) {
        double product = 1;
        rule.setBodySize(rule.getBody().size());
//        List<int[]> body = rule.getBody();
        List<int[]> body = sortPerfectPathBody(rule);
//        if (miniAMIE.Verbose) {
//            System.out.print("Body: ");
//            for (int k = 0 ; k < body.size() ; k++) {
//                int[] triple = body.get(k);
//                System.out.print("["+k+"] "+
//                        miniAMIE.kb.unmap(triple[SUBJECT_POSITION]) + " " +
//                                miniAMIE.kb.unmap(triple[RELATION_POSITION]) + " " +
//                                miniAMIE.kb.unmap(triple[OBJECT_POSITION])
//                );
//            }
//
//            System.out.print("\nBody estimate: (size: " + rule.getBodySize() + ") ");
//        }
        for (int id = 0; id < rule.getBodySize() - 1; id++) {

            int last_id = (int) rule.getBodySize() - 1 ;
            int r_id =  last_id - id ;
            int r_next_id = last_id - id - 1 ;
            int r = body.get(r_id)[RELATION_POSITION];
            int r_next = body.get(r_next_id)[RELATION_POSITION];
            // Computing SO Survival rate
            int rDom = domain(r);
            int r_nextRng = range(r_next);
            int r_nextSize = miniAMIE.kb.relationSize(r_next);

//            if(r == miniAMIE.kb.map("<created>") && r_next == miniAMIE.kb.map("<hasGender>"))
//                System.err.println(rule+ ": " +miniAMIE.kb.unmap(r) + " -> " + miniAMIE.kb.unmap(r_next));
            double soOV = subjectToObjectOverlap(r, r_next);
//            double denom = rDom * r_nextRng;
//            double nom = soOV * r_nextSize;

            double factor = 0 ;
//            if (denom > 0) {
//                factor = nom/denom;
//            } else {
//                factor = 0;
//            }

            /////// ALTERNATIVE METHOD
            double ifun_next = (double) r_nextRng / r_nextSize;
            double soSurv = soOV / rDom ;
            if (ifun_next > 0) {
                factor = soSurv / ifun_next;
            }

            product *= factor;

//            if (miniAMIE.Verbose)
//                System.out.print(
//                        " { r["+(r_id)+"]: " + miniAMIE.kb.unmap(r) + " to " + " r["+(r_next_id)+"] (r_next)" + miniAMIE.kb.unmap(r_next)
//                                + " } "
//                                + " factor: " + factor
//                                + " product : " + product
//                                + " | soSurv " + soSurv
//                                + " | rDom " + rDom
//                                + " | soOV " + soOV
//                                + " | r_nextRng " + r_nextRng
//                                + " | r_nextSize " + r_nextSize);

            if (product == 0)
                break ;
        }
//        if (miniAMIE.Verbose)
//            System.out.print("\n");
        return product;
    }



    /**
     * Support approximation for a closed rule
     *
     * @param rule Closed rule
     * @return Support approximation
     */
    public static long ApproximateSupportClosedRule(Rule rule) {
        String ruleStr = rule.toString();
        if (miniAMIE.Verbose)
            System.out.println(ANSI_GREEN
                    + "ApproximateSupportClosedRule " + ruleStr);
        int rHead = rule.getHeadRelationBS();
        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1 ;
        if(idFirst < 0)
            System.err.println(rule);
        int idLast = 0 ;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];
        int rLastBodyAtom = body.get(idLast)[RELATION_POSITION];


        int objectToObjectOverlap = objectToObjectOverlap(rHead, rFirstBodyAtom);
        int subjectToSubjectOverlap = subjectToSubjectOverlap(rLastBodyAtom, rHead);
        double rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rHeadSize = miniAMIE.kb.relationSize(rHead);


        int domainHead = domain(rHead);
        int domainLast = domain(rLastBodyAtom);
        int rangeFirst = range(rFirstBodyAtom);

        double bodyEstimate = bodyEstimate(rule);

        long result = 0;

        double nom = objectToObjectOverlap * subjectToSubjectOverlap * rFirstSize * rHeadSize;
        double denom = rangeFirst * domainHead * domainLast;

//        if (denom > 0) {
//            result = (long) (nom * bodyEstimate / denom);
//        }

        // ALTERNATIVE
        double inv_ifun_r1 =  rFirstSize / rangeFirst ;
        double inv_fun_rh = rHeadSize / domainHead ;
        double ssSurv = (double) subjectToSubjectOverlap / domainLast ;
        double factor1 = objectToObjectOverlap * inv_ifun_r1 ;
        double factor2 = ssSurv * inv_fun_rh ;
        double product = factor1 * factor2;
        result = (long) (product * bodyEstimate) ;

        if (miniAMIE.Verbose) {

            System.out.println(" s~ " + result
                    + " | bodyEstimate " + bodyEstimate
                    + " | rHead " + miniAMIE.kb.unmap(rHead)
                    + " | rLastBodyAtom " + miniAMIE.kb.unmap(rLastBodyAtom)
                    + " | objectToObjectOverlap " + objectToObjectOverlap
                    + " | subjectToSubjectOverlap " + subjectToSubjectOverlap
                    + " | rFirstSize " + rFirstSize
                    + " | rHeadSize " + rHeadSize
                    + " | rangeFirst " + rangeFirst
                    + " | domainHead " + domainHead
                    + " | domainLast " + domainLast
                    + " | inv_ifun_r1 " + inv_ifun_r1
                    + " | inv_fun_rh " + inv_fun_rh
                    + " | ssSurv " + ssSurv
                    + " | factor1 " + factor1
                    + " | factor2 " + factor2
                    + " | product " + product
            );
        }
        return result;
    }

    /**
     * Support approximation for an open rule
     *
     * @param rule Open rule
     * @return Support approximation
     */
    public static long ApproximateSupportOpenRule(Rule rule) {
        String ruleStr = rule.toString();
        int rHead = rule.getHeadRelationBS();

        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1 ;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];


        double bodyEstimate = bodyEstimate(rule);
        int objectToObjectOverlap = objectToObjectOverlap(rHead, rFirstBodyAtom);
        int rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rangeFirst = range(rFirstBodyAtom);
        double nom = objectToObjectOverlap * rFirstSize;
        double denom = rangeFirst ;
        double result = 0;
//        if (denom > 0)
//            result = (nom * bodyEstimate)/ denom;

        // ALTERNATIVE
        double inv_ifun_r1 =  rFirstSize / rangeFirst ;
        result = objectToObjectOverlap * inv_ifun_r1 ;

//        if (miniAMIE.Verbose) {
//            double realS = RealSupport(rule);
//            double errorRate = ErrorRate(realS, result);
//            double errorContrastRatio = ErrorContrastRatio(realS, result);
//            double errorLog = ErrorRateLog(realS, result);
//            System.out.println(ANSI_BLUE
//                    + "ApproximateSupportOpenRule " + ruleStr
//                    + " : s~ " + result
//                    + " | s " + realS
//                    + " | err (rate, contrast, log)  " + errorRate + " " + errorContrastRatio + " " + errorLog
//                    + " | ooOv " + objectToObjectOverlap
//                    + " | rangeHead " + rangeFirst
//                    + " | headSize " + rFirstSize
//                    + " | inv_ifun_r1 " + inv_ifun_r1
//                    + " | rHead " + miniAMIE.kb.unmap(rHead)
//                    + ANSI_RESET);
//        }

        return (long) result;
    }

    private static double bodyEstimate2(Rule rule) {
        double product = 1;
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = sortPerfectPathBody(rule);
        for (int id = 0; id < rule.getBodySize() - 1; id++) {

            int last_id = (int) rule.getBodySize() - 1 ;
            int r_id =  last_id - id ;
            int r_next_id = last_id - id - 1 ;
            int r = body.get(r_id)[RELATION_POSITION];
            int r_next = body.get(r_next_id)[RELATION_POSITION];
            // Computing SO Survival rate
            int rDom = domain(r);
            int r_nextRng = range(r_next);
            int r_nextSize = miniAMIE.kb.relationSize(r_next);

            double soOV = subjectToObjectOverlap(r, r_next);
            double denom = rDom * r_nextRng;
            double nom = soOV * r_nextSize;

            double factor = 0 ;
            if (denom > 0) {
                factor = nom/denom;
            }
            product *= factor;

            if (product == 0)
                break ;
        }
        if (miniAMIE.Verbose)
            System.out.print("\n");
        return product;
    }
    /**
     * Support approximation for a closed rule
     *
     * @param rule Closed rule
     * @return Support approximation
     */
    public static long ApproximateSupportClosedRule2(Rule rule) {
        String ruleStr = rule.toString();
        int rHead = rule.getHeadRelationBS();
        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1 ;
        int idLast = 0 ;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];
        int rLastBodyAtom = body.get(idLast)[RELATION_POSITION];


        int objectToObjectOverlap = objectToObjectOverlap(rHead, rFirstBodyAtom);
        int subjectToSubjectOverlap = subjectToSubjectOverlap(rLastBodyAtom, rHead);
        double rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rHeadSize = miniAMIE.kb.relationSize(rHead);


        int domainHead = domain(rHead);
        int domainLast = domain(rLastBodyAtom);
        int rangeFirst = range(rFirstBodyAtom);

        double bodyEstimate = bodyEstimate2(rule);

        long result = 0;

        double nom = objectToObjectOverlap * subjectToSubjectOverlap * rFirstSize * rHeadSize;
        double denom = rangeFirst * domainHead * domainLast;

        if (denom > 0) {
            result = (long) (nom * bodyEstimate / denom);
        }

        System.out.println("alt s~ " + result
                + " | bodyEstimate " + bodyEstimate
                + " | nom " + nom
                + " | denom " + denom
                + " | rHead " + miniAMIE.kb.unmap(rHead)
                + " | rLastBodyAtom " + miniAMIE.kb.unmap(rLastBodyAtom)
                + " | objectToObjectOverlap " + objectToObjectOverlap
                + " | subjectToSubjectOverlap " + subjectToSubjectOverlap
                + " | rFirstSize " + rFirstSize
                + " | rHeadSize " + rHeadSize
                + " | rangeFirst " + rangeFirst
                + " | domainHead " + domainHead
                + " | domainLast " + domainLast
        );

        return result;
    }

    public static long RealSupport(Rule rule) {
        return miniAMIE.kb.countProjection(rule.getHead(), rule.getTriples());
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    protected static double ErrorContrastRatio(double real, double estimate) {
        double delta = estimate - real;
        double total = estimate + real;
        return delta / total;
    }

    protected static double ErrorRate(double real, double estimate) {
        double delta = estimate - real;
        double total = real + 1;
        return delta / total;
    }

    protected static double ErrorRateLog(double real, double estimate) {
        double logReal = Math.log10(real + 1);
        double logEstimate = Math.log10(estimate + 1);
        return logReal - logEstimate;
    }

    public enum RuleStateComparison {
        /** Rule has been found by mini-Amie and is in ground truth rule set */
        CORRECT,
        /** Rule has been found by mini-Amie but is not in ground truth rule set */
        FALSE,
        /** Rule has not been found by mini-Amie and is in ground truth rule set
        * BUT should not be found by mini-Amie (ex: rule is not a perfect path, rule has redundant relations) */
        MISSING_OK,
        /** Rule has not been found by mini_Amie and is in ground truth rule set
        * BUT should have been found by mini-Amie */
        MISSING_FAILURE
    }

    public static boolean IsRealPerfectPath(Rule rule) {
        boolean found_x = false ;
        boolean found_y = false ;

        if (rule.containsSinglePath()) {
            for(int[] atom: rule.getBody()) {
                if(atom[SUBJECT_POSITION] == rule.getHead()[SUBJECT_POSITION])
                    found_x = true ;
                if(atom[OBJECT_POSITION] == rule.getHead()[OBJECT_POSITION])
                    found_y = true ;
            }
        }
        return found_x && found_y ;
    }

    public static boolean HasNoRedundancies(Rule rule) {
        HashSet<Integer> relations = new HashSet<>();
        relations.add(rule.getHead()[RELATION_POSITION]);
        // Redundancy check
        for(int[] atom: rule.getBody()) {
            int relation = atom[RELATION_POSITION];
            if(relations.contains(relation)) {
                return false ;
            }
            relations.add(relation);
        }
        return true ;
    }

    /**
     * ShouldHaveBeenFound will seek for a perfect path
     * @param rule
     * @return
     */
    public static boolean ShouldHaveBeenFound(Rule rule) {
        return HasNoRedundancies(rule) && IsRealPerfectPath(rule);
    }

    /**
     * CompareRules will return true if two rules are equivalent (considering atom positions and variable naming)
     * (ex: ?a  < worksAt >  ?c ?c  < isLocatedIn >  ?b => ?a  < isCitizenOf >  ?b
     *  and ?d  < isLocatedIn >  ?b ?a  < worksAt >  ?d => ?a  < isCitizenOf >  ?b are equivalent)
     * @param groundTruthRule
     * @param rule
     * @return
     */
    public static boolean CompareRules(Rule groundTruthRule, Rule rule) {
        int[] groundTruthRuleHead = groundTruthRule.getHead();
        int[] ruleHead = rule.getHead();

        if (ruleHead[RELATION_POSITION] != groundTruthRuleHead[RELATION_POSITION])
            return false;
        List<int[]> groundTruthRuleBody = groundTruthRule.getBody();
        List<int[]> ruleBody = rule.getBody();
        int groundBodySize = groundTruthRuleBody.size();
        int bodySize = ruleBody.size();
        if (bodySize != groundBodySize)
            return false;

        HashSet<Integer> groundBodyRelations = new HashSet<>();
        HashSet<Integer> bodyRelations = new HashSet<>();
        HashMap<Integer, HashSet<Integer>> objectToRelationsGround = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> subjectToRelationsGround = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> objectToRelations = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> subjectToRelations = new HashMap<>();

        for (int i = 0; i < bodySize; i++) {
            groundBodyRelations.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
            bodyRelations.add(ruleBody.get(i)[RELATION_POSITION]);
        }

        // Comparing relation lists
        if (!(groundBodyRelations.containsAll(bodyRelations)) ||
                !(bodyRelations.containsAll(groundBodyRelations)))
            return false;

        // Filling the variable-relation hashmaps
        for (int i = 0; i < bodySize; i++) {
            int groundObject = groundTruthRuleBody.get(i)[OBJECT_POSITION];
            int object = ruleBody.get(i)[OBJECT_POSITION];
            List<Integer> groundRelationsWithObject = new ArrayList<>();
            List<Integer> relationsWithObject = new ArrayList<>();
            for (int k = 0; k < bodySize; k++) {
                if (groundTruthRuleBody.get(i)[OBJECT_POSITION] == groundObject)
                    groundRelationsWithObject.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
                if (ruleBody.get(i)[OBJECT_POSITION] == object)
                    relationsWithObject.add(ruleBody.get(i)[RELATION_POSITION]);
            }

            int groundSubject = groundTruthRuleBody.get(i)[SUBJECT_POSITION];
            int subject = ruleBody.get(i)[SUBJECT_POSITION];
            List<Integer> groundRelationsWithSubject = new ArrayList<>();
            List<Integer> relationsWithSubject = new ArrayList<>();
            for (int k = 0; k < bodySize; k++) {
                if (groundTruthRuleBody.get(i)[SUBJECT_POSITION] == groundSubject)
                    groundRelationsWithSubject.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
                if (ruleBody.get(i)[SUBJECT_POSITION] == subject)
                    relationsWithSubject.add(ruleBody.get(i)[RELATION_POSITION]);
            }
        }

        // Checking head variable object
        int groundHeadObject = groundTruthRuleHead[OBJECT_POSITION];
        int headObject = ruleHead[OBJECT_POSITION];
        HashSet<Integer> headObjectRelationsGround = objectToRelationsGround.get(groundHeadObject);
        HashSet<Integer> headObjectRelations = objectToRelations.get(headObject);

        if ((headObjectRelations == null && headObjectRelationsGround != null)
                || (headObjectRelationsGround == null && headObjectRelations != null))
            return false;

        if (headObjectRelations != null &&
                (!headObjectRelations.containsAll(headObjectRelationsGround)
                        || !headObjectRelationsGround.containsAll(headObjectRelations)))
            return false;

        // Checking head variable subject
        int groundHeadSubject = groundTruthRuleHead[SUBJECT_POSITION];
        int headSubject = ruleHead[SUBJECT_POSITION];
        HashSet<Integer> headSubjectRelationsGround = subjectToRelationsGround.get(groundHeadSubject);
        HashSet<Integer> headSubjectRelations = subjectToRelations.get(headSubject);

        if ((headSubjectRelations == null && headSubjectRelationsGround != null)
                || (headSubjectRelationsGround == null && headSubjectRelations != null))
            return false;


        if (headSubjectRelations != null &&
                (!headSubjectRelations.containsAll(headSubjectRelationsGround)
                        || !headSubjectRelations.containsAll(headSubjectRelations)))
            return false;


        // Comparing body
        for (int i = 0; i < bodySize; i++) {
            int groundObject = groundTruthRuleBody.get(i)[OBJECT_POSITION];
            HashSet<Integer> objectRelationsGround = objectToRelationsGround.get(groundObject);

            boolean objectFound = false;
            // Comparing object variable relation set
            for (int j = 0; j < bodySize; j++) {
                HashSet<Integer> objectRelations = objectToRelations.get(groundObject);

                if ( (objectRelations == null && objectRelationsGround == null) ||
                        (objectRelations.containsAll(objectRelationsGround) &&
                        bodyRelations.containsAll(groundBodyRelations))) {
                    objectFound = true;
                    break;
                }
            }
            if (!objectFound)
                return false;

            // Checking subject
            int groundSubject = groundTruthRuleBody.get(i)[SUBJECT_POSITION];
            HashSet<Integer> subjectRelationsGround = subjectToRelationsGround.get(groundSubject);

            boolean subjectFound = false;
            // Comparing subject variable relation set
            for (int j = 0; j < bodySize; j++) {
                HashSet<Integer> subjectRelations = subjectToRelations.get(groundSubject);

                if ((subjectRelations == null && subjectRelationsGround == null) ||
                        (subjectRelations.containsAll(subjectRelationsGround) &&
                        bodyRelations.containsAll(groundBodyRelations))) {
                    subjectFound = true;
                    break;
                }
            }
            if (!subjectFound)
                return false;
        }
        return true;
    }

    /**
     * GetInitRules provides a sample of rules with empty bodies and head coverage above provided minimum support.
     *
     * @param minSup
     * @return Collection of single atom rules
     */
    public static Collection<Rule> GetInitRules(double minSup) {
        return miningAssistant.getInitialAtoms(minSup);
    }


    public static List<Rule> LoadGroundTruthRules() {
        List<Rule> groundTruthRules = new ArrayList<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(miniAMIE.pathToGroundTruthRules));
            String line = reader.readLine();

            while (line != null) {
                List<int[]> bodyAtoms = new ArrayList<>();
                int[] headAtom;
//                System.out.println(line);

                String regexSpace = "[(\\ )|(\t)]+";
                String regexAtom = "(\\?[a-z]" + regexSpace + "<[a-zA-Z]+>" + regexSpace + "\\?[a-z]" + regexSpace + ")";
                String regexBody = "(" + regexAtom + "+)";
                String regexRule = "(" + regexBody + "=> [(\\ )|(\t)]*" + regexAtom + ")";
                Pattern pat = Pattern.compile(regexRule);
                Matcher matcher = pat.matcher(line);
                if (matcher.find()) {
                    String bodyString = matcher.group(2);
                    String[] bodyParts = bodyString.split(regexSpace);
                    for (int i = 0; i < bodyParts.length; i += 3) {
                        String subjectString = bodyParts[i];
                        String relationString = bodyParts[i + 1];
                        String objectString = bodyParts[i + 2];

                        int subject = miniAMIE.kb.map(subjectString);
                        int relation = miniAMIE.kb.map(relationString);
                        int object = miniAMIE.kb.map(objectString);

                        bodyAtoms.add(new int[]{subject, relation, object});
                    }
                    String headString = matcher.group(4);
                    String[] headParts = headString.split(regexSpace);
                    String subjectString = headParts[0];
                    String relationString = headParts[1];
                    String objectString = headParts[2];

                    if(!miniAMIE.RestrainedHead.isEmpty() &&
                            !Objects.equals(relationString, miniAMIE.RestrainedHead))
                        break ;
                    int subject = miniAMIE.kb.map(subjectString);
                    int relation = miniAMIE.kb.map(relationString);
                    int object = miniAMIE.kb.map(objectString);
                    headAtom = new int[]{subject, relation, object};
                    Rule groundTruthRule = new Rule(headAtom, bodyAtoms, -1, miniAMIE.kb);
                    groundTruthRules.add(groundTruthRule);
                    line = reader.readLine();
                } else {
                    System.out.println("Could not find ground truth rule in "+line);
                }

            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("groundTruthRules "+ groundTruthRules);
        return groundTruthRules;
    }
}
