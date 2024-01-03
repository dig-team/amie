package amie.mining.assistant;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import amie.data.KB;
import amie.mining.assistant.experimental.DefaultMiningAssistantWithOrder;
import amie.mining.assistant.experimental.LazyIteratorMiningAssistant;
import amie.mining.assistant.experimental.LazyMiningAssistant;
import amie.mining.assistant.variableorder.VariableOrder;

/**
 * Class that implements the factory design pattern for instantiating
 * mining assistants, so that this task is not anymore implemented in 
 * the main routine
 */
public class MiningAssistantFactory {
	
	private static MiningAssistantFactory factory = null;

	private MiningAssistantFactory(){}

	public static MiningAssistantFactory getAssistantFactory(){
		if (factory == null){
			MiningAssistantFactory.factory = new MiningAssistantFactory();
		}
		return factory;
	}

	public MiningAssistant getAssistant(String bias, KB dataSource, Map<String, Object> args) throws InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		MiningAssistant mineAssistant = null;
		switch(bias){
			case "oneVar":
                mineAssistant = new MiningAssistant(dataSource);
                break;
            case "default":
                mineAssistant = new DefaultMiningAssistantWithOrder(dataSource, (VariableOrder)args.get("variableOrder"));
                break;
            case "signatured":
                mineAssistant = new RelationSignatureDefaultMiningAssistant(dataSource);
                break;
            case "lazy":
                mineAssistant = new LazyMiningAssistant(dataSource, (VariableOrder)args.get("variableOrder"));
                break;
            case "lazit":
                mineAssistant = new LazyIteratorMiningAssistant(dataSource, (VariableOrder)args.get("variableOrder"));
                break;
            default:
                // To support customized assistant classes
                // The assistant classes must inherit from amie.mining.assistant.MiningAssistant
                // and implement a constructor with the any of the following signatures.
                // ClassName(amie.data.KB), ClassName(amie.data.KB, String), ClassName(amie.data.KB, amie.data.KB)
                Class<?> assistantClass = null;
                try {
                    assistantClass = Class.forName(bias);
                } catch (Exception e) {
                    System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                    e.printStackTrace();
                    System.exit(1);
                }

                Constructor<?> constructor = null;
                try {
                    // Standard constructor
                    constructor = assistantClass.getConstructor(new Class[]{KB.class});
                    mineAssistant = (MiningAssistant) constructor.newInstance(dataSource);
                } catch (NoSuchMethodException e) {
                    try {
                        // Constructor with additional input
                        constructor = assistantClass.getConstructor(new Class[]{KB.class, String.class});
                        System.out.println((VariableOrder)args.get("ef"));
                        mineAssistant = (MiningAssistant) constructor.newInstance(dataSource, (VariableOrder)args.get("ef"));
                    } catch (NoSuchMethodException ep) {
                        // Constructor with schema KB
                        try {
                            constructor = assistantClass.getConstructor(new Class[]{KB.class, KB.class});
                            mineAssistant = (MiningAssistant) constructor.newInstance(dataSource, (KB)args.get("schemaSource"));
                        } catch (Exception e2p) {
                            e.printStackTrace();
                            System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                            e.printStackTrace();
                        }
                    }
                } catch (SecurityException e) {
                    System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                    e.printStackTrace();
                    System.exit(1);
                }
                if (mineAssistant instanceof DefaultMiningAssistantWithOrder) {
                    ((DefaultMiningAssistantWithOrder) mineAssistant).setVariableOrder((VariableOrder)args.get("variableOrder"));
                }

                break;
		}
		mineAssistant.setKbSchema((KB)args.get("schemaSource"));
		return mineAssistant;
	}
}
