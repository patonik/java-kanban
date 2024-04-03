package org.my.util;

import java.util.Arrays;

public class IdGenerator {
    private final char[] valueCounter;
    boolean idOverflow;

    public static final char[] RANGE = new char[]{33, 127};

    public IdGenerator() {
        this.valueCounter = new char[IdGenerator.RANGE[1] - IdGenerator.RANGE[0]];
        Arrays.fill(valueCounter, IdGenerator.RANGE[0]);
        this.idOverflow = false;
    }

    public String generateId() throws IdGeneratorOverflow {
        if (idOverflow) {
            throw new IdGeneratorOverflow();
        }
        String id = String.valueOf(valueCounter);
        nextSymbol(RANGE[1] - RANGE[0] - 1);
        return id;
    }

    private void nextSymbol(int pos) {
        if (pos < 0) {
            idOverflow = true;
            return;
        }
        if (++valueCounter[pos] > RANGE[1] - 1) {
            valueCounter[pos] = RANGE[0];
            nextSymbol(--pos);
        }
    }

    public static class IdGeneratorOverflow extends Exception {

    }
}
