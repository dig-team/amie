package amie.mining.miniAmie.output;

import amie.data.javatools.datatypes.Pair;
import amie.rules.Rule;

import java.util.*;

import static amie.mining.miniAmie.utils.atomSep;
import static amie.mining.miniAmie.utils.bodySep;
import static amie.mining.miniAmie.miniAMIE.Kb;
import static amie.mining.miniAmie.utils.*;

public class Attributes {
    private HashMap<String, String> attributes = initHeader();

    // OPEN RULE
    private static final String HEAD_ATOM_KEY = "headAtom" ;
    private static final String HEAD_OBJECT_KEY = "headObject" ;
    private static final String SORTED_BODY_KEY = "sortedBody" ;
    private static final String SORTED_BODY_SIZE_KEY = "sortedBodySize" ;
    private static final String FIRST_BODY_ATOM_KEY = "firstBodyAtom" ;
    private static final String HEAD_OBJECT_POSITION_IN_FIRST_BODY_ATOM_KEY = "headObjectPositionInFirstBodyAtom" ;
    private static final String FIRST_BODY_RELATION_KEY = "firstBodyRelation" ;
    private static final String OPENING_OVERLAP_KEY = "openingOverlap" ;
    private static final String N_AVG_FIRST_KEY = "nAvgFirst" ;
    private static final String BODY_ESTIMATE_KEY = "bodyEstimate" ;
    private static final String OPEN_RULE_APPROXIMATION_KEY = "openRuleApproximation" ;
    private static final String HEAD_OBJECT_POSITION_IN_LAST_BODY_ATOM_KEY = "headObjectPositionInLastAtom" ;
    private static final String BODY_ATTRIBUTES_KEY = "bodyAttributes" ;

    // CLOSED RULE
    private static final String LAST_BODY_ATOM_KEY = "lastBodyAtom" ;
    private static final String LAST_VARIABLE_POSITION_KEY = "lastVariablePosition" ;
    private static final String CLOSING_SURVIVAL_RATE_KEY = "closingSurvivalRate" ;
    private static final String CLOSING_SURVIVAL_RATE_NOM_KEY = "closingSurvivalRateNom" ;
    private static final String CLOSING_SURVIVAL_RATE_DENOM_KEY = "closingSurvivalRateDenom" ;
    private static final String N_AVG_HEAD_KEY = "nAvgHead" ;
    private static final String APPROXIMATE_SUPPORT_CLOSED_KEY = "approximateSupportClosed" ;
    
    // BODY
    private static final String BODY_ID_KEY = "id" ;
    private static final String BODY_ATOM_ID_KEY = "atomId" ;
    private static final String BODY_ATOM_NEXT_ID_KEY = "atomNextId" ;
    private static final String BODY_ATOM_KEY = "atom" ;
    private static final String BODY_ATOM_NEXT_KEY = "atomNext" ;
    private static final String BODY_VARIABLE_OVERLAP_SIZE_KEY = "variableOverlapSize" ;
    private static final String BODY_VARIABLE_POSITION_KEY = "variablePosition" ;
    private static final String BODY_RELATION_KEY = "relation" ;
    private static final String BODY_RELATION_NEXT_KEY = "relationNext" ;
    private static final String BODY_VARIABLE_SET_SIZE_KEY = "variableSetSize" ;
    private static final String BODY_SURVIVAL_RATE_KEY = "survivalRate" ;
    private static final String BODY_N_AVG_KEY = "nAvg" ;
    private static final String BODY_FACTOR_KEY = "factor" ;
    private static final String BODY_PRODUCT_KEY = "product" ;

    private static final List<String> KEYS = initKeyList() ;

