package selrach.bnetbuilder.model.distributions;

/**
 * This facilitates the communication between a distribution and its
 * corresponding variable
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class DistributionDescriptor {
	public DistributionDescriptor(String key, Double value, String info) {
		this.key = key;
		this.value = value;
		this.info = info;
	}

	private String key;
	private Double value;
	private String info;

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

}
