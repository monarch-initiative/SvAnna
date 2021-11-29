package org.jax.svanna.autoconfigure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "svanna.data.enhancers")
public class EnhancerProperties {

    private boolean useVista = true;
    private boolean useFantom5 = false;
    private double fantom5TissueSpecificity = .5;

    public boolean useVista() {
        return useVista;
    }

    public void setUseVista(boolean useVista) {
        this.useVista = useVista;
    }

    public boolean useFantom5() {
        return useFantom5;
    }

    public void setUseFantom5(boolean useFantom5) {
        this.useFantom5 = useFantom5;
    }

    public double fantom5TissueSpecificity() {
        return fantom5TissueSpecificity;
    }

    public void setFantom5TissueSpecificity(double fantom5TissueSpecificity) {
        this.fantom5TissueSpecificity = fantom5TissueSpecificity;
    }

}
