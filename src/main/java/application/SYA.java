package application;
/**
 * PREFACE:
 * ---------------------------------------------------------------------------------------
 * This file contains all the "calculation" side of the calculator app,
 * but is completely standalone.
 *
 * While the code is "correct", it could use major redesign for coherence,
 * extensibility and performance
 */

/**
 *  This class is a stack - and also contains functionalities for implementing
 *  the Shunting-yard algorithm through:
 *  	* The constructor: SYA(String infix)
 *  	* The method:      toNumber();
 */
public class SYA {
	// The head element of postfix linked list
	public Element head;

	// The size of the stack
	public int size = 0;

	// The original string, in infix notation
	public String inFix;

	// This is just an empty stack
	public SYA(){

	}

	/**
	 * Transforms an infix expression into Reverse Polish Notation.
	 *
	 * Note: "this" stack (from head) contains the elements of the infix
	 * expression in RPN. Another stack is created in the constructor for
	 * the operators only
	 *
	 * @param infix - infix expression
	 * @throws SYAException if an SYAException occurs
	 */
	public SYA(String infix) throws SYAException {
		System.out.println("===[Parsing Infix String]===");

		// Set the original string
		this.inFix = infix;

		// This represents the operator stack
		// The current SYA object is the output (RPN) stack
		SYA operatorStack = new SYA();

		// Check parenthesis match
		int totalPar = 0;

		// Preprocess the infix string
		infix = preprocess(infix);

		// Evaluate the current token and push a token into the stack
		for (int idxT = 0; idxT < infix.length(); idxT++) {
			// Some tokens may be larger than a single character i.e. numbers
			String out = Character.toString(infix.charAt(idxT));

			// Expand the token if applicable
			if (out.matches("[\\d.]")) {
				// Match: number, 0.1 .1 10.0
				// Conflicts with the binary subtraction operator should be resolved by the "_" operator
				int end = SYA.strConsume(infix, "[\\d\\.e\\-]", idxT);
				out = infix.substring(idxT, end);
				idxT = end - 1;
			} else if (out.matches("[a-zA-Z]")) {
				// Match: String (function or variable)
				int end = SYA.strConsume(infix, "[a-zA-Z]", idxT);
				out = infix.substring(idxT, end);
				idxT = end - 1;
			} else if (out.matches("[><]")) {
				// Match: binary shift operator
				int end = SYA.strConsume(infix, "[><]", idxT);
				out = infix.substring(idxT, end);
				idxT = end - 1;
			}

			// Parse the token
			// System.out.println("Parsing:" + out);

			// The following two conditions will not be added to any stacks
			// Right parenthesis
			if (out.equals(")")) {
				// Pop everything until "(" is found
				totalPar--;
				Element poppy = operatorStack.pop();
				while (!poppy.data.equals("(")){
					if (operatorStack.size == 0) {
						throw new SYAException("Unmatched left parenthesis");
					}
					this.add(poppy);
					poppy = operatorStack.pop();
				}
				continue;
			}

			// Argument operator
			if(out.equals(",")){
				// Pop everything until right before "(" is found
				// But do not pop "(" itself
				while (!operatorStack.head.data.equals("(")){
					if (operatorStack.size == 0){
						throw new SYAException("Function argument error");
					}
					this.add(operatorStack.pop());
				}
				continue;
			}

			// The following conditions will be added to a stack
			Element e = new Element(out);

			// Left Parenthesis - add as a temporary member to the operator stack
			// This condition is included for the parenthesis check
			if (out.equals("(")) {
				// Left parenthesis
				// Insert onto operator stack as a TEMPORARY element
				totalPar++;
				operatorStack.add(new Element(out));
				continue;
			}

			// Operands - add only to the RPN stack
			if (e.operands == 0) {
				// Add to RPN stack
				this.add(new Element(out));
				continue;
			}

			// Check the operator stack - pop off elements if * the top of the stack has
			// higher or equal precedence (unless right associative)
			Element comp = operatorStack.head;
			while (operatorStack.size > 0 && (comp.precedence > e.precedence
					|| (comp.precedence == e.precedence && !comp.rightAss))) {
				comp = comp.next;
				this.add(operatorStack.pop());
			}

			// Add to the operator stack
			operatorStack.add(e);

		}

		// Mismatch in number of parentheses
		if (totalPar != 0) {
			throw new SYAException("Unclosed Parentheses: " + totalPar);
		}

		// Add everything else to the stack
		while (operatorStack.size > 0) {
			this.add(operatorStack.pop());
		}
	}

