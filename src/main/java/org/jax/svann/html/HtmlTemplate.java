package org.jax.svann.html;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import org.jax.svann.reference.SvType;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlTemplate {
    /** Map of data that will be used for the FreeMark template. */
    protected final Map<String, Object> templateData= new HashMap<>();
    /** FreeMarker configuration object. */
    protected final Configuration cfg;

    protected static final String EMPTY_STRING="";

    protected static final String NOT_AVAILABLE = "n/a";



    public HtmlTemplate(List<String> htmlList,
                        Map<SvType, Integer> lowImpact,
                        Map<SvType, Integer> intermediateImpact,
                        Map<SvType, Integer> highImpact,
                        Map<String, String> infoMap,
                        Map<TermId, String> userHpoTerms) {
        this.cfg = new Configuration(new Version(String.valueOf(Configuration.VERSION_2_3_30)));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocalizedLookup(false);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
        ClassTemplateLoader templateLoader = new ClassTemplateLoader(HtmlTemplate.class, "");
        cfg.setTemplateLoader(templateLoader);
        cfg.setClassForTemplateLoading(HtmlTemplate.class, "");

        // We simplify and integrate the maps to make it easier to generate a table with Freemarker
        // We put everything into a list of rows
        // We want to have one value (can be zero) for each of our SvTypes
        List<SvTypeCountRow> rows = new ArrayList<>();
        // TODO -- use the desired order for the output
        for (SvType svtype : SvType.values()) {
            SvTypeCountRow row = new SvTypeCountRow(svtype,
                    lowImpact.getOrDefault(svtype,0),
                    intermediateImpact.getOrDefault(svtype, 0),
                    highImpact.getOrDefault(svtype, 0));
            rows.add(row);
        }
        SvTypeCountRow totals = new SvTypeCountRow(lowImpact, intermediateImpact, highImpact);
        rows.add(totals);
        templateData.put("svtypecounts", rows);
        templateData.putIfAbsent("svalist", htmlList);
        templateData.put("n_unparsable", infoMap.getOrDefault("unparsable", NOT_AVAILABLE));
        templateData.put("vcf_file", infoMap.getOrDefault("vcf_file", NOT_AVAILABLE));
        templateData.put("n_affectedGenes", infoMap.getOrDefault("n_affectedGenes", NOT_AVAILABLE));
        templateData.put("n_affectedEnhancers", infoMap.getOrDefault("n_affectedEnhancers", NOT_AVAILABLE));
        List<String> hpos = createHpoLinks(userHpoTerms);
        templateData.put("hpoterms", hpos);
    }

    private List<String> createHpoLinks(Map<TermId, String> userHpoTerms) {
      List<String> observedHPOs = new ArrayList<>();
        for (var e : userHpoTerms.entrySet()) {
            String termId = e.getKey().getValue();
            String label = e.getValue();
            String tstr = String.format("%s (<a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>)",label,termId,termId);
            observedHPOs.add(tstr);
        }
       return observedHPOs;
    }


    public void outputFile(String prefix) {
        String outpath = String.format( "%s.html", prefix);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(outpath))) {
            Template template = cfg.getTemplate("svannHTML.ftl");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            te.printStackTrace();
        }
    }
}
