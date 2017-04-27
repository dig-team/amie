package amie.typing.heuristics;

import java.util.List;

import amie.data.KB;

import javatools.datatypes.ByteString;

public class CommonAttributeOfClass extends TypingHeuristic {

	public CommonAttributeOfClass(KB kb) {
		super(kb);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(ByteString type, List<ByteString[]> clause,
			ByteString variable) {
		// TODO Auto-generated method stub
		return 0;
	}

}