	/** SYA Based Methods **/
	// Process the RPN stack into a number
	public double toNumber() throws SYAException {
		// System.out.println("Got: " + this);

		// Special case: just one little thing
		if (this.head.next == null){
			return Variable.parseVariableOrString(this.head.data);
		}

		// Reverse the stack because the current reference is towards the last element
		// We want reference to the first element. This makes it so that left associative
		// operations become right associative and left associative become right.
		// We adjust this problem by reversing the argument order again when a function is passed.
		// Using a doubly linked list may be a better approach to this problem
		Element elem = this.reverse().head;

		// A stack containing the operands
		SYA operand = new SYA();

		// Iterate until the RPN stack is empty
		while (elem != null) {
			if (elem.operands == 0) {
				// If it's an operand, add it to the operand stack
				operand.add(elem.clone());
			} else {
				// If it's an operator, pop [operands] number of elements and then evaluate them
				String[] args = new String[elem.operands];

				// Go in reverse order because the oper
				for (int i = elem.operands - 1; i >= 0; i--){
					Element oper = operand.pop();
					if (oper.data.equals("rand")){
						// rand variable special case
						args[i] = Double.toString(Math.random());
					} else {
						args[i] = oper.data;
					}
				}
				// Execute the function with the given arguments and add the results to the stack
				operand.add(new Element((elem.operate(args))));
			}
			// Go to the next element
			elem = elem.next;
		}

		if (operand.size > 1) {
			throw new SYAException("SYA Stack size is larger than one");
		}

		// Do some post-processing on the number:
		String data = operand.head.data;

		// Return the number
		return doubleRound(operand.head.data);
	}

	// Preprocess infix string helper
	private static String preprocess(String in){
		// Note: valid potential operands: "x", [0-9], )
		// followed by ( "-" preceding another operand, $ variable identifier
		// Remove spaces from the expression
		in = in.replaceAll(" ", "").toLowerCase();

		// Convert all binary subtraction into _
		// Transforms: 5 - x -> 5 _ x
		int checkIndex = 1;
		int foundIndex = in.indexOf("-", checkIndex);
		while (foundIndex != -1) {
			// Check previous and next element
			if (in.substring(foundIndex - 1, foundIndex + 2).matches("[0-9a-zA-Z)]-[0-9a-zA-Z(\\-]")){
				// Check for the scientific exponent
				if (!(foundIndex > 2 && in.substring(foundIndex - 2, foundIndex + 2).matches("[0-9]e-[0-9]"))){
					in = in.substring(0, foundIndex) + "_" + in.substring(foundIndex + 1);
				}
			}
			// Place the left counter after the current index where "-" was found
			checkIndex = foundIndex + 2;
			foundIndex = in.indexOf("-", checkIndex);
		}

		// Handle (5)(3), (5)3
		// Do this by inserting "*" operator
		checkIndex = 1;
		foundIndex = in.indexOf(")", checkIndex);
		while (foundIndex != - 1 && foundIndex + 1 < in.length()){
			// Check next element
			if (Character.toString(in.charAt(foundIndex + 1)).matches("[0-9a-zA-Z(\\-]")){
				in = in.substring(0, foundIndex + 1) + "*" + in.substring(foundIndex + 1);
			}
			checkIndex = foundIndex + 1;
			foundIndex = in.indexOf(")", checkIndex);
		}

		// Handle 3a, 5pi, 3(5)
		// Do this by inserting "*" operator
		for (int i = 0; i < in.length() - 1; i++){
			// Grab two characters at a time from the string
			if (in.substring(i, i + 2).matches("\\d[a-zA-Z(]")){
				// Ignore scientific exponent case
				if (in.substring(i, i + 2).matches("\\de")){
					continue;
				}

				// Insert in between
				in = in.substring(0, i + 1) + "*" + in.substring(i+1);
			} else if (in.substring(i, i + 2).matches("[a-zA-Z][\\d]")){
				// This case is tricky: we can interpret it as a function: foo(5, 2)
				// Ignore scientific exponent case
				if (in.substring(i, i + 1).matches("e")){
					continue;
				}

				// Insert in between
				in = in.substring(0, i + 1) + "*" + in.substring(i+1);
			}
		}

		System.out.println("Preprocess: " + in);
		return in;
	}

