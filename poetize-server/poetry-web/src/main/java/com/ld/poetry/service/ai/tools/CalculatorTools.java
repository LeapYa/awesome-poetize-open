package com.ld.poetry.service.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 安全的本地计算器工具，避免依赖脚本引擎执行任意代码。
 */
@Service
public class CalculatorTools {

    private static final int MAX_EXPRESSION_LENGTH = 200;
    private static final int DIVISION_SCALE = 12;
    private static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);

    @Tool(description = "计算数学表达式，支持 + - * / % ^、括号、小数、负数、常量 pi/e，以及 sqrt、abs、round、floor、ceil、pow、max、min")
    public String calculate(
            @ToolParam(description = "数学表达式，例如 (2+3)*4、sqrt(16)、pow(2,10)、max(3,7)") String expression) {
        if (expression == null || expression.isBlank()) {
            return "表达式不能为空。";
        }

        try {
            BigDecimal result = evaluateExpression(expression);
            return "计算结果：" + expression.trim() + " = " + formatNumber(result) + "。";
        } catch (IllegalArgumentException ex) {
            return "表达式无效：" + ex.getMessage()
                    + "。支持 + - * / % ^、括号、pi/e 和 sqrt/abs/round/floor/ceil/pow/max/min。";
        }
    }

    BigDecimal evaluateExpression(String expression) {
        String normalized = expression == null ? "" : expression.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("表达式不能为空");
        }
        if (normalized.length() > MAX_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException("表达式过长");
        }
        return new ExpressionParser(normalized).parse();
    }

    private String formatNumber(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0, RoundingMode.UNNECESSARY);
        }
        return normalized.toPlainString();
    }

    private static BigDecimal fromDouble(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("计算结果超出范围");
        }
        return BigDecimal.valueOf(value);
    }

    private static final class ExpressionParser {

        private final String expression;
        private int position;

        private ExpressionParser(String expression) {
            this.expression = expression;
        }

        private BigDecimal parse() {
            BigDecimal result = parseExpression();
            skipWhitespace();
            if (!isAtEnd()) {
                throw new IllegalArgumentException("存在无法识别的字符: " + peek());
            }
            return result;
        }

        private BigDecimal parseExpression() {
            BigDecimal value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value = value.add(parseTerm(), MATH_CONTEXT);
                    continue;
                }
                if (match('-')) {
                    value = value.subtract(parseTerm(), MATH_CONTEXT);
                    continue;
                }
                return value;
            }
        }

        private BigDecimal parseTerm() {
            BigDecimal value = parsePower();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value = value.multiply(parsePower(), MATH_CONTEXT);
                    continue;
                }
                if (match('/')) {
                    BigDecimal divisor = parsePower();
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("除数不能为 0");
                    }
                    value = value.divide(divisor, DIVISION_SCALE, RoundingMode.HALF_UP);
                    continue;
                }
                if (match('%')) {
                    BigDecimal divisor = parsePower();
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("取模除数不能为 0");
                    }
                    value = value.remainder(divisor, MATH_CONTEXT);
                    continue;
                }
                return value;
            }
        }

        private BigDecimal parsePower() {
            BigDecimal base = parseUnary();
            skipWhitespace();
            if (match('^')) {
                BigDecimal exponent = parsePower();
                return fromDouble(Math.pow(base.doubleValue(), exponent.doubleValue()));
            }
            return base;
        }

        private BigDecimal parseUnary() {
            skipWhitespace();
            if (match('+')) {
                return parseUnary();
            }
            if (match('-')) {
                return parseUnary().negate(MATH_CONTEXT);
            }
            return parsePrimary();
        }

        private BigDecimal parsePrimary() {
            skipWhitespace();
            if (match('(')) {
                BigDecimal value = parseExpression();
                skipWhitespace();
                if (!match(')')) {
                    throw new IllegalArgumentException("缺少右括号");
                }
                return value;
            }

            if (isNumberStart(peek())) {
                return parseNumber();
            }

            if (Character.isLetter(peek())) {
                String identifier = parseIdentifier();
                skipWhitespace();
                if (match('(')) {
                    List<BigDecimal> args = parseFunctionArguments();
                    return applyFunction(identifier, args);
                }
                return resolveConstant(identifier);
            }

            if (isAtEnd()) {
                throw new IllegalArgumentException("表达式不完整");
            }
            throw new IllegalArgumentException("存在无法识别的字符: " + peek());
        }

        private List<BigDecimal> parseFunctionArguments() {
            List<BigDecimal> args = new ArrayList<>();
            skipWhitespace();
            if (match(')')) {
                return args;
            }

            do {
                args.add(parseExpression());
                skipWhitespace();
            } while (match(','));

            if (!match(')')) {
                throw new IllegalArgumentException("函数参数缺少右括号");
            }
            return args;
        }

        private BigDecimal applyFunction(String identifier, List<BigDecimal> args) {
            String name = identifier.toLowerCase(Locale.ROOT);
            return switch (name) {
                case "sqrt" -> {
                    requireArgumentCount(name, args, 1);
                    BigDecimal value = args.get(0);
                    if (value.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("sqrt 参数不能为负数");
                    }
                    yield fromDouble(Math.sqrt(value.doubleValue()));
                }
                case "abs" -> {
                    requireArgumentCount(name, args, 1);
                    yield args.get(0).abs(MATH_CONTEXT);
                }
                case "round" -> {
                    requireArgumentCount(name, args, 1);
                    yield args.get(0).setScale(0, RoundingMode.HALF_UP);
                }
                case "floor" -> {
                    requireArgumentCount(name, args, 1);
                    yield args.get(0).setScale(0, RoundingMode.FLOOR);
                }
                case "ceil" -> {
                    requireArgumentCount(name, args, 1);
                    yield args.get(0).setScale(0, RoundingMode.CEILING);
                }
                case "pow" -> {
                    requireArgumentCount(name, args, 2);
                    yield fromDouble(Math.pow(args.get(0).doubleValue(), args.get(1).doubleValue()));
                }
                case "max" -> {
                    requireArgumentCount(name, args, 2);
                    yield args.get(0).max(args.get(1));
                }
                case "min" -> {
                    requireArgumentCount(name, args, 2);
                    yield args.get(0).min(args.get(1));
                }
                default -> throw new IllegalArgumentException("不支持的函数: " + identifier);
            };
        }

        private void requireArgumentCount(String functionName, List<BigDecimal> args, int expectedCount) {
            if (args.size() != expectedCount) {
                throw new IllegalArgumentException(functionName + " 需要 " + expectedCount + " 个参数");
            }
        }

        private BigDecimal resolveConstant(String identifier) {
            String name = identifier.toLowerCase(Locale.ROOT);
            return switch (name) {
                case "pi" -> fromDouble(Math.PI);
                case "e" -> fromDouble(Math.E);
                default -> throw new IllegalArgumentException("不支持的常量: " + identifier);
            };
        }

        private BigDecimal parseNumber() {
            int start = position;
            boolean hasDot = false;
            while (!isAtEnd()) {
                char current = expression.charAt(position);
                if (Character.isDigit(current)) {
                    position++;
                    continue;
                }
                if (current == '.') {
                    if (hasDot) {
                        throw new IllegalArgumentException("数字格式错误");
                    }
                    hasDot = true;
                    position++;
                    continue;
                }
                break;
            }

            String numberText = expression.substring(start, position);
            if (".".equals(numberText)) {
                throw new IllegalArgumentException("数字格式错误");
            }
            try {
                return new BigDecimal(numberText, MATH_CONTEXT);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("数字格式错误");
            }
        }

        private String parseIdentifier() {
            int start = position;
            while (!isAtEnd()) {
                char current = expression.charAt(position);
                if (Character.isLetterOrDigit(current) || current == '_') {
                    position++;
                    continue;
                }
                break;
            }
            return expression.substring(start, position);
        }

        private boolean isNumberStart(char current) {
            return Character.isDigit(current) || current == '.';
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(expression.charAt(position))) {
                position++;
            }
        }

        private boolean match(char expected) {
            if (peek() != expected) {
                return false;
            }
            position++;
            return true;
        }

        private char peek() {
            if (isAtEnd()) {
                return '\0';
            }
            return expression.charAt(position);
        }

        private boolean isAtEnd() {
            return position >= expression.length();
        }
    }
}
