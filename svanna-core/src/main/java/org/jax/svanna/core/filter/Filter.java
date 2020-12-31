/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2017 Queen Mary University of London.
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

/**
 * @param <T>
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public interface Filter<T extends Filterable> {

    /**
     * @return an integer constant (as defined in exomizer.common.Constants)
     * that will act as a flag to generate the output HTML dynamically depending
     * on the filters that the user has chosen.
     */
    FilterType getFilterType();

    /**
     * True or false depending on whether the {@code VariantEvaluation} passes the runFilter or not.
     *
     * @param filterable filterable instance
     * @return true if the {@code VariantEvaluation} passes the runFilter.
     */
    FilterResult runFilter(T filterable);
}
