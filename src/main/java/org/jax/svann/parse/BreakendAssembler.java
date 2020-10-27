package org.jax.svann.parse;

import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.SequenceRearrangement;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Assemble {@link org.jax.svann.parse.BreakendRecord} into {@link SequenceRearrangement}s.
 * <p>
 * Candidate for a future interface.
 */
public class BreakendAssembler {

    private static Adjacency makeAdjacency(BreakendRecord first, BreakendRecord second) {

        // TODO: 22. 10. 2020 resolve

        SimpleBreakend f = new SimpleBreakend(first.getPosition(), first.getId(), first.getRef(), first.getAlt());
        SimpleBreakend s = new SimpleBreakend(second.getPosition(), second.getId(), second.getRef(), second.getAlt());
        return SimpleAdjacency.of(f, s);
    }

    public List<SequenceRearrangement> assemble(Collection<BreakendRecord> breakends) {
        List<SequenceRearrangement> rearrangements = new ArrayList<>();
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

        return rearrangements;
    }

}