    private static List<String> initKeyList() {
        List<String> keys = new ArrayList<>();

        keys.add(HEAD_ATOM_KEY);
        keys.add(HEAD_OBJECT_KEY);
        keys.add(SORTED_BODY_KEY);
        keys.add(SORTED_BODY_SIZE_KEY);
        keys.add(FIRST_BODY_ATOM_KEY);
        keys.add(HEAD_OBJECT_POSITION_IN_FIRST_BODY_ATOM_KEY);
        keys.add(FIRST_BODY_RELATION_KEY);
        keys.add(OPENING_OVERLAP_KEY);
        keys.add(N_AVG_FIRST_KEY);
        keys.add(BODY_ESTIMATE_KEY);
        keys.add(OPEN_RULE_APPROXIMATION_KEY);
        keys.add(HEAD_OBJECT_POSITION_IN_LAST_BODY_ATOM_KEY);
        keys.add(BODY_ATTRIBUTES_KEY);

        // CLOSED RULE
        keys.add(LAST_BODY_ATOM_KEY);
        keys.add(LAST_VARIABLE_POSITION_KEY);
        keys.add(CLOSING_SURVIVAL_RATE_KEY);
        keys.add(CLOSING_SURVIVAL_RATE_NOM_KEY);
        keys.add(CLOSING_SURVIVAL_RATE_DENOM_KEY);
        keys.add(N_AVG_HEAD_KEY);
        keys.add(APPROXIMATE_SUPPORT_CLOSED_KEY);

        return keys;
    }
    public static String GetCSVHeader() {
        String header = KEYS.get(0);
        for(int id = 1 ; id < KEYS.size(); id++ ) {
            header += "," + KEYS.get(id);
        }
        return header;
    }
    public HashMap<String, String> initHeader() {
        HashMap<String, String> attributes = new HashMap<>();
        for (String key : KEYS) {
            attributes.put(key, "");
        }
        return attributes ;
    }

    private static String atomToString(int[] atom) {
        return atom[SUBJECT_POSITION] + atomSep + atom[RELATION_POSITION] +
                atomSep + atom[OBJECT_POSITION] ;
    }

    private static String bodyToString(List<int[]> body) {
        String bodyStr = "" ;
        for (int[] atom: body) {
            bodyStr += atomToString(atom) + bodySep;
        }
        return bodyStr;
    }

    public HashMap<String, String> initHBodyAttributes() {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put(BODY_ID_KEY, "");
        attributes.put(BODY_ATOM_ID_KEY, "");
        attributes.put(BODY_ATOM_NEXT_ID_KEY, "");
        attributes.put(BODY_ATOM_KEY, "");
        attributes.put(BODY_ATOM_NEXT_KEY, "");
        attributes.put(BODY_VARIABLE_OVERLAP_SIZE_KEY, "");
        attributes.put(BODY_VARIABLE_POSITION_KEY, "");
        attributes.put(BODY_RELATION_KEY, "");
        attributes.put(BODY_RELATION_NEXT_KEY, "");
        attributes.put(BODY_VARIABLE_SET_SIZE_KEY, "");
        attributes.put(BODY_SURVIVAL_RATE_KEY, "");
        attributes.put(BODY_N_AVG_KEY, "");
        attributes.put(BODY_FACTOR_KEY, "");
        attributes.put(BODY_PRODUCT_KEY, "");

        return attributes ;
    }

