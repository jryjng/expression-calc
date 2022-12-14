# expression-calc
<img src="/assets/cdemo1.png" alt="calculator" width="300"/>
<img src="/assets/cdemo2.png" alt="calculator" width="300"/>

An expression graphing calculator built using JavaFX. Supports a wide variety of operations such as:

`x=-3*5/floor(5)^-(2*3e2)`

`y=sin(cos(tan(floor(-2))))`

`abs( x / y ) ^ -2`

`5 << 2 >> 3 & 5!`


The list of operations include:
* Standard arithmetic, trignometric, logarithic
* Bitwise operations, radix
* Math functions i.e. absolute value, floor, ceil
* Unary negation and scientific notation
* Variables

Expression precedence is done through an implementation of the [Shunting Yard Algorithm](https://en.wikipedia.org/wiki/Shunting_yard_algorithm).
The expression parser accepts any arbitary valid infix expression and process it into RPN before evaluating the RPN.


The UI is built using JavaFX, FXML, and CSS.
The application only utilizes the JavaFX framework and java.lang.Math library.

### TODO
Create a runtime image.
