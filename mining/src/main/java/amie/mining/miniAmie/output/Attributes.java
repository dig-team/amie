package amie.mining.miniAmie.output;

import amie.mining.miniAmie.MiniAmieClosedRule;
import amie.mining.miniAmie.MiniAmieRule;
import amie.mining.miniAmie.utils;

import java.util.*;

import static amie.mining.miniAmie.utils.atomSep;
import static amie.mining.miniAmie.utils.bodySep;
import static amie.mining.miniAmie.utils.*;

public class Attributes {
    private HashMap<String, String> attributes = initHeader();

    // RULE ATTRIBUTES
    private static final String IS_ACYCLIC_INSTANTIATED = "isAcyclicInstantiated";

    // OPEN RULE
    private static final String HEAD_SIZE = "headAtom" ;
    private static final String HEAD_TO_BODY_SELECTIVITY = "headToBodySelectivity" ;
    private static final String BODY_SELECTIVITY = "bodySelectivity" ;
    private static final String BODY_SELECTIVITY_ATTRIBUTES = "bodySelectivityAttributes" ;

    // CLOSED RULE
    private static final String CLOSURE_FACTOR = "closureFactor" ;

    // BODY
    private static final String ATOM_NEXT_ID = "atomNextId" ;
    private static final String ATOM_NEXT = "atomNext" ;
    private static final String ATOM_PREV = "atomPrev" ;
    private static final String JOIN_VARIABLE_POSITION = "joinVariablePosition" ;
    private static final String JOIN_VARIABLE = "joinVariable" ;
    private static final String PREV_TO_NEXT_SELECTIVITY = "prevToNextSelectivity" ;
    
    private static final List<String> KEYS = initKeyList() ;

    private static List<String> initKeyList() {
        List<String> keys = new ArrayList<>();

        keys.add(IS_ACYCLIC_INSTANTIATED) ;

        // OPENING
        keys.add(HEAD_SIZE);
        keys.add(HEAD_TO_BODY_SELECTIVITY);
        keys.add(BODY_SELECTIVITY);
        keys.add(BODY_SELECTIVITY_ATTRIBUTES);

        // CLOSED RULE
        keys.add(CLOSURE_FACTOR);

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
        HashMap<String, String> atomToNextAttributes = new HashMap<>();
        atomToNextAttributes.put(ATOM_NEXT_ID, "");
        atomToNextAttributes.put(ATOM_NEXT, "");
        atomToNextAttributes.put(ATOM_PREV, "");
        atomToNextAttributes.put(JOIN_VARIABLE_POSITION, "");
        atomToNextAttributes.put(JOIN_VARIABLE, "");
        atomToNextAttributes.put(PREV_TO_NEXT_SELECTIVITY, "");

        return atomToNextAttributes ;
    }

    protected String bodyEstimateAttributes(MiniAmieRule rule) {
        int joinVariablePosition = rule.InitJoinVariablePosition();
        int[] atomPrev = rule.GetFirstSortedBodyAtom() ;

        String bodyAttributes = "" ;
        for (int atomNextId = 1 ; atomNextId <= rule.getBody().size() - 1 ; atomNextId++) {
            HashMap<String, String> atomToNextAttributes = initHBodyAttributes();
            atomToNextAttributes.put(ATOM_NEXT_ID, atomNextId+"");
            int[] atomNext = rule.GetSortedBodyAtom(atomNextId);
            atomToNextAttributes.put(ATOM_NEXT, atomToString(atomNext));
            atomToNextAttributes.put(ATOM_PREV, atomToString(atomPrev));

            int joinVariable = atomPrev[joinVariablePosition] ;
            atomToNextAttributes.put(JOIN_VARIABLE_POSITION, joinVariablePosition+"");
            atomToNextAttributes.put(JOIN_VARIABLE, joinVariable+"");

            double prevToNextSelectivity = MiniAmieRule.getSelectivity().selectivity(
                    atomNext,
                    atomPrev,
                    joinVariable
            ) ;
            atomToNextAttributes.put(PREV_TO_NEXT_SELECTIVITY, prevToNextSelectivity+"");


            joinVariablePosition =
                    utils.NextPosition(
                            utils.VariablePosition(
                                    atomNext,
                                    joinVariable
                            )
                    );
            atomPrev = atomNext;
            bodyAttributes += atomToNextAttributes.toString().replace(commaSep, bodySep) + bodySep;
        }
        return bodyAttributes;
    }

    private void openRuleAttributes(MiniAmieRule rule) {
        attributes.put(IS_ACYCLIC_INSTANTIATED, rule.isAcyclicInstantiated()+"") ;
        attributes.put(HEAD_SIZE, rule.HeadSize()+"");
        attributes.put(HEAD_TO_BODY_SELECTIVITY, rule.HeadToBodySelectivity()+"");
        attributes.put(BODY_SELECTIVITY, rule.BodySelectivity()+"");
        attributes.put(BODY_SELECTIVITY_ATTRIBUTES, bodyEstimateAttributes(rule)) ;

    }

    public Attributes(MiniAmieClosedRule rule) {
        openRuleAttributes(rule);
        attributes.put(CLOSURE_FACTOR, rule.ClosureFactor()+"");
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