	/** Stack-based methods **/
	// Internal Modification Methods
	public void add(Element s) {
		size++;
		if (head != null) {
			s.next = this.head;
		}
		this.head = s;
	}

	// Remove an element from the stack, returning null if the stack is empty
	public Element pop() {
		if (size == 0) {
			return null;
		}
		size--;
		Element tmpRef = head;
		this.head = head.next;
		return tmpRef;
	}

	// Returns an array of the stack
	public Element[] toArray() {
		Element[] arr = new Element[size];
		int i = size - 1;
		Element ref = head;
		while (ref != null) {
			arr[i] = ref;
			i--;
			ref = ref.next;
		}
		return arr;
	}

	// Returns a string representation of the stack
	public String toString() {
		StringBuilder str = new StringBuilder("[ ");
		Element[] dat = this.toArray();
		for (int i2 = 0; i2 < size; i2++) {
			str.append(dat[i2].data).append(i2 != size - 1 ? ", " : "");
		}
		return str + " ]";
	}

	// Reverse the stack
	// Use case: attain the last element
	public SYA reverse() {
		SYA rev = new SYA();
		Element e = head;
		while (e != null) {
			rev.add(e.clone());
			e = e.next;
		}
		return rev;
	}

	/** Utility methods **/
	// String utility method
	// From start + 1, increase the index until string in is exhausted or
	// the next character does not match the pattern string match
	// Returns the index such that in.substring(start + 1, end).matches(match)
	public static int strConsume(String in, String match, int start){
		int end = start;
		while (++end < in.length() && in.substring(end, end + 1).matches(match));
		return end;
	}

	public static double doubleRound(String data){
		// Remove trailing elements

		// Convert into number
		double num = Double.parseDouble(data);

		// Round really small numbers
		if (num > 0 && num < 0.000001){
			num = 0;
		}
		return num;
	}
}

/**
 * Represents an exception thrown while parsing through SYA. Minimal functionality.
 */
class SYAException extends Exception {
	String msg;

	SYAException(String msg) {
		super(msg);
		this.msg = msg;
	}

	public String getMessage() {
		return msg;
	}
}

/**
 * A static class for calculating graph and table positions
 */
class SYAGraph {

	// The list of equation slots
	private static int SYALimit = 1;
	public static SYA[] list = new SYA[SYALimit];

	// Graph
	public static double leftX = -10.0;
	public static double rightX = 10.0;

	// The number of points to calculate to construct the graph
	public static int graphPoints = 250;

	// A special variable used
	private static final Variable graphVar = Variable.assign("x", 0.0);

	// A setter method for the coordinates
	public static void modCoord(double lx, double rx) throws SYAException {
		if (lx > rx) {
			throw new SYAException("leftX has to be smaller than rightX");
		}

		leftX = lx;
		rightX = rx;
	}

