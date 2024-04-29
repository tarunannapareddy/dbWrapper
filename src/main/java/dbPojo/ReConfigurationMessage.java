package dbPojo;

import lombok.Builder;

import java.io.Serializable;

@Builder
public class ReConfigurationMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public int[] ports;
}
