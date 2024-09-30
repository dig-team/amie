package amie.data;

import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.util.HashMap;
import java.util.Map;

public class StoreStatementToKB extends AbstractRDFHandler {

    private static int counter = 0;
    private KB kb;
    private static Map<String,String> prefixes=new HashMap<>();


    StoreStatementToKB(KB kb){
        this.kb = kb;
    }
    public void handleStatement(Statement st) {
        String subject = getFormattedValue(String.valueOf(st.getSubject()));
        String object = getFormattedValue(String.valueOf(st.getObject()));
        String predict = getFormattedValue(String.valueOf(st.getPredicate()));
        kb.add(subject, predict, object);
    }

    public String getFormattedValue(String value) {

        String prefix = "";
//        System.out.println(value);
        String tempValue = value;
        String literal = "";

        // For string objects/subjects
        if (value.contains("\"") && !value.contains("^^")){
            return value;
        }

        //For website objects/subjects
        if (value.endsWith("/") || value.length() - value.replace("/", "").length() == 2) {
            return value;
        }

        //For literal objects/subjects
        if (value.contains("^^")) {
            literal = value.substring(0,value.lastIndexOf("^")+1);
            tempValue = value.substring(value.lastIndexOf("^") + 1);
        }
        if (tempValue.contains("#") && !tempValue.endsWith("#")) {
            prefix = tempValue.substring(0, tempValue.lastIndexOf('#') + 1);
        } else if (value.contains("/")) {
            prefix = tempValue.substring(0, tempValue.lastIndexOf('/') + 1);
        }

        if (!prefix.isEmpty() && !prefixes.containsKey(prefix)) {
            int x = counter++;
            prefixes.put(prefix, "p" +x);
            kb.schema.prefixMap.put( "p" + x, prefix);
        }

        if(StringUtils.isBlank(literal)){
            return prefixes.get(prefix) + ":" + tempValue.substring(prefix.length());
        }
        return literal + prefixes.get(prefix) + ":" + tempValue.substring(prefix.length());
    }
}