	// Get all graph positions for a single equation
	public static auxPair[] getGraphPoints(int eqNum, boolean resize) {
		// Get the equation
		SYA equation = list[eqNum];

		// Get the interval spacing
		double increment = (Math.abs(leftX) + Math.abs(rightX)) / (graphPoints - 1);

		// The initial array of points
		auxPair[] points = new auxPair[graphPoints];

		// Set the graph variable value to the specified left margin
		SYAGraph.graphVar.value = leftX;

		// Construct all points
		int actualPoints = 0;
		for (int i = 0; i < points.length; i++) {
			try {
				// Calculate the equation at the point
				points[i] = new auxPair(SYAGraph.graphVar.value, equation.toNumber());
				actualPoints++;
			} catch (Exception e) {
				System.out.println("WARNING: Failed to calculate equation at " + SYAGraph.graphVar.value);
				points[i] = null;
			}
			// Change the variable value
			// System.out.println(SYAGraph.graphVar.value);
			SYAGraph.graphVar.value = SYA.doubleRound(SYAGraph.graphVar.value + increment + "");
		}

		if (resize) {
			// Resize the array such that no null values are used
			auxPair[] cleanedPoints = new auxPair[actualPoints];
			int i2 = 0;
			for (int i = 0; i < points.length; i++) {
				if (points[i] != null){
					cleanedPoints[i2++] = points[i];
				}
			}
			return cleanedPoints;
		}
		return points;
	}
}

/**
 * Represents a (x, y) coordinate
 */
class auxPair {
	public double x;
	public double y;

	public auxPair(double x, double y) {
		this.x = x;
		this.y = y;
	}

	// Print a 1D array of auxPairs
	public static void auxPrint(auxPair[] e) {
		System.out.print("[ ");
		for (auxPair i : e) {
			System.out.print(i.x + ", " + i.y + " : ");
		}
		System.out.println(" ]");
	}

	// Print a 2D array of auxPairs
	public static void auxPrint(auxPair[][] e) {
		for (auxPair[] auxxy : e) {
			System.out.print("[ ");
			for (auxPair i : auxxy) {
				System.out.print(i.x + ", " + i.y + " : ");
			}
			System.out.print(" ]\n");
		}
	}
}

/**
 * Represents an element in a SYA stack. An element can be both an operator or an operand.
 * Each element is an item in a one-direction linked list which is manipulated as a stack
 * using the SYA class.
 */
class Element {
	// The tag of the element
	public String data;

	// The next element in the linked list
	public Element next = null;

	// The precedence of this element
	public int precedence = -1;

	// If this element is a right associative operator
	public boolean rightAss = false;

	// How many operands this element consumes
	public int operands = 2;

	// Construct an Element using the elements tag
	public Element(String s) throws SYAException {
		this.data = s;

		if (s.matches("[.-]?\\d+.?[\\de\\-]*")) {
			// Match: number
			operands = 0;
		} else if (s.matches("[a-zA-Z]+")) {
			// Match: String
			switch (s){
				case "radix":
					precedence = 0;
					break;
				case "abs":
				case "sin":
				case "cos":
				case "tan":
				case "sqrt":
				case "floor":
				case "ceil":
					precedence = 10;
					operands = 1;
					break;
				case "log":
				case "sqr":
					precedence = 10;
					break;
				default:
					// Variable
					operands = 0;
			}
		} else{
			// Anything else
			switch (s) {
				case "(":
					break;
				case "=":
					precedence = 0;
					break;
				case "~":
					precedence = 1;
					break;
				case "&":
					precedence = 2;
					break;
				case "|":
					precedence = 3;
					break;
				case ">>>":
				case "<<":
				case ">>":
					precedence = 4;
					break;
				case "+":
				case "_":
					// Note that "_" is subtraction
					precedence = 5;
					break;
				case "*":
				case "%":
				case "/":
					precedence = 6;
					break;
				case "^":
					precedence = 7;
					rightAss = true;
					break;
				case "!":
				case "-":
					// Note that "-" is a negative unary operation
					operands = 1;
					precedence = 10;
					break;
				default:
					throw new SYAException("Unrecognized Element:" + s);
			}
		}
	}

	// Manual element for inherited classes
	Element(String data, int precedence, boolean rightAss, int operands) {
		this.data = data;
		this.precedence = precedence;
		this.rightAss = rightAss;
		this.operands = operands;
	}

