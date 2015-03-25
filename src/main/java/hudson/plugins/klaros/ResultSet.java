package hudson.plugins.klaros;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class ResultSet {
    
    private String spec;

    @DataBoundConstructor
    public ResultSet(String spec) {
        
        this.spec = StringUtils.trim(spec);
    }

    public String getSpec() {
        
        return spec;
    }

    
    public void setSpec(String spec) {
    
        this.spec = StringUtils.trim(spec);
    }
}