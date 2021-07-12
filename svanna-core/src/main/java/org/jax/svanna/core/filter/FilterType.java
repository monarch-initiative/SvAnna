/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2020 Queen Mary University of London.
 * Copyright (c) 2012-2016 Charité Universitätsmedizin Berlin and Genome Research Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jax.svanna.core.filter;

import java.util.EnumSet;

/**
 * This is a simple class of enumerated constants that describe the type of
 * filtering that was applied to a Gene/Variant.
 * *
 *
 * @author Peter Robinson
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public enum FilterType {

    FAILED_VARIANT_FILTER("filter", "Failed previous VCF filters"),

    FREQUENCY_FILTER("freq", "Frequency"),

    COVERAGE_FILTER("coverage", "Failed required coverage depth filter");

    private final String vcfValue;
    private final String stringValue;

    FilterType(String vcfValue, String stringValue) {
        this.vcfValue = vcfValue;
        this.stringValue = stringValue;
    }

    public String vcfValue() {
        return vcfValue;
    }

    public String shortName() {
        return stringValue;
    }

    // SvAnna works with these filters
    public static EnumSet<FilterType> svannaFilterTypes() {
        return EnumSet.of(FAILED_VARIANT_FILTER, FREQUENCY_FILTER, COVERAGE_FILTER);
    }
}