	// Clone an Element
	// This is used to remove all references (next) to and from this element
	protected Element clone() {
		return new Element(data, precedence, rightAss, operands);
	}

	// Do an operation depending on the tag
	public String operate(String[] strArg) throws SYAException{
		// Debug statements
		/*
		System.out.print("Evaluating using " + this.data);
		for (String s : strArg) {
			System.out.print(" " + s);
		}
		System.out.println();
		 */

		// Special case for "="
		if (this.data.equals("=")){
			double val2 = Variable.parseVariableOrString(strArg[1]);
			Variable.assign(strArg[0], val2);
			return Double.toString(val2);
		}

		// Convert to numeric
		double[] args = new double[strArg.length];
		for (int i = 0; i < args.length; i++){
			args[i] = Variable.parseVariableOrString(strArg[i]);
		}

		// Perform specified operation
		switch (this.data) {
			case "+":
				args[0] += args[1];
				break;
			case "_":
				args[0] -= args[1];
				break;
			case "*":
				args[0] *= args[1];
				break;
			case "%":
				args[0] %= args[1];
				break;
			case "/":
				args[0] /= args[1];
				break;
			case "^":
				args[0] = Math.pow(args[0], args[1]);
				break;
			case ">>":
				args[0] = Math.round(args[0]) >> Math.round(args[1]);
				break;
			case "<<":
				args[0] = Math.round(args[0]) << Math.round(args[1]);
				break;
			case ">>>":
				args[0] = Math.round(args[0]) >>> Math.round(args[1]);
				break;
			case "~":
				args[0] = Math.round(args[0]) ^ Math.round(args[1]);
				break;
			case "|":
				args[0] = Math.round(args[0]) | Math.round(args[1]);
				break;
			case "&":
				args[0] = Math.round(args[0]) & Math.round(args[1]);
				break;
			case "log":
				args[0] = Math.log10(args[0]) / Math.log10(args[1]);
				break;
			case "sqr":
				args[0] = auxCalc.sqr(args[0], args[1]);
				break;
			case "-":
				args[0] = -args[0];
				break;
			case "sin":
				args[0] = Math.sin(args[0]);
				break;
			case "cos":
				args[0] = Math.cos(args[0]);
				break;
			case "tan":
				args[0] = Math.tan(args[0]);
				break;
			case "abs":
				args[0] = Math.abs(args[0]);
				break;
			case "!":
				args[0] = auxCalc.fac(args[0]);
				break;
			case "radix":
				return Radix.toBase((int)Math.round(args[0]), (int) Math.round(args[1]));
			case "sqrt":
				args[0] = Math.sqrt(args[0]);
				break;
			case "floor":
				args[0] = Math.floor(args[0]);
				break;
			case "ceil":
				args[0] = Math.ceil(args[0]);
				break;
			default:
				throw new SYAException("Invalid element:" + this.data);
		}
		return Double.toString(args[0]);
	}
}

/**
 * Variable - a class for creating variables and retrieving them
 */
class Variable extends Element {

	// Note: The "key" of the variable is the inherited field data
	// The value the variable is storing
	public double value;

	// The list of all variables as a linked list
	// Better implementation: use a hashtable
	private static final SYA varList = new SYA();

	// Constants
	private static final Variable pi = Variable.assign("pi", Math.PI);

	// Create a new variable and add it to the variable list
	// Use is discouraged. Use assign() instead
	private Variable(String tag, double value)  {
		super(tag, -1, false, 0);
		this.value = value;

		// Silently ignores if already exists
		varList.add(this);
	}

	// Retrieve the value of a variable
	public static Variable getVariable(String tag) {
		Element e = varList.head;
		while (e != null) {
			if (e.data.equals(tag)) {
				return (Variable) e;
			}
			e = e.next;
		}
		return null;
	}

	// If the variable doesn't exist, create the variable and assign it a value
	public static Variable assign(String tag, double value){
		// Search for the variable in question
		Variable var = getVariable(tag);
		if (var == null){
			var = new Variable(tag, value);
		} else {
			var.value = value;
		}
		return var;
	}

