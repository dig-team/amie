package amie.mining.assistant.variableorder;

public class VariableOrderFactory {

	public static VariableOrder getVariableOrder(String code) {
		VariableOrder variableOrder = null;
		switch (code) {
			case "app":
				variableOrder = new AppearanceOrder();
				break;
			case "fun":
				variableOrder = new FunctionalOrder();
				break;
			case "ifun":
				variableOrder = InverseOrder.of(new FunctionalOrder());
				break;
			default:
				throw new IllegalArgumentException("The argument for option -vo must be among \"app\", \"fun\" and \"ifun\".");
		}
		return variableOrder;
	}
	
}
