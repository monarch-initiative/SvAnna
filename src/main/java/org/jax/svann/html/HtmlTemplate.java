package org.jax.svann.html;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.*;
import org.jax.svann.vcf.SvAnnOld;

public class HtmlTemplate {
    /** Map of data that will be used for the FreeMark template. */
    protected final Map<String, Object> templateData= new HashMap<>();
    /** FreeMarker configuration object. */
    protected final Configuration cfg;

    protected static final String EMPTY_STRING="";

    public HtmlTemplate(List<SvAnnOld> svAnnList) {
        this.cfg = new Configuration(new Version(String.valueOf(Configuration.VERSION_2_3_30)));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocalizedLookup(false);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
        ClassTemplateLoader templateLoader = new ClassTemplateLoader(HtmlTemplate.class, "");
        cfg.setTemplateLoader(templateLoader);
        cfg.setClassForTemplateLoading(HtmlTemplate.class,"");

        //templateFile.toURI().toURL().toExternalForm()

        templateData.putIfAbsent("svalist", svAnnList);
        templateData.put("n_non_translocations", svAnnList.size());
        int n_low = (int) svAnnList
                .stream()
                .filter(SvAnnOld::isLowPriority)
                .count();
        int n_modifer = (int) svAnnList
                .stream()
                .filter(SvAnnOld::isModifierPriority)
                .count();
        int n_high = (int) svAnnList
                .stream()
                .filter(SvAnnOld::isHighPriority)
                .count();
        templateData.put("n_high", n_high);
        templateData.put("n_modifer", n_modifer);
        templateData.put("n_low",n_low);

    }


    public void outputFile(String prefix) {
        String outpath = String.format( "%s.html", prefix);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(outpath))) {
            Template template = cfg.getTemplate("l2oHTML.ftl");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            te.printStackTrace();
        }
    }
}
