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

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The result set specification.
 */
public class ResultSet {

    private static final String DEFAULT_FORMAT = "junit";

    private String spec;
    private String format;

    public ResultSet() {

        format = DEFAULT_FORMAT;
    }

    public ResultSet(String spec) {

        this(spec, DEFAULT_FORMAT);
    }

    @DataBoundConstructor
    public ResultSet(String spec, String format) {

        this.spec = StringUtils.strip(spec);
        this.format = StringUtils.strip(format);
        if (StringUtils.isBlank(format)) {
            format = DEFAULT_FORMAT;
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
