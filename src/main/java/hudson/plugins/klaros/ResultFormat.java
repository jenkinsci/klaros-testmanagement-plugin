/*
 * Copyright 2003 - 2016 verit Informationssysteme GmbH, Europaallee 10,
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

public class ResultFormat implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;

    /**
     * Instantiates a new result format.
     *
     * @param id the format id
     * @param name the format name
     * @param script the script to transform this format
     */
    ResultFormat(final String id, final String name) {

        this.id = id;
        this.name = name;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {

        return id;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {

        return name;
    }

}