    protected void bodyEstimateAttributes(Rule rule, int initVariablePosition) {
        double product = 1;
        List<int[]> body = SortPerfectPathBody(rule);
        int last_id = body.size() - 1;
        int[] lastAtom = body.get(last_id) ;
        int headObject = rule.getHead()[OBJECT_POSITION];

        int headObjectPositionInLastAtom = VariablePosition(lastAtom, headObject) ;
        attributes.put(HEAD_OBJECT_POSITION_IN_LAST_BODY_ATOM_KEY, headObjectPositionInLastAtom +"") ;


        int variablePosition = nextPosition(headObjectPositionInLastAtom);
        List<HashMap<String, String>> bodyAttributesList = new ArrayList<>() ;
        for (int id = 0; id < body.size() - 1; id++) {
            HashMap<String, String> bodyAttributes = initHBodyAttributes() ;
            bodyAttributesList.add(bodyAttributes) ;

            int atom_id = body.size() - id - 1;
            bodyAttributes.put(BODY_ATOM_ID_KEY, atom_id + "") ;

            int atom_next_id = atom_id - 1 ;
            bodyAttributes.put(BODY_ATOM_NEXT_ID_KEY, atom_next_id + "") ;

            int[] atom = body.get(atom_id);
            bodyAttributes.put(BODY_ATOM_KEY, atomToString(atom)) ;

            int[] atom_next = body.get(atom_next_id);
            bodyAttributes.put(BODY_ATOM_NEXT_KEY, atomToString(atom_next)) ;


            Pair<Integer, Integer> variableOverlapSizeResult = null;
            try {
                variableOverlapSizeResult =
                        VariableOverlapSize(atom, atom_next, variablePosition);
            } catch (Exception e) {
                throw new IllegalArgumentException("body estimate fail id " + id + " rule "
                        + RawBodyHeadToString(body, rule.getHead())
                        + " lastAtom " + Arrays.toString(lastAtom)
                        + " headObject " + headObject
                        + " headObjectPositionInLastAtom " + headObjectPositionInLastAtom
                        + " variablePosition " + variablePosition
                        + " unsorted rule " + RawBodyHeadToString(rule.getBody(), rule.getHead()) +
                        " body size " + body.size() + " rule body size "+ rule.getBodySize(), e) ;
            }


            int variableOverlapSize = variableOverlapSizeResult.first ;
            bodyAttributes.put(BODY_VARIABLE_OVERLAP_SIZE_KEY, variableOverlapSize+"") ;


            variablePosition = variableOverlapSizeResult.second ;
            bodyAttributes.put(BODY_VARIABLE_POSITION_KEY, variablePosition+"") ;


            int r = atom[RELATION_POSITION];
            bodyAttributes.put(BODY_RELATION_KEY, r+"") ;


            int r_next = atom_next[RELATION_POSITION];
            bodyAttributes.put(BODY_RELATION_NEXT_KEY, r_next + "") ;


            int variableSetSize = VariableSetSize(variablePosition, r);
            bodyAttributes.put(BODY_VARIABLE_SET_SIZE_KEY, variableSetSize+"") ;


            double survRate = variableOverlapSize / variableSetSize;
            bodyAttributes.put(BODY_SURVIVAL_RATE_KEY, survRate+"") ;


            double nAvg = AverageParameterRatio(r_next, variablePosition) ;
            bodyAttributes.put(BODY_N_AVG_KEY, nAvg+"") ;



            double factor = survRate * nAvg ;
            bodyAttributes.put(BODY_FACTOR_KEY, factor+"") ;


            product *= factor;
            bodyAttributes.put(BODY_PRODUCT_KEY, product+"") ;

            if (product == 0)
                break;
        }
        String bodyAttributesListStr = "" ;
        for (HashMap<String, String> bodyAttributes : bodyAttributesList) {
            bodyAttributesListStr += bodyAttributes.toString().replace(commaSep, bodySep) ;
        }

        attributes.put(BODY_ATTRIBUTES_KEY, bodyAttributesListStr) ;

    }



    private void openRuleAttributes(Rule rule) {
        if (rule.getBody().isEmpty()) {
            return;
        }

        
        int[] headAtom = rule.getHead() ;
        attributes.put(HEAD_ATOM_KEY, atomToString(headAtom));

        
        int headObject = headAtom[OBJECT_POSITION] ;
        attributes.put(HEAD_OBJECT_KEY, headObject + "");

        
        List<int[]> sortedBody = SortPerfectPathBody(rule);
        attributes.put(SORTED_BODY_KEY, bodyToString(sortedBody));

        
        int bodySize = sortedBody.size();
        attributes.put(SORTED_BODY_SIZE_KEY, bodySize + "");

        int headRelation = headAtom[RELATION_POSITION] ;

        //// Opening
        
        int[] firstBodyAtom = sortedBody.get(bodySize - 1);
        attributes.put(FIRST_BODY_ATOM_KEY, atomToString(firstBodyAtom));

        
        int headObjectPositionInFirstBodyAtom ;

        try {
            headObjectPositionInFirstBodyAtom = VariablePosition(firstBodyAtom, headObject);
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to find init variable "+
                    headObject +" position in " + RawBodyHeadToString(sortedBody, headAtom) +
                    " firstBodyAtom " + Arrays.toString(firstBodyAtom), e) ;
        }
        attributes.put(HEAD_OBJECT_POSITION_IN_FIRST_BODY_ATOM_KEY, headObjectPositionInFirstBodyAtom+"") ;

        
        int firstBodyRelation = firstBodyAtom[RELATION_POSITION];
        attributes.put(FIRST_BODY_RELATION_KEY, firstBodyRelation + "");

        
        int openingOverlap;
        switch (headObjectPositionInFirstBodyAtom) {
            case SUBJECT_POSITION ->
                    openingOverlap = ObjectToObjectOverlapSize(headRelation, firstBodyRelation) ;
            case OBJECT_POSITION ->
                    openingOverlap = SubjectToObjectOverlapSize(firstBodyRelation, headRelation) ;
            default -> throw new IllegalArgumentException("Invalid position " + headObjectPositionInFirstBodyAtom
                    + " in first body atom " + Arrays.toString(firstBodyAtom) + " of rule " + rule);
        }
        attributes.put(OPENING_OVERLAP_KEY, openingOverlap + "");

        
        double nAvgFirst = AverageParameterRatio(firstBodyRelation, nextPosition(headObjectPositionInFirstBodyAtom)) ;
        attributes.put(N_AVG_FIRST_KEY, nAvgFirst + "");