	// Convert a string or a variable into a number
	public static double parseVariableOrString(String elem) throws SYAException{
		if (elem.matches("[a-zA-Z]+")){
			Variable varData = Variable.getVariable(elem);
			if (varData == null){
				throw new SYAException("No value for variable: " + elem);
			}
			return varData.value;
		} else {
			return Double.parseDouble(elem);
		}
	}

	// This method doesn't do anything because it doesn't need to
	// The SYA stack will never insert a Variable object so this method should not be called
	protected Element clone() {
		return this;
	}
}

/**
 * Helper Class with static methods for handling auxiliary calculations
 */
class auxCalc {

	/**
	 * Factorial Method
	 */
	public static double fac(double d) {
		try {
			// System.out.println(d);
			if (d <= 1) {
				return 1;
			}

			return d * fac(d - 1);
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Square Root Function
	 */
	public static double sqr(double d, double name) {
		return Math.pow(d, 1 / name);
	}
}

/**
 * Helper class with static methods for handling radix conversions
 */
class Radix {
	public static int parseBase(String num, int radix) {
		return parseBase(num, radix, false);
	}

	// up to 36 radix, two's complement and irrational unsupp.
	// technically can go beyond - but has issues
	public static int parseBase(String num, int radix, boolean steps) {
		// abcdefghijklmnopqrstuvwxyz
		if (radix < 2) {
			System.out.println("UnsupportedRadix");
			return -1;
		}

		int i = num.length() - 1;
		int all = 0;
		while (i != -1) {
			char ch = Character.toUpperCase(num.charAt(i));
			int number = 0;
			if (Character.isDigit(ch)) {
				number = Character.getNumericValue(ch);
			} else {
				if (ch == '-') {
					all = -all;
					if (i != 0) {
						System.out.println("Error: \"-\" character not at end");
						return -1;
					}
				} else {
					number = ch - 55;
				}
			}
			if (number >= radix) {
				System.out.println("Error: Number greater than base : " + ch);
				return -1;
			}
			double v = number * Math.pow(radix, (num.length() - 1 - i));
			if (steps) {
				System.out.print(v + " + ");

			}
			all += v;
			i--;

		}
		return all;
	}

	// takes decimal number same restrictions apply
	public static String toBase(int num, int radix) {
		return toBase(num, radix, false);
	}

	public static String toBase(int num, int radix, boolean check) {
		if (radix < 2) {
			return "UnsupportedRadix";
		}

		boolean negative = false;
		if (num < -num) {
			negative = true;
			num = -num;
		}

		int orig = num;
		int pos = 0;
		int of = 1;
		String trueNum = "";
		// iterably finds the largest pos/of possible
		for (; num >= of * Math.pow(radix, pos); of++) {
			if (radix - 1 < of) {
				pos++;
				of = 0;
			}
		}

		// directly 1 below
		of--;
		if (of == 0) {
			pos--;
			of = radix - 1;
		}
		while (num > 0 && pos >= 0) {
			// System.out.print( of * Math.pow(radix, pos) + " " + of + " " +
			// radix + " " + pos + " " + num + " " + " " + trueNum );
			// adds to trueNum

			num -= of * Math.pow(radix, pos);
			if (of > 9) {
				trueNum += (char) (of + 55);
			} else {
				trueNum += of;
			}
			pos--;
			of = radix - 1;
			// System.out.println(":" + trueNum);

			if (num <= 0) {
				break;
			}
			// then grabs the ones that fit
			for (; num < of * Math.pow(radix, pos) || of == 0; of--) {
				if (of == 0) {
					of = radix;
					pos--;
					trueNum += 0;
				}
			}
		}

		while (pos >= 0) {
			trueNum += 0;
			pos--;
		}
		if (check) {
			int i = parseBase(trueNum, radix);
			if (i != orig) {
				System.out.println("ERROR: Possible Conflict: " + i + " " + orig);
				return "-1";
			}
		}

		return (negative ? "-" : "") + trueNum;
	}

}
