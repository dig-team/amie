package amie.mining.assistant;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import amie.data.AbstractKB;
import amie.data.KB;
import amie.mining.utils.AMIEOptions;

/**
 * Class that implements the factory design pattern for instantiating
 * mining assistants, so that this task is not anymore implemented in
 * the main routine
 */
public class MiningAssistantFactory {

    private static MiningAssistantFactory factory = null;

    private MiningAssistantFactory() {
    }

    public static MiningAssistantFactory getAssistantFactory() {
        if (factory == null) {
            MiningAssistantFactory.factory = new MiningAssistantFactory();
        }
        return factory;
    }

    public MiningAssistant getAssistant(String bias, AbstractKB dataSource, AbstractKB schemaSource)
            throws InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        MiningAssistant mineAssistant = null;
        switch (bias) {
            case AMIEOptions.Bias.ONE_VAR:
                mineAssistant = new MiningAssistant(dataSource);
                break;
            case AMIEOptions.Bias.DEFAULT:
                mineAssistant = new DefaultMiningAssistantWithOrder(dataSource);
                break;
            case AMIEOptions.Bias.SIGNATURED:
                mineAssistant = new RelationSignatureDefaultMiningAssistant(dataSource);
                break;
            case AMIEOptions.Bias.LAZY:
                mineAssistant = new LazyMiningAssistant(dataSource);
                break;
            case AMIEOptions.Bias.LAZIT:
                mineAssistant = new LazyIteratorMiningAssistant(dataSource);
                break;
            default:
                // To support customized assistant classes
                // The assistant classes must inherit from amie.mining.assistant.MiningAssistant
                // and implement a constructor with the any of the following signatures.
                // ClassName(amie.data.AbstractKB),
                // ClassName(amie.data.AbstractKB, amie.data.AbstractKB)
                Class<?> assistantClass = null;
                try {
                    assistantClass = Class.forName(bias);
                } catch (Exception e) {
                    System.err.println(AMIEOptions.AMIE_PLUS_CMD_LINE_SYNTAX);
                    e.printStackTrace();
                    System.exit(1);
                }

                Constructor<?> constructor = null;
                try {
                    // Standard constructor
                    constructor = assistantClass.getConstructor(new Class[] { KB.class });
                    mineAssistant = (MiningAssistant) constructor.newInstance(dataSource);
                } catch (NoSuchMethodException e) {
                    try {
                        constructor = assistantClass.getConstructor(new Class[] { KB.class, KB.class });
                        mineAssistant = (MiningAssistant) constructor.newInstance(dataSource, schemaSource);
                    } catch (Exception e2p) {
                        e.printStackTrace();
                        System.err.println(AMIEOptions.AMIE_PLUS_CMD_LINE_SYNTAX);
                        e.printStackTrace();
                    }
                } catch (SecurityException e) {
                    System.err.println(AMIEOptions.AMIE_PLUS_CMD_LINE_SYNTAX);
                    e.printStackTrace();
                    System.exit(1);
                }
        }
        return mineAssistant;
    }
}
