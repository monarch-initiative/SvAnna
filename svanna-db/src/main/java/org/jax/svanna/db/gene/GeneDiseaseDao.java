package org.jax.svanna.db.gene;

import org.jax.svanna.model.HpoDiseaseSummary;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.jdbc.core.*;
import xyz.ielis.silent.genes.model.GeneIdentifier;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneDiseaseDao {

    private static final Pattern NCBI_GENE_PATTERN = Pattern.compile("NCBIGene:(?<value>\\d+)");
    private static final Pattern HGNC_GENE_PATTERN = Pattern.compile("HGNC:(?<value>\\d+)");

    private static final RowMapper<GeneIdentifier> GENE_IDENTIFIER_ROW_MAPPER = (rs, i) -> GeneIdentifier.of(
            rs.getString("ACCESSION"),
            rs.getString("SYMBOL"),
            "HGNC:" + rs.getString("HGNC_ID"),
            "NCBIGene:" + rs.getString("NCBI_GENE"));
    private static final ResultSetExtractor<Map<String, List<HpoDiseaseSummary>>> GENE_TO_DISEASES_EXTRACTOR = rs -> {
        Map<String, List<HpoDiseaseSummary>> builder = new HashMap<>();
        while (rs.next()) {
            String hgncId = "HGNC:" + rs.getString("HGNC_ID");
            builder.computeIfAbsent(hgncId, key -> new LinkedList<>())
                    .add(HpoDiseaseSummary.of(rs.getString("DISEASE_ID"), rs.getString("DISEASE_NAME")));
        }
        return Map.copyOf(builder);
    };
    private static final ResultSetExtractor<Map<String, List<TermId>>> DISEASE_TO_PHENOTYPES_EXTRACTOR = rs -> {
        Map<String, List<TermId>> results = new HashMap<>();
        while (rs.next()) {
            String diseaseId = rs.getString(1);
            String hpoId = rs.getString(2);
            results.computeIfAbsent(diseaseId, k -> new LinkedList<>()).add(TermId.of(hpoId));
        }
        return Map.copyOf(results);
    };

    private final JdbcTemplate jdbcTemplate;

    public GeneDiseaseDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<GeneIdentifier> geneIdentifiers() {
        return jdbcTemplate.query("select ACCESSION, SYMBOL, HGNC_ID, NCBI_GENE from SVANNA.GENE_IDENTIFIER", GENE_IDENTIFIER_ROW_MAPPER);
    }

    public Map<String, List<TermId>> diseaseToPhenotypes() {
        return jdbcTemplate.query("select DISEASE_ID, TERM_ID from SVANNA.DISEASE_TO_PHENOTYPE", DISEASE_TO_PHENOTYPES_EXTRACTOR);
    }

    public Map<String, List<HpoDiseaseSummary>> geneToDiseases() {
        return jdbcTemplate.query("select distinct gd.HGNC_ID, hd.DISEASE_ID, hd.DISEASE_NAME " +
                        " from SVANNA.GENE_TO_DISEASE gd " +
                        "   join SVANNA.HPO_DISEASE_SUMMARY hd " +
                        "     on gd.DISEASE_ID = hd.DISEASE_ID",
                GENE_TO_DISEASES_EXTRACTOR);
    }

    public int insertGeneIdentifiers(List<GeneIdentifier> geneIdentifiers) {
        String sql = "insert into SVANNA.GENE_IDENTIFIER(ACCESSION, SYMBOL, HGNC_ID, NCBI_GENE) values ( ?, ?, ?, ? )";

        int[] updated = jdbcTemplate.batchUpdate(sql, new GeneIdentifierBatchPreparedStatementSetter(geneIdentifiers));

        int all = 0;
        for (int i : updated) {
            all += i;
        }
        return all;
    }

    public int insertGeneToDisease(Map<Integer, List<HpoDiseaseSummary>> geneToDisease) {
        int all = 0;

        // insert GENE_TO_DISEASE
        String geneToDiseaseSql = "insert into SVANNA.GENE_TO_DISEASE(HGNC_ID, DISEASE_ID) values ( ?, ? )";
        for (Map.Entry<Integer, List<HpoDiseaseSummary>> entry : geneToDisease.entrySet()) {
            int hgncId = entry.getKey();
            for (HpoDiseaseSummary hpoDiseaseSummary : entry.getValue()) {
                all += jdbcTemplate.update(geneToDiseaseSql, pss -> {
                    pss.setInt(1, hgncId);
                    pss.setString(2, hpoDiseaseSummary.getDiseaseId());
                });
            }
        }

        // insert HPO_DISEASE_SUMMARY
        String hpoDiseaseSummary = "insert into SVANNA.HPO_DISEASE_SUMMARY(DISEASE_ID, DISEASE_NAME) values ( ?, ? )";
        all += geneToDisease.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .mapToInt(diseaseSummary -> jdbcTemplate.update(hpoDiseaseSummary,
                        pss -> {
                            pss.setString(1, diseaseSummary.getDiseaseId());
                            pss.setString(2, diseaseSummary.getDiseaseName());
                        }))
                .sum();

        return all;
    }

    public int insertDiseaseToPhenotypes(String diseaseId, List<TermId> phenotypes) {
        int updated = 0;
        String insertDiseaseToPhenotypes = "insert into SVANNA.DISEASE_TO_PHENOTYPE(DISEASE_ID, TERM_ID) VALUES ( ?, ? )";
        for (TermId phenotype : phenotypes) {
            updated += jdbcTemplate.update(insertDiseaseToPhenotypes,
                    pss -> {
                        pss.setString(1, diseaseId);
                        pss.setString(2, phenotype.getValue());
                    });
        }
        return updated;
    }

    private static class GeneIdentifierBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

        private final List<GeneIdentifier> geneIdentifiers;

        private GeneIdentifierBatchPreparedStatementSetter(List<GeneIdentifier> geneIdentifiers) {
            this.geneIdentifiers = geneIdentifiers;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            GeneIdentifier gi = geneIdentifiers.get(i);
            ps.setString(1, gi.accession());
            ps.setString(2, gi.symbol());
            if (gi.hgncId().isPresent()) {
                Matcher matcher = HGNC_GENE_PATTERN.matcher(gi.hgncId().get());
                if (matcher.matches())
                    ps.setInt(3, Integer.parseInt(matcher.group("value")));
                else
                    ps.setNull(3, Types.INTEGER);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            if (gi.ncbiGeneId().isPresent()) {
                Matcher matcher = NCBI_GENE_PATTERN.matcher(gi.ncbiGeneId().get());
                if (matcher.matches())
                    ps.setInt(4, Integer.parseInt(matcher.group("value")));
                else
                    ps.setNull(4, Types.INTEGER);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
        }

        @Override
        public int getBatchSize() {
            return geneIdentifiers.size();
        }
    }

}
