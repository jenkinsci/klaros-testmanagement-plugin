package hudson.plugins.klaros;

public class ResultFormat {

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