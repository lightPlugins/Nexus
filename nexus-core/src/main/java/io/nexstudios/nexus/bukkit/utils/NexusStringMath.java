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
        // Entfernt Leerzeichen für eine saubere Verarbeitung
        expression = expression.replaceAll("\\s", "");

        // Stacks für Zahlen und Operatoren
        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();
        int length = expression.length();

        for (int i = 0; i < length; i++) {
            char c = expression.charAt(i);

            // Wenn es sich um eine Zahl handelt (multi-digit unterstützt)
            if (Character.isDigit(c) || c == '.') {
                // Puffer für Ganzzahl/Floats
                StringBuilder buffer = new StringBuilder();
                while (i < length && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    buffer.append(expression.charAt(i));
                    i++;
                }
                i--; // Rückschritt, da der Schleifenindex um eins erhöht wird
                numbers.push(Double.parseDouble(buffer.toString()));
            }
            // Wenn es eine öffnende Klammer ist
            else if (c == '(') {
                operators.push(c);
            }
            // Wenn es eine schließende Klammer ist
            else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    numbers.push(applyOperation(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.pop(); // Öffnende Klammer entfernen
            }
            // Operatoren: +, -, *, /, ^
            else if (isOperator(c)) {
                // Präzedenzregeln anwenden
                while (!operators.isEmpty() && precedence(c) <= precedence(operators.peek())) {
                    numbers.push(applyOperation(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.push(c);
            }
        }

        // Restliche Operatoren verarbeiten
        while (!operators.isEmpty()) {
            numbers.push(applyOperation(operators.pop(), numbers.pop(), numbers.pop()));
        }

        return numbers.pop();
    }

    /**
     * Überprüft, ob ein Zeichen ein Operator ist.
     */
    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    /**
     * Gibt die Priorität eines Operators zurück.
     */
    private static int precedence(char operator) {
        return switch (operator) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            case '^' -> // Höchste Priorität
                    3;
            default -> 0;
        };
    }

    /**
     * Wendet eine Operation auf zwei Operanden an.
     */
    private static double applyOperation(char operator, double b, double a) {
        return switch (operator) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> {
                if (b == 0) {
                    throw new ArithmeticException("Cant divide by zero");
                }
                yield a / b;
            }
            case '^' -> // Exponentiation (Hochrechnen)
                    Math.pow(a, b);
            default -> 0;
        };
    }
}

