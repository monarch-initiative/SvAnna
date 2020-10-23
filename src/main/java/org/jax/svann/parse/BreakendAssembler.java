package org.jax.svann.parse;

import org.jax.svann.reference.BreakendDirection;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Assemble {@link org.jax.svann.parse.BreakendRecord} into {@link Adjacency} objects.
 * <p>
 * Candidate for a future interface.
 */
public class BreakendAssembler {

    private static final Charset CHARSET = StandardCharsets.US_ASCII;

    private static Adjacency makeAdjacency(BreakendRecord first, BreakendRecord second) {
        String fAlt = new String(first.getAlt(), CHARSET);
        String sAlt = new String(second.getAlt(), CHARSET);

        // parse directions, the directions might affect one another
        // TODO: 22. 10. 2020 resolve
        BreakendDirection fDirection = null;
        BreakendDirection sDirection = null;

        // TODO: 22. 10. 2020 resolve
        byte[] fInserted = null;
        byte[] sInserted = null;

        SimpleBreakend f = new SimpleBreakend(first.getPosition(), fDirection, first.getId(), first.getRef(), fInserted);
        SimpleBreakend s = new SimpleBreakend(second.getPosition(), sDirection, second.getId(), second.getRef(), sInserted);
        return new Adjacency(f, s);
    }

    public List<Adjacency> assemble(Collection<BreakendRecord> breakends) {
        List<Adjacency> adjacencies = new ArrayList<>();

        Map<String, BreakendRecord> bndById = breakends.stream()
                .filter(br -> br.getId() != null)
                .collect(toMap(BreakendRecord::getId, Function.identity()));
        Set<String> processed = new HashSet<>();

        for (BreakendRecord record : breakends) {
            String id = record.getId();
            String mateId = record.getMateId();
            if (id == null || mateId == null) {
                // do not process breakends with missing IDs
                continue;
            }

            if (processed.contains(id)) {
                // we've already processed this record as mate of another record
                continue;
            }

            Adjacency adjacency = makeAdjacency(record, bndById.get(mateId));

            adjacencies.add(adjacency);
            processed.add(id);
            processed.add(mateId);
        }

        return adjacencies;
    }

}
