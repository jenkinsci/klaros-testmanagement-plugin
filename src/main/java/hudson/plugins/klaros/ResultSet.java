/*
 * Copyright 2003 - 2015 verit Informationssysteme GmbH, Europaallee 10,
 * 67657 Kaiserslautern, Germany, http://www.verit.de.
 * 
 * All rights reserved.
 * 
 * This product or document is protected by copyright and distributed
 * under licenses restricting its use, copying, distribution, and
 * decompilation. No part of this product or documentation may be
 * reproduced in any form by any means without prior written authorization
 * of verit Informationssysteme GmbH and its licensors, if any.
 */
package hudson.plugins.klaros;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The result set specification.
 */
public final class ResultSet implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_FORMAT = "junit";

    private String spec;
    private String format;

    public ResultSet() {

        format = DEFAULT_FORMAT;
    }

    /**
     * Instantiates a new result set.
     *
     * @param spec the spec
     */
    public ResultSet(String spec) {

        this(spec, DEFAULT_FORMAT);
    }

    /**
     * Instantiates a new result set from a data bound constructor.
     *
     * @param spec the spec
     * @param format the format
     */
    @DataBoundConstructor
    public ResultSet(String spec, String format) {

        this.spec = StringUtils.strip(spec);
        this.format = StringUtils.strip(format);
        if (StringUtils.isBlank(format)) {
            this.format = DEFAULT_FORMAT;
        }
    }

    /**
     * Gets the spec.
     *
     * @return the spec
     */
    public String getSpec() {

        return spec;
    }

    /**
     * Sets the spec.
     *
     * @param spec the new spec
     */
    public void setSpec(String spec) {

        this.spec = StringUtils.trim(spec);
    }

    /**
     * Gets the format.
     *
     * @return the format
     */
    public String getFormat() {

        return format;
    }

    /**
     * Sets the format.
     *
     * @param format the new format
     */
    public void setFormat(String format) {

        this.format = format;
    }
}
