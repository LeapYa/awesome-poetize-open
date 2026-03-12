package com.ld.poetry.service.ai.tools;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorToolsTest {

    private final CalculatorTools calculatorTools = new CalculatorTools();

    @Test
    void shouldCalculateArithmeticExpression() {
        BigDecimal result = calculatorTools.evaluateExpression("(2 + 3) * 4 - 5 / 2");

        assertEquals("17.5", result.stripTrailingZeros().toPlainString());
    }

    @Test
    void shouldSupportFunctionsAndConstants() {
        BigDecimal result = calculatorTools.evaluateExpression("pow(2, 8) + sqrt(16) + abs(-3) + floor(pi)");

        assertEquals("266", result.stripTrailingZeros().toPlainString());
    }

    @Test
    void shouldReturnReadableErrorForInvalidExpression() {
        String result = calculatorTools.calculate("1 + unknown(2)");

        assertTrue(result.contains("表达式无效"));
        assertTrue(result.contains("不支持的函数"));
    }

    @Test
    void shouldRejectDivisionByZero() {
        String result = calculatorTools.calculate("10 / 0");

        assertTrue(result.contains("表达式无效"));
        assertTrue(result.contains("除数不能为 0"));
    }
}
