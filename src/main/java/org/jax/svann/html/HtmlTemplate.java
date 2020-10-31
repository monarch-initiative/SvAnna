package org.jax.svann.html;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.viz.Visualizable;
import org.jax.svann.viz.Visualizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlTemplate {
    /** Map of data that will be used for the FreeMark template. */
    protected final Map<String, Object> templateData= new HashMap<>();
    /** FreeMarker configuration object. */
    protected final Configuration cfg;

    protected static final String EMPTY_STRING="";

    public HtmlTemplate(List<Visualizer> svAnnList) {
        this.cfg = new Configuration(new Version(String.valueOf(Configuration.VERSION_2_3_30)));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocalizedLookup(false);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
        ClassTemplateLoader templateLoader = new ClassTemplateLoader(HtmlTemplate.class, "");
        cfg.setTemplateLoader(templateLoader);
        cfg.setClassForTemplateLoading(HtmlTemplate.class, "");

        //templateFile.toURI().toURL().toExternalForm()

        templateData.putIfAbsent("svalist", svAnnList);
        templateData.put("n_structural_vars", svAnnList.size());
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
