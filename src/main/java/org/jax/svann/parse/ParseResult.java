package org.jax.svann.parse;

import java.util.List;
import java.util.Objects;

@Deprecated
public class ParseResult {

    private static final ParseResult EMPTY = new ParseResult(List.of(), List.of());
    private final List<SvEvent> anns;
    private final List<BreakendRecord> breakends;

    public ParseResult(List<SvEvent> anns, List<BreakendRecord> breakends) {
        this.anns = anns;
        this.breakends = breakends;
    }

    public static ParseResult empty() {
        return EMPTY;
    }

    public List<BreakendRecord> getBreakends() {
        return breakends;
    }

    public List<SvEvent> getAnns() {
        return anns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParseResult that = (ParseResult) o;
        return Objects.equals(anns, that.anns) &&
                Objects.equals(breakends, that.breakends);
    }

    @Override
    public int hashCode() {
        return Objects.hash(anns, breakends);
    }

    @Override
    public String toString() {
        return "ParseResult{" +
                "anns=" + anns +
                ", breakends=" + breakends +
                '}';
    }
}
