package org.ttb.integration.mvc.view;

/**
 * Enum describing columns in trace pattern table.
 * 
 * @author Piotr Dorobisz
 * 
 */
public enum TracePatternColumn {
    ENABLED("Enabled", 60), LOCAL("Local", 60), MODULE_NAME("Module name", 150), FUNCTION_NAME("Function name", 150), ARITY("Arity", 40);

    private final String name;
    private final int width;

    private TracePatternColumn(String name, int width) {
        this.name = name;
        this.width = width;
    }

    public static TracePatternColumn getByIndex(int index) {
        for (TracePatternColumn column : TracePatternColumn.values()) {
            if (column.ordinal() == index)
                return column;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }
}
