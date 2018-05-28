package com.orhanobut.logger;

public class PrettyFormatStrategy implements FormatStrategy {

    /**
     * Android's max limit for a log entry is ~4076 bytes,
     * so 4000 bytes is used as chunk size since default charset
     * is UTF-8
     */


    /**
     * 日志类名.
     */
    private static final String LOG_CLASS_NAME = PrettyFormatStrategy.class.getName();
    /**
     * 日志的打印方法名.
     */
    private static final String LOG_PRINT_METHOD_NAME = "logHeaderContent";

    private static final int CHUNK_SIZE = 4000;

    /**
     * The minimum stack trace index, starts at this class after two native calls.
     */
    private static final int MIN_STACK_OFFSET = 5;

    /**
     * Drawing toolbox
     */
    private static final char TOP_LEFT_CORNER = '┌';
    private static final char BOTTOM_LEFT_CORNER = '└';
    private static final char MIDDLE_CORNER = '├';
    private static final char HORIZONTAL_LINE = '│';
    private static final String DOUBLE_DIVIDER = "────────────────────────────────────────────────────────";
    private static final String SINGLE_DIVIDER = "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄";
    private static final String TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String BOTTOM_BORDER = BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String MIDDLE_BORDER = MIDDLE_CORNER + SINGLE_DIVIDER + SINGLE_DIVIDER;

    private final boolean showThreadInfo;
    private final LogStrategy logStrategy;
    private final String tag;
    private int mPackagedLevel;

    private PrettyFormatStrategy(Builder builder) {
        mPackagedLevel = builder.packagedLevel;
        showThreadInfo = builder.showThreadInfo;
        logStrategy = builder.logStrategy;
        tag = builder.tag;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public void log(int priority, String onceOnlyTag, String message) {
        String tag = formatTag(onceOnlyTag);

        logTopBorder(priority, tag);
        logHeaderContent(priority, tag);

        //get bytes of message with system's default charset (which is UTF-8 for Android)
        byte[] bytes = message.getBytes();
        int length = bytes.length;
        if (length <= CHUNK_SIZE) {
            logContent(priority, tag, message);
            logBottomBorder(priority, tag);
            return;
        }

        for (int i = 0; i < length; i += CHUNK_SIZE) {
            int count = Math.min(length - i, CHUNK_SIZE);
            //create a new String with system's default charset (which is UTF-8 for Android)
            logContent(priority, tag, new String(bytes, i, count));
        }
        logBottomBorder(priority, tag);
    }

    private void logTopBorder(int logType, String tag) {
        logChunk(logType, tag, TOP_BORDER);
    }


    /**
     * 获取调用日志类输出方法的堆栈元素索引.
     *
     * @param elements 堆栈元素
     * @return 索引位置，-1 - 不可用
     */
    private int getStackIndex(StackTraceElement[] elements) {
        boolean isChecked = false;
        StackTraceElement element;
        for (int i = 0; i < elements.length; i++) {
            element = elements[i];
            if (LOG_CLASS_NAME.equals(element.getClassName())
                    && LOG_PRINT_METHOD_NAME.equals(element.getMethodName())) {
                isChecked = true;
            }
            if (isChecked) {
                int index = i + 7 + mPackagedLevel;
                if (index < elements.length) {
                    return index;
                }
            }
        }
        return -1;
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private void logHeaderContent(int logType, String tag) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (showThreadInfo) {
            logChunk(logType, tag, HORIZONTAL_LINE + " Thread: " + Thread.currentThread().getName());
            logDivider(logType, tag);
        }
        String level = "";

        int index = getStackIndex(trace);
        //corresponding method count with the current stack may exceeds the stack trace. Trims the count
        if (index == -1) {
            throw new IllegalStateException("set -keep class com.orhanobut.logger.** { *; } in your " +
                    "proguard config file" +
                    " or reduce packageLevel");
        }


        StringBuilder builder = new StringBuilder();
        builder.append(HORIZONTAL_LINE)
                .append(' ')
                .append(level)
                .append(getSimpleClassName(trace[index].getClassName()))
                .append(".")
                .append(trace[index].getMethodName())
                .append(" ")
                .append(" (")
                .append(trace[index].getFileName())
                .append(":")
                .append(trace[index].getLineNumber())
                .append(")");
        logChunk(logType, tag, builder.toString());

    }

    private void logBottomBorder(int logType, String tag) {
        logChunk(logType, tag, BOTTOM_BORDER);
    }

    private void logDivider(int logType, String tag) {
        logChunk(logType, tag, MIDDLE_BORDER);
    }

    private void logContent(int logType, String tag, String chunk) {
        String[] lines = chunk.split(System.getProperty("line.separator"));
        for (String line : lines) {
            logChunk(logType, tag, HORIZONTAL_LINE + " " + line);
        }
    }

    private void logChunk(int priority, String tag, String chunk) {
        logStrategy.log(priority, tag, chunk);
    }

    private String getSimpleClassName(String name) {
        int lastIndex = name.lastIndexOf(".");
        return name.substring(lastIndex + 1);
    }


    private String formatTag(String tag) {
        if (!Utils.isEmpty(tag) && !Utils.equals(this.tag, tag)) {
            return this.tag + "-" + tag;
        }
        return this.tag;
    }

    public static class Builder {
        boolean showThreadInfo = true;
        LogStrategy logStrategy;
        String tag = "PRETTY_LOGGER";
        int packagedLevel = 0;

        private Builder() {
        }

        public Builder setPackagedLevel(int level) {
            packagedLevel = level;
            return this;
        }

        public Builder showThreadInfo(boolean val) {
            showThreadInfo = val;
            return this;
        }

        public Builder logStrategy(LogStrategy val) {
            logStrategy = val;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public PrettyFormatStrategy build() {
            if (logStrategy == null) {
                logStrategy = new LogcatLogStrategy();
            }
            return new PrettyFormatStrategy(this);
        }
    }

}
