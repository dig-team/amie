package amie.mining.miniAmie;

import amie.rules.QueryEquivalenceChecker;
import amie.rules.Rule;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.*;

public class CompareToGT {


    protected static final String ANSI_WHITE = "\u001B[37m";
    protected static final String ANSI_CYAN = "\u001B[36m";
    protected static final String ANSI_PURPLE = "\u001B[35m";
    protected static final String ANSI_BLUE = "\u001B[34m";
    protected static final String ANSI_YELLOW = "\u001B[33m";
    protected static final String ANSI_GREEN = "\u001B[32m";
    protected static final String ANSI_RED = "\u001B[31m";
    protected static final String ANSI_BLACK = "\u001B[30m";
    protected static final String ANSI_RESET = "\u001B[0m";
    protected static String bodySep = ";" ;
    protected static String sep = ",";

    // todo test this
    public static boolean IsRealPerfectPath(Rule rule) {

        boolean found_x = false ;
        boolean found_y = false ;

        // Completes containsSinglePath method by checking that x is always subject and y is always object
        if (rule.containsSinglePath()) {
            for(int[] atom: rule.getBody()) {
                if(atom[utils.SUBJECT_POSITION] == rule.getHead()[utils.SUBJECT_POSITION])
                    found_x = true ;
                if(atom[utils.OBJECT_POSITION] == rule.getHead()[utils.OBJECT_POSITION])
                    found_y = true ;
            }
        }
        return found_x && found_y ;
    }

