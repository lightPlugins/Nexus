package io.nexstudios.nexus.bukkit.utils;

import java.util.Stack;

public class NexusStringMath {

    /**
     * Methode, die einen Ausdruck als String entgegennimmt und das Ergebnis zurückgibt.
     *
     * @param expression Der mathematische Ausdruck als String.
     * @return Das Ergebnis des Ausdrucks als double.
     */
    public static double evaluateExpression(String expression) {
        return evaluate(expression);
    }

    private static double evaluate(String expression) {
        expression = expression.replaceAll("\\s", "");

        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();
        int length = expression.length();

        for (int i = 0; i < length; i++) {
            char c = expression.charAt(i);

            // Zahl (unterstützt: Dezimalpunkt, wissenschaftliche Notation mit e/E und optionalem +/-)
            if (Character.isDigit(c) || c == '.' || isUnarySign(expression, i)) {
                StringBuilder buffer = new StringBuilder();

                // optionales Vorzeichen (unäres + oder -)
                if (isUnarySign(expression, i)) {
                    buffer.append(c);
                    i++;
                    if (i >= length) break;
                    c = expression.charAt(i);
                }

                boolean sawDigit = false;

                // Mantisse (Ziffern und Punkt)
                while (i < length && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    buffer.append(expression.charAt(i));
                    sawDigit = true;
                    i++;
                }

                // Exponententeil: e/E[+/-]?Ziffern+
                if (i < length && (expression.charAt(i) == 'e' || expression.charAt(i) == 'E')) {
                    int expStart = i;
                    buffer.append(expression.charAt(i)); // e/E
                    i++;

                    if (i < length && (expression.charAt(i) == '+' || expression.charAt(i) == '-')) {
                        buffer.append(expression.charAt(i));
                        i++;
                    }

                    int expDigitsStart = i;
                    while (i < length && Character.isDigit(expression.charAt(i))) {
                        buffer.append(expression.charAt(i));
                        i++;
                    }
                    // wenn keine Exponenten-Ziffern, rolle zurück (e als Operator wäre ungültig, deshalb zurücknehmen)
                    if (i == expDigitsStart) {
                        // kein gültiger Exponent, zurücksetzen auf Position vor e/E
                        i = expStart;
                        buffer.setLength(buffer.length() - 1); // e/E entfernen
                    }
                }

                // Schleifenindex korrigieren, da for-Schleife i++ macht
                i--;

                if (!sawDigit) {
                    throw new NumberFormatException("Ungültige Zahl in Ausdruck nahe Index " + i);
                }

                numbers.push(Double.parseDouble(buffer.toString()));
            }
            else if (c == '(') {
                operators.push(c);
            }
            else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    numbers.push(applyOperation(operators.pop(), numbers.pop(), numbers.pop()));
                }
                if (!operators.isEmpty() && operators.peek() == '(') {
                    operators.pop();
                } else {
                    throw new IllegalArgumentException("Fehlende öffnende Klammer");
                }
            }
            else if (isOperator(c)) {
                // Präzedenzregeln
                while (!operators.isEmpty() && precedence(c) <= precedence(operators.peek())) {
                    char op = operators.peek();
                    if (op == '(') break;
                    numbers.push(applyOperation(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.push(c);
            }
            else {
                throw new IllegalArgumentException("Unerwartetes Zeichen im Ausdruck: '" + c + "'");
            }
        }

        while (!operators.isEmpty()) {
            char op = operators.pop();
            if (op == '(' || op == ')') {
                throw new IllegalArgumentException("Unausgeglichene Klammern");
            }
            numbers.push(applyOperation(op, numbers.pop(), numbers.pop()));
        }

        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Leerer oder ungültiger Ausdruck");
        }
        return numbers.pop();
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private static int precedence(char operator) {
        return switch (operator) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            case '^' -> 3;
            default -> 0;
        };
    }

    private static double applyOperation(char operator, double b, double a) {
        return switch (operator) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> {
                if (b == 0) throw new ArithmeticException("Cant divide by zero");
                yield a / b;
            }
            case '^' -> Math.pow(a, b);
            default -> 0;
        };
    }

    // erkennt unäres +/-, z. B. am Anfang, nach '(', oder nach einem Operator
    private static boolean isUnarySign(String expr, int i) {
        char c = expr.charAt(i);
        if (c != '+' && c != '-') return false;
        if (i == 0) return true;
        char prev = expr.charAt(i - 1);
        return prev == '(' || isOperator(prev);
    }

}

