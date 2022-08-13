package application;

import javafx.application.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import java.io.IOException;

/**
 * A calculator app
 *
 * This class serves as the launcher and controller for the GUI.
 * The GUI itself is defined in FXML and styled using CSS.
 *
 * Test:
 * --------------------
 * sin(cos(a))(5-2)^(-2)+9*(9/4)--((a3E3sqrt(3!)))
 * -1(2(-3(4(-5)4)-3)2)-1
 * parse complex non-delimited arithmetic expressions
 *
 * Known Issues:
 * --------------------
 * Graph mode breaks with too large values,
 * Some floating point math edge cases i.e. -Infinity, too large numbers
 * Can't differentiate between variables and functions i.e. a(5) is treated like a function
 * Issues with the scientific exponent may come up, do not use it as a variable name
 *
 */
public class Main extends Application {
	/** FXML Elements - not all are defined **/
	// TODO: why does it have to be public?
	// The bottom right buttons defined from top left to bottom right
	@FXML public Button buttonOper1;
	@FXML public Button buttonOper2;
	@FXML public Button buttonOper3;
	@FXML public Button buttonOper4;
	@FXML public Button buttonOper5;
	@FXML public Button buttonOper6;

	// The elements in the center
	@FXML public VBox mathScene;
	@FXML public VBox graphScene;
	@FXML public TextField textInputMath;
	@FXML public TextField textInputGraph;
	@FXML public TextField textInputGraph1;
	@FXML public TextField textInputGraph2;
	@FXML public TextArea textLog;
	@FXML public ScatterChart<Double, Double> lineChart;

	// The buttons on top
	@FXML public Button buttonStandard;
	@FXML public Button buttonExtended;
	@FXML public Button buttonBitwise;
	@FXML public Button buttonToggleView;

	// Mode for operand buttons
	int operMode = 0;

	// Which text input to use
	int textInputMode = 0;

	// Maximum difference in graph points
	double GRAPH_MAXIMUM = 1e5;

