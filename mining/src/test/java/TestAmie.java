import amie.data.KB;
import amie.data.Schema;
import amie.mining.AMIE;
import amie.rules.Rule;
import amie.data.javatools.administrative.Announce;
import junit.framework.TestCase;

import java.util.List;

public class TestAmie extends TestCase {
    List<Rule> outputRuleList;
    public void setUp() throws Exception{
        Schema.loadSchemaConf();
        System.out.println("Assuming " + KB.unmap(Schema.typeRelationBS) + " as type relation");
        long loadingStartTime = System.currentTimeMillis();
        String inputParams = "-mins\n" +
                "1\n" +
                "-minis\n" +
                "1\n" +
                "-minc\n" +
                "0.1\n" +
                "-maxad\n" +
                "10\n" +
                "-maxad\n" +
                "10\n" +
                "-rl\n" +
                "10\n" +
                "-noHeuristics\n" +
                "sample5.tsv";
        String[] args = inputParams.split("\n");

        AMIE miner = AMIE.getInstance(args);
        long loadingTime = System.currentTimeMillis() - loadingStartTime;

        System.out.println("MRT calls: " + String.valueOf(KB.STAT_NUMBER_OF_CALL_TO_MRT.get()));
        Announce.doing("Starting the mining phase");

        long time = System.currentTimeMillis();
        outputRuleList = miner.mine();
        long miningTime = System.currentTimeMillis() - time;
        System.out.println("Mining done in " + String.format("%d s", miningTime / 1000));
        Announce.done("Total time " + String.format("%d s", (miningTime + loadingTime) / 1000));
        System.out.println(outputRuleList.size() + " rules mined.");
    }
//    public List<Rule> initExpectRules() {
//        List<Rule> expectRuleList = new ArrayList<>();
//        Rule expectRule_0 =  new Rule(KB.triple("?a", "<https://w3id.org/biolink/vocab/interacts_with>", "?b"),
//                KB.triples(KB.triple("?a", "<https://w3id.org/biolink/vocab/interacts_with>", "?j"), KB.triple("?e", "<https://w3id.org/biolink/vocab/interacts_with>", "?j"), KB.triple("?e", "<https://w3id.org/biolink/vocab/interacts_with>", "?b")), 1);
//        Rule expectRule_1 =  new Rule(KB.triple("?a", "<https://w3id.org/biolink/vocab/interacts_with>", "?b"),
//                KB.triples(KB.triple("?q", "<https://w3id.org/biolink/vocab/interacts_with>", "?n"), KB.triple("?i", "<https://w3id.org/biolink/vocab/interacts_with>", "?n"), KB.triple("?i", "<https://w3id.org/biolink/vocab/interacts_with>", "?f"), KB.triple("?a", "<https://w3id.org/biolink/vocab/interacts_with>", "?f"), KB.triple("?q", "<https://w3id.org/biolink/vocab/interacts_with>", "?b")), 1);
//        Rule expectRule_2 =  new Rule(KB.triple("?a", "<https://w3id.org/biolink/vocab/interacts_with>", "?b"),
//                KB.triples(KB.triple("?y", "<https://w3id.org/biolink/vocab/interacts_with>", "?v"), KB.triple("?q", "<https://w3id.org/biolink/vocab/interacts_with>", "?v"), KB.triple("?q", "<https://w3id.org/biolink/vocab/interacts_with>", "?n"), KB.triple("?i", "<https://w3id.org/biolink/vocab/interacts_with>", "?n"), KB.triple("?i", "<https://w3id.org/biolink/vocab/interacts_with>", "?f"), KB.triple("?a", "<https://w3id.org/biolink/vocab/interacts_with>", "?f"), KB.triple("?y", "<https://w3id.org/biolink/vocab/interacts_with>", "?b")), 1);
//        Rule expectRule_3 =  new Rule(KB.triple("?a", "<https://w3id.org/biolink/vocab/interacts_with>", "?b"),
//                KB.triples(KB.triple("?y", "<https://w3id.org/biolink/vocab/interacts_with>", "?v"), KB.triple("?q", "<https://w3id.org/biolink/vocab/interacts_with>", "?v"), KB.triple("?q", "<https://w3id.org/biolink/vocab/interacts_with>", "?n"), KB.triple("?i", "<https://w3id.org/biolink/vocab/interacts_with>", "?n"), KB.triple("?i", "<https://w3id.org/biolink/vocab/interacts_with>", "?f"), KB.triple("?a", "<https://w3id.org/biolink/vocab/interacts_with>", "?f"), KB.triple("?a6", "<https://w3id.org/biolink/vocab/interacts_with>", "?b"), KB.triple("?y", "<https://w3id.org/biolink/vocab/interacts_with>", "?a3"), KB.triple("?a6", "<https://w3id.org/biolink/vocab/interacts_with>", "?a3")), 1);
//        expectRuleList.add(expectRule_0);
//        expectRuleList.add(expectRule_1);
//        expectRuleList.add(expectRule_2);
//        expectRuleList.add(expectRule_3);
//        return  expectRuleList;
//    }
    public void test() {
        int expectRuleListSize = 100000;
        assertTrue(expectRuleListSize == outputRuleList.size());
    }
}