    protected static boolean HasNoRedundancies(Rule rule) {
        HashSet<Integer> relations = new HashSet<>();
        relations.add(rule.getHead()[utils.RELATION_POSITION]);
        // Redundancy check
        for(int[] atom: rule.getBody()) {
            int relation = atom[utils.RELATION_POSITION];
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
        return QueryEquivalenceChecker.areEquivalent(groundTruthRule.getTriples(), rule.getTriples());
        //        // Comparing head relations
//        int[] groundTruthRuleHead = groundTruthRule.getHead();
//        int[] ruleHead = rule.getHead();
//        if (ruleHead[RELATION_POSITION] != groundTruthRuleHead[RELATION_POSITION])
//            return false;
//
//        // ---
//        List<int[]> groundTruthRuleBody = groundTruthRule.getBody();
//        List<int[]> ruleBody = rule.getBody();
//        int groundBodySize = groundTruthRuleBody.size();
//        int bodySize = ruleBody.size();
//
//        // Comparing body sizes
//        if (bodySize != groundBodySize)
//            return false;
//
//        // Absent body, equal body size
//        if (bodySize == 0)
//            return true;
//
//
//        Set<Integer> groundRelations = new HashSet<>();
//        groundRelations.add(groundTruthRuleHead[RELATION_POSITION]);
//        Set<Integer> relations = new HashSet<>();
//        relations.add(ruleHead[RELATION_POSITION]);
//
//        HashMap<Integer, List<HashSet<Integer>>> objectToRelationsGround = new HashMap<>();
//        HashMap<Integer, List<HashSet<Integer>>> subjectToRelationsGround = new HashMap<>();
//        HashMap<Integer, List<HashSet<Integer>>> objectToRelations = new HashMap<>();
//        HashMap<Integer, List<HashSet<Integer>>> subjectToRelations = new HashMap<>();
//
//        // Filling relation maps
//        for (int i = 0; i < bodySize; i++) {
//            int[] groundBodyAtom = groundTruthRuleBody.get(i);
//            int[] bodyAtom = ruleBody.get(i);
//
////            groundRelations.add(groundBodyAtom[RELATION_POSITION]);
////            relations.add(bodyAtom[RELATION_POSITION]);
//            if(groundRelations.contains(groundBodyAtom[RELATION_POSITION])) {
//                groundRelations.add(groundBodyAtom[RELATION_POSITION]);
//
//            } else {
//
//            }
//
//            relations.add(bodyAtom[RELATION_POSITION]);
//
//            HashSet<Integer> objectRelationGroundSet ;
//            if (objectToRelationsGround.containsKey(groundBodyAtom[OBJECT_POSITION])) {
//                objectRelationGroundSet = objectToRelationsGround.get(groundBodyAtom[OBJECT_POSITION]) ;
//            } else {
//                objectRelationGroundSet = new HashSet<>();
//                objectToRelationsGround.put(groundBodyAtom[OBJECT_POSITION], objectRelationGroundSet);
//            }
//            objectRelationGroundSet.add(groundBodyAtom[RELATION_POSITION]);
//
//            HashSet<Integer> objectRelationSet ;
//            if (objectToRelations.containsKey(bodyAtom[OBJECT_POSITION])) {
//                objectRelationSet = objectToRelations.get(bodyAtom[OBJECT_POSITION]) ;
//            } else {
//                objectRelationSet = new HashSet<>();
//                objectToRelations.put(bodyAtom[OBJECT_POSITION], objectRelationSet);
//            }
//            objectRelationSet.add(bodyAtom[RELATION_POSITION]);
//
//            HashSet<Integer> subjectRelationGroundSet ;
//            if (subjectToRelationsGround.containsKey(groundBodyAtom[SUBJECT_POSITION])) {
//                subjectRelationGroundSet = subjectToRelationsGround.get(groundBodyAtom[OBJECT_POSITION]) ;
//            } else {
//                subjectRelationGroundSet = new HashSet<>();
//                subjectToRelationsGround.put(groundBodyAtom[SUBJECT_POSITION], subjectRelationGroundSet);
//            }
//            subjectRelationGroundSet.add(groundBodyAtom[RELATION_POSITION]);
//
//            HashSet<Integer> subjectRelationSet ;
//            if (subjectToRelations.containsKey(bodyAtom[SUBJECT_POSITION])) {
//                subjectRelationSet = subjectToRelations.get(bodyAtom[OBJECT_POSITION]) ;
//            } else {
//                subjectRelationSet = new HashSet<>();
//                subjectToRelations.put(bodyAtom[SUBJECT_POSITION], subjectRelationSet);
//            }
//            subjectRelationSet.add(bodyAtom[RELATION_POSITION]);
//
//        }
//
//        // Comparing relations
//        if (!(groundRelations.containsAll(relations)) ||
//                !(relations.containsAll(groundRelations)))
//            return false;
//
//        // Filling the variable-relation hashmaps
//        for (int i = 0; i < bodySize; i++) {
//            int groundObject = groundTruthRuleBody.get(i)[OBJECT_POSITION];
//            int object = ruleBody.get(i)[OBJECT_POSITION];
//            List<Integer> groundRelationsWithObject = new ArrayList<>();
//            List<Integer> relationsWithObject = new ArrayList<>();
//            for (int k = 0; k < bodySize; k++) {
//                if (groundTruthRuleBody.get(i)[OBJECT_POSITION] == groundObject)
//                    groundRelationsWithObject.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
//                if (ruleBody.get(i)[OBJECT_POSITION] == object)
//                    relationsWithObject.add(ruleBody.get(i)[RELATION_POSITION]);
//            }
//
//            int groundSubject = groundTruthRuleBody.get(i)[SUBJECT_POSITION];
//            int subject = ruleBody.get(i)[SUBJECT_POSITION];
//            List<Integer> groundRelationsWithSubject = new ArrayList<>();
//            List<Integer> relationsWithSubject = new ArrayList<>();
//            for (int k = 0; k < bodySize; k++) {
//                if (groundTruthRuleBody.get(i)[SUBJECT_POSITION] == groundSubject)
//                    groundRelationsWithSubject.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
//                if (ruleBody.get(i)[SUBJECT_POSITION] == subject)
//                    relationsWithSubject.add(ruleBody.get(i)[RELATION_POSITION]);
//            }
//        }
//
//        // Checking head variable object
//        int groundHeadObject = groundTruthRuleHead[OBJECT_POSITION];
//        int headObject = ruleHead[OBJECT_POSITION];
//        HashSet<Integer> headObjectRelationsGround = objectToRelationsGround.get(groundHeadObject);
//        HashSet<Integer> headObjectRelations = objectToRelations.get(headObject);
//
//        if ((headObjectRelations == null && headObjectRelationsGround != null)
//                || (headObjectRelationsGround == null && headObjectRelations != null))
//            return false;
//
//        if (headObjectRelations != null &&
//                (!headObjectRelations.containsAll(headObjectRelationsGround)
//                        || !headObjectRelationsGround.containsAll(headObjectRelations)))
//            return false;
//
//        // Checking head variable subject
//        int groundHeadSubject = groundTruthRuleHead[SUBJECT_POSITION];
//        int headSubject = ruleHead[SUBJECT_POSITION];
//        HashSet<Integer> headSubjectRelationsGround = subjectToRelationsGround.get(groundHeadSubject);
//        HashSet<Integer> headSubjectRelations = subjectToRelations.get(headSubject);
//
//        if ((headSubjectRelations == null && headSubjectRelationsGround != null)
//                || (headSubjectRelationsGround == null && headSubjectRelations != null))
//            return false;
//
//
//        if (headSubjectRelations != null &&
//                (!headSubjectRelations.containsAll(headSubjectRelationsGround)
//                        || !headSubjectRelations.containsAll(headSubjectRelations)))
//            return false;
//
//
//        // Comparing body
//        for (int i = 0; i < bodySize; i++) {
//            int groundObject = groundTruthRuleBody.get(i)[OBJECT_POSITION];
//            HashSet<Integer> objectRelationsGround = objectToRelationsGround.get(groundObject);
//
//            boolean objectFound = false;
//            // Comparing object variable relation set
//            for (int j = 0; j < bodySize; j++) {
//                HashSet<Integer> objectRelations = objectToRelations.get(groundObject);
//
//                if (
//                        (objectRelations == null && objectRelationsGround == null) ||
//                        (objectRelations != null &&
//                                (objectRelations.containsAll(objectRelationsGround) &&
//                        relations.containsAll(groundRelations)))
//                ) {
//                    objectFound = true;
//                    break;
//                }
//            }
//            if (!objectFound)
//                return false;
//
//            // Checking subject
//            int groundSubject = groundTruthRuleBody.get(i)[SUBJECT_POSITION];
//            HashSet<Integer> subjectRelationsGround = subjectToRelationsGround.get(groundSubject);
//
//            boolean subjectFound = false;
//            // Comparing subject variable relation set
//            for (int j = 0; j < bodySize; j++) {
//                HashSet<Integer> subjectRelations = subjectToRelations.get(groundSubject);
//
//                if ((subjectRelations == null && subjectRelationsGround == null) ||
//                (subjectRelations!=null &&
//                        (subjectRelations.containsAll(subjectRelationsGround) &&
//                                subjectRelationsGround.containsAll(subjectRelations)))
//                ) {
//                    subjectFound = true;
//                    break;
//                }
//            }
//            if (!subjectFound)
//                return false;
//        }
//        return true;>
    }

    protected static List<Rule> LoadGroundTruthRules() {
        List<Rule> groundTruthRules = new ArrayList<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(miniAMIE.pathToGroundTruthRules));
            String line = reader.readLine();
            String regexSpace = "[(\\ )|(\t)]+";
            String regexAtom = "(\\?[a-z]" + regexSpace + "<[^>]+>" + regexSpace + "\\?[a-z]" + regexSpace + ")";
            String regexBody = "(" + regexAtom + "+)";
            String regexRule = "(" + regexBody + "=> [(\\ )|(\t)]*" + regexAtom + ")";
            Pattern pat = Pattern.compile(regexRule);
            while (line != null) {
                List<int[]> bodyAtoms = new ArrayList<>();
                int[] headAtom;
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
                    String subjectString = headParts[utils.SUBJECT_POSITION];
                    String relationString = headParts[utils.RELATION_POSITION];
                    String objectString = headParts[utils.OBJECT_POSITION];

                    if(miniAMIE.RestrainedHead != null &&
                            !miniAMIE.RestrainedHead.isEmpty() &&
                            !Objects.equals(relationString, miniAMIE.RestrainedHead))
                        break ;
                    int subject = miniAMIE.kb.map(subjectString);
                    int relation = miniAMIE.kb.map(relationString);
                    int object = miniAMIE.kb.map(objectString);
                    headAtom = new int[]{subject, relation, object};
                    Rule groundTruthRule = new Rule(headAtom, bodyAtoms, -1, miniAMIE.kb);
                    groundTruthRules.add(groundTruthRule);
                } else {
                    System.err.println("Could not find ground truth rule in "+line);
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
//        System.out.println("groundTruthRules "+ groundTruthRules);
        return groundTruthRules;
    }

    protected static List<FactorsOfBody> GetFactorsOfBody(Rule rule) {
        List<FactorsOfBody> factorsOfBodies = new ArrayList<>();
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = utils.sortPerfectPathBody(rule);
        for (int id = 0; id < rule.getBodySize() - 1; id++) {
            int last_id = (int) rule.getBodySize() - 1 ;
            int r_id =  last_id - id ;
            int r_next_id = last_id - id - 1 ;
            int r = body.get(r_id)[utils.RELATION_POSITION];
            int r_next = body.get(r_next_id)[utils.RELATION_POSITION];
            // Computing SO Survival rate
            int rDom = utils.domainSize(r);
            int r_nextRng = utils.rangeSize(r_next);
            int r_nextSize = miniAMIE.kb.relationSize(r_next);
            double soOV = utils.subjectToObjectOverlapSize(r, r_next);
            FactorsOfBody factors = new FactorsOfBody(
                    miniAMIE.kb.unmap(r),
                    miniAMIE.kb.unmap(r_next),
                    rDom,
                    r_nextSize,
                    r_nextRng,
                    soOV) ;
            factorsOfBodies.add(factors) ;
        }
        return factorsOfBodies;
    }

    protected static FactorsOfApproximateSupportClosedRule GetFactorsOfApproximateSupportClosedRule(Rule rule) {
        int rHead = rule.getHeadRelationBS();
        List<int[]> body = utils.sortPerfectPathBody(rule);
        rule.setBodySize(body.size());
        int bodySize = body.size();
        int idFirst = bodySize - 1 ;
        if(idFirst < 0)
            System.err.println(rule);
        int idLast = 0 ;
        int rFirstBodyAtom = body.get(idFirst)[utils.RELATION_POSITION];
        int rLastBodyAtom = body.get(idLast)[utils.RELATION_POSITION];


        int objectToObjectOverlap = utils.objectToObjectOverlapSize(rHead, rFirstBodyAtom);
        int subjectToSubjectOverlap = utils.subjectToSubjectOverlapSize(rLastBodyAtom, rHead);
        double rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rHeadSize = miniAMIE.kb.relationSize(rHead);


        int domainHead = utils.domainSize(rHead);
        int domainLast = utils.domainSize(rLastBodyAtom);
        int rangeFirst = utils.rangeSize(rFirstBodyAtom);

        double bodyEstimate = utils.bodyEstimate(rule);
        double ifun_r1 =  rangeFirst / rFirstSize  ;
        double fun_rh =  domainHead / rHeadSize;

        return new FactorsOfApproximateSupportClosedRule(
                miniAMIE.kb.unmap(rHead),
                miniAMIE.kb.unmap(rFirstBodyAtom),
                miniAMIE.kb.unmap(rLastBodyAtom),
                objectToObjectOverlap,
                subjectToSubjectOverlap,
                rFirstSize,
                rHeadSize,
                rangeFirst,
                domainHead,
                domainLast,
                ifun_r1,
                fun_rh,
                bodyEstimate,
                GetFactorsOfBody(rule)
        ) ;
    }

    protected enum RuleStateComparison {
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

    protected static class FactorsOfBody {
        String atomRelation = "atomRelation: ";
        String nextAtomRelation = "nextAtomRelation: " ;
        String atomRelationDomain = "atomRelationDomain: -1" ;
        String nextAtomRelationSize = "nextAtomRelationSize: -1" ;
        String nextAtomRelationIfun = "nextAtomRelationIfun: -1" ;
        String atomRelationSubjectToNextAtomRelationObjectOverlap =
                "atomRelationSubjectToNextAtomRelationObjectOverlap: -1" ;

        public FactorsOfBody(String atomRelation, String nextAtomRelation, double atomRelationDomain,
                             double nextAtomRelationSize, double nextAtomRelationIfun,
                             double atomRelationSubjectToNextAtomRelationObjectOverlap) {
            this.atomRelation = "atomRelation: "+atomRelation;
            this.nextAtomRelation = "nextAtomRelation: "+nextAtomRelation;
            this.atomRelationDomain = "atomRelationDomain: "+atomRelationDomain ;
            this.nextAtomRelationSize = "nextAtomRelationSize: "+nextAtomRelationSize;
            this.nextAtomRelationIfun = "nextAtomRelationIfun: "+nextAtomRelationIfun;
            this.atomRelationSubjectToNextAtomRelationObjectOverlap =
                    "atomRelationSubjectToNextAtomRelationObjectOverlap: "
                            +atomRelationSubjectToNextAtomRelationObjectOverlap;
        }
    }

    protected static class FactorsOfApproximateSupportClosedRule {
        String relationHeadAtom = "" ;
        String relationFirstBodyAtom = "";
        String relationLastBodyAtom = "";
        double headAtomObjectToFirstBodyAtomObjectOverlap = -1 ;
        double lastBodyAtomSubjectToHeadAtomSubjectOverlap = -1 ;
        double relationFirstBodyAtomSize = -1 ;
        double relationHeadSize = -1 ;
        double rangeFirstBodyAtom = -1 ;
        double domainHeadAtom = -1 ;
        double domainLastBodyAtom = -1 ;
        double ifunRelationFirstBodyAtom = -1 ;
        double funRelationHeadAtom = -1 ;
        double bodyEstimate = -1 ;
        List<FactorsOfBody> factorsOfBodies = new ArrayList<>();

        public FactorsOfApproximateSupportClosedRule() {
        }



        public FactorsOfApproximateSupportClosedRule(String relationHeadAtom, String relationFirstBodyAtom,
                                                     String relationLastBodyAtom,
                                                     double headAtomObjectToFirstBodyAtomObjectOverlap,
                                                     double lastBodyAtomSubjectToHeadAtomSubjectOverlap,
                                                     double relationFirstBodyAtomSize, double relationHeadSize,
                                                     double rangeFirstBodyAtom, double domainHeadAtom,
                                                     double domainLastBodyAtom, double ifunRelationFirstBodyAtom,
                                                     double funRelationHeadAtom, double bodyEstimate,
                                                     List<FactorsOfBody> factorsOfBodies) {
            this.relationHeadAtom = relationHeadAtom;
            this.relationFirstBodyAtom = relationFirstBodyAtom;
            this.relationLastBodyAtom = relationLastBodyAtom;
            this.headAtomObjectToFirstBodyAtomObjectOverlap = headAtomObjectToFirstBodyAtomObjectOverlap;
            this.lastBodyAtomSubjectToHeadAtomSubjectOverlap = lastBodyAtomSubjectToHeadAtomSubjectOverlap;
            this.relationFirstBodyAtomSize = relationFirstBodyAtomSize;
            this.relationHeadSize = relationHeadSize;
            this.rangeFirstBodyAtom = rangeFirstBodyAtom;
            this.domainHeadAtom = domainHeadAtom;
            this.domainLastBodyAtom = domainLastBodyAtom;
            this.ifunRelationFirstBodyAtom = ifunRelationFirstBodyAtom;
            this.funRelationHeadAtom = funRelationHeadAtom;
            this.bodyEstimate = bodyEstimate;
            this.factorsOfBodies = factorsOfBodies;
        }

        @Override
        public String toString() {
            String formatFactors =  "[" ;
            for(FactorsOfBody factors: factorsOfBodies) {
                formatFactors += "{"
                        + factors.atomRelationDomain + bodySep
                        + factors.nextAtomRelation + bodySep
                        + factors.atomRelationDomain + bodySep
                        + factors.nextAtomRelationSize + bodySep
                        + factors.atomRelationSubjectToNextAtomRelationObjectOverlap
                        + "} " ;
            }
            formatFactors += "]" ;
            return "" + relationHeadAtom + sep
                    + relationFirstBodyAtom + sep
                    + relationLastBodyAtom + sep
                    + headAtomObjectToFirstBodyAtomObjectOverlap + sep
                    + lastBodyAtomSubjectToHeadAtomSubjectOverlap + sep
                    + relationFirstBodyAtomSize + sep
                    + relationHeadSize + sep
                    + rangeFirstBodyAtom + sep
                    + domainHeadAtom + sep
                    + domainLastBodyAtom + sep
                    + ifunRelationFirstBodyAtom + sep
                    + funRelationHeadAtom + sep
                    + bodyEstimate + sep
                    + formatFactors ;
        }
    }

    static public void PrintComparisonCSV(List<Rule> finalRules, List<Rule> groundTruthRules) {
        // Generating comparison map
        ConcurrentHashMap<Rule, RuleStateComparison> comparisonMap = new ConcurrentHashMap<>();
        for (Rule rule : finalRules) {
            comparisonMap.put(rule, CompareToGT.RuleStateComparison.FALSE);
        }
        for (Rule groundTruthRule : groundTruthRules) {
            boolean found = false;
            for (Rule rule : finalRules) {
                if (CompareToGT.CompareRules(rule, groundTruthRule)) {
                    found = true;
                    comparisonMap.put(rule, CompareToGT.RuleStateComparison.CORRECT);
                    break;
                }
            }
            if (!found) {
                if (CompareToGT.ShouldHaveBeenFound(groundTruthRule))
                    comparisonMap.put(groundTruthRule, CompareToGT.RuleStateComparison.MISSING_FAILURE);
                else
                    comparisonMap.put(groundTruthRule, CompareToGT.RuleStateComparison.MISSING_OK);
            }
        }

        // Displaying comparison map
        System.out.println(" Comparison to ground truth: ");
        try {
            File outputComparisonCsvFile = new File(outputComparisonCsvPath);
            if (outputComparisonCsvFile.createNewFile()) {
                System.out.println("Created CSV comparison to ground rules output: " + outputComparisonCsvPath);
            } else {
                System.err.println("Could not create CSV output: " + outputComparisonCsvPath +
                        ". Maybe name already exists?");
            }

            FileWriter outputComparisonCsvWriter = new FileWriter(outputComparisonCsvPath);

            // TODO move this part to CompareToGT
            String csvColumnLine = String.format(
                    "rule" + CompareToGT.sep // RULE
                            + "headRelation" + CompareToGT.sep
                            + "size" + CompareToGT.sep
                            + "isFalse" + CompareToGT.sep // FALSE
                            + "isCorrect" + CompareToGT.sep // CORRECT
                            + "isMissingFailure" + CompareToGT.sep // MISSING_FAILURE
                            + "isMissingOK" + CompareToGT.sep // MISSING_OK
                            + "isPerfectPath" + CompareToGT.sep
                            + "hasRedundancies" + CompareToGT.sep
                            + "appSupport" + CompareToGT.sep // APP SUPPORT
                            + "realSupport" + CompareToGT.sep
                            + "appHeadCoverage" + CompareToGT.sep
                            + "realHeadCoverage" + CompareToGT.sep
                            + "appSupportNano" + CompareToGT.sep
                            + "realSupportNano" + CompareToGT.sep
                            + "relationHeadAtom" + CompareToGT.sep
                            + "relationFirstBodyAtom" + CompareToGT.sep
                            + "relationLastBodyAtom" + CompareToGT.sep
                            + "headAtomObjectToFirstBodyAtomObjectOverlap" + CompareToGT.sep
                            + "lastBodyAtomSubjectToHeadAtomSubjectOverlap" + CompareToGT.sep
                            + "relationFirstBodyAtomSize" + CompareToGT.sep
                            + "relationHeadSize" + CompareToGT.sep
                            + "rangeFirstBodyAtom" + CompareToGT.sep
                            + "domainHeadAtom" + CompareToGT.sep
                            + "domainLastBodyAtom" + CompareToGT.sep
                            + "ifunRelationFirstBodyAtom" + CompareToGT.sep
                            + "funRelationHeadAtom" + CompareToGT.sep
                            + "bodyEstimate" + CompareToGT.sep
                            + "bodyProductElements"
                            + "\n"
            );
            outputComparisonCsvWriter.write(csvColumnLine);
            System.out.print(csvColumnLine);

            // Computing real support using available cores
            Set<Rule> totalRules = comparisonMap.keySet();
            if (NThreads == 1) {
                for (Rule rule : totalRules)
                    rule.setSupport(RealSupport(rule));
            } else {
                System.out.println("Computing real support ...");
                CountDownLatch totalRulesLatch = new CountDownLatch(totalRules.size());
                for (Rule rule : totalRules) {
                    executor.submit(() -> {
                        rule.setSupport(RealSupport(rule));
                        rule.setHeadCoverage(RealHeadCoverage(rule));
                        totalRulesLatch.countDown();
                    });
                }
                totalRulesLatch.await();
            }



            for (Rule rule : totalRules) {
                String comparisonCharacter;
                CompareToGT.RuleStateComparison compRule = comparisonMap.get(rule);
                switch (compRule) {
                    case FALSE -> comparisonCharacter = CompareToGT.ANSI_YELLOW;
                    case CORRECT -> comparisonCharacter = CompareToGT.ANSI_GREEN;
                    case MISSING_FAILURE -> comparisonCharacter = CompareToGT.ANSI_RED;
                    case MISSING_OK -> comparisonCharacter = CompareToGT.ANSI_PURPLE;
                    default -> throw new RuntimeException("Unknown comparison rule " + rule);
                }

                // Printing to csv file
                long startReal = System.nanoTime();
                long realNano = System.nanoTime() - startReal;
                double appHC = -1 ;
                double app = -1;
                long appNano = -1;
                CompareToGT.FactorsOfApproximateSupportClosedRule factors = new CompareToGT.FactorsOfApproximateSupportClosedRule();
                if (CompareToGT.ShouldHaveBeenFound(rule)) {
                    long startApp = System.nanoTime();
                    appHC = ApproximateHeadCoverageClosedRule(rule) ;
                    app = ApproximateSupportClosedRule(rule);
                    appNano = System.nanoTime() - startApp;
                    factors = CompareToGT.GetFactorsOfApproximateSupportClosedRule(rule);
                }


                String csvLine = String.format(
                        rule + CompareToGT.sep + // RULE
                                kb.unmap(rule.getHead()[RELATION_POSITION]) + CompareToGT.sep // HEAD RELATION
                                + (rule.getBody().size() + 1) + CompareToGT.sep // RULE SIZE
                                + (compRule == CompareToGT.RuleStateComparison.FALSE ? 1 : 0) + CompareToGT.sep // FALSE
                                + (compRule == CompareToGT.RuleStateComparison.CORRECT ? 1 : 0) + CompareToGT.sep // CORRECT
                                + (compRule == CompareToGT.RuleStateComparison.MISSING_FAILURE ? 1 : 0) + CompareToGT.sep // MISSING_FAILURE
                                + (compRule == CompareToGT.RuleStateComparison.MISSING_OK ? 1 : 0) + CompareToGT.sep // MISSING_OK
                                + (CompareToGT.IsRealPerfectPath(rule) ? 1 : 0) + CompareToGT.sep
                                + (CompareToGT.HasNoRedundancies(rule) ? 0 : 1) + CompareToGT.sep
                                + app + CompareToGT.sep // APP SUPPORT
                                + rule.getSupport() + CompareToGT.sep
                                + appHC + CompareToGT.sep
                                + rule.getHeadCoverage() + CompareToGT.sep
                                + appNano + CompareToGT.sep
                                + realNano + CompareToGT.sep
                                + factors
                                + "\n"
                );
                outputComparisonCsvWriter.write(csvLine);
                // Printing comparison to console
                System.out.print(comparisonCharacter + csvLine + CompareToGT.ANSI_RESET);
            }


        } catch (Exception e) {
//                System.err.println("Couldn't create output file: "+ outputComparisonCsvPath+ ". Maybe file already exists.");
            e.printStackTrace();
        }


    }
}