        // -------------------------------------

        int initVariablePosition =  nextPosition(headObjectPositionInFirstBodyAtom) ;
        double bodyEstimate = BodyEstimate(rule, initVariablePosition);
        attributes.put(BODY_ESTIMATE_KEY, bodyEstimate + "");
        bodyEstimateAttributes(rule, initVariablePosition);


        long openRuleApproximation = (long) (openingOverlap * nAvgFirst * bodyEstimate);
        attributes.put(OPEN_RULE_APPROXIMATION_KEY, openRuleApproximation + "") ;

    }


    public Attributes(Rule rule) {
        long approximateSupportOpen = ApproximateSupportOpenRule(rule) ;
        openRuleAttributes(rule);

        int[] headAtom = rule.getHead() ;
        int headSubject = headAtom[SUBJECT_POSITION] ;
        List<int[]> body = SortPerfectPathBody(rule);
        // Closed rule factor (closing)

        ///// Closing
        int headRelation = headAtom[RELATION_POSITION] ;
        int idLast = 0;
        
        int[] lastBodyAtom =  body.get(idLast) ;
        attributes.put(LAST_BODY_ATOM_KEY, atomToString(lastBodyAtom));

        int lastBodyRelation = lastBodyAtom[RELATION_POSITION] ;

        
        int lastVariablePosition ;
        try {
            lastVariablePosition = VariablePosition(lastBodyAtom, headSubject) ;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to find last variable "+
                    Kb.unmap(headSubject) + " position in " + rule) ;
        }
        attributes.put(LAST_VARIABLE_POSITION_KEY, lastVariablePosition + "") ;

        
        double closingSurvivalRate ;
        int nom ;
        double denom ;

        switch (lastVariablePosition) {

            case SUBJECT_POSITION -> {
                nom = SubjectToObjectOverlapSize(lastBodyRelation, headRelation) ;
                denom = DomainSize(lastBodyRelation);
            }
            case OBJECT_POSITION -> {
                nom = SubjectToObjectOverlapSize(headRelation, lastBodyRelation) ;
                denom = RangeSize(lastBodyRelation);
            }
            default -> throw new IllegalArgumentException("Invalid position " + lastVariablePosition
                    + " in last body atom " + Arrays.toString(lastBodyAtom) + " of rule " + rule);

        }
        attributes.put(CLOSING_SURVIVAL_RATE_NOM_KEY, nom + "") ;
        attributes.put(CLOSING_SURVIVAL_RATE_DENOM_KEY, denom + "") ;
        if (denom > 0) {
            closingSurvivalRate = nom / denom;
        }
        else {
            closingSurvivalRate = 0;
        }
        attributes.put(CLOSING_SURVIVAL_RATE_KEY, closingSurvivalRate + "") ;

        
        double nAvgHead = AverageParameterRatio(headRelation, SUBJECT_POSITION) ;
        attributes.put(N_AVG_HEAD_KEY, nAvgHead + "") ;

        
        long approximateSupportClosed = (long) (approximateSupportOpen * closingSurvivalRate * nAvgHead);
        attributes.put(APPROXIMATE_SUPPORT_CLOSED_KEY, approximateSupportClosed + "") ;
    }

    @Override
    public String toString() {
        String csvLine = attributes.get(KEYS.get(0));
        for(int id = 1 ; id < KEYS.size(); id++ ) {
            csvLine += commaSep + attributes.get(KEYS.get(id));
        }
        return csvLine;
    }
}