	@Override
	public void start(Stage primaryStage) throws IOException {
		// Root Scene
		Parent root = FXMLLoader.load(getClass().getResource("calcfx/gui.fxml"));
		Scene rootScene = new Scene(root);

		// Stage
		primaryStage.setTitle("CalcFX");
		primaryStage.setScene(rootScene);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

	// Evaluates the text input and pushes it onto the log.
	public void enterTextInput(){
		String input = getTextInput().getText();
		logString(input);

		// Ignore if blank
		if (!input.matches("//s+") && !input.equals("")){
			logString(expressionEval(input, true));
		}
		getTextInput().clear();
	}

	// Grab the active text input
	public TextField getTextInput(){
		if (textInputMode == 0){
			return textInputMath;
		} else {
			return textInputGraph;
		}
	}

	// Evaluate something using SYA
	public String expressionEval(String exp, boolean prefix){
		try {
			SYA rpn = new SYA(exp);
			return (prefix ? "\t= " : "") + rpn.toNumber();
		} catch (Exception e){
			return e.getMessage();
		}
	}

	// Log something to the text log
	public void logString(String input){
		textLog.appendText(input + "\n");
	}

	// Set the graph
	public void setGraph(){
		String input = getTextInput().getText();
		// Verify valid input
		if (input.matches("\\s+") || input.equals("")){
			return;
		}

		// Verify that bounds are correct
		try {
			SYAGraph.modCoord(Double.parseDouble(textInputGraph1.getText()),
					Double.parseDouble(textInputGraph2.getText()));
		} catch (Exception e){
			return;
		}


		// Get graph points and add them to the line chart
		// If there is an error, then print the message in the main text input
		try {
			SYAGraph.list[0] = new SYA(input);
			auxPair[] coords = SYAGraph.getGraphPoints(0, true);
			XYChart.Series<Double, Double> series = new XYChart.Series<>();
			for (int i = 0; i < coords.length; i++){
				// Handle problem: coordinates that grow too much (infinity)
				if (i > 0) {
					if(Math.abs(coords[i-1].y - coords[i].y) > GRAPH_MAXIMUM){
						System.out.println("Skipping " + coords[i].x + " " + coords[i].y);
						continue;
					}
				}

				// Handle problem NaN and Infinity?
				if (!Double.isFinite(coords[i].y)  || Double.isNaN(coords[i].y)){
					continue;
				}

				series.getData().add(new XYChart.Data<Double, Double>(coords[i].x, coords[i].y));
			}
			lineChart.getData().add(series);


			// Do some stackpane magic to manipulate the size of each point
			for (XYChart.Data<Double, Double> data : series.getData()){
				StackPane stackPane =  (StackPane) data.getNode();
				stackPane.setPrefWidth(5);
				stackPane.setPrefHeight(5);
			}

		} catch (Exception e){
			getTextInput().clear();
			e.printStackTrace();
			getTextInput().setText(e.getMessage());
		}
	}

	/** FXML Methods **/
	// Number grid methods
	public void _1(){
		getTextInput().appendText("1");
	}
	public void _2(){
		getTextInput().appendText("2");
	}
	public void _3(){
		getTextInput().appendText("3");
	}
	public void _4(){
		getTextInput().appendText("4");
	}
	public void _5(){
		getTextInput().appendText("5");
	}
	public void _6(){
		getTextInput().appendText("6");
	}
	public void _7(){
		getTextInput().appendText("7");
	}
	public void _8(){
		getTextInput().appendText("8");
	}
	public void _9(){
		getTextInput().appendText("9");
	}
	public void _0(){
		getTextInput().appendText("0");
	}
	public void _dot(){
		getTextInput().appendText("-");
	}
	public void _enter(){
		if (textInputMode == 0) {
			enterTextInput();
		} else {
			setGraph();
		}
	}

	// Text center methods
	public void _handleTextInput(KeyEvent value){
		if (value.getCode().toString().equals("ENTER")){
			enterTextInput();
		}
	}
	public void _handleTextInputGraph(KeyEvent value){
		if (value.getCode().toString().equals("ENTER")){
			setGraph();
		}
	}

	// Top row buttons
	public void _standard(){
		// Don't do anything if the button is already active
		if (operMode != 0){
			// Switch focus to this one
			switch (operMode){
				case 1:
					buttonExtended.setId("");
					break;
				case 2:
					buttonBitwise.setId("");
					break;
			}
			buttonStandard.setId("buttonmode");

			// Set the text
			buttonOper1.setText("+");
			buttonOper2.setText("-");
			buttonOper3.setText("*");
			buttonOper4.setText("/");
			buttonOper5.setText("^");
			buttonOper6.setText("√");

			// Clear style class
			buttonOper1.getStyleClass().remove("textbutton2");
			buttonOper2.getStyleClass().remove("textbutton2");
			buttonOper3.getStyleClass().remove("textbutton2");
			buttonOper4.getStyleClass().remove("textbutton2");
			buttonOper5.getStyleClass().remove("textbutton2");
			buttonOper6.getStyleClass().remove("textbutton2");

			// Set the mode
			operMode = 0;
		}
	}
	public void _extended(){
		// Don't do anything if the button is already active
		if (operMode != 1){
			// Switch focus to this one
			switch (operMode){
				case 0:
					buttonStandard.setId("");
					break;
				case 2:
					buttonBitwise.setId("");
					break;
			}
			buttonExtended.setId("buttonmode");

			// Set the text
			buttonOper1.setText("sin");
			buttonOper2.setText("log");
			buttonOper3.setText("cos");
			buttonOper4.setText("!");
			buttonOper5.setText("tan");
			buttonOper6.setText("var");

			// Set style class
			buttonOper1.getStyleClass().add("textbutton2");
			buttonOper2.getStyleClass().add("textbutton2");
			buttonOper3.getStyleClass().add("textbutton2");
			buttonOper4.getStyleClass().remove("textbutton2");
			buttonOper5.getStyleClass().add("textbutton2");
			buttonOper6.getStyleClass().add("textbutton2");

			// Set the mode
			operMode = 1;
		}
	}
	public void _bitwise(){
		// Don't do anything if the button is already active
		if (operMode != 2){
			// Switch focus to this one
			switch (operMode){
				case 0:
					buttonStandard.setId("");
					break;
				case 1:
					buttonExtended.setId("");
					break;
			}
			buttonBitwise.setId("buttonmode");

			// Set the text
			buttonOper1.setText("<<");
			buttonOper2.setText("or");
			buttonOper3.setText(">>");
			buttonOper4.setText("and");
			buttonOper5.setText(">>>");
			buttonOper6.setText("xor");

			// Clear style class
			buttonOper1.getStyleClass().add("textbutton2");
			buttonOper2.getStyleClass().add("textbutton2");
			buttonOper3.getStyleClass().add("textbutton2");
			buttonOper4.getStyleClass().add("textbutton2");
			buttonOper5.getStyleClass().add("textbutton2");
			buttonOper6.getStyleClass().add("textbutton2");

			// Set the mode
			operMode = 2;
		}
	}
	public void _toggleMode(){
		// Flip the modes
		if (textInputMode == 0){
			buttonToggleView.setId("graphmode");
			buttonToggleView.setText("Graph");
			mathScene.setVisible(false);
			graphScene.setVisible(true);
			textInputMode = 1;
		} else {
			buttonToggleView.setId("mathmode");
			buttonToggleView.setText("Math");
			graphScene.setVisible(false);
			mathScene.setVisible(true);
			textInputMode = 0;
		}
	}
	public void _clear(){
		if (textInputMode == 0) {
			textLog.clear();
		} else {
			lineChart.getData().clear();
		}
	}

	// Right bottom operand grid methods
	public void _oper1(){
		if (operMode == 0){
			getTextInput().appendText(" + ");
		}else if (operMode == 1){
			getTextInput().appendText(" sin(");
		}else if (operMode == 2){
			getTextInput().appendText(" << ");
		}
	}
	public void _oper2(){
		if (operMode == 0){
			getTextInput().appendText(" - ");
		}else if (operMode == 1){
			getTextInput().appendText(" log(");
		}else if (operMode == 2){
			getTextInput().appendText(" | ");
		}
	}
	public void _oper3(){
		if (operMode == 0){
			getTextInput().appendText(" * ");
		}else if (operMode == 1){
			getTextInput().appendText(" cos(");
		}else if (operMode == 2){
			getTextInput().appendText(" >> ");
		}
	}
	public void _oper4(){
		if (operMode == 0){
			getTextInput().appendText(" / ");
		}else if (operMode == 1){
			getTextInput().appendText("!");
		}else if (operMode == 2){
			getTextInput().appendText(" & ");
		}
	}
	public void _oper5(){
		if (operMode == 0){
			getTextInput().appendText(" ^ ");
		}else if (operMode == 1){
			getTextInput().appendText(" tan(");
		}else if (operMode == 2){
			getTextInput().appendText(" >>> ");
		}
	}
	public void _oper6(){
		if (operMode == 0){
			getTextInput().appendText(" sqrt(");
		}else if (operMode == 1){
			getTextInput().appendText(" = ");
		}else if (operMode == 2){
			getTextInput().appendText(" ~ ");
		}
	}
}