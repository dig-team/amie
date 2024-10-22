package amie.mining.miniAmie;

import amie.rules.Rule;

import java.util.ArrayList;
import java.util.List;

public class FactorsOfApproximateSupportClosedRule {
    String relationHeadAtom = "";
    String relationFirstBodyAtom = "";
    String relationLastBodyAtom = "";
    double headAtomObjectToFirstBodyAtomObjectOverlap = -1;
    double lastBodyAtomSubjectToHeadAtomSubjectOverlap = -1;
    double relationFirstBodyAtomSize = -1;
    double relationHeadSize = -1;
    double rangeFirstBodyAtom = -1;
    double domainHeadAtom = -1;
    double domainLastBodyAtom = -1;
    double ifunRelationFirstBodyAtom = -1;
    double funRelationHeadAtom = -1;
    double bodyEstimate = -1;
    List<strFactorsOfBody> factorsOfBodies = new ArrayList<>();

    public FactorsOfApproximateSupportClosedRule() {
    }

    public FactorsOfApproximateSupportClosedRule(Rule rule) {
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

        this.relationHeadAtom = miniAMIE.kb.unmap(rHead);
        this.relationFirstBodyAtom = miniAMIE.kb.unmap(rFirstBodyAtom);
        this.relationLastBodyAtom = miniAMIE.kb.unmap(rLastBodyAtom);
        this.headAtomObjectToFirstBodyAtomObjectOverlap = objectToObjectOverlap;
        this.lastBodyAtomSubjectToHeadAtomSubjectOverlap = subjectToSubjectOverlap;
        this.relationFirstBodyAtomSize = rFirstSize;
        this.relationHeadSize = rHeadSize;
        this.rangeFirstBodyAtom = rangeFirst;
        this.domainHeadAtom = domainHead;
        this.domainLastBodyAtom = domainLast;
        this.ifunRelationFirstBodyAtom = ifun_r1;
        this.funRelationHeadAtom = fun_rh;
        this.bodyEstimate = bodyEstimate;
        this.factorsOfBodies = GetFactorsOfBody(rule);
    }

    private static class strFactorsOfBody {
        String atomRelation = "atomRelation: ";
        String nextAtomRelation = "nextAtomRelation: " ;
        String atomRelationDomain = "atomRelationDomain: -1" ;
        String nextAtomRelationSize = "nextAtomRelationSize: -1" ;
        String nextAtomRelationIfun = "nextAtomRelationIfun: -1" ;
        String atomRelationSubjectToNextAtomRelationObjectOverlap =
                "atomRelationSubjectToNextAtomRelationObjectOverlap: -1" ;

        public strFactorsOfBody(String atomRelation, String nextAtomRelation, double atomRelationDomain,
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


    private static List<strFactorsOfBody> GetFactorsOfBody(Rule rule) {
        List<strFactorsOfBody> factorsOfBodies = new ArrayList<>();
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
            strFactorsOfBody factors = new strFactorsOfBody(
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

    @Override
    public String toString() {
        String formatFactors = "[";
        for (strFactorsOfBody factors : factorsOfBodies) {
            formatFactors += "{"
                    + factors.atomRelationDomain + CompareToGT.bodySep
                    + factors.nextAtomRelation + CompareToGT.bodySep
                    + factors.atomRelationDomain + CompareToGT.bodySep
                    + factors.nextAtomRelationSize + CompareToGT.bodySep
                    + factors.atomRelationSubjectToNextAtomRelationObjectOverlap
                    + "} ";
        }
        formatFactors += "]";
        return "" + relationHeadAtom + utils.commaSep
                + relationFirstBodyAtom + utils.commaSep
                + relationLastBodyAtom + utils.commaSep
                + headAtomObjectToFirstBodyAtomObjectOverlap + utils.commaSep
                + lastBodyAtomSubjectToHeadAtomSubjectOverlap + utils.commaSep
                + relationFirstBodyAtomSize + utils.commaSep
                + relationHeadSize + utils.commaSep
                + rangeFirstBodyAtom + utils.commaSep
                + domainHeadAtom + utils.commaSep
                + domainLastBodyAtom + utils.commaSep
                + ifunRelationFirstBodyAtom + utils.commaSep
                + funRelationHeadAtom + utils.commaSep
                + bodyEstimate + utils.commaSep
                + formatFactors;
    }
}
